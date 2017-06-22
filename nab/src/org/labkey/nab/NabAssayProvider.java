/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.dilution.AbstractDilutionAssayProvider;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.assay.nab.NabSpecimen;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.LsidManager;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Lookup;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.actions.PlateUploadForm;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayPipelineProvider;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayProviderSchema;
import org.labkey.api.study.assay.AssayRunCreator;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;
import org.labkey.api.study.assay.SampleMetadataInputFormat;
import org.labkey.api.study.assay.ThawListResolverType;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.nab.query.NabProtocolSchema;
import org.labkey.nab.query.NabProviderSchema;
import org.labkey.nab.query.NabRunCreator;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: brittp
 * Date: Sep 21, 2007
 * Time: 2:33:52 PM
 */
public class NabAssayProvider extends AbstractDilutionAssayProvider<NabRunUploadForm>
{
    public static final String RESOURCE_NAME = "NAb";
    public static final String NAME = "TZM-bl Neutralization (NAb)";

    public static final String CUSTOM_DETAILS_VIEW_NAME = "CustomDetailsView";
    private static final String NAB_RUN_LSID_PREFIX = "NabAssayRun";
    private static final String NAB_ASSAY_PROTOCOL = "NabAssayProtocol";
    public static final String VIRUS_ID_PROPERTY_NAME = "VirusID";
    public static final String HOST_CELL_PROPERTY_NAME = "HostCell";
    public static final String STUDY_NAME_PROPERTY_NAME = "StudyName";
    public static final String EXPERIMENT_PERFORMER_PROPERTY_NAME = "ExperimentPerformer";
    public static final String EXPERIMENT_ID_PROPERTY_NAME = "ExperimentID";
    public static final String INCUBATION_TIME_PROPERTY_NAME = "IncubationTime";
    public static final String PLATE_NUMBER_PROPERTY_NAME = "PlateNumber";
    public static final String EXPERIMENT_DATE_PROPERTY_NAME = "ExperimentDate";
    public static final String FILE_ID_PROPERTY_NAME = "FileID";

    public static final String VIRUS_LSID_COLUMN_NAME = "VirusLsid";
    public static final String ASSAY_DOMAIN_VIRUS_WELLGROUP = ExpProtocol.ASSAY_DOMAIN_PREFIX + "VirusWellGroup";

    public NabAssayProvider()
    {
        super(NAB_ASSAY_PROTOCOL, NAB_RUN_LSID_PREFIX, SinglePlateNabDataHandler.NAB_DATA_TYPE, ModuleLoader.getInstance().getModule(NabModule.class));
    }

    public NabAssayProvider(String protocolLSIDPrefix, String runLSIDPrefix, AssayDataType dataType)
    {
        super(protocolLSIDPrefix, runLSIDPrefix, dataType, ModuleLoader.getInstance().getModule(NabModule.class));
    }

    @Override
    public AssayProviderSchema createProviderSchema(User user, Container container, Container targetStudy)
    {
        return new NabProviderSchema(user, container, this, targetStudy, false);
    }

