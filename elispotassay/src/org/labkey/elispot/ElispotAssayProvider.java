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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.PropertyStorageSpec;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainKind;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Lookup;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.qc.DataExchangeHandler;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayPipelineProvider;
import org.labkey.api.study.assay.AssayProviderSchema;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayTableMetadata;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.assay.DetectionMethodAssayProvider;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.ThawListResolverType;
import org.labkey.api.study.assay.plate.ExcelPlateReader;
import org.labkey.api.study.assay.plate.PlateReader;
import org.labkey.api.study.assay.plate.TextPlateReader;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.elispot.plate.AIDPlateReader;
import org.labkey.elispot.query.ElispotAntigenDomainKind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: Karl Lum
 * Date: Jan 7, 2008
 */
public class ElispotAssayProvider extends AbstractPlateBasedAssayProvider implements DetectionMethodAssayProvider
{
    public static final String NAME = "ELISpot";
    public static final String ASSAY_DOMAIN_ANTIGEN_WELLGROUP = ExpProtocol.ASSAY_DOMAIN_PREFIX + "AntigenWellGroup";
    public static final String ASSAY_DOMAIN_ANALYTE = ExpProtocol.ASSAY_DOMAIN_PREFIX + "Analyte";

    // run properties
    public static final String READER_PROPERTY_NAME = "PlateReader";
    public static final String READER_PROPERTY_CAPTION = "Plate Reader";
    public static final String BACKGROUND_WELL_PROPERTY_NAME = "SubtractBackground";
    public static final String BACKGROUND_WELL_PROPERTY_CAPTION = "Background Subtraction";

    // sample well groups
    public static final String SAMPLE_DESCRIPTION_PROPERTY_NAME = "SampleDescription";
    public static final String SAMPLE_DESCRIPTION_PROPERTY_CAPTION = "Sample Description";

    // antigen well groups
    public static final String CELLWELL_PROPERTY_NAME = "CellWell";
    public static final String CELLWELL_PROPERTY_CAPTION = "Cells per Well";
    public static final String ANTIGENID_PROPERTY_NAME = "AntigenID";
    public static final String ANTIGENID_PROPERTY_CAPTION = "Antigen ID";
    public static final String ANTIGENNAME_PROPERTY_NAME = "AntigenName";
    public static final String ANTIGENNAME_PROPERTY_CAPTION = "Antigen Name";

    enum PlateReaderType
    {
        CTL("Cellular Technology Ltd. (CTL)", ExcelPlateReader.class),
        AID("AID", AIDPlateReader.class),
        ZEISS("Zeiss", TextPlateReader.class);

        private String _label;
        private Class _class;

        private PlateReaderType(String label, Class cls)
        {
            _label = label;
            _class = cls;
        }

        public String getLabel()
        {
            return _label;
        }

        public PlateReader getInstance()
        {
            try
            {
                return (PlateReader)_class.newInstance();
            }
            catch (InstantiationException | IllegalAccessException x)
            {
                throw new RuntimeException(x);
            }
        }

        public static PlateReaderType fromLabel(String label)
        {
            for (PlateReaderType type : values())
            {
                if (type.getLabel().equals(label))
                    return type;
            }
            return null;
        }
    }

    public enum DetectionMethodType
    {
        COLORIMETRIC("colorimetric"),
        FLUORESCENT("fluorescent");

        private String _label;

        private DetectionMethodType(String label)
        {
            _label = label;
        }

        public String getLabel()
        {
            return _label;
        }

        public static DetectionMethodType fromLabel(String label)
        {
            for (DetectionMethodType type : values())
            {
                if (type.getLabel().equals(label))
                    return type;
            }
            return null;
        }
    }

    public ElispotAssayProvider()
    {
        super("ElispotAssayProtocol", "ElispotAssayRun", (AssayDataType) ExperimentService.get().getDataType(ElispotDataHandler.NAMESPACE), ModuleLoader.getInstance().getModule(ElispotModule.class));
    }

    public ExpData getDataForDataRow(Object dataRowId, ExpProtocol protocol)
    {
        if (!(dataRowId instanceof Integer))
        {
            return null;
        }

        RunDataRow dataRow = ElispotManager.get().getRunDataRow((Integer) dataRowId);
        if (dataRow == null)
        {
            throw new IllegalStateException("No ELISpot run data row was found with RowId: " + dataRowId);
        }

        ExpRun expRun = ExperimentService.get().getExpRun(dataRow.getRunId());
        List<ExpData> dataOutputs = expRun.getDataOutputs();
        for (ExpData dataOutput : dataOutputs)
        {
            if (ElispotDataHandler.NAMESPACE.equals(dataOutput.getLSIDNamespacePrefix()))
            {
                return dataOutput;
            }
        }
        throw new IllegalStateException("No ELISpot Experiment data row was found for RunId: " + dataRow.getRunId());
    }

