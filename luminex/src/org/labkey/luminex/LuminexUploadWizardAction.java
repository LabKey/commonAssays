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
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.HttpView;
import org.labkey.api.data.*;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.query.ValidationException;
import org.labkey.api.query.ValidationError;
import org.labkey.api.action.SpringActionController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.*;

/**
 * User: jeckels
* Date: Aug 8, 2007
*/
@RequiresPermission(ACL.PERM_INSERT)
public class LuminexUploadWizardAction extends UploadWizardAction<LuminexRunUploadForm>
{
    public LuminexUploadWizardAction()
    {
        super(LuminexRunUploadForm.class);
        addStepHandler(new AnalyteStepHandler());
    }

    public ModelAndView getView(LuminexRunUploadForm assayRunUploadForm, BindException errors) throws Exception
    {
        return super.getView(assayRunUploadForm, errors);
    }

    protected void addRunActionButtons(LuminexRunUploadForm newRunForm, InsertView insertView, ButtonBar bbar)
    {
        PropertyDescriptor[] analyteColumns = AbstractAssayProvider.getPropertiesForDomainPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
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


    protected Map<String, String> getDefaultValues(String suffix, LuminexRunUploadForm form) throws ExperimentException
    {
        if (!form.isResetDefaultValues() && AnalyteStepHandler.NAME.equals(suffix))
        {
            Map<String, String> result = new HashMap<String, String>();
            Analyte[] analytes = getAnalytes(form.getDataId());
            PropertyDescriptor[] analyteColumns = AbstractAssayProvider.getPropertiesForDomainPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);

            try
            {
                for (Analyte analyte : analytes)
                {
                    SQLFragment sql = new SQLFragment("SELECT ObjectURI FROM " + OntologyManager.getTinfoObject() + " WHERE Container = ? AND ObjectId = (SELECT MAX(ObjectId) FROM " + OntologyManager.getTinfoObject() + " o, " + LuminexSchema.getTableInfoAnalytes() + " a WHERE o.ObjectURI = a.LSID AND a.Name = ?)");
                    sql.add(form.getContainer().getId());
                    sql.add(analyte.getName());
                    String objectURI = Table.executeSingleton(LuminexSchema.getSchema(), sql.getSQL(), sql.getParamsArray(), String.class, true);
                    if (objectURI != null)
                    {
                        Map<String, ObjectProperty> values = OntologyManager.getPropertyObjects(getContainer(), objectURI);
                        for (PropertyDescriptor analytePD : analyteColumns)
                        {
                            String name = getAnalytePropertyName(analyte, analytePD);
                            ObjectProperty objectProp = values.get(analytePD.getPropertyURI());
                            if (objectProp != null && objectProp.value() != null)
                            {
                                result.put(name, objectProp.value().toString());
                            }
                        }
                    }
                }
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
            return result;
        }
        else
        {
            return super.getDefaultValues(suffix, form);
        }
    }

    protected ModelAndView afterRunCreation(LuminexRunUploadForm form, ExpRun run, BindException errors) throws ServletException, SQLException
    {
        PropertyDescriptor[] analyteColumns = AbstractAssayProvider.getPropertiesForDomainPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
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

    private ModelAndView getAnalytesView(int dataRowId, LuminexRunUploadForm form, boolean reshow, BindException errors)
            throws SQLException
    {
        Map<PropertyDescriptor, String> map = new LinkedHashMap<PropertyDescriptor, String>();

        String lsidColumn = "RowId";
        form.setDataId(dataRowId);
        InsertView view = createInsertView(LuminexSchema.getTableInfoAnalytes(), lsidColumn, map, reshow, form.isResetDefaultValues(), AnalyteStepHandler.NAME, form, errors);

        view.getDataRegion().addHiddenFormField("dataId", Integer.toString(dataRowId));

        Analyte[] analytes = getAnalytes(dataRowId);
        PropertyDescriptor[] pds = AbstractAssayProvider.getPropertiesForDomainPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        if (reshow && !form.isResetDefaultValues())
        {
            view.setInitialValues(getViewContext().getRequest().getParameterMap());
        }
        for (PropertyDescriptor pd : pds)
        {
            List<DisplayColumn> cols = new ArrayList<DisplayColumn>();
            for (Analyte analyte : analytes)
            {
                ColumnInfo info = pd.createColumnInfo(view.getDataRegion().getTable(), lsidColumn, getViewContext().getUser());
                info.setName(getAnalytePropertyName(analyte, pd));
                cols.add(info.getRenderer());
            }
            view.getDataRegion().addGroup(new DisplayColumnGroup(cols, pd.getName(), true));
        }
        view.getDataRegion().setHorizontalGroups(false);
        List<String> analyteNames = new ArrayList<String>();
        for (Analyte analyte : analytes)
        {
            analyteNames.add(analyte.getName());
        }
        view.getDataRegion().setGroupHeadings(analyteNames);

        addHiddenUploadSetProperties(form, view);

        ButtonBar bbar = new ButtonBar();
        addFinishButtons(form, view, bbar);
        addResetButton(form, view, bbar);

        ActionButton cancelButton = new ActionButton("Cancel", getSummaryLink(_protocol));
        bbar.add(cancelButton);

        _stepDescription = "Analyte Properties";

        view.getDataRegion().setButtonBar(bbar, DataRegion.MODE_INSERT);

        return view;
    }

    private String getAnalytePropertyName(Analyte analyte, PropertyDescriptor pd)
    {
        return "_analyte_" + analyte.getRowId() + "_" + ColumnInfo.propNameFromName(pd.getName());
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
                        Map<PropertyDescriptor, String> properties = form.getAnalyteProperties(analyte.getRowId());

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
                        Map<PropertyDescriptor, String> properties = form.getAnalyteProperties(analyte.getRowId());

                        ObjectProperty[] objProperties = new ObjectProperty[properties.size()];
                        int i = 0;
                        for (Map.Entry<PropertyDescriptor, String> entry : properties.entrySet())
                        {
                            ObjectProperty property = new ObjectProperty(analyte.getLsid(),
                                    getContainer(), entry.getKey().getPropertyURI(),
                                    entry.getValue(), entry.getKey().getPropertyType());
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
