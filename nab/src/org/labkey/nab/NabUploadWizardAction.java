package org.labkey.nab;

import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.WellGroupTemplate;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.view.InsertView;
import org.labkey.api.data.*;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

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

    protected InsertView createRunInsertView(NabRunUploadForm newRunForm, boolean reshow)
    {
        NabAssayProvider provider = (NabAssayProvider) getProvider(newRunForm);
        PlateTemplate template = provider.getPlateTemplate(newRunForm.getContainer(), getProtocol(newRunForm));
        InsertView parent = super.createRunInsertView(newRunForm, reshow);
        if (template != null)
        {
            PropertyDescriptor[] sampleProperties = provider.getSampleWellGroupColumns(_protocol);
            for (final WellGroupTemplate wellgroup : template.getWellGroups())
            {
                if (wellgroup.getType() == WellGroup.Type.SPECIMEN)
                {
                    for (PropertyDescriptor sampleProperty : sampleProperties)
                    {
                        ColumnInfo col = createColumnInfo(sampleProperty, OntologyManager.getTinfoObject(), "ObjectURI");
                        col.setName(getInputName(sampleProperty, wellgroup));
                        parent.getDataRegion().addColumn(new NabSpecimenInputColumn(col, wellgroup));
                    }
                }
            }
        }
        return parent;
    }

    private String getInputName(PropertyDescriptor property, WellGroupTemplate wellgroup)
    {
        return wellgroup.getName() + "_" + property.getName();
    }

    protected StepHandler getRunStepHandler()
    {
        return new NabRunStepHandler();
    }
    
    protected class NabRunStepHandler extends RunStepHandler
    {
        private Map<PropertyDescriptor, String> _postedSampleProperties = null;

        protected boolean validatePost(NabRunUploadForm form)
        {
            boolean runPropsValid = super.validatePost(form);

            NabAssayProvider provider = (NabAssayProvider) getProvider(form);
            PlateTemplate template = provider.getPlateTemplate(form.getContainer(), getProtocol(form));
            _postedSampleProperties = new HashMap<PropertyDescriptor, String>();
            if (template != null)
            {
                PropertyDescriptor[] sampleProperties = provider.getSampleWellGroupColumns(_protocol);
                for (final WellGroupTemplate wellgroup : template.getWellGroups())
                {
                    if (wellgroup.getType() == WellGroup.Type.SPECIMEN)
                    {
                        for (PropertyDescriptor sampleProperty : sampleProperties)
                        {
                            String name = getInputName(sampleProperty, wellgroup);
                            PropertyDescriptor copy = sampleProperty.clone();
                            copy.setName(name);
                            String value = ColumnInfo.propNameFromName(name);
                            _postedSampleProperties.put(copy, form.getRequest().getParameter(value));
                        }
                    }
                }
            }
            boolean samplePropsValid = validatePostedProperties(_postedSampleProperties);
            return runPropsValid && samplePropsValid;
        }

        protected ModelAndView handleSuccessfulPost(NabRunUploadForm form) throws SQLException, ServletException
        {
            saveDefaultValues(_postedSampleProperties, RunStepHandler.NAME);
            return super.handleSuccessfulPost(form);
        }
    }

    private class NabSpecimenInputColumn extends DataColumn
    {
        public NabSpecimenInputColumn(ColumnInfo col, WellGroupTemplate wellgroup)
        {
            super(col);
            setCaption(wellgroup.getName() + ": " + col.getCaption());
        }

        public boolean isEditable()
        {
            return true;
        }

        protected Object getInputValue(RenderContext ctx)
        {
            TableViewForm viewForm = ctx.getForm();
            return viewForm.getStrings().get(getName());
        }
    }

}
