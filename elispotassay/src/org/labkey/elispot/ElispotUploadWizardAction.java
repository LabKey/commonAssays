/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

package org.labkey.elispot;

import org.apache.commons.lang3.math.NumberUtils;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DbScope;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.SamplePropertyHelper;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.gwt.client.DefaultValueType;
import org.labkey.api.query.ValidationError;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;
import org.labkey.api.study.assay.PreviouslyUploadedDataCollector;
import org.labkey.api.study.assay.plate.PlateReader;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.InsertView;
import org.labkey.elispot.plate.PlateInfo;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: Karl Lum
 * Date: Jan 9, 2008
 */

@RequiresPermission(InsertPermission.class)
public class ElispotUploadWizardAction extends UploadWizardAction<ElispotRunUploadForm, ElispotAssayProvider>
{
    public ElispotUploadWizardAction()
    {
        super(ElispotRunUploadForm.class);
        addStepHandler(new AntigenStepHandler());
        addStepHandler(new AnalyteStepHandler());
    }

    protected InsertView createRunInsertView(ElispotRunUploadForm newRunForm, boolean errorReshow, BindException errors) throws ExperimentException
    {
        InsertView view = super.createRunInsertView(newRunForm, errorReshow, errors);

        ElispotAssayProvider provider = newRunForm.getProvider();
        ParticipantVisitResolverType resolverType = getSelectedParticipantVisitResolverType(provider, newRunForm);

        PlateSamplePropertyHelper helper = provider.getSamplePropertyHelper(newRunForm, resolverType);
        try
        {
            helper.addSampleColumns(view, newRunForm.getUser(), newRunForm, errorReshow);

            Map<String, Object> propNameToValue = new HashMap<>();
            for (String name : helper.getSampleNames())
                propNameToValue.put(name, name);

            addDefaultValues(view, helper, ElispotAssayProvider.PARTICIPANTID_PROPERTY_NAME, propNameToValue);
        }
        catch (ExperimentException e)
        {
            errors.addError(new ObjectError("main", null, null, e.toString()));
        }

        return view;
    }

    /**
     * Helper to populate the default values for sample groups if they don't have any existing values.
     *
     * @param helper - a sample property helper to pull sample names from
     * @param propName - the name of the property to set default values for
     * @param propertyNamesToValue - a map of sample group names to default values
     */
    private void addDefaultValues(InsertView view, SamplePropertyHelper<String> helper, String propName,
                                  Map<String, Object> propertyNamesToValue)
    {
        DomainProperty prop = null;

        // find the property we want to check default values for
        for (DomainProperty dp : helper.getDomainProperties())
        {
            if (dp.getName().equals(propName))
            {
                prop = dp;
                break;
            }
        }

        if (prop != null)
        {
            // we only set the default value for props whose default value type is: LAST ENTERED

            if (prop.getDefaultValueTypeEnum() == DefaultValueType.LAST_ENTERED)
            {
                Map<String, Object> initialValues = view.getInitialValues();
                for (Map.Entry<String, Object> entry : propertyNamesToValue.entrySet())
                {
                    String inputName = UploadWizardAction.getInputName(prop, entry.getKey());
                    Object value = initialValues.get(inputName);
                    if (value == null)
                    {
                        view.setInitialValue(inputName, entry.getValue());
                    }
                }
            }
        }
    }

    public PlateAntigenPropertyHelper createAntigenPropertyHelper(Container container, ExpProtocol protocol, ElispotAssayProvider provider)
    {
        PlateTemplate template = provider.getPlateTemplate(container, protocol);
        Set<DomainProperty> domainProperties = new LinkedHashSet<>();
        Domain domain = provider.getAntigenWellGroupDomain(protocol);
        domainProperties.add(domain.getPropertyByName(ElispotAssayProvider.ANTIGENNAME_PROPERTY_NAME));
        for (DomainProperty domainProperty : domain.getProperties())
        {
            if (!domainProperty.isHidden())
                domainProperties.add(domainProperty);
        }
        return new PlateAntigenPropertyHelper(new ArrayList<>(domainProperties), template);
    }

