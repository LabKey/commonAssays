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

package org.labkey.nab;

import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.*;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.ActionURL;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.Map;

/**
 * User: brittp
 * Date: Sep 27, 2007
 * Time: 3:48:53 PM
 */
@RequiresPermissionClass(InsertPermission.class)
public class NabUploadWizardAction extends UploadWizardAction<NabRunUploadForm, NabAssayProvider>
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
    protected InsertView createInsertView(TableInfo baseTable, String lsidCol, DomainProperty[] properties, boolean errorReshow, String uploadStepName, NabRunUploadForm form, BindException errors)
    {
        InsertView view = super.createInsertView(baseTable, lsidCol, properties, errorReshow, uploadStepName, form, errors);
        if (form.getReRunId() != null)
            view.getDataRegion().addHiddenFormField("reRunId", "" + form.getReRunId());
        return view;
    }

    @Override
    protected InsertView createRunInsertView(NabRunUploadForm newRunForm, boolean errorReshow, BindException errors)
    {
        NabAssayProvider provider = (NabAssayProvider) getProvider(newRunForm);
        InsertView parent = super.createRunInsertView(newRunForm, errorReshow, errors);
        ParticipantVisitResolverType resolverType = getSelectedParticipantVisitResolverType(provider, newRunForm);
        PlateSamplePropertyHelper helper = provider.getSamplePropertyHelper(newRunForm, resolverType);
        try
        {
            helper.addSampleColumns(parent, newRunForm.getUser(), newRunForm, errorReshow);
        }
        catch (ExperimentException e)
        {
            errors.addError(new ObjectError("main", null, null, e.toString()));
        }
        return parent;
    }

    protected NabRunStepHandler getRunStepHandler()
    {
        return new NabRunStepHandler();
    }

    protected class NabRunStepHandler extends RunStepHandler
    {
        private Map<String, Map<DomainProperty, String>> _postedSampleProperties = null;

        @Override
        protected boolean validatePost(NabRunUploadForm form, BindException errors)
        {
            boolean runPropsValid = super.validatePost(form, errors);

            NabAssayProvider provider = (NabAssayProvider) getProvider(form);
            PlateSamplePropertyHelper helper = provider.getSamplePropertyHelper(form, getSelectedParticipantVisitResolverType(provider, form));

            boolean samplePropsValid = true;
            try
            {
                _postedSampleProperties = helper.getPostedPropertyValues(form.getRequest());
                for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedSampleProperties.entrySet())
                {
                    // if samplePropsValid flips to false, we want to leave it false (via the "&&" below).  We don't
                    // short-circuit the loop because we want to run through all samples every time, so all errors can be reported.
                    samplePropsValid = validatePostedProperties(entry.getValue(), errors) && samplePropsValid;
                }
            }
            catch (ExperimentException e)
            {
                errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
            }
            return runPropsValid && samplePropsValid && !errors.hasErrors();
        }

        protected ModelAndView handleSuccessfulPost(NabRunUploadForm form, BindException errors) throws SQLException, ServletException
        {
            form.setSampleProperties(_postedSampleProperties);
            for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedSampleProperties.entrySet())
            {
                try
                {
                    form.saveDefaultValues(entry.getValue(), entry.getKey());
                }
                catch (org.labkey.api.exp.ExperimentException e)
                {
                    errors.addError(new ObjectError("main", null, null, e.toString()));
                }
            }
            return super.handleSuccessfulPost(form, errors);
        }
    }

    protected ModelAndView afterRunCreation(NabRunUploadForm form, ExpRun run, BindException errors) throws ServletException
    {
        if (form.getReRunId() != null)
            ExperimentService.get().deleteExperimentRunsByRowIds(getContainer(), getViewContext().getUser(), form.getReRunId().intValue());
        if (form.isMultiRunUpload())
            return super.afterRunCreation(form, run, errors);
        else
        {
            HttpView.throwRedirect(new ActionURL(NabAssayController.DetailsAction.class,
                    run.getContainer()).addParameter("rowId", run.getRowId()).addParameter("newRun", "true"));
            return null;
        }
    }
}
