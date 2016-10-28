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
import org.labkey.api.data.Container;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.IAssayDomainType;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.gwt.client.DefaultValueType;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.qc.DataExchangeHandler;
import org.labkey.api.qc.PlateBasedDataExchangeHandler;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.User;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AbstractTsvAssayProvider;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayPipelineProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayTableMetadata;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.StudyParticipantVisitResolverType;
import org.labkey.api.study.assay.ThawListResolverType;
import org.labkey.api.study.assay.plate.ExcelPlateReader;
import org.labkey.api.study.assay.plate.PlateReader;
import org.labkey.api.study.assay.plate.TextPlateReader;
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

/**
 * User: klum
 * Date: 10/6/12
 */
public class ElisaAssayProvider extends AbstractPlateBasedAssayProvider
{
    public static final String NAME = "ELISA";
    public static final String ASSAY_DOMAIN_CONCENTRATION_WELLGROUP = ExpProtocol.ASSAY_DOMAIN_PREFIX + "ConcentrationWellGroup";

    public static final String ABSORBANCE_PROPERTY_NAME = "Absorption";
    public static final String ABSORBANCE_PROPERTY_CAPTION = "Absorption";

    public static final String CONCENTRATION_PROPERTY_NAME = "Concentration";
    public static final String CONCENTRATION_PROPERTY_CAPTION = "Concentration (ug/ml)";

    public static final String WELL_PROPERTY_NAME = "WellLocation";
    public static final String WELL_PROPERTY_CAPTION = "Well Location";

    public static final String WELLGROUP_PROPERTY_NAME = "WellgroupLocation";
    public static final String WELLGROUP_PROPERTY_CAPTION = "Well Group";

    public static final String CORRELATION_COEFFICIENT_PROPERTY_NAME = "RSquared";
    public static final String CORRELATION_COEFFICIENT_CAPTION = "Coefficient of Determination";