    public String getName()
    {
        return NAME;
    }

    @NotNull
    @Override
    public AssayTableMetadata getTableMetadata(@NotNull ExpProtocol protocol)
    {
        return new AssayTableMetadata(
                this,
                protocol,
                FieldKey.fromParts(ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY, "Property"),
                FieldKey.fromParts("Run"),
                FieldKey.fromParts("RowId"));
    }

    public Domain getResultsDomain(ExpProtocol protocol)
    {
        return null;
    }

    public List<Pair<Domain, Map<DomainProperty, Object>>> createDefaultDomains(Container c, User user)
    {
        List<Pair<Domain, Map<DomainProperty, Object>>> result = super.createDefaultDomains(c, user);
        result.add(createAntigenWellGroupDomain(c));
        result.add(createAnalyteDomain(c));

        return result;
    }
    
    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("The data file is the output file from the plate reader that has been selected.");
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createSampleWellGroupDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result = super.createSampleWellGroupDomain(c, user);

        Domain domain = result.getKey();
        addProperty(domain, SPECIMENID_PROPERTY_NAME, SPECIMENID_PROPERTY_CAPTION, PropertyType.STRING);
        addProperty(domain, PARTICIPANTID_PROPERTY_NAME, PARTICIPANTID_PROPERTY_CAPTION, PropertyType.STRING);
        addProperty(domain, VISITID_PROPERTY_NAME, VISITID_PROPERTY_CAPTION, PropertyType.DOUBLE);
        addProperty(domain, DATE_PROPERTY_NAME, DATE_PROPERTY_CAPTION, PropertyType.DATE_TIME);
        addProperty(domain, SAMPLE_DESCRIPTION_PROPERTY_NAME, SAMPLE_DESCRIPTION_PROPERTY_CAPTION, PropertyType.STRING);

