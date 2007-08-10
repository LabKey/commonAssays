package org.labkey.luminex;

import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.DefaultAssayProvider;
import org.labkey.api.view.InsertView;
import org.labkey.api.data.*;
import org.labkey.api.exp.*;
import org.labkey.api.util.PageFlowUtil;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

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
        PropertyDescriptor[] analyteColumns = DefaultAssayProvider.getPropertiesForDomainPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        if (analyteColumns.length == 0)
        {
            super.addRunActionButtons(insertView, bbar);
        }
        else
        {
            ActionButton nextButton = new ActionButton(getViewContext().getViewURLHelper().getAction() + ".view", "Next");
            nextButton.setActionType(ActionButton.Action.POST);
            bbar.add(nextButton);
        }
    }

    protected ModelAndView afterRunCreation(LuminexRunUploadForm form, ExperimentRun run) throws ServletException, SQLException
    {
        PropertyDescriptor[] analyteColumns = DefaultAssayProvider.getPropertiesForDomainPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
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
        InsertView view = createInsertView(LuminexSchema.getTableInfoAnalytes(), lsidColumn, map, false, AnalyteStepHandler.NAME);

        view.getDataRegion().addHiddenFormField("dataId", Integer.toString(dataRowId));

        Analyte[] analytes = getAnalytes(dataRowId);
        PropertyDescriptor[] pds = DefaultAssayProvider.getPropertiesForDomainPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        if (reshow)
        {
            view.setInitialValues(getViewContext().getRequest().getParameterMap());
        }
        for (Analyte analyte : analytes)
        {
            for (PropertyDescriptor pd : pds)
            {
                ColumnInfo info = createColumnInfo(pd, view.getDataRegion().getTable(), lsidColumn);
                info.setName("_analyte_" + analyte.getRowId() + "_" + ColumnInfo.propNameFromName(pd.getName()));
                info.setCaption(analyte.getName() + " " + pd.getName());
                view.getDataRegion().addColumn(info);
            }
        }

        addHiddenSampleSetProperties(form, view);

        ButtonBar bbar = new ButtonBar();
        addFinishButtons(view, bbar);

        ActionButton cancelButton = new ActionButton("Cancel", getSummaryLink(_protocol));
        bbar.add(cancelButton);

        view.getDataRegion().setButtonBar(bbar, DataRegion.MODE_INSERT);

        return view;
    }

    private Analyte[] getAnalytes(int dataRowId)
            throws SQLException
    {
        return Table.select(LuminexSchema.getTableInfoAnalytes(), Table.ALL_COLUMNS, new SimpleFilter("DataId", dataRowId), new Sort("RowId"), Analyte.class);
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
                for (Analyte analyte : getAnalytes(form.getDataId()))
                {
                    Map<PropertyDescriptor, String> properties = form.getAnalyteProperties(analyte.getRowId());

                    validatePost(properties);
                }
                if (PageFlowUtil.getActionErrors(getViewContext().getRequest(), true).isEmpty())
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
