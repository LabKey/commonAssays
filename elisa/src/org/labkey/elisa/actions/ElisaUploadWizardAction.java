/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.elisa.actions;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.PropertyValidationError;
import org.labkey.api.query.ValidationError;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.assay.plate.PlateTemplate;
import org.labkey.api.assay.actions.PlateBasedUploadWizardAction;
import org.labkey.api.assay.AssayUrls;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.assay.plate.PlateSamplePropertyHelper;
import org.labkey.api.assay.PreviouslyUploadedDataCollector;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.InsertView;
import org.labkey.elisa.ElisaAssayProvider;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

/**
 * User: klum
 * Date: 10/9/12
 */
@RequiresPermission(InsertPermission.class)
public class ElisaUploadWizardAction extends PlateBasedUploadWizardAction<ElisaRunUploadForm, ElisaAssayProvider>
{
    public ElisaUploadWizardAction()
    {
        setCommandClass(ElisaRunUploadForm.class);
        addStepHandler(new ConcentrationStepHandler());
    }

    @Override
    protected InsertView createRunInsertView(ElisaRunUploadForm form, boolean errorReshow, BindException errors) throws ExperimentException
    {
        InsertView view = super.createRunInsertView(form, errorReshow, errors);
        view.getDataRegion().setHorizontalGroups(false);

        return view;
    }

