/*
 * Copyright (c) 2012-2016 LabKey Corporation
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

package org.labkey.elisa;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AbstractTsvAssayProvider;
import org.labkey.api.assay.AssayDataType;
import org.labkey.api.assay.AssayPipelineProvider;
import org.labkey.api.assay.AssayProtocolSchema;
import org.labkey.api.assay.AssayProviderSchema;
import org.labkey.api.assay.AssaySchema;
import org.labkey.api.assay.AssayTableMetadata;
import org.labkey.api.assay.AssayUrls;
import org.labkey.api.assay.actions.AssayRunUploadForm;
import org.labkey.api.assay.plate.AbstractPlateBasedAssayProvider;
import org.labkey.api.assay.plate.PlateBasedDataExchangeHandler;
import org.labkey.api.assay.plate.PlateReader;
import org.labkey.api.assay.plate.PlateSamplePropertyHelper;
import org.labkey.api.assay.plate.PlateTemplate;
import org.labkey.api.data.Container;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.IAssayDomainType;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Lookup;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.gwt.client.DefaultValueType;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.qc.DataExchangeHandler;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.User;
import org.labkey.api.settings.ExperimentalFeatureService;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.SampleMetadataInputFormat;
import org.labkey.api.study.assay.StudyParticipantVisitResolverType;
import org.labkey.api.study.assay.ThawListResolverType;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.UniqueID;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.api.visualization.GenericChartReport;
import org.labkey.elisa.actions.ElisaRunUploadForm;
import org.labkey.elisa.actions.ElisaUploadWizardAction;
import org.labkey.elisa.plate.BioTekPlateReader;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.labkey.api.exp.api.ExpProtocol.ASSAY_DOMAIN_DATA;
import static org.labkey.api.exp.api.ExpProtocol.ASSAY_DOMAIN_RUN;
import static org.labkey.elisa.ElisaModule.EXPERIMENTAL_MULTI_PLATE_SUPPORT;

/**
 * User: klum
 * Date: 10/6/12
 */
public class ElisaAssayProvider extends AbstractPlateBasedAssayProvider
{
    public static final String NAME = "ELISA";

    // run properties
    public static final String CORRELATION_COEFFICIENT_PROPERTY = "RSquared";
    public static final String CURVE_FIT_METHOD_PROPERTY = "CurveFitMethod";
    public static final String CURVE_FIT_PARAMETERS_PROPERTY = "CurveFitParams";
    public static final String CONCENTRATION_UNITS_PROPERTY = "ConcentrationUnits";

    // results properties
    public static final String ABSORBANCE_PROPERTY = "Absorption";
    public static final String MEAN_ABSORPTION_PROPERTY = "Absorption_Mean";
    public static final String CV_ABSORPTION_PROPERTY = "Absorption_CV";
    public static final String STANDARD_CONCENTRATION_PROPERTY = "Std_Concentration";
    public static final String CONCENTRATION_PROPERTY = "Concentration";
    public static final String MEAN_CONCENTRATION_PROPERTY = "Concentration_Mean";
    public static final String CV_CONCENTRATION_PROPERTY = "Concentration_CV";
    public static final String WELL_LOCATION_PROPERTY = "WellLocation";
    public static final String WELLGROUP_PROPERTY = "WellgroupLocation";
    public static final String SPOT_PROPERTY = "Spot";
    public static final String DILUTION_PROPERTY = "Dilution";
    public static final String EXCLUDED_PROPERTY = "Excluded";
    public static final String PERCENT_RECOVERY = "Recovery_Percent";
    public static final String PERCENT_RECOVERY_MEAN = "Recovery_Mean";
    public static final String PLATE_PROPERTY = "PlateName";
    public static final String CONTROL_ID_PROPERTY = "ControlId";

    // sample properties
    public static final String AUC_PROPERTY = "AUC";
    public static final Set<String> EXTRA_SAMPLE_PROPS;

    public static Set<String> REQUIRED_RESULT_PROPERTIES;

