/*
 * Copyright (c) 2007-2010 LabKey Corporation
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
import org.labkey.api.view.HttpView;
import org.labkey.api.view.InsertView;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import java.io.File;
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

    private ModelAndView getAnalytesView(String[] analyteNames, LuminexRunUploadForm form, boolean errorReshow, BindException errors) throws ServletException
    {
        try {
            String lsidColumn = "RowId";
            InsertView view = createInsertView(LuminexSchema.getTableInfoAnalytes(), lsidColumn, new DomainProperty[0], form.isResetDefaultValues(), AnalyteStepHandler.NAME, form, errors);

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
                            errors.addError(new ObjectError("main", null, null, e.toString()));
                        }
                        domains.put(defaultValueKey,  defaultValues);
                    }
                    String inputName = getAnalytePropertyName(analyte, analyteDP);
                    if (defaultValues != null)
                        view.setInitialValue(inputName, defaultValues.get(analyteDP));

                    ColumnInfo info = analyteDP.getPropertyDescriptor().createColumnInfo(view.getDataRegion().getTable(), lsidColumn, getViewContext().getUser());
                    info.setName(inputName);
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

            ButtonBar bbar = new ButtonBar();
            addFinishButtons(form, view, bbar);
            addResetButton(form, view, bbar);

            ActionButton cancelButton = new ActionButton("Cancel", getSummaryLink(_protocol));
            bbar.add(cancelButton);

            _stepDescription = "Analyte Properties";

            view.getDataRegion().setButtonBar(bbar, DataRegion.MODE_INSERT);

            return view;
        }
        catch (ExperimentException e)
        {
            throw new ServletException(e);
        }
    }

    private String getAnalytePropertyName(String analyte, DomainProperty dp)
    {
        return "_analyte_" + ColumnInfo.propNameFromName(analyte) + "_" + ColumnInfo.propNameFromName(dp.getName());
    }

    private String[] getAnalyteNames(LuminexRunUploadForm form) throws ExperimentException
    {
        List<String> names = new ArrayList<String>();

        assert form.getUploadedData().containsKey(AssayDataCollector.PRIMARY_FILE);
        File dataFile = form.getUploadedData().get(AssayDataCollector.PRIMARY_FILE);
        LuminexExcelDataHandler.LuminexExcelParser parser = new LuminexExcelDataHandler.LuminexExcelParser(form.getProtocol(), dataFile);
        for (Analyte analyte : parser.getSheets().keySet())
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

    protected class LuminexRunStepHandler extends RunStepHandler
    {
        @Override
        protected ModelAndView handleSuccessfulPost(LuminexRunUploadForm form, BindException errors) throws SQLException, ServletException
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
        public ModelAndView handleStep(LuminexRunUploadForm form, BindException errors) throws ServletException, SQLException
        {
            try
            {
                LuminexSchema.getSchema().getScope().beginTransaction();
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
                    HttpView.throwRedirect(PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(getContainer(), _protocol, LuminexUploadWizardAction.class));
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
                            if (LuminexExcelDataHandler.LUMINEX_TRANSFORMED_DATA_TYPE.matches(new Lsid(data.getLSID())))
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

                    LuminexSchema.getSchema().getScope().commitTransaction();
                    getCompletedUploadAttemptIDs().add(form.getUploadAttemptID());
                    form.resetUploadAttemptID();
                    return runUploadComplete(form, errors);
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

            if (errors.hasErrors())
                return getAnalytesView(form.getAnalyteNames(), form, true, errors);
            return runUploadComplete(form, errors);
        }

        public String getName()
        {
            return NAME;
        }
    }
}
