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

package org.labkey.ms2.metadata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayPipelineProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayRunCreator;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.AssayTableMetadata;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.ms2.MS2Module;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;
import org.labkey.ms2.pipeline.MS2PipelineManager;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * User: Peter@labkey.com
 * Date: Oct 17, 2008
 * Time: 5:54:45 PM
 */


public class MassSpecMetadataAssayProvider extends AbstractAssayProvider
{
    public static final String PROTOCOL_LSID_NAMESPACE_PREFIX = "MassSpecMetadataProtcool";
    public static final String NAME = "Mass Spec Metadata";
    public static final AssayDataType MS_ASSAY_DATA_TYPE = new AssayDataType("MZXMLData", AbstractMS2SearchProtocol.FT_MZXML);
    public static final String RUN_LSID_NAMESPACE_PREFIX = "MassSpecMetadataRun";

    public static final String FRACTION_DOMAIN_PREFIX = ExpProtocol.ASSAY_DOMAIN_PREFIX + "Fractions";
    public static final String FRACTION_SET_NAME = "Fraction Fields";
    public static final String FRACTION_SET_LABEL = "These fields are used to describe searches where one sample has been divided into multiple fractions. The fields should describe the properties that vary from fraction to fraction.";

    public MassSpecMetadataAssayProvider()
    {
        super(PROTOCOL_LSID_NAMESPACE_PREFIX, RUN_LSID_NAMESPACE_PREFIX, MS_ASSAY_DATA_TYPE, ModuleLoader.getInstance().getModule(MS2Module.MS2_MODULE_NAME));
    }

    @NotNull
    @Override
    public AssayTableMetadata getTableMetadata(@NotNull ExpProtocol protocol)
    {
        return new AssayTableMetadata(
                this,
                protocol,
                FieldKey.fromParts("Run"),
                FieldKey.fromParts("Run"),
                FieldKey.fromParts("RowId"));
    }

    @Override
    protected Pair<Domain, Map<DomainProperty, Object>> createBatchDomain(Container c, User user)
    {
        // don't call the standard createBatchDomain because we don't want the target study or participant data resolver
        Domain domain = PropertyService.get().createDomain(c, getPresubstitutionLsid(ExpProtocol.ASSAY_DOMAIN_BATCH), "Batch Fields");
        domain.setDescription("The user is prompted for batch properties once for each set of runs they import. Batches " +
                "are a convenience to let users set properties that seldom change in one place and import many runs " +
                "using them. This is the first step of the import process.");

        return new Pair<>(domain, Collections.emptyMap());
    }

    public AssayRunCreator getRunCreator()
    {
        return new MassSpecRunCreator(this);
    }

    @Override
    public AssayProtocolSchema createProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return new MassSpecMetadataProtocolSchema(user, container, this, protocol, targetStudy);
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createFractionDomain(Container c)
    {
        String domainLsid = getPresubstitutionLsid(FRACTION_DOMAIN_PREFIX);
        Domain fractionDomain = PropertyService.get().createDomain(c, domainLsid, FRACTION_SET_NAME);
        fractionDomain.setDescription(FRACTION_SET_LABEL);
        return new Pair<>(fractionDomain, Collections.emptyMap());
    }
    
    public List<Pair<Domain, Map<DomainProperty, Object>>> createDefaultDomains(Container c, User user)
    {
        List<Pair<Domain, Map<DomainProperty, Object>>> result = super.createDefaultDomains(c, user);
        result.add(createFractionDomain(c));
        return result;
    }

    public String getName()
    {
        return NAME;
    }

    public List<AssayDataCollector> getDataCollectors(Map<String, File> uploadedFiles, AssayRunUploadForm context)
    {
        return Collections.singletonList(new MassSpecMetadataDataCollector());
    }

    public ExpData getDataForDataRow(Object dataRowId, ExpProtocol protocol)
    {
        return ExperimentService.get().getExpData(((Number)dataRowId).intValue());
    }

    public Domain getResultsDomain(ExpProtocol protocol)
    {
        return null;
    }
    
    public Domain getFractionDomain(ExpProtocol protocol)
    {
        return getDomainByPrefix(protocol, FRACTION_DOMAIN_PREFIX);
    }

    public String getDescription()
    {
        return "Describes metadata for mass spec data files, including mzXML";
    }

    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Collections.emptyList();
    }

    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return null;
    }

    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, MassSpecMetadataUploadAction.class);
    }

    @NotNull
    public static ExpSampleSet getFractionSampleSet(AssayRunUploadContext context) throws ExperimentException
    {
        String domainURI = getDomainURIForPrefix(context.getProtocol(), FRACTION_DOMAIN_PREFIX);
        ExpSampleSet sampleSet=null;
        if (null != domainURI)
            sampleSet = ExperimentService.get().getSampleSet(domainURI);

        if (sampleSet == null)
        {
            sampleSet = ExperimentService.get().createSampleSet();
            sampleSet.setContainer(context.getProtocol().getContainer());
            sampleSet.setName("Fractions: " + context.getProtocol().getName());
            sampleSet.setLSID(domainURI);

            Lsid.LsidBuilder sampleSetLSID = new Lsid.LsidBuilder(domainURI);
            sampleSetLSID.setNamespacePrefix("Sample");
            sampleSetLSID.setNamespaceSuffix(context.getProtocol().getContainer().getRowId() + "." + context.getProtocol().getName());
            sampleSetLSID.setObjectId("");
            String prefix = sampleSetLSID.toString();

            sampleSet.setMaterialLSIDPrefix(prefix);
            sampleSet.save(context.getUser());
        }
        return sampleSet;
    }

    public static final PipelineProvider.FileEntryFilter FILE_FILTER = new PipelineProvider.FileEntryFilter()
    {
        public boolean accept(File f)
        {
            // TODO:  If no corresponding mzXML file, show raw files.
            return MS2PipelineManager.isMzXMLFile(f);
        }
    };

    public PipelineProvider getPipelineProvider()
    {
        return new AssayPipelineProvider(MS2Module.class, FILE_FILTER, this, "Describe Samples");
    }

}
