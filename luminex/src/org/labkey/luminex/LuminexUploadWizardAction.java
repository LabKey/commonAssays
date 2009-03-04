/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.HttpView;
import org.labkey.api.data.*;
import org.labkey.api.exp.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.query.ValidationException;
import org.labkey.api.query.ValidationError;
import org.labkey.api.action.SpringActionController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.*;

/**
 * User: jeckels
* Date: Aug 8, 2007
*/
@RequiresPermission(ACL.PERM_INSERT)
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

    protected ModelAndView afterRunCreation(LuminexRunUploadForm form, ExpRun run, BindException errors) throws ServletException
    {
        Domain analyteDomain = AbstractAssayProvider.getDomainByPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        DomainProperty[] analyteColumns = analyteDomain.getProperties();
        if (analyteColumns.length == 0)
        {
            return super.afterRunCreation(form, run, errors);
        }
        else
        {
            List<ExpData> outputs = run.getDataOutputs();
            assert outputs.size() == 1;
            return getAnalytesView(outputs.get(0).getRowId(), form, false, errors);
        }
    }

    private ModelAndView getAnalytesView(int dataRowId, LuminexRunUploadForm form, boolean errorReshow, BindException errors)
    {
        String lsidColumn = "RowId";
        form.setDataId(dataRowId);
        InsertView view = createInsertView(LuminexSchema.getTableInfoAnalytes(), lsidColumn, new DomainProperty[0], form.isResetDefaultValues(), AnalyteStepHandler.NAME, form, errors);

        view.getDataRegion().addHiddenFormField("dataId", Integer.toString(dataRowId));

        Analyte[] analytes = getAnalytes(dataRowId);
        Domain analyteDomain = AbstractAssayProvider.getDomainByPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        DomainProperty[] analyteColumns = analyteDomain.getProperties();

        // each analyte may have a different set of default values.  Because it may be expensive to query for the
        // entire set of values for every property, we use the following map to cache the default value sets by analyte name.
        Map<String, Map<DomainProperty, Object>> domains = new HashMap<String, Map<DomainProperty, Object>>();
        for (DomainProperty analyteDP : analyteColumns)
        {
            List<DisplayColumn> cols = new ArrayList<DisplayColumn>();
            for (Analyte analyte : analytes)
            {
                // from SamplePropertyHelper:
                // get the map of default values that corresponds to our current sample:
                String defaultValueKey = analyte.getName() + "_" + analyteDP.getDomain().getName();
                Map<DomainProperty, Object> defaultValues = domains.get(defaultValueKey);
                if (defaultValues == null)
                {
                    try
                    {
                        defaultValues = form.getDefaultValues(analyteDP.getDomain(), analyte.getName());
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
        List<String> analyteNames = new ArrayList<String>();
        for (Analyte analyte : analytes)
        {
            analyteNames.add(analyte.getName());
        }
        view.getDataRegion().setGroupHeadings(analyteNames);

        addHiddenBatchProperties(form, view);

        ButtonBar bbar = new ButtonBar();
        addFinishButtons(form, view, bbar);
        addResetButton(form, view, bbar);

        ActionButton cancelButton = new ActionButton("Cancel", getSummaryLink(_protocol));
        bbar.add(cancelButton);

        _stepDescription = "Analyte Properties";

        view.getDataRegion().setButtonBar(bbar, DataRegion.MODE_INSERT);

        return view;
    }

    private String getAnalytePropertyName(Analyte analyte, DomainProperty dp)
    {
        return "_analyte_" + analyte.getRowId() + "_" + ColumnInfo.propNameFromName(dp.getName());
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

    protected void addSampleInputColumns(ExpProtocol protocol, InsertView insertView)
    {
        // Don't add any columns - they're part of the uploaded spreadsheet
    }

    public class AnalyteStepHandler extends StepHandler<LuminexRunUploadForm>
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
                    for (Analyte analyte : getAnalytes(form.getDataId()))
                    {
                        Map<DomainProperty, String> properties = form.getAnalyteProperties(analyte.getRowId());

                        validatePostedProperties(properties, errors);
                    }
                }

                if (getCompletedUploadAttemptIDs().contains(form.getUploadAttemptID()))
                {
                    HttpView.throwRedirect(PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(getContainer(), _protocol, LuminexUploadWizardAction.class));
                }

                if (!form.isResetDefaultValues() && errors.getErrorCount() == 0)
                {
                    for (Analyte analyte : getAnalytes(form.getDataId()))
                    {
                        Map<DomainProperty, String> properties = form.getAnalyteProperties(analyte.getRowId());

                        ObjectProperty[] objProperties = new ObjectProperty[properties.size()];
                        int i = 0;
                        for (Map.Entry<DomainProperty, String> entry : properties.entrySet())
                        {
                            ObjectProperty property = new ObjectProperty(analyte.getLsid(),
                                    getContainer(), entry.getKey().getPropertyURI(),
                                    entry.getValue(), entry.getKey().getPropertyDescriptor().getPropertyType());
                            objProperties[i++] = property;
                        }
                        try {
                            OntologyManager.insertProperties(getContainer(), analyte.getLsid(), objProperties);
                        }
                        catch (ValidationException ve)
                        {
                            for (ValidationError error : ve.getErrors())
                                errors.reject(SpringActionController.ERROR_MSG, PageFlowUtil.filter(error.getMessage()));
                        }

                        try
                        {
                            form.saveDefaultValues(properties, analyte.getName());
                        }
                        catch (ExperimentException e)
                        {
                            errors.addError(new ObjectError("main", null, null, e.toString()));
                        }
                    }

                    LuminexSchema.getSchema().getScope().commitTransaction();
                    getCompletedUploadAttemptIDs().add(form.getUploadAttemptID());
                    form.resetUploadAttemptID();
                    return runUploadComplete(form, errors);
                }
                else
                {
                    return getAnalytesView(form.getDataId(), form, true, errors);
                }
            }
            finally
            {
                LuminexSchema.getSchema().getScope().closeConnection();
            }
        }

        public String getName()
        {
            return NAME;
        }
    }
}
