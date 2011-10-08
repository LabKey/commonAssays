/*
 * Copyright (c) 2007-2011 LabKey Corporation
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
import org.labkey.api.data.*;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.ValidationError;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.*;
import org.labkey.api.util.PageFlowUtil;
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
import java.util.*;

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
        DomainProperty[] analyteColumns = analyteDomain.getProperties();
        if (analyteColumns.length == 0)
        {
            super.addRunActionButtons(newRunForm, insertView, bbar);
        }
        else
        {
            addNextButton(bbar);
            addResetButton(newRunForm, insertView, bbar);
        }
    }

    private ModelAndView getAnalytesView(String[] analyteNames, LuminexRunUploadForm form, final boolean errorReshow, BindException errors) throws ServletException
    {
        try {
            String lsidColumn = "RowId";
            VBox vbox = new VBox();

            // if there are titrations in the uploaded data, show the well role definition section
            if (form.getParser().getTitrations().size() > 0)
            {
                JspView<LuminexRunUploadForm> top = new JspView<LuminexRunUploadForm>("/org/labkey/luminex/titrationWellRoles.jsp", form);
                top.setTitle("Define Titration Well Roles");
                vbox.addView(top);
            }

            InsertView view = createInsertView(LuminexSchema.getTableInfoAnalytes(), lsidColumn, new DomainProperty[0], form.isResetDefaultValues(), AnalyteStepHandler.NAME, form, errors);
            view.setTitle("Analyte Properties");

            for (String analyte : analyteNames)
                view.getDataRegion().addHiddenFormField("analyteNames", analyte);

            Domain analyteDomain = AbstractAssayProvider.getDomainByPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
            DomainProperty[] analyteColumns = analyteDomain.getProperties();

            // each analyte may have a different set of default values.  Because it may be expensive to query for the
            // entire set of values for every property, we use the following map to cache the default value sets by analyte name.
            Map<String, Map<DomainProperty, Object>> domains = new HashMap<String, Map<DomainProperty, Object>>();
            for (DomainProperty analyteDP : analyteColumns)
            {
                List<DisplayColumn> cols = new ArrayList<DisplayColumn>();
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
                    if (defaultValues != null)
                        view.setInitialValue(inputName, defaultValues.get(analyteDP));

                    ColumnInfo info = analyteDP.getPropertyDescriptor().createColumnInfo(view.getDataRegion().getTable(), lsidColumn, getViewContext().getUser());
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
                view.getDataRegion().addGroup(new DisplayColumnGroup(cols, analyteDP.getName(), true));
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

            PreviouslyUploadedDataCollector collector = new PreviouslyUploadedDataCollector(form.getUploadedData());
            collector.addHiddenFormFields(view, form);

            Map<String, String> defaultWellRoleValues = PropertyManager.getProperties(getViewContext().getUser().getUserId(),
                getContainer().getId(), _protocol.getName() + ": Well Role");
            
            // get a set of which titrations are going to be pre-selected as standards (based on default value, well type, etc.)
            final HashSet<Titration> standardTitrations = new HashSet<Titration>();
            for (Map.Entry<String, Titration> titrationEntry : form.getParser().getTitrationsWithTypes().entrySet())
            {
                // titrations of type unknown are not availble as standards
                if (titrationEntry.getValue().isUnknown())
                {
                    continue;
                }

                String propertyName = getTitrationTypeCheckboxName(Titration.Type.standard, titrationEntry.getValue());
                String defVal = defaultWellRoleValues.get(propertyName);

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

                if (!titrationEntry.getValue().isUnknown())
                {
                    propertyName = getTitrationTypeCheckboxName(Titration.Type.standard, titrationEntry.getValue());
                    value = standardTitrations.contains(titrationEntry.getValue()) ? "true" : "";
                    view.getDataRegion().addHiddenFormField(propertyName, value);

                    // field to help w/ when to store default values
                    value = "";
                    if (errorReshow && getViewContext().getRequest().getParameter(propertyName + "_showcol").equals("true"))
                        value = "true";
                    else if (!errorReshow && standardTitrations.size() > 1 && standardTitrations.contains(titrationEntry.getValue()))
                        value = "true";
                    view.getDataRegion().addHiddenFormField(propertyName + "_showcol", value);

                    propertyName = getTitrationTypeCheckboxName(Titration.Type.qccontrol, titrationEntry.getValue());
                    defVal = defaultWellRoleValues.get(propertyName);
                    value = setInitialTitrationInput(errorReshow, propertyName, defVal, titrationEntry.getValue().isQcControl()) ? "true" : "";
                    view.getDataRegion().addHiddenFormField(propertyName, value);
                }
                else
                {
                    propertyName = getTitrationTypeCheckboxName(Titration.Type.unknown, titrationEntry.getValue());
                    defVal = defaultWellRoleValues.get(propertyName);
                    value = setInitialTitrationInput(errorReshow, propertyName, defVal, titrationEntry.getValue().isUnknown()) ? "true" : "";
                    view.getDataRegion().addHiddenFormField(propertyName, value);
                }
            }

            // add a column to the analyte properties section for each of the titrations that might be used for a Standard
            for (final Map.Entry<String, Titration> titrationEntry : form.getParser().getTitrationsWithTypes().entrySet())
            {
                // skip over those titrations that are of type Unknown as they will not be used as standards
                if (titrationEntry.getValue().isUnknown())
                {
                    continue;
                }

                final Map<String, String> defaultTitrationValues = PropertyManager.getProperties(getViewContext().getUser().getUserId(),
                        getContainer().getId(), _protocol.getName() + ": " + titrationEntry.getValue().getName());

                List<DisplayColumn> cols = new ArrayList<DisplayColumn>();
                for (final String analyte : analyteNames)
                {
                    DisplayColumn col = new SimpleDisplayColumn()
                    {
                        @Override
                        public void renderInputHtml(RenderContext ctx, Writer out, Object value) throws IOException
                        {
                            String propertyName = PageFlowUtil.filter(getTitrationCheckboxName(titrationEntry.getValue().getName(), analyte));
                            String defVal = defaultTitrationValues.get(propertyName);
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
                            String propertyName = getTitrationTypeCheckboxName(Titration.Type.standard, titrationEntry.getValue());
                            boolean hideCell = true;
                            if (errorReshow && getViewContext().getRequest().getParameter(propertyName + "_showcol").equals("true"))
                                hideCell = false;
                            else if (!errorReshow && standardTitrations.size() > 1 && standardTitrations.contains(titrationEntry.getValue()))
                                hideCell = false;
                            out.write("<td colspan=" + span + " name='" + PageFlowUtil.filter(getTitrationColumnCellName(titrationEntry.getValue().getName()))
                                    + "' style='display:" + (hideCell ? "none" : "table-cell") + "' >");
                            renderInputHtml(ctx, out, 1);
                            out.write("</td>");
                        }

                        @Override
                        public void renderDetailsCaptionCell(RenderContext ctx, Writer out) throws IOException
                        {
                            String propertyName = getTitrationTypeCheckboxName(Titration.Type.standard, titrationEntry.getValue());
                            boolean hideCell = true;
                            if (errorReshow && getViewContext().getRequest().getParameter(propertyName + "_showcol").equals("true"))
                                hideCell = false;
                            else if (!errorReshow && standardTitrations.size() > 1 && standardTitrations.contains(titrationEntry.getValue()))
                                hideCell = false;
                            out.write("<td name='" + PageFlowUtil.filter(getTitrationColumnCellName(titrationEntry.getValue().getName())) + "' "
                                + " class='labkey-form-label' style='display:" + (hideCell ? "none" : "table-cell") + "' >");
                            renderTitle(ctx, out);
                            out.write("</td>");
                        }
                    };

                    col.setCaption("Use " + titrationEntry.getKey() + " Standard");
                    cols.add(col);
                }
                view.getDataRegion().addGroup(new DisplayColumnGroup(cols, titrationEntry.getKey(), false));
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
        catch (ExperimentException e)
        {
            throw new ServletException(e);
        }
    }

    public static String getTitrationCheckboxName(String titration, String analyte)
    {
        return "titration_" + analyte + "_" + titration;
    }

    public static String getAnalytePropertyName(String analyte, DomainProperty dp)
    {
        return "_analyte_" + analyte + "_" + dp.getName();
    }

    private boolean setInitialTitrationInput(boolean errorReshow, String propName, String defVal, boolean typeMatch)
    {
        // return true if 1. errorReshow and previously checked, 2. has a default value that was checked, or 3. titration type matches
        return (errorReshow && getViewContext().getRequest().getParameter(propName).equals("true"))
                || (!errorReshow && defVal != null && defVal.toLowerCase().equals("true"))
                || (!errorReshow && defVal == null && typeMatch);
    }    

    private String[] getAnalyteNames(LuminexRunUploadForm form) throws ExperimentException
    {
        List<String> names = new ArrayList<String>();

        for (Analyte analyte : form.getParser().getSheets().keySet())
        {
            names.add(analyte.getName());
        }
        return names.toArray(new String[names.size()]);
    }

    private Analyte[] getAnalytes(int dataRowId)
    {
        try
        {
            return Table.select(LuminexSchema.getTableInfoAnalytes(), Table.ALL_COLUMNS, new SimpleFilter("DataId", dataRowId), new Sort("RowId"), Analyte.class);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
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

    public static String getTitrationColumnCellName(String titrationName)
    {
        return "_titrationcell_" + titrationName;
    }

    protected class LuminexRunStepHandler extends RunStepHandler
    {
        @Override
        protected ModelAndView handleSuccessfulPost(LuminexRunUploadForm form, BindException errors) throws SQLException, ServletException, ExperimentException
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
            try
            {
                LuminexSchema.getSchema().getScope().ensureTransaction();
                if (!form.isResetDefaultValues())
                {
                    for (String analyte : form.getAnalyteNames())
                    {
                        Map<DomainProperty, String> properties = form.getAnalyteProperties(analyte);

                        validatePostedProperties(properties, errors);
                    }
                }

                if (getCompletedUploadAttemptIDs().contains(form.getUploadAttemptID()))
                {
                    throw new RedirectException(PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(getContainer(), _protocol, LuminexUploadWizardAction.class));
                }

                if (!form.isResetDefaultValues() && errors.getErrorCount() == 0)
                {
                    ExpRun run = saveExperimentRun(form);

                    List<ExpData> outputs = run.getDataOutputs();
                    int dataId = 0;
                    if (!form.getTransformResult().getTransformedData().isEmpty())
                    {
                        // data transform occurred, need to find the transformed output that was persisted
                        for (ExpData data : outputs)
                        {
                            if (LuminexDataHandler.LUMINEX_TRANSFORMED_DATA_TYPE.matches(new Lsid(data.getLSID())))
                            {
                                dataId = data.getRowId();
                                break;
                            }
                        }
                    }
                    else
                    {
                        for (ExpData output : outputs)
                        {
                            if (form.getProvider().getDataType().matches(new Lsid(output.getLSID())))
                            {
                                dataId = output.getRowId();
                            }
                        }
                        if (dataId == 0)
                        {
                            throw new IllegalStateException("Could not find primary file in run outputs");
                        }
                    }

                    for (Analyte analyte : getAnalytes(dataId))
                    {
                        Map<DomainProperty, String> properties = form.getAnalyteProperties(analyte.getName());

                        ObjectProperty[] objProperties = new ObjectProperty[properties.size()];
                        int i = 0;
                        for (Map.Entry<DomainProperty, String> entry : properties.entrySet())
                        {
                            ObjectProperty property = new ObjectProperty(analyte.getLsid(),
                                    getContainer(), entry.getKey().getPropertyURI(),
                                    entry.getValue(), entry.getKey().getPropertyDescriptor().getPropertyType());
                            objProperties[i++] = property;
                        }
                        OntologyManager.insertProperties(getContainer(), analyte.getLsid(), objProperties);
                        form.saveDefaultValues(properties, analyte.getName());
                    }

                    // save the defalut values for the analyte standards/titrations information in 2 categories: well roles and titrations
                    PropertyManager.PropertyMap defaultWellRoleValues = PropertyManager.getWritableProperties(
                            getViewContext().getUser().getUserId(), getContainer().getId(), _protocol.getName() + ": Well Role", true);
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
                                    getViewContext().getUser().getUserId(), getContainer().getId(),
                                    _protocol.getName() + ": " + titrationEntry.getValue().getName(), true);
                            for (Analyte analyte : getAnalytes(dataId))
                            {
                                propertyName = getTitrationCheckboxName(titrationEntry.getValue().getName(), analyte.getName());
                                value = getViewContext().getRequest().getParameter(propertyName) != null;
                                defaultTitrationValues.put(propertyName, Boolean.toString(value));
                            }
                            PropertyManager.saveProperties(defaultTitrationValues);
                        }
                    }
                    PropertyManager.saveProperties(defaultWellRoleValues);

                    LuminexSchema.getSchema().getScope().commitTransaction();
                    getCompletedUploadAttemptIDs().add(form.getUploadAttemptID());
                    form.resetUploadAttemptID();
                    return afterRunCreation(form, run, errors);
                }
                else
                {
                    return getAnalytesView(form.getAnalyteNames(), form, true, errors);
                }
            }
            catch (ValidationException ve)
            {
                for (ValidationError error : ve.getErrors())
                    errors.reject(SpringActionController.ERROR_MSG, PageFlowUtil.filter(error.getMessage()));
            }
            catch (ExperimentException e)
            {
                errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
            }
            finally
            {
                LuminexSchema.getSchema().getScope().closeConnection();
            }

            return getAnalytesView(form.getAnalyteNames(), form, true, errors);
        }

        public String getName()
        {
            return NAME;
        }
    }

    @Override
    protected boolean shouldShowDataCollectorUI(LuminexRunUploadForm newRunForm)
    {
        // Always expect input files, regardless of whether we're configured to import spot-level data
        return true;
    }
}
