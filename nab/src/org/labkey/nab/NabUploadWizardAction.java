/*
 * Copyright (c) 2007-2019 LabKey Corporation
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
import org.labkey.api.assay.actions.UploadWizardAction;
import org.labkey.api.assay.plate.PlateSamplePropertyHelper;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.ElevatedUser;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.roles.EditorRole;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.InsertView;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import jakarta.servlet.ServletException;
import java.sql.SQLException;
import java.util.Map;

@RequiresPermission(InsertPermission.class)
public class NabUploadWizardAction extends UploadWizardAction<NabRunUploadForm, NabAssayProvider>
{
    public NabUploadWizardAction()
    {
        super(NabRunUploadForm.class);
    }

    @Override
    protected InsertView createRunInsertView(NabRunUploadForm newRunForm, boolean errorReshow, BindException errors) throws ExperimentException
    {
        NabAssayProvider provider = newRunForm.getProvider();
        InsertView parent = super.createRunInsertView(newRunForm, errorReshow, errors);

        ParticipantVisitResolverType resolverType = getSelectedParticipantVisitResolverType(provider, newRunForm);
        try
        {
            PlateSamplePropertyHelper virusHelper = provider.getVirusPropertyHelper(newRunForm, true);
            if (null != virusHelper)
                virusHelper.addSampleColumns(parent, newRunForm.getUser(), newRunForm, errorReshow);

            PlateSamplePropertyHelper helper = provider.getSamplePropertyHelper(newRunForm, resolverType);
            helper.addSampleColumns(parent, newRunForm.getUser(), newRunForm, errorReshow);
        }
        catch (ExperimentException e)
        {
            errors.addError(new ObjectError("main", null, null, e.toString()));
        }
        return parent;
    }

    @Override
    protected NabRunStepHandler getRunStepHandler()
    {
        return new NabRunStepHandler();
    }

    protected class NabRunStepHandler extends RunStepHandler
    {
        private Map<String, Map<DomainProperty, String>> _postedSampleProperties = null;
        private Map<String, Map<DomainProperty, String>> _postedVirusProperties = null;

        @Override
        public void validateStep(NabRunUploadForm form, Errors errors)
        {
            super.validateStep(form, errors);
            NabAssayProvider provider = form.getProvider();

            try
            {
                PlateSamplePropertyHelper helper = provider.getSamplePropertyHelper(form, getSelectedParticipantVisitResolverType(provider, form));
                _postedSampleProperties = helper.getPostedPropertyValues(form.getRequest());
                for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedSampleProperties.entrySet())
                {
                    // if samplePropsValid flips to false, we want to leave it false (via the "&&" below).  We don't
                    // short-circuit the loop because we want to run through all samples every time, so all errors can be reported.
                    validatePostedProperties(getViewContext(), entry.getValue(), errors);
                }

                PlateSamplePropertyHelper virusHelper = provider.getVirusPropertyHelper(form, false);
                if (null != virusHelper)
                {
                    _postedVirusProperties = virusHelper.getPostedPropertyValues(form.getRequest());
                    for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedVirusProperties.entrySet())
                    {
                        validatePostedProperties(getViewContext(), entry.getValue(), errors);
                    }
                }
            }
            catch (ExperimentException e)
            {
                errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
            }
        }

        @Override
        public boolean executeStep(NabRunUploadForm form, BindException errors) throws ServletException, SQLException, ExperimentException
        {
            execute(form, errors, _postedVirusProperties);
            execute(form, errors, _postedSampleProperties);
            boolean success = !errors.hasErrors() && super.executeStep(form, errors);

            if (success && _run != null)
            {
                User elevatedUser = getUser();
                if (_run.getCreatedBy().equals(getUser()) && !getContainer().hasPermission(getUser(), DeletePermission.class))
                {
                    elevatedUser = ElevatedUser.ensureContextualRoles(getContainer(), getUser(), Pair.of(DeletePermission.class, EditorRole.class));
                }

                if (form.getReRun() != null)
                    form.getReRun().delete(elevatedUser);
            }
            return success;
        }

        private void execute(NabRunUploadForm form, BindException errors, Map<String, Map<DomainProperty, String>> postedVirusProperties)
        {
            if (postedVirusProperties != null)
            {
                form.setSampleProperties(postedVirusProperties);
                for (Map.Entry<String, Map<DomainProperty, String>> entry : postedVirusProperties.entrySet())
                {
                    try
                    {
                        form.saveDefaultValues(entry.getValue(), entry.getKey());
                    }
                    catch (ExperimentException e)
                    {
                        errors.addError(new ObjectError("main", null, null, e.toString()));
                    }
                }
            }
        }
    }

    @Override
    protected ActionURL getUploadWizardCompleteURL(NabRunUploadForm form, ExpRun run)
    {
        NabAssayProvider provider = form.getProvider();
        return provider.getUploadWizardCompleteURL(form, run);
    }

    @Override
    protected boolean shouldShowDataCollectorUI(NabRunUploadForm newRunForm)
    {
        return true;
    }
}