    public PlateAnalytePropertyHelper createAnalytePropertyHelper(ElispotRunUploadForm form) throws ExperimentException
    {
        return new PlateAnalytePropertyHelper(form, form.getProvider().getAnalyteDomain(form.getProtocol()).getProperties());
    }

    protected void addRunActionButtons(ElispotRunUploadForm newRunForm, InsertView insertView, ButtonBar bbar)
    {
        Domain antigenDomain = AbstractAssayProvider.getDomainByPrefix(_protocol, ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
        List<? extends DomainProperty> antigenColumns = antigenDomain.getProperties();
        if (antigenColumns.isEmpty())
        {
            super.addRunActionButtons(newRunForm, insertView, bbar);
        }
        else
        {
            addNextButton(bbar);
            addResetButton(newRunForm, insertView, bbar);
        }
    }

    private ModelAndView getAnalyteView(ElispotRunUploadForm form, boolean errorReshow, BindException errors) throws ExperimentException
    {
        InsertView view = createInsertView(ExperimentService.get().getTinfoExperimentRun(),
                "lsid", Collections.emptyList(), errorReshow, AnalyteStepHandler.NAME, form, errors);

        PlateAnalytePropertyHelper analyteHelper = createAnalytePropertyHelper(form);
        analyteHelper.addSampleColumns(view, form.getUser(), form, errorReshow);

        // add existing page properties
        addHiddenBatchProperties(form, view);
        addHiddenRunProperties(form, view);

        ElispotAssayProvider provider = form.getProvider();
        PlateSamplePropertyHelper helper = provider.getSamplePropertyHelper(form, getSelectedParticipantVisitResolverType(provider, form));
        for (Map.Entry<String, Map<DomainProperty, String>> sampleEntry : helper.getPostedPropertyValues(form.getRequest()).entrySet())
            addHiddenProperties(sampleEntry.getValue(), view, sampleEntry.getKey());

        PreviouslyUploadedDataCollector collector = new PreviouslyUploadedDataCollector(form.getUploadedData(), PreviouslyUploadedDataCollector.Type.PassThrough);
        collector.addHiddenFormFields(view, form);

        ParticipantVisitResolverType resolverType = getSelectedParticipantVisitResolverType(form.getProvider(), form);
        resolverType.addHiddenFormFields(form, view);

        // add any existing antigen properties
        PlateAntigenPropertyHelper antigenPropertyHelper = createAntigenPropertyHelper(form.getContainer(), form.getProtocol(), form.getProvider());
        for (Map.Entry<String, Map<DomainProperty, String>> sampleEntry : antigenPropertyHelper.getPostedPropertyValues(form.getRequest()).entrySet())
            addHiddenProperties(sampleEntry.getValue(), view, sampleEntry.getKey());

        ButtonBar bbar = new ButtonBar();
        addFinishButtons(form, view, bbar);
        addResetButton(form, view, bbar);

        ActionButton cancelButton = new ActionButton("Cancel", getSummaryLink(_protocol));
        bbar.add(cancelButton);

        _stepDescription = "Analyte Properties";

        view.getDataRegion().setHorizontalGroups(false);
        view.getDataRegion().setButtonBar(bbar, DataRegion.MODE_INSERT);
        return view;
    }

    private ModelAndView getAntigenView(ElispotRunUploadForm form, boolean errorReshow, BindException errors, boolean isLastView) throws ExperimentException
    {
        InsertView view = createInsertView(ExperimentService.get().getTinfoExperimentRun(),
                "lsid", Collections.emptyList(), errorReshow, AntigenStepHandler.NAME, form, errors);

        PlateAntigenPropertyHelper antigenHelper = createAntigenPropertyHelper(form.getContainer(), form.getProtocol(), form.getProvider());
        antigenHelper.addSampleColumns(view, form.getUser(), form, errorReshow);

        Map<String, Object> propNameToValue = new HashMap<>();
        for (String name : antigenHelper.getSampleNames())
            propNameToValue.put(name, name);

        addDefaultValues(view, antigenHelper, ElispotAssayProvider.ANTIGENNAME_PROPERTY_NAME, propNameToValue);

        // add existing page properties
        addHiddenBatchProperties(form, view);
        addHiddenRunProperties(form, view);

        ElispotAssayProvider provider = form.getProvider();
        PlateSamplePropertyHelper helper = provider.getSamplePropertyHelper(form, getSelectedParticipantVisitResolverType(provider, form));
        for (Map.Entry<String, Map<DomainProperty, String>> sampleEntry : helper.getPostedPropertyValues(form.getRequest()).entrySet())
            addHiddenProperties(sampleEntry.getValue(), view, sampleEntry.getKey());

        PreviouslyUploadedDataCollector collector = new PreviouslyUploadedDataCollector(form.getUploadedData(), PreviouslyUploadedDataCollector.Type.PassThrough);
        collector.addHiddenFormFields(view, form);

        ParticipantVisitResolverType resolverType = getSelectedParticipantVisitResolverType(form.getProvider(), form);
        resolverType.addHiddenFormFields(form, view);

        ButtonBar bbar = new ButtonBar();
        if (isLastView)
            addFinishButtons(form, view, bbar);
        addResetButton(form, view, bbar);
        if (!isLastView)
            addNextButton(bbar);

        ActionButton cancelButton = new ActionButton("Cancel", getSummaryLink(_protocol));
        bbar.add(cancelButton);

        _stepDescription = "Antigen Properties";

        view.getDataRegion().setHorizontalGroups(false);
        view.getDataRegion().setButtonBar(bbar, DataRegion.MODE_INSERT);

        return view;
    }

    protected ElispotRunStepHandler getRunStepHandler()
    {
        return new ElispotRunStepHandler();
    }

    protected class ElispotRunStepHandler extends RunStepHandler
    {
        private Map<String, Map<DomainProperty, String>> _postedSampleProperties = null;

        @Override
        protected boolean validatePost(ElispotRunUploadForm form, BindException errors) throws ExperimentException
        {
            boolean runPropsValid = super.validatePost(form, errors);
            boolean samplePropsValid = true;

            if (runPropsValid)
            {
                try {
                    form.getUploadedData();
                    ElispotAssayProvider provider = form.getProvider();

                    PlateTemplate template = provider.getPlateTemplate(getContainer(), form.getProtocol());
                    if (template == null)
                    {
                        errors.reject(SpringActionController.ERROR_MSG, "The template for this assay is either missing or invalid.");
                        return false;
                    }
                    PlateSamplePropertyHelper helper = provider.getSamplePropertyHelper(form, getSelectedParticipantVisitResolverType(provider, form));
                    _postedSampleProperties = helper.getPostedPropertyValues(form.getRequest());
                    for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedSampleProperties.entrySet())
                    {
                        // if samplePropsValid flips to false, we want to leave it false (via the "&&" below).  We don't
                        // short-circuit the loop because we want to run through all samples every time, so all errors can be reported.
                        samplePropsValid = validatePostedProperties(getViewContext(), entry.getValue(), errors) && samplePropsValid;
                    }
                }
                catch (ExperimentException e)
                {
                    errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
                    return false;
                }
            }
            return runPropsValid && samplePropsValid;
        }

        @Override
        protected ModelAndView handleSuccessfulPost(ElispotRunUploadForm form, BindException errors) throws ExperimentException
        {
            form.setSampleProperties(_postedSampleProperties);
            for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedSampleProperties.entrySet())
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

            Domain antigenDomain = AbstractAssayProvider.getDomainByPrefix(_protocol, ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
            List<? extends DomainProperty> antigenColumns = antigenDomain.getProperties();
            if (antigenColumns.isEmpty())
            {
                return super.handleSuccessfulPost(form, errors);
            }
            // NOTE: is there ever the case that we need to go past antigen view to analyte view???

            String detectionMethod = form.getProvider().getSelectedDetectionMethod(form.getContainer(), form.getProtocol());
            return getAntigenView(form, false, errors, (detectionMethod == null || detectionMethod.equals(ElispotAssayProvider.DetectionMethodType.COLORIMETRIC.getLabel())) );
        }
    }

