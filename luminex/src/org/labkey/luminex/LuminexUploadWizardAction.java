/*
 * Copyright (c) 2007-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.luminex;

import org.labkey.api.action.LabkeyError;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.DisplayColumnGroup;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.ValidationError;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.PreviouslyUploadedDataCollector;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.VBox;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: jeckels
* Date: Aug 8, 2007
*/
@RequiresPermissionClass(InsertPermission.class)
public class LuminexUploadWizardAction extends UploadWizardAction<LuminexRunUploadForm, LuminexAssayProvider>
{
    public LuminexUploadWizardAction()
    {
        super(LuminexRunUploadForm.class);
        addStepHandler(new AnalyteStepHandler());
    }

    protected void addRunActionButtons(LuminexRunUploadForm newRunForm, InsertView insertView, ButtonBar bbar)
    {
        Domain analyteDomain = AbstractAssayProvider.getDomainByPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        List<? extends DomainProperty> analyteColumns = analyteDomain.getProperties();
        if (analyteColumns.isEmpty())
        {
            super.addRunActionButtons(newRunForm, insertView, bbar);
        }
        else
        {
            addNextButton(bbar);
            addResetButton(newRunForm, insertView, bbar);
        }
    }

    private ModelAndView getAnalytesView(String[] analyteNames, final LuminexRunUploadForm form, final boolean errorReshow, BindException errors) throws ExperimentException
    {
        String lsidColumn = "RowId";
        VBox vbox = new VBox();

        if (form.getReRun() != null)
        {
            // In the case of a re-run, check if the old run has any exclusions. If so, warn the user that
            // they won't be carried over to the new run.
            long exclusionCount = new TableSelector(LuminexProtocolSchema.getTableInfoRunExclusion(), new SimpleFilter(FieldKey.fromParts("RunId"), form.getReRunId()), null).getRowCount();
            exclusionCount += new TableSelector(LuminexProtocolSchema.getTableInfoWellExclusion(), new SimpleFilter(FieldKey.fromParts("DataId", "RunId"), form.getReRunId()), null).getRowCount();

            if (exclusionCount > 0)
            {
                StringBuilder sb = new StringBuilder("<span class=\"labkey-error\">The run you are replacing has ");
                if (exclusionCount == 1)
                {
                    sb.append("one exclusion");
                }
                else
                {
                    sb.append(exclusionCount);
                    sb.append(" exclusions");
                }
                sb.append(", which will not be carried over to the replacement run. You may want to review the ");
                ActionURL url = new ActionURL(LuminexController.ExcludedDataAction.class, form.getContainer());
                url.addParameter("rowId", form.getRowId());
                sb.append(PageFlowUtil.textLink("exclusions report", url));
                sb.append("before proceeding.</span>");
                HtmlView warningView = new HtmlView(sb.toString());
                warningView.setTitle("Exclusion Warning");
                vbox.addView(warningView);
            }
        }

        // if there are titrations in the uploaded data, show the well role definition section
        if (form.getParser().getTitrations().size() > 0)
        {
            JspView<LuminexRunUploadForm> top = new JspView<>("/org/labkey/luminex/titrationWellRoles.jsp", form);
            top.setTitle("Define Well Roles");
            top.setTitlePopupHelp("Define Well Roles", "Samples that are titrated across different wells can used in different ways. Standards are used to calculate a titration curve against which unknowns are fit. QC Controls also define a curve and are used to compare runs against each other. The performance of single point controls may also be tracked over time. Choose the purpose(s) for each titration or single point control.");
            vbox.addView(top);
        }

        InsertView view = createInsertView(LuminexProtocolSchema.getTableInfoAnalytes(), lsidColumn, Collections.<DomainProperty>emptyList(), form.isResetDefaultValues(), AnalyteStepHandler.NAME, form, errors);
        view.setTitle("Analyte Properties");
        view.setTitlePopupHelp("Analyte Properties", "Each Luminex assay design defines a set of properties to track for analytes. Additionally, if multiple titrations are present in a given run, each analyte may be assigned to the appropriate set of titrations.");

        for (String analyte : analyteNames)
            view.getDataRegion().addHiddenFormField("analyteNames", analyte);

        Domain analyteDomain = AbstractAssayProvider.getDomainByPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        List<? extends DomainProperty> analyteColumns = analyteDomain.getProperties();

        // each analyte may have a different set of default values.  Because it may be expensive to query for the
        // entire set of values for every property, we use the following map to cache the default value sets by analyte name.
        Map<String, Map<DomainProperty, Object>> domains = new HashMap<>();
        for (DomainProperty analyteDP : analyteColumns)
        {
            List<DisplayColumn> cols = new ArrayList<>();
            for (String analyte : analyteNames)
            {
                // from SamplePropertyHelper:
                // get the map of default values that corresponds to our current sample:
                String defaultValueKey = analyte + "_" + analyteDP.getDomain().getName();
                Map<DomainProperty, Object> defaultValues = domains.get(defaultValueKey);
                if (defaultValues == null)
                {
                    try
                    {
                        defaultValues = form.getDefaultValues(analyteDP.getDomain(), analyte);
                    }
                    catch (ExperimentException e)
                    {
                        errors.addError(new LabkeyError(e));
                    }
                    domains.put(defaultValueKey,  defaultValues);
                }
                final String inputName = getAnalytePropertyName(analyte, analyteDP);
                Object analyteDefaultValue = defaultValues != null ? defaultValues.get(analyteDP) : null;

                if (analyteDP.isShownInInsertView())
                {
                    view.setInitialValue(inputName, analyteDefaultValue);

                    ColumnInfo info = analyteDP.getPropertyDescriptor().createColumnInfo(view.getDataRegion().getTable(), lsidColumn, getUser(), getContainer());
                    info.setName(inputName);
                    info.setDisplayColumnFactory(new DisplayColumnFactory()
                    {
                        @Override
                        public DisplayColumn createRenderer(ColumnInfo colInfo)
                        {
                            return new DataColumn(colInfo)
                            {
                                @Override
                                public String getFormFieldName(RenderContext ctx)
                                {
                                    return inputName;
                                }
                            };
                        }
                    });
                    cols.add(info.getRenderer());
                }
                // issue 19149: Analyte fields do not respect Shown in Insert View or default values
                else if (analyteDefaultValue != null)
                {
                    view.getDataRegion().addHiddenFormField(inputName, analyteDefaultValue.toString());
                }
            }

            if (cols.size() > 0)
                view.getDataRegion().addGroup(new DisplayColumnGroup(cols, analyteDP.getName(), true));
        }

        Map<String, String> defaultAnalyteColumnValues = form.getAnalyteColumnDefaultValues(_protocol);

        // add the Negative Bead column for each analyte
        List<DisplayColumn> negativeBeadCols = new ArrayList<>();
        for (String analyte : analyteNames)
        {
            ColumnInfo info = new ColumnInfo(LuminexProtocolSchema.getTableInfoAnalytes().getColumn(LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME), view.getDataRegion().getTable());
            String inputName = getAnalytePropertyName(analyte, LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME);
            info.setName(inputName);
            info.setDisplayColumnFactory(createAnalytePropertyDisplayColumnFactory(inputName, LuminexDataHandler.NEGATIVE_BEAD_DISPLAY_NAME));
            view.setInitialValue(inputName, defaultAnalyteColumnValues.get(inputName));
            DisplayColumn col = info.getRenderer();
            negativeBeadCols.add(col);
        }
        view.getDataRegion().addGroup(new DisplayColumnGroup(negativeBeadCols, LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME, true));

        // add the Positivity Threshold column for each analyte if there was a run property indicating that Positivity should be calculated
        String calcPositivityValue = form.getRequest().getParameter(LuminexDataHandler.CALCULATE_POSITIVITY_COLUMN_NAME);
        if (null != calcPositivityValue && calcPositivityValue.equals("1"))
        {
            List<DisplayColumn> posThresholdCols = new ArrayList<>();
            for (String analyte : analyteNames)
            {
                ColumnInfo info = new ColumnInfo(LuminexProtocolSchema.getTableInfoAnalytes().getColumn(LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME), view.getDataRegion().getTable());
                final String inputName = getAnalytePropertyName(analyte, LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME);
                info.setName(inputName);
                info.setDisplayColumnFactory(createAnalytePropertyDisplayColumnFactory(inputName, LuminexDataHandler.POSITIVITY_THRESHOLD_DISPLAY_NAME));
                // use a default value of 100 if there is no last entry value to populate the initial value
                view.setInitialValue(inputName, defaultAnalyteColumnValues.get(inputName) != null ? defaultAnalyteColumnValues.get(inputName) : 100);
                DisplayColumn col = info.getRenderer();
                posThresholdCols.add(col);
            }
            view.getDataRegion().addGroup(new DisplayColumnGroup(posThresholdCols, LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME, false));
        }

        if (errorReshow)
            view.setInitialValues(getViewContext().getRequest().getParameterMap());

        view.getDataRegion().setHorizontalGroups(false);
        view.getDataRegion().setGroupHeadings(Arrays.asList(analyteNames));

        addHiddenBatchProperties(form, view);
        addHiddenRunProperties(form, view);

        view.getDataRegion().addHiddenFormField("name", form.getName());
        view.getDataRegion().addHiddenFormField("comments", form.getComments());

        ParticipantVisitResolverType resolverType = getSelectedParticipantVisitResolverType(form.getProvider(), form);
        resolverType.addHiddenFormFields(form, view);

        PreviouslyUploadedDataCollector collector = new PreviouslyUploadedDataCollector(form.getUploadedData(), PreviouslyUploadedDataCollector.Type.PassThrough);
        collector.addHiddenFormFields(view, form);

        Map<String, String> defaultWellRoleValues = PropertyManager.getProperties(getUser(),
            getContainer(), _protocol.getName() + ": Well Role");

        final Map<String, Titration> existingTitrations = getExistingTitrations(form.getReRun());
        final Map<String, Analyte> existingAnalytes = getExistingAnalytes(form.getReRun());
        final Set<String> existingSinglePointControls = getExistingSinglePointControls(form.getReRun());

        // get a set of which titrations are going to be pre-selected as standards (based on default value, well type, etc.)
        final HashSet<Titration> standardTitrations = new HashSet<>();
        for (Map.Entry<String, Titration> titrationEntry : form.getParser().getTitrationsWithTypes().entrySet())
        {
            // titrations of type unknown are not available as standards
            if (titrationEntry.getValue().isUnknown())
            {
                continue;
            }

            Titration existingTitration = existingTitrations.get(titrationEntry.getKey());
            String propertyName = getTitrationTypeCheckboxName(Titration.Type.standard, titrationEntry.getValue());
            // If we have an existing titration as a baseline from the run we're replacing, use its value
            String defVal = existingTitration == null ? defaultWellRoleValues.get(propertyName) : Boolean.toString(existingTitration.isStandard());

            // add the titration to the list of standards if reshowing on error and it was selected
            if (errorReshow)
            {
                if (getViewContext().getRequest().getParameter(propertyName) != null)
                    standardTitrations.add(titrationEntry.getValue());
            }
            // add the titration if there is a default value and it was selected as a standard
            else if (defVal != null)
            {
                if (defVal.toLowerCase().equals("true"))
                    standardTitrations.add(titrationEntry.getValue());
            }
            // otherwise add the titration if the well role is of type standard
            else if (titrationEntry.getValue().isStandard())
            {
                standardTitrations.add(titrationEntry.getValue());
            }
        }


        // add hidden form fields (3 types) for the titration well role definition section (controlled by titrationWellRoles.jsp after render)
        for (final Map.Entry<String, Titration> titrationEntry : form.getParser().getTitrationsWithTypes().entrySet())
        {
            String propertyName;
            String defVal;
            String value;

            Titration existingTitration = existingTitrations.get(titrationEntry.getKey());

            if (!titrationEntry.getValue().isUnknown())
            {
                propertyName = getTitrationTypeCheckboxName(Titration.Type.standard, titrationEntry.getValue());
                // If we have an existing titration as a baseline from the run we're replacing, use its value
                defVal = existingTitration == null ? defaultWellRoleValues.get(propertyName) : Boolean.toString(existingTitration.isStandard());
                value = setInitialTitrationInput(errorReshow, propertyName, defVal, titrationEntry.getValue().isStandard()) ? "true" : "";
                view.getDataRegion().addHiddenFormField(propertyName, value);

                // field to help w/ when to store default values
                value = "";
                if (errorReshow && getViewContext().getRequest().getParameter(propertyName + "_showcol").equals("true"))
                    value = "true";
                else if (!errorReshow && standardTitrations.size() > 1 && standardTitrations.contains(titrationEntry.getValue()))
                    value = "true";
                view.getDataRegion().addHiddenFormField(propertyName + "_showcol", value);

                propertyName = getTitrationTypeCheckboxName(Titration.Type.qccontrol, titrationEntry.getValue());
                // If we have an existing titration as a baseline from the run we're replacing, use its value
                defVal = existingTitration == null ? defaultWellRoleValues.get(propertyName) : Boolean.toString(existingTitration.isQcControl());
                value = setInitialTitrationInput(errorReshow, propertyName, defVal, titrationEntry.getValue().isQcControl()) ? "true" : "";
                view.getDataRegion().addHiddenFormField(propertyName, value);
            }
            else
            {
                propertyName = getTitrationTypeCheckboxName(Titration.Type.unknown, titrationEntry.getValue());
                // If we have an existing titration as a baseline from the run we're replacing, use its value
                defVal = existingTitration == null ? defaultWellRoleValues.get(propertyName) : Boolean.toString(existingTitration.isUnknown());
                value = setInitialTitrationInput(errorReshow, propertyName, defVal, titrationEntry.getValue().isUnknown()) ? "true" : "";
                view.getDataRegion().addHiddenFormField(propertyName, value);
            }
        }

        // add hidden form fields for the single point control section (controlled by titrationWellRoles.jsp after render)
        for (final String singlePointControl : form.getParser().getSinglePointControls())
        {
            String propertyName;
            String defVal;
            String value;

            boolean existingSinglePointControl = existingSinglePointControls.contains(singlePointControl);

            propertyName = getSinglePointControlCheckboxName(singlePointControl);
            // If we have an existing singlePointControl as a baseline from the run we're replacing, use its value
            defVal = existingSinglePointControl ? "true" : defaultWellRoleValues.get(propertyName);
            value = setInitialSinglePointControlInput(errorReshow, propertyName, defVal) ? "true" : "";
            view.getDataRegion().addHiddenFormField(propertyName, value);
        }

        // add a column to the analyte properties section for each of the titrations that might be used for a Standard
        for (final Map.Entry<String, Titration> titrationEntry : form.getParser().getTitrationsWithTypes().entrySet())
        {
            // skip over those titrations that are of type Unknown as they will not be used as standards
            if (titrationEntry.getValue().isUnknown())
            {
                continue;
            }

            final Map<String, String> defaultTitrationValues = PropertyManager.getProperties(getUser(),
                    getContainer(), _protocol.getName() + ": " + titrationEntry.getValue().getName());

            String titrationCheckboxName = getTitrationTypeCheckboxName(Titration.Type.standard, titrationEntry.getValue());
            final String titrationCellName = PageFlowUtil.filter(getTitrationColumnCellName(titrationEntry.getValue().getName()));
            final boolean hideCell;
            if (errorReshow && getViewContext().getRequest().getParameter(titrationCheckboxName + "_showcol").equals("true"))
                hideCell = false;
            else if (!errorReshow && standardTitrations.contains(titrationEntry.getValue()))
                hideCell = false;
            else
                hideCell = true;

            final List<DisplayColumn> cols = new ArrayList<>();
            for (final String analyte : analyteNames)
            {
                final DisplayColumn col = new SimpleDisplayColumn()
                {
                    @Override
                    public void renderInputHtml(RenderContext ctx, Writer out, Object value) throws IOException
                    {
                        String titrationName = titrationEntry.getValue().getName();
                        String propertyName = PageFlowUtil.filter(getTitrationCheckboxName(titrationName, analyte));
                        String defVal = defaultTitrationValues.get(propertyName);
                        // If we're replacing this run, and the previous version of the run had the analyte/titration
                        // combination, see if it they were used together
                        if (form.getReRun() != null && existingTitrations.containsKey(titrationName) && existingAnalytes.containsKey(analyte))
                        {
                            SQLFragment selectedSQL = new SQLFragment("SELECT at.* FROM ");
                            selectedSQL.append(LuminexProtocolSchema.getTableInfoAnalyteTitration(), "at");
                            selectedSQL.append(", ");
                            selectedSQL.append(LuminexProtocolSchema.getTableInfoTitration(), "t");
                            selectedSQL.append(", ");
                            selectedSQL.append(LuminexProtocolSchema.getTableInfoAnalytes(), "a");
                            selectedSQL.append(" WHERE LOWER(a.Name) = LOWER(?) AND a.RowId = at.AnalyteId AND ");
                            selectedSQL.add(analyte);
                            selectedSQL.append("t.RowId = at.TitrationId AND t.RunId = ? AND LOWER(t.Name) = LOWER(?)");
                            selectedSQL.add(form.getReRun().getRowId());
                            selectedSQL.add(titrationName);

                            if (new SqlSelector(LuminexProtocolSchema.getSchema(), selectedSQL).exists())
                            {
                                defVal = Boolean.TRUE.toString();
                            }

                        }
                        String checked = "";

                        // if reshowing form on error, preselect based on request value
                        if (errorReshow)
                        {
                            if (getViewContext().getRequest().getParameter(propertyName) != null)
                                checked = "CHECKED";
                        }
                        // if there is only one standard, then preselect the checkbox
                        else if (standardTitrations.size() == 1 && standardTitrations.contains(titrationEntry.getValue()))
                        {
                            checked = "CHECKED";
                        }
                        else if (standardTitrations.size() > 1)
                        {
                            // if > 1 standard and default value exists, set checkbox based on default value
                            if (defVal != null && defVal.toLowerCase().equals("true"))
                                checked = "CHECKED";
                            // if no default value and titration is standard, then preselect the checkbox
                            else if (defVal == null && standardTitrations.contains(titrationEntry.getValue()))
                                checked = "CHECKED";
                        }

                        out.write("<input type='checkbox' value='" + value + "' name='" + propertyName + "' " + checked + " />");
                    }

                    @Override
                    public void renderInputCell(RenderContext ctx, Writer out, int span) throws IOException
                    {
                        out.write("<td colspan=" + span + " name='" + titrationCellName
                                + "' style='display:" + (hideCell ? "none" : "table-cell") + "' >");
                        renderInputHtml(ctx, out, 1);
                        out.write("</td>");
                    }

                    @Override
                    public void renderDetailsCaptionCell(RenderContext ctx, Writer out) throws IOException
                    {
                        out.write("<td name='" + titrationCellName + "' "
                            + " class='labkey-form-label' style='display:" + (hideCell ? "none" : "table-cell") + "' >");
                        renderTitle(ctx, out);
                        out.write("</td>");
                    }

                    @Override
                    public String getFormFieldName(RenderContext ctx)
                    {
                        return PageFlowUtil.filter(getTitrationCheckboxName(titrationEntry.getValue().getName(), analyte));
                    }
                };

                col.setCaption("Use " + titrationEntry.getKey() + " Standard");
                cols.add(col);
            }
            view.getDataRegion().addGroup(new DisplayColumnGroup(cols, titrationEntry.getKey(), true)
            {
                @Override
                public void writeSameCheckboxCell(RenderContext ctx, Writer out) throws IOException
                {
                    String groupName = ColumnInfo.propNameFromName(getColumns().get(0).getFormFieldName(ctx));
                    out.write("<td name='" + titrationCellName + "' style='display:" + (hideCell ? "none" : "table-cell") + "' >");
                    out.write("<input type=checkbox name='" + groupName + "CheckBox' id='" + groupName + "CheckBox' onchange=\"");
                    out.write(" b = this.checked;");
                    for (int i = 1; i < getColumns().size(); i++)
                    {
                        DisplayColumn col = getColumns().get(i);
                        out.write("document.getElementsByName('" + col.getFormFieldName(ctx) + "')[0].style.display = b ? 'none' : 'block';\n");
                    }
                    out.write(" if (b) { " + groupName + "Updated(); }\">");
                    out.write("</td>");
                }

                @Override
                public void writeCopyableJavaScript(RenderContext ctx, Writer out) throws IOException
                {
                    String groupName = ColumnInfo.propNameFromName(getColumns().get(0).getFormFieldName(ctx));
                    out.write("function " + groupName + "Updated() {\n");
                    out.write("  if (document.getElementById('" + groupName + "CheckBox') != null && document.getElementById('" + groupName + "CheckBox').checked) {\n");
                    out.write("    var v = document.getElementsByName('" + getColumns().get(0).getFormFieldName(ctx) + "')[0].checked;\n");
                    for (int i = 1; i < getColumns().size(); i++)
                    {
                        out.write("    document.getElementsByName('" + getColumns().get(i).getFormFieldName(ctx) + "')[0].checked = v;\n");
                    }
                    out.write("  }\n");
                    out.write("}\n");
                    out.write("var e = document.getElementsByName('" + getColumns().get(0).getFormFieldName(ctx) + "')[0];\n");
                    out.write("e.onchange=" + groupName + "Updated;\n");
                    out.write("e.onkeyup=" + groupName + "Updated;\n");
                    out.write("\n");
                }
            });
        }

        ButtonBar bbar = new ButtonBar();
        addFinishButtons(form, view, bbar);
        addResetButton(form, view, bbar);

        ActionButton cancelButton = new ActionButton("Cancel", getSummaryLink(_protocol));
        bbar.add(cancelButton);

        _stepDescription = "Analyte Properties";

        view.getDataRegion().setButtonBar(bbar, DataRegion.MODE_INSERT);

        vbox.addView(view);

        return vbox;
    }