    @Override
    protected RunStepHandler getRunStepHandler()
    {
        return new PlateBasedRunStepHandler() {
            @Override
            public void validateStep(ElisaRunUploadForm form, Errors errors)
            {
                super.validateStep(form, errors);

                if (!errors.hasErrors())
                {
                    try
                    {
                        form.getUploadedData();
                    }
                    catch (ExperimentException e)
                    {
                        errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
                    }
                }
            }

            @Override
            public boolean executeStep(ElisaRunUploadForm form, BindException errors) throws ServletException, SQLException, ExperimentException
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
                return false;
            }

            @Override
            public ModelAndView getNextStep(ElisaRunUploadForm form, BindException errors) throws ServletException, SQLException, ExperimentException
            {
                if (form.isResetDefaultValues() || errors.hasErrors())
                    return getRunPropertiesView(form, !form.isResetDefaultValues(), false, errors);
                else
                    return getConcentrationsView(form, false, errors);
            }
        };
    }

    @Override
    protected void addRunActionButtons(ElisaRunUploadForm form, InsertView insertView, ButtonBar bbar)
    {
        addNextButton(bbar);
        addResetButton(form, insertView, bbar);
    }

    protected InsertView getConcentrationsView(ElisaRunUploadForm form, boolean errorReshow, BindException errors) throws ExperimentException
    {
        InsertView view = createInsertView(ExperimentService.get().getTinfoExperimentRun(),
                "lsid", Collections.emptyList(), errorReshow, ConcentrationStepHandler.NAME, form, errors);

        PlateConcentrationPropertyHelper concentrationsHelper = createConcentrationPropertyHelper(form.getContainer(), form.getProtocol(), form.getProvider());
        concentrationsHelper.addSampleColumns(view, form.getUser(), form, errorReshow);

        // add existing page properties
        addHiddenBatchProperties(form, view);
        addHiddenRunProperties(form, view);

        ElisaAssayProvider provider = form.getProvider();
        PlateSamplePropertyHelper helper = provider.getSamplePropertyHelper(form, getSelectedParticipantVisitResolverType(provider, form));
        for (Map.Entry<String, Map<DomainProperty, String>> sampleEntry : helper.getPostedPropertyValues(form.getRequest()).entrySet())
            addHiddenProperties(sampleEntry.getValue(), view, sampleEntry.getKey());

        PreviouslyUploadedDataCollector<ElisaRunUploadForm> collector = new PreviouslyUploadedDataCollector<>(form.getUploadedData(), PreviouslyUploadedDataCollector.Type.PassThrough);
        collector.addHiddenFormFields(view, form);

        ParticipantVisitResolverType resolverType = getSelectedParticipantVisitResolverType(form.getProvider(), form);
        resolverType.addHiddenFormFields(form, view);

        ButtonBar bbar = new ButtonBar();
        addFinishButtons(form, view, bbar);
        addResetButton(form, view, bbar);

        ActionButton cancelButton = new ActionButton("Cancel", PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), _protocol));
        bbar.add(cancelButton);

        _stepDescription = "Concentrations for Standard Wells";

        view.getDataRegion().setHorizontalGroups(false);
        view.getDataRegion().setButtonBar(bbar, DataRegion.MODE_INSERT);

        return view;
    }

    protected PlateConcentrationPropertyHelper createConcentrationPropertyHelper(Container container, ExpProtocol protocol, ElisaAssayProvider provider)
    {
        PlateTemplate template = provider.getPlateTemplate(container, protocol);
        return new PlateConcentrationPropertyHelper(provider.getConcentrationWellGroupDomain(protocol).getProperties(), template);
    }

    public class ConcentrationStepHandler extends RunStepHandler
    {
        public static final String NAME = "CONCENTRATIONS";

        @Override
        public String getName()
        {
            return NAME;
        }

        @Override
        public void validateStep(ElisaRunUploadForm form, Errors errors)
        {
            super.validateStep(form, errors);
            try
            {
                PlateConcentrationPropertyHelper helper = createConcentrationPropertyHelper(form.getContainer(), form.getProtocol(), form.getProvider());

                for (Map.Entry<String, Map<DomainProperty, String>> entry : helper.getPostedPropertyValues(form.getRequest()).entrySet())
                {
                    // validate that there are no blank values and that all values are numeric
                    for (String prop : entry.getValue().values())
                    {
                        if (StringUtils.isBlank(prop))
                        {
                            errors.reject(SpringActionController.ERROR_MSG, "Value for well group: " + entry.getKey() + " cannot be blank.");
                            break;
                        }
                        if (!NumberUtils.isCreatable(prop))
                        {
                            errors.reject(SpringActionController.ERROR_MSG, "Value for well group: " + entry.getKey() + " must be a number.");
                            break;
                        }
                    }
                }
            }
            catch (ExperimentException e)
            {
                errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
            }
        }

        @Override
        public boolean executeStep(ElisaRunUploadForm form, BindException errors) throws ServletException, SQLException, ExperimentException
        {
            ExpRun run;
            try
            {
                PlateConcentrationPropertyHelper helper = createConcentrationPropertyHelper(form.getContainer(), form.getProtocol(), form.getProvider());

                Map<String, Map<DomainProperty, String>> postedProps = helper.getPostedPropertyValues(form.getRequest());
                form.setConcentrationProperties(postedProps);

                run = saveExperimentRun(form);

                for (Map.Entry<String, Map<DomainProperty, String>> entry : postedProps.entrySet())
                    form.saveDefaultValues(entry.getValue(), entry.getKey());
            }
            catch (ValidationException e)
            {
                for (ValidationError error : e.getErrors())
                {
                    if (error instanceof PropertyValidationError)
                        errors.addError(new FieldError("AssayUploadForm", ((PropertyValidationError)error).getProperty(), null, false,
                                new String[]{SpringActionController.ERROR_MSG}, new Object[0], error.getMessage() == null ? error.toString() : error.getMessage()));
                    else
                        errors.reject(SpringActionController.ERROR_MSG, error.getMessage() == null ? error.toString() : error.getMessage());
                }
            }
            catch (ExperimentException e)
            {
                errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
            }

            return !errors.hasErrors();
        }

        @Override
        public ModelAndView getNextStep(ElisaRunUploadForm form, BindException errors) throws ServletException, SQLException, ExperimentException
        {
            if (form.isResetDefaultValues() || errors.hasErrors())
                return getConcentrationsView(form, !form.isResetDefaultValues(), errors);
            else
                return null;
        }
    }

    @Override
    protected void addHiddenRunProperties(ElisaRunUploadForm form, InsertView view) throws ExperimentException
    {
        super.addHiddenRunProperties(form, view);
        if (form.getName() != null)
            view.getDataRegion().addHiddenFormField("name", form.getName());
        if (form.getComments() != null)
            view.getDataRegion().addHiddenFormField("comments", form.getComments());
    }
}