    public class AntigenStepHandler extends ElispotStepHandler
    {
        public static final String NAME = "ANTIGEN";
        private Map<String, Map<DomainProperty, String>> _postedAntigenProperties = null;

        public ModelAndView handleStep(ElispotRunUploadForm form, BindException errors) throws ServletException, SQLException, ExperimentException
        {
            if (!form.isResetDefaultValues() && validatePost(form, errors))
                return handleSuccessfulPost(form, errors);

            String detectionMethod = form.getProvider().getSelectedDetectionMethod(form.getContainer(), form.getProtocol());
            return getAntigenView(form, true, errors, (detectionMethod == null || detectionMethod.equals(ElispotAssayProvider.DetectionMethodType.COLORIMETRIC.getLabel())) );
        }

        protected boolean validatePost(ElispotRunUploadForm form, BindException errors)
        {
            PlateAntigenPropertyHelper helper = createAntigenPropertyHelper(form.getContainer(), form.getProtocol(), form.getProvider());

            boolean antigenPropsValid = true;
            try
            {
                _postedAntigenProperties = helper.getPostedPropertyValues(form.getRequest());
            }
            catch (ExperimentException e)
            {
                errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
            }
            for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedAntigenProperties.entrySet())
            {
                // if samplePropsValid flips to false, we want to leave it false (via the "&&" below).  We don't
                // short-circuit the loop because we want to run through all samples every time, so all errors can be reported.
                antigenPropsValid = validatePostedProperties(getViewContext(), entry.getValue(), errors) && antigenPropsValid;
            }
            return antigenPropsValid;
        }

