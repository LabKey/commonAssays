/*
 * Copyright (c) 2007-2016 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.assay.dilution.DilutionAssayProvider;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.LimitedUser;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.roles.EditorRole;
import org.labkey.api.security.roles.Role;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.InsertView;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: brittp
 * Date: Sep 27, 2007
 * Time: 3:48:53 PM
 */
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

    protected NabRunStepHandler getRunStepHandler()
    {
        return new NabRunStepHandler();
    }

    protected class NabRunStepHandler extends RunStepHandler
    {
        private Map<String, Map<DomainProperty, String>> _postedSampleProperties = null;
        private Map<String, Map<DomainProperty, String>> _postedVirusProperties = null;

        @Override
        protected boolean validatePost(NabRunUploadForm form, BindException errors) throws ExperimentException
        {
            boolean runPropsValid = super.validatePost(form, errors);

            NabAssayProvider provider = form.getProvider();

            boolean samplePropsValid = true;
            boolean virusPropsValid = true;
            try
            {
                PlateSamplePropertyHelper helper = provider.getSamplePropertyHelper(form, getSelectedParticipantVisitResolverType(provider, form));
                _postedSampleProperties = helper.getPostedPropertyValues(form.getRequest());
                for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedSampleProperties.entrySet())
                {
                    // if samplePropsValid flips to false, we want to leave it false (via the "&&" below).  We don't
                    // short-circuit the loop because we want to run through all samples every time, so all errors can be reported.
                    samplePropsValid = validatePostedProperties(getViewContext(), entry.getValue(), errors) && samplePropsValid;
                }

                PlateSamplePropertyHelper virusHelper = provider.getVirusPropertyHelper(form, false);
                if (null != virusHelper)
                {
                    _postedVirusProperties = virusHelper.getPostedPropertyValues(form.getRequest());
                    for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedVirusProperties.entrySet())
                    {
                        virusPropsValid = validatePostedProperties(getViewContext(), entry.getValue(), errors) && virusPropsValid;
                    }
                }
            }
            catch (ExperimentException e)
            {
                errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
            }
            return runPropsValid && samplePropsValid && virusPropsValid && !errors.hasErrors();
        }

        protected ModelAndView handleSuccessfulPost(NabRunUploadForm form, BindException errors) throws ExperimentException
        {
            if (_postedVirusProperties != null)
            {
                form.setSampleProperties(_postedVirusProperties);
                for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedVirusProperties.entrySet())
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
            }
            if (_postedSampleProperties != null)
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
            }
            return super.handleSuccessfulPost(form, errors);
        }
    }

    protected ModelAndView afterRunCreation(NabRunUploadForm form, ExpRun run, BindException errors) throws ExperimentException
    {
        User elevatedUser = getUser();
        if (run.getCreatedBy().equals(getUser()) && !getContainer().hasPermission(getUser(), DeletePermission.class))
        {
            User currentUser = getUser();
            Set<Role> contextualRoles = new HashSet<>(currentUser.getStandardContextualRoles());
            contextualRoles.add(RoleManager.getRole(EditorRole.class));
            elevatedUser = new LimitedUser(currentUser, currentUser.getGroups(), contextualRoles, false);
        }

        if (form.getReRun() != null)
            form.getReRun().delete(elevatedUser);
        return super.afterRunCreation(form, run, errors);
    }

    @Override
    protected ActionURL getUploadWizardCompleteURL(NabRunUploadForm form, ExpRun run)
    {
        DilutionAssayProvider provider = form.getProvider();
        return provider.getUploadWizardCompleteURL(form, run);
    }

    @Override
    protected boolean shouldShowDataCollectorUI(NabRunUploadForm newRunForm)
    {
        return true;
    }

