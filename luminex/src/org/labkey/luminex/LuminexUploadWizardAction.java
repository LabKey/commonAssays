package org.labkey.luminex;

import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.HttpView;
import org.labkey.api.data.*;
import org.labkey.api.exp.*;
import org.labkey.api.util.PageFlowUtil;
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

    protected void addRunActionButtons(InsertView insertView, ButtonBar bbar)
    {
        PropertyDescriptor[] analyteColumns = AbstractAssayProvider.getPropertiesForDomainPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        if (analyteColumns.length == 0)
        {
            super.addRunActionButtons(insertView, bbar);
        }
        else
        {
            addNextButton(bbar);
            addResetButton(insertView, bbar);
        }
    }


    protected Map<String, String> getDefaultValues(String suffix, LuminexRunUploadForm form)
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
                        Map<String, ObjectProperty> values = OntologyManager.getPropertyObjects(getContainer().getId(), objectURI);
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

    protected ModelAndView afterRunCreation(LuminexRunUploadForm form, ExperimentRun run) throws ServletException, SQLException
    {
        PropertyDescriptor[] analyteColumns = AbstractAssayProvider.getPropertiesForDomainPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        if (analyteColumns.length == 0)
        {
            return super.afterRunCreation(form, run);
        }
        else
        {
            List<Data> outputs = run.retrieveOutputDataList();
            assert outputs.size() == 1;
            return getAnalytesView(outputs.get(0).getRowId(), form, false);
        }
    }

    private ModelAndView getAnalytesView(int dataRowId, LuminexRunUploadForm form, boolean reshow)
            throws SQLException
    {
        Map<PropertyDescriptor, String> map = new LinkedHashMap<PropertyDescriptor, String>();

        String lsidColumn = "RowId";
        form.setDataId(dataRowId);
        InsertView view = createInsertView(LuminexSchema.getTableInfoAnalytes(), lsidColumn, map, reshow, form.isResetDefaultValues(), AnalyteStepHandler.NAME, form);

        view.getDataRegion().addHiddenFormField("dataId", Integer.toString(dataRowId));

        Analyte[] analytes = getAnalytes(dataRowId);
        PropertyDescriptor[] pds = AbstractAssayProvider.getPropertiesForDomainPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        if (reshow && !form.isResetDefaultValues())
        {
            view.setInitialValues(getViewContext().getRequest().getParameterMap());
        }
        for (Analyte analyte : analytes)
        {
            for (PropertyDescriptor pd : pds)
            {
                ColumnInfo info = createColumnInfo(pd, view.getDataRegion().getTable(), lsidColumn);
                info.setName(getAnalytePropertyName(analyte, pd));
                info.setCaption(analyte.getName() + " " + pd.getName());
                view.getDataRegion().addColumn(info);
            }
        }

        addHiddenUploadSetProperties(form, view);

        ButtonBar bbar = new ButtonBar();
        addFinishButtons(view, bbar);
        addResetButton(view, bbar);

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

    protected void addSampleInputColumns(Protocol protocol, InsertView insertView)
    {
        // Don't add any columns - they're part of the uploaded spreadsheet
    }

    public class AnalyteStepHandler extends StepHandler
    {
        public static final String NAME = "ANALYTE";

        public ModelAndView handleStep(LuminexRunUploadForm form) throws ServletException, SQLException
        {
            try
            {
                LuminexSchema.getSchema().getScope().beginTransaction();
                if (!form.isResetDefaultValues())
                {
                    for (Analyte analyte : getAnalytes(form.getDataId()))
                    {
                        Map<PropertyDescriptor, String> properties = form.getAnalyteProperties(analyte.getRowId());

                        validatePost(properties);
                    }
                }

                if (getCompletedUploadAttemptIDs().contains(form.getUploadAttemptID()))
                {
                    HttpView.throwRedirect(form.getProvider().getUploadWizardURL(getContainer(), _protocol));
                }

                if (!form.isResetDefaultValues() && PageFlowUtil.getActionErrors(getViewContext().getRequest(), true).isEmpty())
                {
                    for (Analyte analyte : getAnalytes(form.getDataId()))
                    {
                        Map<PropertyDescriptor, String> properties = form.getAnalyteProperties(analyte.getRowId());

                        ObjectProperty[] objProperties = new ObjectProperty[properties.size()];
                        int i = 0;
                        for (Map.Entry<PropertyDescriptor, String> entry : properties.entrySet())
                        {
                            ObjectProperty property = new ObjectProperty(analyte.getLsid(),
                                    getContainer().getId(), entry.getKey().getPropertyURI(),
                                    entry.getValue(), entry.getKey().getPropertyType());
                            objProperties[i++] = property;
                        }
                        OntologyManager.insertProperties(getContainer().getId(), objProperties, analyte.getLsid());
                    }

                    LuminexSchema.getSchema().getScope().commitTransaction();
                    getCompletedUploadAttemptIDs().add(form.getUploadAttemptID());
                    form.resetUploadAttemptID();
                    return runUploadComplete(form);
                }
                else
                {
                    return getAnalytesView(form.getDataId(), form, true);
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