    static
    {
        EXTRA_SAMPLE_PROPS = Set.of(AUC_PROPERTY);

        REQUIRED_RESULT_PROPERTIES = Set.of(ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY,
                WELL_LOCATION_PROPERTY,
                WELLGROUP_PROPERTY,
                ABSORBANCE_PROPERTY,
                CONCENTRATION_PROPERTY,
                STANDARD_CONCENTRATION_PROPERTY,
                EXCLUDED_PROPERTY);
    }

    enum PlateReaderType
    {
        BIOTEK(BioTekPlateReader.LABEL, BioTekPlateReader.class);

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

    public ElisaAssayProvider()
    {
        super("ElisaAssayProtocol", "ElisaAssayRun", "Elisa" + RESULT_LSID_PREFIX_PART, (AssayDataType) ExperimentService.get().getDataType(ElisaDataHandler.NAMESPACE), ModuleLoader.getInstance().getModule(ElisaModule.class));
    }

    @NotNull
    @Override
    public AssayTableMetadata getTableMetadata(@NotNull ExpProtocol protocol)
    {
        return new AssayTableMetadata(
                this,
                protocol,
                FieldKey.fromParts(ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY, "Property"),
                FieldKey.fromParts("Run"),
                FieldKey.fromParts(AbstractTsvAssayProvider.ROW_ID_COLUMN_NAME),
                AbstractTsvAssayProvider.ROW_ID_COLUMN_NAME,
                FieldKey.fromParts("LSID"));
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public List<Pair<Domain, Map<DomainProperty, Object>>> createDefaultDomains(Container c, User user)
    {
        List<Pair<Domain, Map<DomainProperty, Object>>> result = super.createDefaultDomains(c, user);

        Pair<Domain, Map<DomainProperty, Object>> resultDomain = createResultDomain(c, user);
        if (resultDomain != null)
            result.add(resultDomain);

        return result;
    }

    @Override
    protected Pair<Domain, Map<DomainProperty, Object>> createRunDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result = super.createRunDomain(c, user);
        Domain domain = result.getKey();

        addProperty(domain, CONCENTRATION_UNITS_PROPERTY, "Concentration Units", PropertyType.STRING, "Units eg. (ug/ml)");

        if (ExperimentalFeatureService.get().isFeatureEnabled(EXPERIMENTAL_MULTI_PLATE_SUPPORT))
        {
            Container lookupContainer = c.getProject();
            DomainProperty method = addProperty(domain, CURVE_FIT_METHOD_PROPERTY, "Curve Fit Method", PropertyType.STRING);
            method.setLookup(new Lookup(lookupContainer, AssaySchema.NAME + "." + getResourceName(), ElisaProviderSchema.CURVE_FIT_METHOD_TABLE_NAME));
            method.setRequired(true);
            method.setShownInUpdateView(false);
        }

        return result;
    }

    @Override
    protected Pair<Domain, Map<DomainProperty, Object>> createSampleWellGroupDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result = super.createSampleWellGroupDomain(c, user);
        Domain domain = result.getKey();

        DomainProperty specimenProperty = addProperty(domain, SPECIMENID_PROPERTY_NAME, SPECIMENID_PROPERTY_CAPTION, PropertyType.STRING);
        specimenProperty.setImportAliasSet(Set.of("Sample"));
        addProperty(domain, PARTICIPANTID_PROPERTY_NAME, PARTICIPANTID_PROPERTY_CAPTION, PropertyType.STRING);
        addProperty(domain, VISITID_PROPERTY_NAME, VISITID_PROPERTY_CAPTION, PropertyType.DOUBLE);
        addProperty(domain, DATE_PROPERTY_NAME, DATE_PROPERTY_CAPTION, PropertyType.DATE_TIME);

        // extra computed properties at the sample scope
        addPropertyWithFormat(domain, AUC_PROPERTY, AUC_PROPERTY, PropertyType.DOUBLE, "Area Under the Curve", "0.0000");

        return result;
    }