/*    public class SampleStepHandler extends RunStepHandler
    {
        public static final String NAME = "SAMPLES";
        private Map<String, Map<DomainProperty, String>> _postedSampleProperties = null;

        public ModelAndView handleStep(NabRunUploadForm form, BindException errors) throws ServletException, SQLException, ExperimentException
        {
            if (!form.isResetDefaultValues() && validatePost(form, errors))
                return handleSuccessfulPost(form, errors);
            else
                return getSamplesView(form, true, errors);

            //return getPlateSummary(form, false);
        }

        protected boolean validatePost(NabRunUploadForm form, BindException errors) throws ExperimentException
        {
            return validatePostedSampleProperties(form, errors);
        }

        protected ModelAndView handleSuccessfulPost(NabRunUploadForm form, BindException errors) throws ExperimentException
        {
            DilutionAssayProvider provider = form.getProvider();
            PlateSamplePropertyHelper helper = provider.getSamplePropertyHelper(form, getSelectedParticipantVisitResolverType(provider, form));
            _postedSampleProperties = helper.getPostedPropertyValues(form.getRequest());
            if (_postedSampleProperties != null)
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
            }
            return super.handleSuccessfulPost(form, errors);
        }

        public String getName()
        {
            return NAME;
        }
    }

    protected InsertView getSamplesView(NabRunUploadForm form, boolean errorReshow, BindException errors) throws ServletException, ExperimentException
    {
        InsertView view = createInsertView(ExperimentService.get().getTinfoExperimentRun(),
                "lsid", Collections.<DomainProperty>emptyList(), errorReshow, SampleStepHandler.NAME, form, errors);

        ParticipantVisitResolverType resolverType = getSelectedParticipantVisitResolverType(form.getProvider(), form);
        PlateSamplePropertyHelper helper = form.getProvider().getSamplePropertyHelper(form, resolverType);
        helper.addSampleColumns(view, form.getUser(), form, errorReshow);

        // add existing page properties
        addHiddenBatchProperties(form, view);
        addHiddenRunProperties(form, view);

/*
        ElisaAssayProvider provider = form.getProvider();
        PlateSamplePropertyHelper helper = provider.getSamplePropertyHelper(form, getSelectedParticipantVisitResolverType(provider, form));
        for (Map.Entry<String, Map<DomainProperty, String>> sampleEntry : helper.getPostedPropertyValues(form.getRequest()).entrySet())
            addHiddenProperties(sampleEntry.getValue(), view, sampleEntry.getKey());

        PreviouslyUploadedDataCollector<ElisaRunUploadForm> collector = new PreviouslyUploadedDataCollector<>(form.getUploadedData(), PreviouslyUploadedDataCollector.Type.PassThrough);
        collector.addHiddenFormFields(view, form);
*/     /*

//        ParticipantVisitResolverType resolverType = getSelectedParticipantVisitResolverType(form.getProvider(), form);
        resolverType.addHiddenFormFields(form, view);

        ButtonBar bbar = new ButtonBar();
        addFinishButtons(form, view, bbar);
        addResetButton(form, view, bbar);

        ActionButton cancelButton = new ActionButton("Cancel", getSummaryLink(_protocol));
        bbar.add(cancelButton);

        _stepDescription = "Concentrations for Standard Wells";

        view.getDataRegion().setHorizontalGroups(false);
        view.getDataRegion().setButtonBar(bbar, DataRegion.MODE_INSERT);

        return view;
    }

    private boolean validatePostedSampleProperties(NabRunUploadForm form, BindException errors)
    {
        DilutionAssayProvider provider = form.getProvider();
        PlateSamplePropertyHelper helper = provider.getSamplePropertyHelper(form, getSelectedParticipantVisitResolverType(provider, form));

        boolean samplePropsValid = true;
        try
        {
            Map<String, Map<DomainProperty, String>> postedSampleProperties = helper.getPostedPropertyValues(form.getRequest());
            for (Map.Entry<String, Map<DomainProperty, String>> entry : postedSampleProperties.entrySet())
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
        return samplePropsValid;
    }

*/
}