    public static final String CURVE_FIT_PARAMETERS = "CurveFitParams";
    public static final String CURVE_FIT_PARAMETERS_CAPTION = "Fit Parameters";

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
        super("ElisaAssayProtocol", "ElisaAssayRun", (AssayDataType) ExperimentService.get().getDataType(ElisaDataHandler.NAMESPACE), ModuleLoader.getInstance().getModule(ElisaModule.class));
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
                FieldKey.fromParts(AbstractTsvAssayProvider.ROW_ID_COLUMN_NAME));
    }

    @Override
    public String getName()
    {
        return NAME;
    }

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

        DomainProperty fitProp = addProperty(domain, CORRELATION_COEFFICIENT_PROPERTY_NAME, CORRELATION_COEFFICIENT_CAPTION, PropertyType.DOUBLE, "Coefficient of Determination of the calibration curve.");
        fitProp.setFormat("0.000");
        fitProp.setShownInInsertView(false);

        DomainProperty fitParams = addProperty(domain, CURVE_FIT_PARAMETERS, CURVE_FIT_PARAMETERS_CAPTION, PropertyType.STRING, "Curve fit parameters.");
        fitParams.setShownInInsertView(false);
        fitParams.setShownInDetailsView(false);
        fitParams.setShownInUpdateView(false);
        fitParams.setHidden(true);

        return result;
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createSampleWellGroupDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result = super.createSampleWellGroupDomain(c, user);
        Domain domain = result.getKey();

        addProperty(domain, SPECIMENID_PROPERTY_NAME, SPECIMENID_PROPERTY_CAPTION, PropertyType.STRING);
        addProperty(domain, PARTICIPANTID_PROPERTY_NAME, PARTICIPANTID_PROPERTY_CAPTION, PropertyType.STRING);
        addProperty(domain, VISITID_PROPERTY_NAME, VISITID_PROPERTY_CAPTION, PropertyType.DOUBLE);
        addProperty(domain, DATE_PROPERTY_NAME, DATE_PROPERTY_CAPTION, PropertyType.DATE_TIME);

        return result;
    }

    protected Pair<Domain,Map<DomainProperty,Object>> createResultDomain(Container c, User user)
    {
        Domain dataDomain = PropertyService.get().createDomain(c, "urn:lsid:" + XarContext.LSID_AUTHORITY_SUBSTITUTION + ":" + ExpProtocol.ASSAY_DOMAIN_DATA + ".Folder-" + XarContext.CONTAINER_ID_SUBSTITUTION + ":" + ASSAY_NAME_SUBSTITUTION, "Data Fields");
        dataDomain.setDescription("The user is prompted to enter data values for row of data associated with a run, typically done as uploading a file.  This is part of the second step of the upload process.");

        DomainProperty specimenLsid = addProperty(dataDomain, ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY, "Specimen", PropertyType.STRING, "Specimen Data Lookup");
        specimenLsid.setHidden(true);
        specimenLsid.setShownInInsertView(false);
        specimenLsid.setShownInDetailsView(false);
        specimenLsid.setShownInUpdateView(false);

        addProperty(dataDomain, WELL_PROPERTY_NAME, WELL_PROPERTY_CAPTION, PropertyType.STRING, "Well location.");
        addProperty(dataDomain, WELLGROUP_PROPERTY_NAME, WELLGROUP_PROPERTY_CAPTION, PropertyType.STRING, "Well Group location.");

        DomainProperty absProp = addProperty(dataDomain, ABSORBANCE_PROPERTY_NAME,  ABSORBANCE_PROPERTY_CAPTION, PropertyType.DOUBLE, "Well group value measuring the absorption.");
        absProp.setFormat("0.000");
        DomainProperty concProp = addProperty(dataDomain, CONCENTRATION_PROPERTY_NAME,  CONCENTRATION_PROPERTY_CAPTION, PropertyType.DOUBLE, "Well group value measuring the concentration.");
        concProp.setFormat("0.000");
        concProp.setDefaultValueTypeEnum(DefaultValueType.LAST_ENTERED);

        return new Pair<>(dataDomain, Collections.emptyMap());
    }

    @Override
    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("The ELISA data files must be in the BioTek Microplate Reader Excel file format (.xls or .xlsx extension).");
    }

    @Override
    public AssayProtocolSchema createProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return new ElisaProtocolSchema(user, container, this, protocol, targetStudy);
    }

    protected Map<String, Set<String>> getRequiredDomainProperties()
    {
        Map<String, Set<String>> domainMap = super.getRequiredDomainProperties();

        if (!domainMap.containsKey(ExpProtocol.ASSAY_DOMAIN_DATA))
            domainMap.put(ExpProtocol.ASSAY_DOMAIN_DATA, new HashSet<String>());

        if (!domainMap.containsKey(ExpProtocol.ASSAY_DOMAIN_RUN))
            domainMap.put(ExpProtocol.ASSAY_DOMAIN_RUN, new HashSet<String>());

        Set<String> runProperties = domainMap.get(ExpProtocol.ASSAY_DOMAIN_RUN);

        runProperties.add(CORRELATION_COEFFICIENT_PROPERTY_NAME);
        runProperties.add(CURVE_FIT_PARAMETERS);

        Set<String> dataProperties = domainMap.get(ExpProtocol.ASSAY_DOMAIN_DATA);

        dataProperties.add(ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY);
        dataProperties.add(WELL_PROPERTY_NAME);
        dataProperties.add(WELLGROUP_PROPERTY_NAME);
        dataProperties.add(ABSORBANCE_PROPERTY_NAME);
        dataProperties.add(CONCENTRATION_PROPERTY_NAME);

        return domainMap;
    }

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
        VBox view = new VBox();
        ElisaController.GenericReportForm form = new ElisaController.GenericReportForm();
        AssaySchema schema = createProtocolSchema(context.getUser(), context.getContainer(), protocol, null);

        form.setComponentId("generic-report-panel-" + UniqueID.getRequestScopedUID(context.getRequest()));
        form.setSchemaName(schema.getPath().toString());
        form.setQueryName(AssayProtocolSchema.DATA_TABLE_NAME);
        form.setRunTableName(AssayProtocolSchema.RUNS_TABLE_NAME);
        form.setRenderType(GenericChartReport.RenderType.SCATTER_PLOT.getId());
        form.setRunId(run.getRowId());
        form.setDataRegionName(QueryView.DATAREGIONNAME_DEFAULT);

        // setup the plot for the calibration curve (absorption vs concentration)
        form.setAutoColumnXName(CONCENTRATION_PROPERTY_NAME);
        form.setAutoColumnYName(ABSORBANCE_PROPERTY_NAME);

        Domain runDomain = getRunDomain(protocol);
        DomainProperty prop = runDomain.getPropertyByName(CURVE_FIT_PARAMETERS);

        Domain sampleDomain = getSampleWellGroupDomain(protocol);
        List<String> sampleColumns = new ArrayList<>();
        for (DomainProperty property : sampleDomain.getProperties())
        {
            sampleColumns.add(property.getName());
        }
        form.setSampleColumns(sampleColumns.toArray(new String[sampleColumns.size()]));

        if (prop != null)
        {
            Object fitParams = run.getProperty(prop);
            if (fitParams != null)
            {
                List<Double> params = new ArrayList<>();
                for (String param : fitParams.toString().split("&"))
                    params.add(Double.parseDouble(param));

                form.setFitParams(params.toArray(new Double[params.size()]));
            }
        }
        JspView chartView = new JspView<>("/org/labkey/elisa/view/runDetailsView.jsp", form);

        chartView.setTitle("Calibration Curve");
        chartView.setFrame(WebPartView.FrameType.PORTAL);

        view.addView(chartView);

        return view;
    }

    @Override
    public DataExchangeHandler createDataExchangeHandler()
    {
        return new PlateBasedDataExchangeHandler();
    }

    public Domain getConcentrationWellGroupDomain(ExpProtocol protocol)
    {
        return getDomainByPrefix(protocol, ExpProtocol.ASSAY_DOMAIN_DATA);
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