    private DisplayColumnFactory createAnalytePropertyDisplayColumnFactory(final String inputName, final String displayName)
    {
        return new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new DataColumn(colInfo)
                {
                    @Override
                    public String getFormFieldName(RenderContext ctx)
                    {
                        return inputName;
                    }

                    @Override
                    public void renderTitle(RenderContext ctx, Writer out) throws IOException
                    {
                        out.write(displayName);
                    }

                    @Override
                    public void renderDetailsCaptionCell(RenderContext ctx, Writer out) throws IOException
                    {
                        out.write("<td class='labkey-form-label'>");
                        renderTitle(ctx, out);
                        StringBuilder sb = new StringBuilder();
                        sb.append("Type: ").append(getBoundColumn().getFriendlyTypeName()).append("\n");
                        out.write(PageFlowUtil.helpPopup(displayName, sb.toString()));
                        out.write("</td>");
                    }
                };
            }
        };
    }

    private Map<String, Analyte> getExistingAnalytes(ExpRun reRun)
    {
        if (reRun == null)
        {
            return Collections.emptyMap();
        }

        Map<String, Analyte> result = new CaseInsensitiveHashMap<>();
        SQLFragment analyteSQL = new SQLFragment("SELECT a.* FROM ");
        analyteSQL.append(LuminexProtocolSchema.getTableInfoAnalytes(), "a");
        analyteSQL.append(" WHERE a.DataId IN (SELECT d.RowId FROM ");
        analyteSQL.append(ExperimentService.get().getTinfoData(), "d");
        analyteSQL.append(" WHERE d.RunId = ?)");
        analyteSQL.add(reRun.getRowId());
        for (Analyte analyte : new SqlSelector(LuminexProtocolSchema.getSchema(), analyteSQL).getArrayList(Analyte.class))
        {
            result.put(analyte.getName(), analyte);
        }
        return result;
    }

    private Map<String, Titration> getExistingTitrations(ExpRun reRun)
    {
        if (reRun == null)
        {
            return Collections.emptyMap();
        }

        Map<String, Titration> result = new CaseInsensitiveHashMap<>();
        SQLFragment titrationSQL = new SQLFragment("SELECT t.* FROM ");
        titrationSQL.append(LuminexProtocolSchema.getTableInfoTitration(), "t");
        titrationSQL.append(" WHERE t.RunId = ?");
        titrationSQL.add(reRun.getRowId());
        for (Titration titration : new SqlSelector(LuminexProtocolSchema.getSchema(), titrationSQL).getArrayList(Titration.class))
        {
            result.put(titration.getName(), titration);
        }
        return result;
    }

    private Set<String> getExistingSinglePointControls(ExpRun reRun)
    {
        if (reRun == null)
        {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        SQLFragment titrationSQL = new SQLFragment("SELECT t.* FROM ");
        titrationSQL.append(LuminexProtocolSchema.getTableInfoSinglePointControl(), "t");
        titrationSQL.append(" WHERE t.RunId = ?");
        titrationSQL.add(reRun.getRowId());
        for (SinglePointControl singlePointControl : new SqlSelector(LuminexProtocolSchema.getSchema(), titrationSQL).getArrayList(SinglePointControl.class))
        {
            result.add(singlePointControl.getName());
        }
        return result;
    }

    public static String getTitrationCheckboxName(String titration, String analyte)
    {
        return "titration_" + analyte + "_" + titration;
    }

    public static String getAnalytePropertyName(String analyte, DomainProperty dp)
    {
        return getAnalytePropertyName(analyte, dp.getName());
    }

    public static String getAnalytePropertyName(String analyte, String property)
    {
        return "_analyte_" + analyte + "_" + property;
    }

    private boolean setInitialTitrationInput(boolean errorReshow, String propName, String defVal, boolean typeMatch)
    {
        // return true if 1. errorReshow and previously checked, 2. has a default value that was checked, or 3. titration type matches
        return (errorReshow && getViewContext().getRequest().getParameter(propName).equals("true"))
                || (!errorReshow && defVal != null && defVal.toLowerCase().equals("true"))
                || (!errorReshow && defVal == null && typeMatch);
    }

    private boolean setInitialSinglePointControlInput(boolean errorReshow, String propName, String defVal)
    {
        // return true if 1. errorReshow and previously checked, 2. has a default value that was checked
        return (errorReshow && getViewContext().getRequest().getParameter(propName).equals("true"))
                || (!errorReshow && defVal != null && defVal.toLowerCase().equals("true"));
    }

    private String[] getAnalyteNames(LuminexRunUploadForm form) throws ExperimentException
    {
        List<String> names = new ArrayList<>();

        for (Analyte analyte : form.getParser().getSheets().keySet())
        {
            names.add(analyte.getName());
        }
        return names.toArray(new String[names.size()]);
    }

    protected void addSampleInputColumns(LuminexRunUploadForm form, InsertView insertView)
    {
        // Don't add any columns - they're part of the uploaded spreadsheet
    }

    @Override
    protected RunStepHandler getRunStepHandler()
    {
        return new LuminexRunStepHandler();
    }

    public static String getTitrationTypeCheckboxName(Titration.Type type, Titration titration)
    {
        return "_titrationRole_" + type + "_" + titration.getName();
    }

    public static String getSinglePointControlCheckboxName(String singlePointControl)
    {
        return "_singlePointControl_" + singlePointControl;
    }

    public static String getTitrationColumnCellName(String titrationName)
    {
        return "_titrationcell_" + titrationName;
    }

    protected class LuminexRunStepHandler extends RunStepHandler
    {
        @Override
        protected ModelAndView handleSuccessfulPost(LuminexRunUploadForm form, BindException errors) throws ExperimentException
        {
            try {
                String[] analyteNames = getAnalyteNames(form);

                if (analyteNames.length == 0)
                    return super.handleSuccessfulPost(form, errors);
                else
                    return getAnalytesView(analyteNames, form, false, errors);
            }
            catch (ExperimentException e)
            {
                errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
                return getRunPropertiesView(form, true, false, errors);
            }
        }
    }

    public class AnalyteStepHandler extends RunStepHandler
    {
        public static final String NAME = "ANALYTE";

        @Override
        public ModelAndView handleStep(LuminexRunUploadForm form, BindException errors) throws ServletException, SQLException, ExperimentException
        {
            if (!form.isResetDefaultValues())
            {
                for (String analyte : form.getAnalyteNames())
                {
                    // validate analyte domain properties
                    Map<DomainProperty, String> properties = form.getAnalyteProperties(analyte);
                    validatePostedProperties(properties, errors);

                    // validate analyte column properties
                    Map<ColumnInfo, String> colProperties = form.getAnalyteColumnProperties(analyte);
                    validateColumnProperties(colProperties, errors);
                }
            }

            if (getCompletedUploadAttemptIDs().contains(form.getUploadAttemptID()))
            {
                throw new RedirectException(PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(getContainer(), _protocol, LuminexUploadWizardAction.class));
            }

            if (!form.isResetDefaultValues() && errors.getErrorCount() == 0)
            {
                try (DbScope.Transaction transaction = LuminexProtocolSchema.getSchema().getScope().ensureTransaction())
                {
                    ExpRun run = saveExperimentRun(form);

                    // Save default values for analytes
                    PropertyManager.PropertyMap defaultAnalyteColumnValues = PropertyManager.getWritableProperties(
                            getUser(), getContainer(), form.getAnatlyeColumnCategory(_protocol), true);
                    for (String analyteName : form.getAnalyteNames())
                    {
                        // for analyte domain properties use the standard assay default value persistance
                        Map<DomainProperty, String> properties = form.getAnalyteProperties(analyteName);
                        form.saveDefaultValues(properties, analyteName);

                        // for analyte column properties use the PropertyManager (similar to how well role and titration defaults values are persisted)
                        for (Map.Entry<ColumnInfo, String> colPropEntry : form.getAnalyteColumnProperties(analyteName).entrySet())
                        {
                            // issue 20549: default values reset incorrectly when PositivityThreshold column is hidden
                            boolean includeDefaultAnalyteColumnValue = hasAnalytePropertyInRequestParams(form, analyteName, colPropEntry.getKey());

                            // These need to be repopulated just like the rest of the analyte domain properties,
                            // but they aren't actually part of the domain- they're hard columns on the luminex.Analyte table
                            String inputName = getAnalytePropertyName(analyteName, colPropEntry.getKey().getName());
                            if (includeDefaultAnalyteColumnValue)
                                defaultAnalyteColumnValues.put(inputName, colPropEntry.getValue());
                        }
                    }
                    PropertyManager.saveProperties(defaultAnalyteColumnValues);

                    // save the defalut values for the analyte standards/titrations information in 2 categories: well roles and titrations
                    PropertyManager.PropertyMap defaultWellRoleValues = PropertyManager.getWritableProperties(
                            getUser(), getContainer(), _protocol.getName() + ": Well Role", true);

                    for (final Map.Entry<String, Titration> titrationEntry : form.getParser().getTitrationsWithTypes().entrySet())
                    {
                        String propertyName;
                        Boolean value;

                        // add the name/value pairs for the titration well role definition section
                        if (!titrationEntry.getValue().isUnknown())
                        {
                            propertyName = getTitrationTypeCheckboxName(Titration.Type.standard, titrationEntry.getValue());
                            value = getViewContext().getRequest().getParameter(propertyName).equals("true");
                            defaultWellRoleValues.put(propertyName, Boolean.toString(value));

                            propertyName = getTitrationTypeCheckboxName(Titration.Type.qccontrol, titrationEntry.getValue());
                            value = getViewContext().getRequest().getParameter(propertyName).equals("true");
                            defaultWellRoleValues.put(propertyName, Boolean.toString(value));
                        }
                        else
                        {
                            propertyName = getTitrationTypeCheckboxName(Titration.Type.unknown, titrationEntry.getValue());
                            value = getViewContext().getRequest().getParameter(propertyName).equals("true");
                            defaultWellRoleValues.put(propertyName, Boolean.toString(value));
                        }

                        // add the name/value pairs for each of the analyte standards if the columns was shown in the UI
                        propertyName = getTitrationTypeCheckboxName(Titration.Type.standard, titrationEntry.getValue()) + "_showcol";
                        if (!titrationEntry.getValue().isUnknown() && getViewContext().getRequest().getParameter(propertyName).equals("true"))
                        {
                            PropertyManager.PropertyMap defaultTitrationValues = PropertyManager.getWritableProperties(
                                    getUser(), getContainer(),
                                    _protocol.getName() + ": " + titrationEntry.getValue().getName(), true);
                            for (String analyteName : form.getAnalyteNames())
                            {
                                propertyName = getTitrationCheckboxName(titrationEntry.getValue().getName(), analyteName);
                                value = getViewContext().getRequest().getParameter(propertyName) != null;
                                defaultTitrationValues.put(propertyName, Boolean.toString(value));
                            }
                            PropertyManager.saveProperties(defaultTitrationValues);
                        }
                    }
                    // save default values for SinglePointControls
                    for (String singlePointControl : form.getParser().getSinglePointControls())
                    {
                        // add the name/value pairs for the singlePointControl well role definition section
                        String propertyName = getSinglePointControlCheckboxName(singlePointControl);
                        Boolean value = getViewContext().getRequest().getParameter(propertyName).equals("true");
                        defaultWellRoleValues.put(propertyName, Boolean.toString(value));
                    }

                    PropertyManager.saveProperties(defaultWellRoleValues);

                    transaction.commit();
                    getCompletedUploadAttemptIDs().add(form.getUploadAttemptID());
                    form.resetUploadAttemptID();
                    return afterRunCreation(form, run, errors);
                }
                catch (ValidationException ve)
                {
                    for (ValidationError error : ve.getErrors())
                        errors.addError(new LabkeyError(error.getMessage()));
                }
                catch (ExperimentException e)
                {
                    errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
                }
            }
            else
            {
                return getAnalytesView(form.getAnalyteNames(), form, true, errors);
            }

            return getAnalytesView(form.getAnalyteNames(), form, true, errors);
        }

        public String getName()
        {
            return NAME;
        }
    }

    private boolean hasAnalytePropertyInRequestParams(LuminexRunUploadForm form, String analyteName, ColumnInfo col)
    {
        return form.getRequest().getParameterMap().containsKey(LuminexUploadWizardAction.getAnalytePropertyName(analyteName, col.getName()));
    }

    @Override
    protected boolean shouldShowDataCollectorUI(LuminexRunUploadForm newRunForm)
    {
        // Always expect input files, regardless of whether we're configured to import spot-level data
        return true;
    }
}