        protected ModelAndView handleSuccessfulPost(ElispotRunUploadForm form, BindException errors) throws ExperimentException
        {
            form.setAntigenProperties(_postedAntigenProperties);

            for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedAntigenProperties.entrySet())
                form.saveDefaultValues(entry.getValue(), entry.getKey());

            String detectionMethod = form.getProvider().getSelectedDetectionMethod(form.getContainer(), form.getProtocol());
            if (detectionMethod == null || detectionMethod.equals(ElispotAssayProvider.DetectionMethodType.COLORIMETRIC.getLabel()))
            {
                PlateSamplePropertyHelper helper = form.getProvider().getSamplePropertyHelper(form,
                        getSelectedParticipantVisitResolverType(form.getProvider(), form));
                form.setSampleProperties(helper.getPostedPropertyValues(form.getRequest()));

                ExpRun run = finishPost(form, errors);
                if (run != null)
                    return afterRunCreation(form, run, errors);

                return getAntigenView(form, true, errors, true);
            }

            return getAnalyteView(form, false, errors);
        }

        public String getName()
        {
            return NAME;
        }
    }

    public class AnalyteStepHandler extends ElispotStepHandler
    {
        public static final String NAME = "ANALYTE";
        private Map<String, Map<DomainProperty, String>> _postedAnalyteProperties = null;

        public ModelAndView handleStep(ElispotRunUploadForm form, BindException errors) throws ServletException, SQLException, ExperimentException
        {
            if (!form.isResetDefaultValues() && validatePost(form, errors))
                return handleSuccessfulPost(form, errors);

            return getAnalyteView(form, true, errors);
        }

        @Override
        protected boolean validatePost(ElispotRunUploadForm form, BindException errors) throws ExperimentException
        {
            PlateAnalytePropertyHelper helper = createAnalytePropertyHelper(form);

            boolean analytePropsValid = true;
            try
            {
                _postedAnalyteProperties = helper.getPostedPropertyValues(form.getRequest());
            }
            catch (ExperimentException e)
            {
                errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
            }
            for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedAnalyteProperties.entrySet())
            {
                analytePropsValid = validatePostedProperties(getViewContext(), entry.getValue(), errors) && analytePropsValid;
            }
            return analytePropsValid;
        }

        @Override
        protected ModelAndView handleSuccessfulPost(ElispotRunUploadForm form, BindException errors) throws ExperimentException
        {
            PlateSamplePropertyHelper helper = form.getProvider().getSamplePropertyHelper(form,
                    getSelectedParticipantVisitResolverType(form.getProvider(), form));
            form.setSampleProperties(helper.getPostedPropertyValues(form.getRequest()));

            PlateAntigenPropertyHelper antigenHelper = createAntigenPropertyHelper(form.getContainer(), form.getProtocol(), form.getProvider());
            form.setAntigenProperties(antigenHelper.getPostedPropertyValues(form.getRequest()));

            form.setAnalyteProperties(_postedAnalyteProperties);

            for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedAnalyteProperties.entrySet())
                form.saveDefaultValues(entry.getValue(), entry.getKey());

            ExpRun run = finishPost(form, errors);
            if (run != null)
                return afterRunCreation(form, run, errors);

            return getAnalyteView(form, true, errors);
        }

        public String getName()
        {
            return NAME;
        }
    }

    private abstract class ElispotStepHandler extends RunStepHandler
    {
        protected ExpRun finishPost(ElispotRunUploadForm form, BindException errors)
        {
            ElispotAssayProvider provider = form.getProvider();
            try (DbScope.Transaction transaction = ExperimentService.get().getSchema().getScope().ensureTransaction())
            {
                ExpRun run = saveExperimentRun(form);

                List<? extends ExpData> data = run.getOutputDatas(ExperimentService.get().getDataType(ElispotDataHandler.NAMESPACE));
                if (data.size() != 1)
                    throw new ExperimentException("Elispot should only upload a single file per run.");

                PlateTemplate template = provider.getPlateTemplate(form.getContainer(), form.getProtocol());
                Map<PlateInfo, Plate> plates = Collections.EMPTY_MAP;
                PlateReader reader = null;

                // populate property name to value map
                Map<String, String> runPropMap = new HashMap<>();
                for (Map.Entry<DomainProperty, String> entry : form.getRunProperties().entrySet())
                    runPropMap.put(entry.getKey().getName(), entry.getValue());

                if (runPropMap.containsKey(ElispotAssayProvider.READER_PROPERTY_NAME))
                {
                    reader = provider.getPlateReader(runPropMap.get(ElispotAssayProvider.READER_PROPERTY_NAME));
                    plates = ElispotDataHandler.initializePlates(form.getProtocol(), data.get(0).getFile(), template, reader);
                }

                boolean subtractBackground = NumberUtils.toInt(runPropMap.get(ElispotAssayProvider.BACKGROUND_WELL_PROPERTY_NAME), 0) > 0;
                Map<String, Object> postedPropMap = new HashMap<>();

                for (Map.Entry<String, Map<DomainProperty, String>> groupEntry : form.getAntigenProperties().entrySet())
                {
                    String groupName = groupEntry.getKey();
                    Map<DomainProperty, String> properties = groupEntry.getValue();
                    for (Map.Entry<DomainProperty, String> propEntry : properties.entrySet())
                        postedPropMap.put(getInputName(propEntry.getKey(), groupName), propEntry.getValue());
                }

                if (form.getAnalyteProperties() != null)
                {
                    for (Map.Entry<String, Map<DomainProperty, String>> groupEntry : form.getAnalyteProperties().entrySet())
                    {
                        String analyteName = groupEntry.getKey();
                        Map<DomainProperty, String> properties = groupEntry.getValue();
                        for (String value : properties.values())
                        {
                            postedPropMap.put(analyteName, value);
                        }
                    }
                }

                for (Map.Entry<PlateInfo, Plate> entry : plates.entrySet())
                {
                    Plate plate = entry.getValue();
                    if (plate != null)
                    {
                        ElispotDataHandler.populateAntigenDataProperties(run, plate, reader, postedPropMap);
                        ElispotDataHandler.populateAntigenRunProperties(run, plate, reader, postedPropMap, false, subtractBackground, false);
                        break;      // Only need to call these for 1 plate
                    }
                }
                transaction.commit();

                if (!errors.hasErrors())
                {
                    return run;
                }
            }
            catch (ValidationException ve)
            {
                for (ValidationError error : ve.getErrors())
                    errors.reject(SpringActionController.ERROR_MSG, PageFlowUtil.filter(error.getMessage()));
            }
            catch (ExperimentException e)
            {
                errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
            }

            return null;
        }
    }

    @Override
    protected boolean shouldShowDataCollectorUI(ElispotRunUploadForm newRunForm)
    {
        return true;
    }
}