    protected Pair<Domain,Map<DomainProperty,Object>> createResultDomain(Container c, User user)
    {
        Domain dataDomain = PropertyService.get().createDomain(c, getPresubstitutionLsid(ASSAY_DOMAIN_DATA), "Data Fields");
        dataDomain.setDescription("Define the results fields for this assay design. The user is prompted for these fields for individual rows within the imported run, typically done as a file upload.");

        DomainProperty specimenLsid = addProperty(dataDomain, ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY, "Specimen", PropertyType.STRING, "Specimen Data Lookup");
        specimenLsid.setHidden(true);
        specimenLsid.setShownInInsertView(false);
        specimenLsid.setShownInDetailsView(false);
        specimenLsid.setShownInUpdateView(false);

        addProperty(dataDomain, WELL_LOCATION_PROPERTY, "Well Location", PropertyType.STRING, "Well location");
        addProperty(dataDomain, WELLGROUP_PROPERTY, "Well Group", PropertyType.STRING, "Replicate Well Group");

        addPropertyWithFormat(dataDomain, ABSORBANCE_PROPERTY,  "Absorption", PropertyType.DOUBLE, "Raw signal value", "0.000");
        DomainProperty concProp = addPropertyWithFormat(dataDomain, CONCENTRATION_PROPERTY,  "Concentration", PropertyType.DOUBLE, "Calculated concentration", "0.000");
        concProp.setDefaultValueTypeEnum(DefaultValueType.LAST_ENTERED);
        addPropertyWithFormat(dataDomain, STANDARD_CONCENTRATION_PROPERTY,  "Std Concentration", PropertyType.DOUBLE, "Standard control concentration", "0.000");

        addProperty(dataDomain, SPOT_PROPERTY, "Spot", PropertyType.INTEGER, "Spot or Analyte for the well");
        addProperty(dataDomain, PLATE_PROPERTY, "Plate Name", PropertyType.STRING, "Plate Name for multi-plate imports");
        addProperty(dataDomain, DILUTION_PROPERTY, "Dilution", PropertyType.DOUBLE, "Dilution for the well");
        addProperty(dataDomain, EXCLUDED_PROPERTY, "Excluded", PropertyType.BOOLEAN, null);
        addPropertyWithFormat(dataDomain, PERCENT_RECOVERY, "Percent Recovery", PropertyType.DOUBLE, "Ratio of computed concentration to specified", "0.000%");
        addPropertyWithFormat(dataDomain, PERCENT_RECOVERY_MEAN, "Percent Recovery Mean", PropertyType.DOUBLE, "Mean of recovery for the control", "0.000%");

        addPropertyWithFormat(dataDomain, MEAN_ABSORPTION_PROPERTY, "Absorption Mean", PropertyType.DOUBLE, "Mean Absorption across replicates", "0.000");
        addPropertyWithFormat(dataDomain, CV_ABSORPTION_PROPERTY, "Absorption CV", PropertyType.DOUBLE, "Absorption CV across replicates", "0.000");
        addPropertyWithFormat(dataDomain, MEAN_CONCENTRATION_PROPERTY, "Concentration Mean", PropertyType.DOUBLE, "Mean Concentration across replicates", "0.000");
        addPropertyWithFormat(dataDomain, CV_CONCENTRATION_PROPERTY, "Concentration CV", PropertyType.DOUBLE, "Concentration CV across replicates", "0.000");
        addProperty(dataDomain, CONTROL_ID_PROPERTY, "Control ID", PropertyType.STRING, "Control Well Identifier");

        return new Pair<>(dataDomain, Collections.emptyMap());
    }

    private DomainProperty addPropertyWithFormat(Domain domain, String name, String label, PropertyType type, String description, String format)
    {
        DomainProperty prop = addProperty(domain, name, label, type, description);
        prop.setFormat(format);

        return prop;
    }