    @Override
    public NabProtocolSchema createProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return new NabProtocolSchema(user, container, this, protocol, targetStudy);
    }

    @Override
    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Arrays.asList(new ParticipantVisitLookupResolverType(), new ParticipantDateLookupResolverType(), new ParticipantVisitDateLookupResolverType(),
                new SpecimenIDLookupResolverType(), new ThawListResolverType());
    }

    public void registerLsidHandler()
    {
        LsidManager.get().registerHandler(_runLSIDPrefix, new LsidManager.ExpRunLsidHandler()
        {
            @Override
            protected ActionURL getDisplayURL(Container c, ExpProtocol protocol, ExpRun run)
            {
                return new ActionURL(NabAssayController.DetailsAction.class, run.getContainer()).addParameter("rowId", run.getRowId());
            }

            @Override
            public boolean hasPermission(Lsid lsid, @NotNull User user, @NotNull Class<? extends Permission> perm)
            {
                // defer permission checking until user attempts to view the details page
                return true;

//                ExpRun run = getObject(lsid);
//                if (run == null)
//                    return false;
//                Container c = run.getContainer();
//                return c.hasPermission(user, perm, RunDatasetContextualRoles.getContextualRolesForRun(c, user, run));
            }
        });
    }

    @Override
    public ExpData getDataForDataRow(Object dataRowId, ExpProtocol protocol)
    {
        if (!(dataRowId instanceof Integer))
            return null;

        // dataRowId is NabSpecimen rowId
        NabSpecimen nabSpecimen = NabManager.get().getNabSpecimen((Integer)dataRowId);
        if (null != nabSpecimen)
            return ExperimentService.get().getExpData(nabSpecimen.getDataId());
        return null;
    }

    @Override
    protected void addPassThroughRunProperties(Domain runDomain)
    {
        if (!supportsMultiVirusPlate())
        {
            addProperty(runDomain, VIRUS_NAME_PROPERTY_NAME, "Virus Name", PropertyType.STRING);
            addProperty(runDomain, VIRUS_ID_PROPERTY_NAME, "Virus ID", PropertyType.STRING);
        }
        addProperty(runDomain, HOST_CELL_PROPERTY_NAME, "Host Cell", PropertyType.STRING);
        addProperty(runDomain, STUDY_NAME_PROPERTY_NAME, "Study Name", PropertyType.STRING);
        addProperty(runDomain, EXPERIMENT_PERFORMER_PROPERTY_NAME, "Experiment Performer", PropertyType.STRING);
        addProperty(runDomain, EXPERIMENT_ID_PROPERTY_NAME, "Experiment ID", PropertyType.STRING);
        addProperty(runDomain, INCUBATION_TIME_PROPERTY_NAME, "Incubation Time", PropertyType.STRING);
        addProperty(runDomain, PLATE_NUMBER_PROPERTY_NAME, "Plate Number", PropertyType.STRING);
        addProperty(runDomain, EXPERIMENT_DATE_PROPERTY_NAME, "Experiment Date", PropertyType.DATE_TIME);
        addProperty(runDomain, FILE_ID_PROPERTY_NAME, "File ID", PropertyType.STRING);
        addProperty(runDomain, LOCK_AXES_PROPERTY_NAME, LOCK_AXES_PROPERTY_CAPTION, PropertyType.BOOLEAN);
    }

    @Override
    protected void addPassThroughSampleWellGroupProperties(Container c, Domain sampleWellGroupDomain)
    {
        Container lookupContainer = c.getProject();
        addProperty(sampleWellGroupDomain, SPECIMENID_PROPERTY_NAME, SPECIMENID_PROPERTY_CAPTION, PropertyType.STRING);
        addProperty(sampleWellGroupDomain, PARTICIPANTID_PROPERTY_NAME, PARTICIPANTID_PROPERTY_CAPTION, PropertyType.STRING);
        addProperty(sampleWellGroupDomain, VISITID_PROPERTY_NAME, VISITID_PROPERTY_CAPTION, PropertyType.DOUBLE);
        addProperty(sampleWellGroupDomain, DATE_PROPERTY_NAME, DATE_PROPERTY_CAPTION, PropertyType.DATE_TIME);
        addProperty(sampleWellGroupDomain, SAMPLE_DESCRIPTION_PROPERTY_NAME, SAMPLE_DESCRIPTION_PROPERTY_CAPTION, PropertyType.STRING);
        addProperty(sampleWellGroupDomain, SAMPLE_INITIAL_DILUTION_PROPERTY_NAME, SAMPLE_INITIAL_DILUTION_PROPERTY_CAPTION, PropertyType.DOUBLE).setRequired(true);
        addProperty(sampleWellGroupDomain, SAMPLE_DILUTION_FACTOR_PROPERTY_NAME, SAMPLE_DILUTION_FACTOR_PROPERTY_CAPTION, PropertyType.DOUBLE).setRequired(true);
        DomainProperty method = addProperty(sampleWellGroupDomain, SAMPLE_METHOD_PROPERTY_NAME, SAMPLE_METHOD_PROPERTY_CAPTION, PropertyType.STRING);
        method.setImportAliasSet(new HashSet<>(Collections.singletonList("Well Method")));
        method.setLookup(new Lookup(lookupContainer, AssaySchema.NAME + "." + getResourceName(), NabProviderSchema.SAMPLE_PREPARATION_METHOD_TABLE_NAME));
        method.setRequired(true);
    }

    @Override
    protected Map<String, Set<String>> getRequiredDomainProperties()
    {
        Map<String, Set<String>> domainMap = super.getRequiredDomainProperties();
        Set<String> sampleProperties = domainMap.get(ASSAY_DOMAIN_SAMPLE_WELLGROUP);

        sampleProperties.add(SPECIMENID_PROPERTY_NAME);
        sampleProperties.add(PARTICIPANTID_PROPERTY_NAME);
        sampleProperties.add(VISITID_PROPERTY_NAME);
        sampleProperties.add(DATE_PROPERTY_NAME);

        if (!domainMap.containsKey(ASSAY_DOMAIN_VIRUS_WELLGROUP))
            domainMap.put(ASSAY_DOMAIN_VIRUS_WELLGROUP, new HashSet<String>());

        domainMap.get(ASSAY_DOMAIN_VIRUS_WELLGROUP).add(VIRUS_NAME_PROPERTY_NAME);

        return domainMap;
    }

    @Override
    public String getResourceName()
    {
        return RESOURCE_NAME;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("The NAb data file is a specially formatted TSV, CSV or Excel file.");
    }

    @Override
    public DilutionDataHandler getDataHandler()
    {
        return new SinglePlateNabDataHandler();
    }

    @Override
    public String getDescription()
    {
        return "Imports a specially formatted TSV, CSV or Excel file. " +
                "Measures neutralization in TZM-bl cells as a function of a reduction in Tat-induced luciferase (Luc) " +
                "reporter gene expression after a single round of infection. Montefiori, D.C. 2004" +
                PageFlowUtil.helpPopup("NAb", "<a href=\"http://www.ncbi.nlm.nih.gov/pubmed/18432938\">" +
                        "Evaluating neutralizing antibodies against HIV, SIV and SHIV in luciferase " +
                        "reporter gene assays</a>.  Current Protocols in Immunology, (Coligan, J.E., " +
                        "A.M. Kruisbeek, D.H. Margulies, E.M. Shevach, W. Strober, and R. Coico, eds.), John Wiley & Sons, 12.11.1-12.11.15.", true);
    }

    @Override
    public Pair<ExpProtocol, List<Pair<Domain, Map<DomainProperty, Object>>>> getAssayTemplate(User user, Container targetContainer, ExpProtocol toCopy)
    {
        try
        {
            NabManager.get().ensurePlateTemplate(targetContainer, user);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        return super.getAssayTemplate(user, targetContainer, toCopy);
    }

    @Override
    public Pair<ExpProtocol, List<Pair<Domain, Map<DomainProperty, Object>>>> getAssayTemplate(User user, Container targetContainer)
    {
        try
        {
            NabManager.get().ensurePlateTemplate(targetContainer, user);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        return super.getAssayTemplate(user, targetContainer);
    }

    @Override
    public PipelineProvider getPipelineProvider()
    {
        return new AssayPipelineProvider(NabModule.class,
                new PipelineProvider.FileTypesEntryFilter(getDataType().getFileType()), this, "Import NAb");
    }

    @Override
    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, NabUploadWizardAction.class);
    }

    @Override
    public ActionURL getUploadWizardCompleteURL(NabRunUploadForm form, ExpRun run)
    {
        return new ActionURL(NabAssayController.DetailsAction.class,
                    run.getContainer()).addParameter("rowId", run.getRowId()).addParameter("newRun", "true");
    }

    public List<Pair<Domain, Map<DomainProperty, Object>>> createDefaultDomains(Container c, User user)
    {
        List<Pair<Domain, Map<DomainProperty, Object>>> result = super.createDefaultDomains(c, user);
        if (supportsMultiVirusPlate())
            result.add(createVirusWellGroupDomain(c, user));
        return result;
    }

    public boolean supportsMultiVirusPlate()
    {
        return true;
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createVirusWellGroupDomain(Container c, User user)
    {
        String domainLsid = getPresubstitutionLsid(ASSAY_DOMAIN_VIRUS_WELLGROUP);
        Domain virusWellGroupDomain = PropertyService.get().createDomain(c, domainLsid, "Virus Fields");

        virusWellGroupDomain.setDescription("The user will be prompted to enter these properties for each of the virus well groups in their chosen plate template.");
        addProperty(virusWellGroupDomain, VIRUS_NAME_PROPERTY_NAME, PropertyType.STRING);
        addProperty(virusWellGroupDomain, VIRUS_ID_PROPERTY_NAME, PropertyType.STRING);

        return new Pair<>(virusWellGroupDomain, Collections.emptyMap());
    }

    public Domain getVirusWellGroupDomain(ExpProtocol protocol)
    {
        if (supportsMultiVirusPlate())
            return getDomainByPrefixIfExists(protocol, ASSAY_DOMAIN_VIRUS_WELLGROUP);
        else
            return null;
    }

    public PlateSamplePropertyHelper getVirusPropertyHelper(PlateUploadForm context, boolean insertView)
    {
        PlateTemplate template = getPlateTemplate(context.getContainer(), context.getProtocol());
        try
        {
            AssayProvider provider = context.getProvider();
            if (supportsMultiVirusPlate() && provider instanceof NabAssayProvider)
            {
                Domain domain = ((NabAssayProvider)provider).getVirusWellGroupDomain(context.getProtocol());

                if (domain != null)
                {
                    int wellGroupCount = template.getWellGroupCount(WellGroup.Type.VIRUS);
                    boolean isFileBased = getMetadataInputFormat(context.getProtocol()) == SampleMetadataInputFormat.FILE_BASED;

                    // Always use the NabVirusPropertyHelper for the upload wizard insert view, since that will be the same for manual vs file based
                    // NabVirusFilePropertyHelper to be used for file based metadata parsing in multi-virus case only
                    if (!insertView && isFileBased && wellGroupCount > 1)
                        return new NabVirusFilePropertyHelper(context.getContainer(), context.getProtocol(), domain.getProperties(), template);
                    else
                        return new NabVirusPropertyHelper(domain.getProperties(), template, getMetadataInputFormat(context.getProtocol()));
                }
            }
            return null;
        }
        catch (IllegalArgumentException e)
        {
            return null;        // TODO: temp until we deal with assays created before this version
        }
    }

    @Override
    public AssayRunCreator getRunCreator()
    {
        return new NabRunCreator(this);
    }

    @Override
    public SampleMetadataInputFormat[] getSupportedMetadataInputFormats()
    {
        return new SampleMetadataInputFormat[]{SampleMetadataInputFormat.MANUAL, SampleMetadataInputFormat.FILE_BASED};
    }

    @Nullable
    @Override
    public ActionURL getAssayQCRunURL(ViewContext context, ExpRun run)
    {
        ActionURL url = new ActionURL(NabAssayController.QCDataAction.class, context.getContainer());
        url.addParameters(context.getActionURL().getParameters());
        //url.addParameter("rowId", run.getRowId());

        return url;
    }
}
