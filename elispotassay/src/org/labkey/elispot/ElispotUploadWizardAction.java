/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;
import org.labkey.api.study.assay.PreviouslyUploadedDataCollector;
import org.labkey.api.study.assay.plate.PlateReader;
import org.labkey.api.study.assay.plate.PlateReaderService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.InsertView;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Karl Lum
 * Date: Jan 9, 2008
 */

@RequiresPermissionClass(InsertPermission.class)
public class ElispotUploadWizardAction extends UploadWizardAction<ElispotRunUploadForm, ElispotAssayProvider>
{
    public ElispotUploadWizardAction()
    {
        super(ElispotRunUploadForm.class);
        addStepHandler(new AntigenStepHandler());
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
        return new PlateAntigenPropertyHelper(provider.getAntigenWellGroupDomain(protocol).getProperties(), template);
    }

    protected void addRunActionButtons(ElispotRunUploadForm newRunForm, InsertView insertView, ButtonBar bbar)
    {
        Domain antigenDomain = AbstractAssayProvider.getDomainByPrefix(_protocol, ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
        DomainProperty[] antigenColumns = antigenDomain.getProperties();
        if (antigenColumns.length == 0)
        {
            super.addRunActionButtons(newRunForm, insertView, bbar);
        }
        else
        {
            addNextButton(bbar);
            addResetButton(newRunForm, insertView, bbar);
        }
    }

    private ModelAndView getAntigenView(ElispotRunUploadForm form, boolean errorReshow, BindException errors) throws ServletException
    {
        InsertView view = createInsertView(ExperimentService.get().getTinfoExperimentRun(),
                "lsid", new DomainProperty[0], errorReshow, AntigenStepHandler.NAME, form, errors);

        try {
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
            addFinishButtons(form, view, bbar);
            addResetButton(form, view, bbar);
            //addNextButton(bbar);

            ActionButton cancelButton = new ActionButton("Cancel", getSummaryLink(_protocol));
            bbar.add(cancelButton);

            _stepDescription = "Antigen Properties";

            view.getDataRegion().setHorizontalGroups(false);
            view.getDataRegion().setButtonBar(bbar, DataRegion.MODE_INSERT);
        }
        catch (ExperimentException e)
        {
            throw new ServletException(e);
        }
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
                        samplePropsValid = validatePostedProperties(entry.getValue(), errors) && samplePropsValid;
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
        protected ModelAndView handleSuccessfulPost(ElispotRunUploadForm form, BindException errors) throws SQLException, ServletException, ExperimentException
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
            DomainProperty[] antigenColumns = antigenDomain.getProperties();
            if (antigenColumns.length == 0)
            {
                return super.handleSuccessfulPost(form, errors);
            }
            else
            {
                return getAntigenView(form, false, errors);
            }
        }
    }

    public class AntigenStepHandler extends RunStepHandler
    {
        public static final String NAME = "ANTIGEN";
        private Map<String, Map<DomainProperty, String>> _postedAntigenProperties = null;

        public ModelAndView handleStep(ElispotRunUploadForm form, BindException errors) throws ServletException, SQLException, ExperimentException
        {
            if (!form.isResetDefaultValues() && validatePost(form, errors))
                return handleSuccessfulPost(form, errors);
            else
                return getAntigenView(form, true, errors);

            //return getPlateSummary(form, false);
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
                antigenPropsValid = validatePostedProperties(entry.getValue(), errors) && antigenPropsValid;
            }
            return antigenPropsValid;
        }

        protected ModelAndView handleSuccessfulPost(ElispotRunUploadForm form, BindException errors) throws SQLException, ServletException, ExperimentException
        {
            PlateSamplePropertyHelper helper = form.getProvider().getSamplePropertyHelper(form,
                    getSelectedParticipantVisitResolverType(form.getProvider(), form));
            form.setSampleProperties(helper.getPostedPropertyValues(form.getRequest()));

            form.setAntigenProperties(_postedAntigenProperties);
            ElispotAssayProvider provider = form.getProvider();
            try (DbScope.Transaction transaction = ExperimentService.get().getSchema().getScope().ensureTransaction())
            {
                ExpRun run = saveExperimentRun(form);

                for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedAntigenProperties.entrySet())
                form.saveDefaultValues(entry.getValue(), entry.getKey());

                ExpData[] data = run.getOutputDatas(ExperimentService.get().getDataType(ElispotDataHandler.NAMESPACE));
                if (data.length != 1)
                    throw new ExperimentException("Elispot should only upload a single file per run.");

                PlateTemplate template = provider.getPlateTemplate(form.getContainer(), form.getProtocol());
                Plate plate = null;

                // populate property name to value map
                Map<String, String> runPropMap = new HashMap<>();
                for (Map.Entry<DomainProperty, String> entry : form.getRunProperties().entrySet())
                    runPropMap.put(entry.getKey().getName(), entry.getValue());

                if (runPropMap.containsKey(ElispotAssayProvider.READER_PROPERTY_NAME))
                {
                    PlateReader reader = PlateReaderService.getPlateReaderFromName(runPropMap.get(ElispotAssayProvider.READER_PROPERTY_NAME), form.getUser(), form.getContainer(), provider);
                    plate = ElispotDataHandler.initializePlate(data[0].getFile(), template, reader);
                }

                boolean subtractBackground = NumberUtils.toInt(runPropMap.get(ElispotAssayProvider.BACKGROUND_WELL_PROPERTY_NAME), 0) > 0;
                Map<String, Object> postedPropMap = new HashMap<>();

                for (Map.Entry<String, Map<DomainProperty, String>> groupEntry : _postedAntigenProperties.entrySet())
                {
                    String groupName = groupEntry.getKey();
                    Map<DomainProperty, String> properties = groupEntry.getValue();
                    for (Map.Entry<DomainProperty, String> propEntry : properties.entrySet())
                        postedPropMap.put(getInputName(propEntry.getKey(), groupName), propEntry.getValue());
                }

                if (plate != null)
                {
                    ElispotDataHandler.populateAntigenDataProperties(run, plate, postedPropMap, false, subtractBackground);
                    ElispotDataHandler.populateAntigenRunProperties(run, plate, postedPropMap, false, subtractBackground);
                }

                if (!errors.hasErrors())
                {
                    ModelAndView result = afterRunCreation(form, run, errors);
                    transaction.commit();
                    return result;
                }
                transaction.commit();
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

            return getAntigenView(form, true, errors);
        }

        public String getName()
        {
            return NAME;
        }
    }

    @Override
    protected boolean shouldShowDataCollectorUI(ElispotRunUploadForm newRunForm)
    {
        return true;
    }
}