    @Override
    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        if (form instanceof ElisaRunUploadForm)
        {
            if (((ElisaRunUploadForm)form).getSampleMetadataInputFormat() == SampleMetadataInputFormat.COMBINED)
                return new JspView<>("/org/labkey/assay/view/tsvDataDescription.jsp", form);
        }
        return new HtmlView(HtmlString.of("The ELISA data files must be in the BioTek Microplate Reader Excel file format (.xls or .xlsx extension)."));
    }

    @Override
    public AssayProviderSchema createProviderSchema(User user, Container container, Container targetStudy)
    {
        return new ElisaProviderSchema(user, container, this, targetStudy);
    }

    @Override
    public AssayProtocolSchema createProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return new ElisaProtocolSchema(user, container, this, protocol, targetStudy);
    }

    @Override
    public SampleMetadataInputFormat[] getSupportedMetadataInputFormats()
    {
        if (ExperimentalFeatureService.get().isFeatureEnabled(EXPERIMENTAL_MULTI_PLATE_SUPPORT))
            return new SampleMetadataInputFormat[]{SampleMetadataInputFormat.MANUAL, SampleMetadataInputFormat.COMBINED};
        else
            return new SampleMetadataInputFormat[]{SampleMetadataInputFormat.MANUAL};
    }

    @Override
    protected PlateSamplePropertyHelper createSampleFilePropertyHelper(Container c, ExpProtocol protocol, List<? extends DomainProperty> sampleProperties, PlateTemplate template, SampleMetadataInputFormat inputFormat)
    {
        // some of the sample properties are calculated versus collected
        List<DomainProperty> properties = sampleProperties.stream()
                .filter(dp -> !EXTRA_SAMPLE_PROPS.contains(dp.getName()))
                .collect(Collectors.toList());

        if (inputFormat == SampleMetadataInputFormat.MANUAL)
            return new PlateSamplePropertyHelper(properties, template);
        else
            return new ElisaSampleFilePropertyHelper(c, protocol, properties, template, inputFormat);
    }

    @Override
    protected Map<String, Set<String>> getRequiredDomainProperties()
    {
        Map<String, Set<String>> domainMap = super.getRequiredDomainProperties();

        domainMap.computeIfAbsent(ASSAY_DOMAIN_DATA, s -> new HashSet<>());
        domainMap.computeIfAbsent(ASSAY_DOMAIN_RUN, s -> new HashSet<>());
        domainMap.computeIfAbsent(ASSAY_DOMAIN_SAMPLE_WELLGROUP, s -> new HashSet<>());

        Set<String> resultProperties = domainMap.get(ASSAY_DOMAIN_DATA);
        resultProperties.addAll(REQUIRED_RESULT_PROPERTIES);

        domainMap.get(ASSAY_DOMAIN_SAMPLE_WELLGROUP).add(AUC_PROPERTY);

        return domainMap;
    }

    @Override
    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Arrays.asList(new StudyParticipantVisitResolverType(), new SpecimenIDLookupResolverType(), new ParticipantDateLookupResolverType(), new ThawListResolverType());
    }

    @Override
    public PipelineProvider getPipelineProvider()
    {
        return new AssayPipelineProvider(ElisaModule.class,
                new PipelineProvider.FileTypesEntryFilter(
                        ((AssayDataType) ExperimentService.get().getDataType(ElisaDataHandler.NAMESPACE)).getFileType()
                ),
                this, "Import ELISA");
    }

    @Override
    public String getDescription()
    {
        return "Imports raw data files from BioTek ELISA Microplate reader.";
    }

    @Override
    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, ElisaUploadWizardAction.class);
    }

    @Override
    public boolean hasCustomView(IAssayDomainType domainType, boolean details)
    {
        return ExpProtocol.AssayDomainTypes.Run == domainType && details;
    }

    @Override
    public ModelAndView createRunDetailsView(ViewContext context, ExpProtocol protocol, ExpRun run)
    {
        AssaySchema schema = createProtocolSchema(context.getUser(), context.getContainer(), protocol, null);

        ElisaController.RunDetailsForm form = new ElisaController.RunDetailsForm();
        form.setProtocolId(protocol.getRowId());
        form.setSchemaName(schema.getPath().toString());
        form.setRunId(run.getRowId());
        form.setRunName(run.getName());

        // fake params until we refactor the details view to query from the CurveFit table
        form.setFitParams(new Double[2]);

        return new JspView<>("/org/labkey/elisa/view/runDetailsView.jsp", form);
    }

    @Override
    public DataExchangeHandler createDataExchangeHandler()
    {
        return new PlateBasedDataExchangeHandler();
    }

    public Domain getConcentrationWellGroupDomain(ExpProtocol protocol)
    {
        return getDomainByPrefix(protocol, ASSAY_DOMAIN_DATA);
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
}