        return result;
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createAntigenWellGroupDomain(Container c)
    {
        String domainLsid = getPresubstitutionLsid(ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
        Domain antigenWellGroupDomain = PropertyService.get().createDomain(c, domainLsid, "Antigen Fields");
        antigenWellGroupDomain.setDescription("The user will be prompted to enter these properties for each of the antigen well groups in their chosen plate template.");

        // Add properties for all required fields
        DomainKind domainKind = PropertyService.get().getDomainKindByName(ElispotAntigenDomainKind.KINDNAME);
        for (PropertyStorageSpec propSpec : domainKind.getBaseProperties(null))
        {
            DomainProperty prop = antigenWellGroupDomain.addProperty(propSpec);
            prop.setShownInInsertView(false);
            prop.setShownInUpdateView(false);
            prop.setShownInDetailsView(false);
            prop.setHidden(true);
            prop.setPropertyURI(null);      // Issue 29277; URI here, which addProperty created, is presubstitution and does not later get substituted
        }

        addProperty(antigenWellGroupDomain, ANTIGENID_PROPERTY_NAME, ANTIGENID_PROPERTY_CAPTION, PropertyType.INTEGER);
        addProperty(antigenWellGroupDomain, CELLWELL_PROPERTY_NAME, CELLWELL_PROPERTY_CAPTION, PropertyType.INTEGER);

        return new Pair<>(antigenWellGroupDomain, Collections.emptyMap());
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createAnalyteDomain(Container c)
    {
        String domainLsid = getPresubstitutionLsid(ASSAY_DOMAIN_ANALYTE);
        Domain analyteWellGroupDomain = PropertyService.get().createDomain(c, domainLsid, "Analyte Fields");

        analyteWellGroupDomain.setDescription("The user will be prompted to enter these properties for each of the analyte well groups");
        addProperty(analyteWellGroupDomain, "CytokineName", "Cytokine Name", PropertyType.STRING);

        return new Pair<>(analyteWellGroupDomain, Collections.emptyMap());
    }

    protected Pair<Domain,Map<DomainProperty,Object>> createRunDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result =  super.createRunDomain(c, user);
        Domain runDomain = result.getKey();

        addProperty(runDomain, "ProtocolName", "ProtocolName", PropertyType.STRING);
        addProperty(runDomain, "LabID", "Lab ID", PropertyType.STRING);
        addProperty(runDomain, "PlateID", "Plate ID", PropertyType.STRING);
        addProperty(runDomain, "TemplateID", "Template ID", PropertyType.STRING);
        addProperty(runDomain, "ExperimentDate", "Experiment Date", PropertyType.DATE_TIME);
        addProperty(runDomain, BACKGROUND_WELL_PROPERTY_NAME, BACKGROUND_WELL_PROPERTY_CAPTION, PropertyType.BOOLEAN);

        Container lookupContainer = c.getProject();
        DomainProperty reader = addProperty(runDomain, READER_PROPERTY_NAME, READER_PROPERTY_CAPTION, PropertyType.STRING);
        reader.setLookup(new Lookup(lookupContainer, AssaySchema.NAME + "." + getResourceName(), ElispotProviderSchema.ELISPOT_PLATE_READER_TABLE));
        reader.setRequired(true);
        reader.setShownInUpdateView(false);

        return result;
    }

    protected Map<String, Set<String>> getRequiredDomainProperties()
    {
        Map<String, Set<String>> domainMap = super.getRequiredDomainProperties();
        Set<String> runProperties = domainMap.get(ExpProtocol.ASSAY_DOMAIN_RUN);
        if (runProperties == null)
        {
            runProperties = new HashSet<>();
            domainMap.put(ExpProtocol.ASSAY_DOMAIN_RUN, runProperties);
        }
        runProperties.add(BACKGROUND_WELL_PROPERTY_NAME);
        runProperties.add(READER_PROPERTY_NAME);

        Set<String> requiredAntigenProps = new HashSet<>();
        for (PropertyStorageSpec propSpec : PropertyService.get().getDomainKindByName(ElispotAntigenDomainKind.KINDNAME).getBaseProperties(null))
            requiredAntigenProps.add(propSpec.getName());
        domainMap.put(ASSAY_DOMAIN_ANTIGEN_WELLGROUP, requiredAntigenProps);
        return domainMap;
    }

    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Arrays.asList(new ParticipantVisitLookupResolverType(), new SpecimenIDLookupResolverType(), new ParticipantDateLookupResolverType(), new ThawListResolverType());
    }

    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, ElispotUploadWizardAction.class);
    }

    public Domain getAntigenWellGroupDomain(ExpProtocol protocol)
    {
        return getDomainByPrefix(protocol, ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
    }

    public Domain getAnalyteDomain(ExpProtocol protocol)
    {
        return getDomainByPrefix(protocol, ASSAY_DOMAIN_ANALYTE);
    }

    public String getDescription()
    {
        return "Imports raw data files from CTL and AID instruments.";
    }

    @Override
    public DataExchangeHandler createDataExchangeHandler()
    {
        return new ElispotDataExchangeHandler();
    }

    @Override
    public AssayProviderSchema createProviderSchema(User user, Container container, Container targetStudy)
    {
        return new ElispotProviderSchema(user, container, this, targetStudy);
    }

    @Override
    public ElispotProtocolSchema createProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return new ElispotProtocolSchema(user, container, this, protocol, targetStudy);
    }

    public PipelineProvider getPipelineProvider()
    {
        return new AssayPipelineProvider(ElispotModule.class,
                new PipelineProvider.FileTypesEntryFilter(
                        ((AssayDataType) ExperimentService.get().getDataType(ElispotDataHandler.NAMESPACE)).getFileType()),
                this, "Import ELISpot");
    }

    @Override
    public PlateReader getPlateReader(String readerName)
    {
        PlateReaderType type = PlateReaderType.fromLabel(readerName);
        if (type != null)
            return type.getInstance();
        else
            return super.getPlateReader(readerName);
    }

    @Override
    public void setSelectedDetectionMethod(Container container, ExpProtocol protocol, String method)
    {
        // consider a detectionMethod bit (look at AbstractPlateBasedASsayProvider setPlateTemplate)
        Map<String, ObjectProperty> props = new HashMap<>(protocol.getObjectProperties());
        ObjectProperty prop = new ObjectProperty(protocol.getLSID(), protocol.getContainer(),
                protocol.getLSID() + "#SelectedDetectionMethod", method);
        props.put(prop.getPropertyURI(), prop);
        protocol.setObjectProperties(props);
    }

    @Override
    public String getSelectedDetectionMethod(Container container, ExpProtocol protocol)
    {
        ObjectProperty prop = protocol.getObjectProperties().get(protocol.getLSID() + "#SelectedDetectionMethod");
        return prop != null ? prop.getStringValue() : null;
    }

    @Nullable
    public DetectionMethodType getDetectionMethod(Container container, ExpProtocol protocol)
    {
        String method = getSelectedDetectionMethod(container, protocol);
        if (method != null)
        {
            return DetectionMethodType.fromLabel(method);
        }
        return DetectionMethodType.COLORIMETRIC;
    }

    @Override
    public List<String> getAvailableDetectionMethods()
    {
        List<String> methods = new ArrayList<>();
        for (DetectionMethodType type : DetectionMethodType.values())
        {
            methods.add(type.getLabel());
        }
        return methods;
    }
}
