/*
 * Copyright (c) 2008-2010 LabKey Corporation
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

import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.ValidationError;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.*;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.Position;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;
import org.labkey.api.study.assay.PreviouslyUploadedDataCollector;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.Stats;
import org.labkey.elispot.plate.ElispotPlateReaderService;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
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
        InsertView parent = super.createRunInsertView(newRunForm, errorReshow, errors);

        ElispotAssayProvider provider = (ElispotAssayProvider) getProvider(newRunForm);
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

            // add existing page properties
            addHiddenBatchProperties(form, view);
            addHiddenRunProperties(form, view);

            ElispotAssayProvider provider = (ElispotAssayProvider) getProvider(form);
            PlateSamplePropertyHelper helper = provider.getSamplePropertyHelper(form, getSelectedParticipantVisitResolverType(provider, form));
            for (Map.Entry<String, Map<DomainProperty, String>> sampleEntry : helper.getPostedPropertyValues(form.getRequest()).entrySet())
                addHiddenProperties(sampleEntry.getValue(), view, sampleEntry.getKey());

            PreviouslyUploadedDataCollector collector = new PreviouslyUploadedDataCollector(form.getUploadedData());
            collector.addHiddenFormFields(view, form);

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
                    ElispotAssayProvider provider = (ElispotAssayProvider) getProvider(form);

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
            try
            {
                PlateSamplePropertyHelper helper = form.getProvider().getSamplePropertyHelper(form, 
                        getSelectedParticipantVisitResolverType(form.getProvider(), form));
                form.setSampleProperties(helper.getPostedPropertyValues(form.getRequest()));

                form.setAntigenProperties(_postedAntigenProperties);
                ElispotAssayProvider provider = form.getProvider();
                ExpRun run = saveExperimentRun(form);

                for (Map.Entry<String, Map<DomainProperty, String>> entry : _postedAntigenProperties.entrySet())
                    form.saveDefaultValues(entry.getValue(), entry.getKey());

                ExperimentService.get().getSchema().getScope().beginTransaction();
                ExpData[] data = run.getOutputDatas(ElispotDataHandler.ELISPOT_DATA_TYPE);
                if (data.length != 1)
                    throw new ExperimentException("Elispot should only upload a single file per run.");

                PlateTemplate template = provider.getPlateTemplate(form.getContainer(), form.getProtocol());
                Plate plate = null;

                for (Map.Entry<DomainProperty, String> entry : form.getRunProperties().entrySet())
                {
                    if (ElispotAssayProvider.READER_PROPERTY_NAME.equals(entry.getKey().getName()))
                    {
                        ElispotPlateReaderService.I reader = ElispotPlateReaderService.getPlateReaderFromName(entry.getValue(), form.getContainer());
                        plate = ElispotDataHandler.initializePlate(data[0].getDataFile(), template, reader);
                        break;
                    }
                }

                Domain antigenDomain = AbstractAssayProvider.getDomainByPrefix(form.getProtocol(), ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
                DomainProperty[] antigenProps = antigenDomain.getProperties();
                Map<String, String> postedPropMap = new HashMap<String, String>();

                for (Map.Entry<String, Map<DomainProperty, String>> groupEntry : _postedAntigenProperties.entrySet())
                {
                    String groupName = groupEntry.getKey();
                    Map<DomainProperty, String> properties = groupEntry.getValue();
                    for (Map.Entry<DomainProperty, String> propEntry : properties.entrySet())
                        postedPropMap.put(getInputName(propEntry.getKey(), groupName), propEntry.getValue());
                }

                if (plate != null)
                {
                    // we want to 'collapse' all the antigen well groups to 'per well'
                    List<ObjectProperty> results = new ArrayList<ObjectProperty>();
                    for (WellGroup group : plate.getWellGroups(WellGroup.Type.ANTIGEN))
                    {
                        for (Position pos : group.getPositions())
                        {
                            results.clear();
                            Lsid dataRowLsid = ElispotDataHandler.getDataRowLsid(data[0].getLSID(), pos);

                            for (DomainProperty dp : antigenProps)
                            {
                                String key = getInputName(dp, group.getName());
                                if (postedPropMap.containsKey(key))
                                {
                                    ObjectProperty op = ElispotDataHandler.getResultObjectProperty(form.getContainer(),
                                            form.getProtocol(),
                                            dataRowLsid.toString(),
                                            dp.getName(),
                                            postedPropMap.get(key),
                                            dp.getPropertyDescriptor().getPropertyType(),
                                            dp.getPropertyDescriptor().getFormat());

                                    results.add(op);
                                }
                            }

                            if (!results.isEmpty())
                            {
                                // include the antigen wellgroup name
                                ObjectProperty op = ElispotDataHandler.getResultObjectProperty(form.getContainer(),
                                        form.getProtocol(), dataRowLsid.toString(),
                                        ElispotDataHandler.ANTIGEN_WELLGROUP_PROPERTY_NAME,
                                        group.getName(), PropertyType.STRING);
                                results.add(op);

                                OntologyManager.ensureObject(form.getContainer(), dataRowLsid.toString(),  data[0].getLSID());
                                OntologyManager.insertProperties(form.getContainer(), dataRowLsid.toString(), results.toArray(new ObjectProperty[results.size()]));
                            }
                        }
                    }

                    // calculate antigen statistics on a per sample basis
                    Map<String, ExpMaterial> materialMap = new HashMap<String, ExpMaterial>();

                    for (ExpMaterial material : run.getMaterialInputs().keySet())
                        materialMap.put(material.getName(), material);

                    // get the URI for the spot count
                    String spotCountURI = new Lsid(ElispotDataHandler.ELISPOT_PROPERTY_LSID_PREFIX,
                            form.getProtocol().getName(),
                            ElispotDataHandler.SFU_PROPERTY_NAME).toString();
                    DomainProperty antigenNameProp = antigenDomain.getPropertyByName(ElispotAssayProvider.ANTIGENNAME_PROPERTY_NAME);

                    for (WellGroup group : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                    {
                        ExpMaterial material = materialMap.get(group.getName());

                        if (material != null)
                        {
                            List<ObjectProperty> antigenResults = new ArrayList<ObjectProperty>();
                            Lsid rowLsid = ElispotDataHandler.getAntigenRowLsid(data[0].getLSID(), material.getName());

                            for (WellGroup antigenGroup : group.getOverlappingGroups(WellGroup.Type.ANTIGEN))
                            {
                                List<Position> positions = antigenGroup.getPositions();
                                double[] statsData = new double[positions.size()];
                                int i = 0;

                                for (Position pos : positions)
                                {
                                    if (group.contains(pos))
                                    {
                                        Lsid dataRowLsid = ElispotDataHandler.getDataRowLsid(data[0].getLSID(), pos);
                                        Map<String, ObjectProperty> props = OntologyManager.getPropertyObjects(getContainer(), dataRowLsid.toString());

                                        if (props.containsKey(spotCountURI))
                                        {
                                            ObjectProperty o = props.get(spotCountURI);

                                            double value = o.getFloatValue();

                                            if (isValid(value))
                                                statsData[i++] = value;
                                        }
                                    }
                                }
                                statsData = Arrays.copyOf(statsData, i);
                                Stats.DoubleStats stats = new Stats.DoubleStats(statsData);

                                String key = getInputName(antigenNameProp, antigenGroup.getName());
                                if (postedPropMap.containsKey(key))
                                {
                                    // for each antigen group, create two columns for mean and median values
                                    String antigenName = postedPropMap.get(key);

                                    ObjectProperty mean = ElispotDataHandler.getAntigenResultObjectProperty(form.getContainer(),
                                            form.getProtocol(),
                                            rowLsid.toString(),
                                            antigenName + "_Mean",
                                            stats.getMean(),
                                            PropertyType.DOUBLE, "0.0");
                                    ObjectProperty median = ElispotDataHandler.getAntigenResultObjectProperty(form.getContainer(),
                                            form.getProtocol(),
                                            rowLsid.toString(),
                                            antigenName + "_Median",
                                            stats.getMedian(),
                                            PropertyType.DOUBLE, "0.0");

                                    antigenResults.add(mean);
                                    antigenResults.add(median);
                                }
                            }

                            if (!antigenResults.isEmpty())
                            {
                                ObjectProperty sample = ElispotDataHandler.getAntigenResultObjectProperty(form.getContainer(),
                                        form.getProtocol(),
                                        rowLsid.toString(),
                                        ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY,
                                        material.getLSID(), PropertyType.STRING, null);
                                antigenResults.add(sample);

                                OntologyManager.ensureObject(form.getContainer(), rowLsid.toString(),  data[0].getLSID());
                                OntologyManager.insertProperties(form.getContainer(), rowLsid.toString(), antigenResults.toArray(new ObjectProperty[antigenResults.size()]));
                            }
                        }
                    }
                }
                ExperimentService.get().getSchema().getScope().commitTransaction();

                if (!errors.hasErrors())
                {
                    return afterRunCreation(form, run, errors);
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
            finally
            {
                ExperimentService.get().getSchema().getScope().closeConnection();
            }
            
            return getAntigenView(form, true, errors);
        }

        private boolean isValid(double value)
        {
            // negative sfu values are error codes
            return value >= 0;
        }

        public String getName()
        {
            return NAME;
        }
    }
}
