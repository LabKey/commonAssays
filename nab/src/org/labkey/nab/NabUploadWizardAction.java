package org.labkey.nab;

import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.ActionURL;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.Map;

/**
 * User: brittp
 * Date: Sep 27, 2007
 * Time: 3:48:53 PM
 */
@RequiresPermission(ACL.PERM_INSERT)
public class NabUploadWizardAction extends UploadWizardAction<NabRunUploadForm>
{
    public NabUploadWizardAction()
    {
        super(NabRunUploadForm.class);
    }

    public ModelAndView getView(NabRunUploadForm assayRunUploadForm, BindException errors) throws Exception
    {
        return super.getView(assayRunUploadForm, errors);
    }

    @Override
    protected InsertView createInsertView(TableInfo baseTable, String lsidCol, Map<PropertyDescriptor, String> propertyDescriptors, boolean reshow, boolean resetDefaultValues, String uploadStepName, NabRunUploadForm form, BindException errors)
    {
        InsertView view = super.createInsertView(baseTable, lsidCol, propertyDescriptors, reshow, resetDefaultValues, uploadStepName, form, errors);
        if (form.getReplaceRunId() != null)
            view.getDataRegion().addHiddenFormField("replaceRunId", "" + form.getReplaceRunId());
        return view;
    }

    @Override
    protected InsertView createRunInsertView(NabRunUploadForm newRunForm, boolean reshow, BindException errors)
    {
        NabAssayProvider provider = (NabAssayProvider) getProvider(newRunForm);
        InsertView parent = super.createRunInsertView(newRunForm, reshow, errors);
        ParticipantVisitResolverType resolverType = getSelectedParticipantVisitResolverType(provider, newRunForm);
        PlateSamplePropertyHelper helper = provider.createSamplePropertyHelper(newRunForm.getContainer(), newRunForm.getProtocol(), resolverType);
        helper.addSampleColumns(parent.getDataRegion(), getViewContext().getUser());
        return parent;
    }
/*
    protected Map<String, String> getDefaultValues(String suffix, NabRunUploadForm form)
    {
        if (form.getReplaceRunId() == null)
            return super.getDefaultValues(suffix, form);
        else
        {
            ExpRun run = ExperimentService.get().getExpRun(form.getReplaceRunId());
            NabAssayProvider provider = (NabAssayProvider) form.getProvider();
            ExpProtocol protocol = run.getProtocol();

            Map<String, String> properties = new HashMap<String, String>();

            properties.put("name", run.getName());
            properties.put("comments", run.getComments());
            properties.put("uploadedFile", NabDataHandler.getDataFile(run).getPath());
            
            for (PropertyDescriptor column : provider.getRunPropertyColumns(protocol))
                properties.put(ColumnInfo.propNameFromName(column.getName()), nullSafeToStr(run.getProperty(column)));
            for (PropertyDescriptor column : provider.getUploadSetColumns(protocol))
                properties.put(ColumnInfo.propNameFromName(column.getName()), nullSafeToStr(run.getProperty(column)));

            try
            {
                Map<WellGroup, Map<PropertyDescriptor, Object>> wellgroups = NabDataHandler.getWellGroupProperties(run);
                for (Map.Entry<WellGroup, Map<PropertyDescriptor, Object>> wellGroupMapEntry : wellgroups.entrySet())
                {
                    WellGroup group = wellGroupMapEntry.getKey();
                    for (Map.Entry<PropertyDescriptor, Object> wellgroupProperty : wellGroupMapEntry.getValue().entrySet())
                    {
                        PropertyDescriptor property = wellgroupProperty.getKey();
                        properties.put(SamplePropertyHelper.getSpecimenPropertyInputName(group.getName(), property), nullSafeToStr(wellgroupProperty.getValue()));
                    }
                }
            }
            catch (ExperimentException e)
            {
                throw new RuntimeException(e);
            }
            return properties;
        }
    }

    private String nullSafeToStr(Object obj)
    {
        return "" + (obj != null ? obj.toString() : "");
    }
*/
    protected StepHandler getRunStepHandler()
    {
        return new NabRunStepHandler();
    }
    
    protected class NabRunStepHandler extends RunStepHandler
    {
        private Map<PropertyDescriptor, String> _postedSampleProperties = null;

        @Override
        protected boolean validatePost(NabRunUploadForm form, BindException errors)
        {
            boolean runPropsValid = super.validatePost(form, errors);

            NabAssayProvider provider = (NabAssayProvider) getProvider(form);
            PlateSamplePropertyHelper helper = provider.createSamplePropertyHelper(getContainer(), _protocol,
                    getSelectedParticipantVisitResolverType(provider, form));
            _postedSampleProperties = helper.getPostedPropertyValues(form.getRequest());
            boolean samplePropsValid = validatePostedProperties(_postedSampleProperties, getViewContext().getRequest(), errors);
            return runPropsValid && samplePropsValid;
        }

        protected ModelAndView handleSuccessfulPost(NabRunUploadForm form, BindException error) throws SQLException, ServletException
        {
            saveDefaultValues(_postedSampleProperties, form.getRequest(), form.getProvider(), RunStepHandler.NAME);
            return super.handleSuccessfulPost(form, error);
        }
    }

    protected ModelAndView afterRunCreation(NabRunUploadForm form, ExpRun run, BindException errors) throws ServletException, SQLException
    {
        if (form.isMultiRunUpload())
            return super.afterRunCreation(form, run, errors);
        else
        {
            HttpView.throwRedirect(new ActionURL("NabAssay", "details",
                    run.getContainer()).addParameter("rowId", run.getRowId()).addParameter("newRun", "true"));
            return null;
        }
    }
}
