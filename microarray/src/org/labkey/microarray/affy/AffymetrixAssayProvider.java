/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
package org.labkey.microarray.affy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Lookup;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AbstractTsvAssayProvider;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayPipelineProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayRunCreator;
import org.labkey.api.study.assay.AssayTableMetadata;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.util.FileType;
import org.labkey.api.util.Pair;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.microarray.MicroarrayModule;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class AffymetrixAssayProvider extends AbstractTsvAssayProvider
{
    public static final String PROTOCOL_PREFIX = "AffymetrixAssayProtocol";
    public static final String NAME = "Affymetrix";
    public static final AssayDataType GENE_TITAN_DATA_TYPE = new AssayDataType("AssayRunAffymetrixData", new FileType(Arrays.asList(".xls", ".xlsx"), ".xls"));
    public static final AssayDataType CEL_DATA_TYPE = new AssayDataType("AssayRunAffymetrixCELData", new FileType(Arrays.asList(".CEL", ".cel"), ".CEL"));
    public static final String SAMPLE_ID_COLUMN = "SampleId";
    public static final String SAMPLE_NAME_COLUMN = "SampleName";
    public static final String CEL_FILE_ID_COLUMN = "CelFileId";

    public AffymetrixAssayProvider()
    {
        super(PROTOCOL_PREFIX, "AffymetrixAssayRun", GENE_TITAN_DATA_TYPE, ModuleLoader.getInstance().getModule(MicroarrayModule.class));
    }

    @Override
    protected Pair<Domain, Map<DomainProperty, Object>> createBatchDomain(Container c, User user)
    {
        return super.createBatchDomain(c, user, false);
    }

    @Override
    public AssayProtocolSchema createProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return new AffymetrixProtocolSchema(user, container, this, protocol, targetStudy);
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @NotNull
    @Override
    public org.labkey.api.study.assay.AssayTableMetadata getTableMetadata(@NotNull ExpProtocol protocol)
    {
        return new AssayTableMetadata(
                this,
                protocol,
                null,
                FieldKey.fromParts("Run"),
                FieldKey.fromParts(AbstractTsvAssayProvider.ROW_ID_COLUMN_NAME)
        );
    }

    @Override
    public AssayRunCreator getRunCreator()
    {
        return new AffymetrixRunCreator(this);
    }

    @Override
    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("");
    }

    @Override
    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Collections.emptyList();
    }

    @Override
    public PipelineProvider getPipelineProvider()
    {
        return new AssayPipelineProvider(MicroarrayModule.class,
                        new PipelineProvider.FileTypesEntryFilter(getDataType().getFileType()), this, "Import Affymetrix");
    }

    @Override
    public String getDescription()
    {
        return "Imports Affymetrix microarray runs from GeneTitan Excel files.";
    }

    public List<Pair<Domain, Map<DomainProperty, Object>>> createDefaultDomains(Container c, User user)
    {
        List<Pair<Domain, Map<DomainProperty, Object>>> result = super.createDefaultDomains(c, user);

        Pair<Domain, Map<DomainProperty, Object>> resultDomain = createResultDomain(c, user);
        if (resultDomain != null)
            result.add(resultDomain);
        return result;
    }

    protected Pair<Domain,Map<DomainProperty,Object>> createResultDomain(Container c, User user)
    {
        Domain dataDomain = PropertyService.get().createDomain(c, "urn:lsid:" + XarContext.LSID_AUTHORITY_SUBSTITUTION + ":" + ExpProtocol.ASSAY_DOMAIN_DATA + ".Folder-" + XarContext.CONTAINER_ID_SUBSTITUTION + ":" + ASSAY_NAME_SUBSTITUTION, "Data Fields");
        dataDomain.setDescription("The user is prompted to enter data values for row of data associated with a run, typically done as uploading a file.  This is part of the second step of the upload process.");

        DomainProperty sampleId = addProperty(dataDomain, SAMPLE_ID_COLUMN, "Sample Id", PropertyType.INTEGER);
        sampleId.setRequired(true);
        sampleId.setHidden(true);

        DomainProperty sampleName = addProperty(dataDomain, SAMPLE_NAME_COLUMN, "Sample Name", PropertyType.STRING);
        sampleName.setRequired(true);

        DomainProperty celFileId = addProperty(dataDomain, CEL_FILE_ID_COLUMN, "CEL File Id", PropertyType.INTEGER);
        // Don't set a target container so that it's interpreted relative to the data, not the assay design's container
        celFileId.setLookup(new Lookup(null, ExpSchema.SCHEMA_NAME, "Data"));
        celFileId.setRequired(true);

        return new Pair<>(dataDomain, Collections.emptyMap());
    }

    @Override
    protected Pair<Domain, Map<DomainProperty, Object>> createRunDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result = super.createRunDomain(c, user);
        return result;
    }

    @Override
    protected Map<String, Set<String>> getRequiredDomainProperties()
    {
        Map<String, Set<String>> domainMap = super.getRequiredDomainProperties();
        Set<String> dataProperties = domainMap.get(ExpProtocol.ASSAY_DOMAIN_DATA);

        if (dataProperties == null)
        {
            dataProperties = new HashSet<>();
            domainMap.put(ExpProtocol.ASSAY_DOMAIN_DATA, dataProperties);

        }

        dataProperties.add(SAMPLE_ID_COLUMN);
        dataProperties.add(SAMPLE_NAME_COLUMN);
        dataProperties.add(CEL_FILE_ID_COLUMN);

        return domainMap;
    }
}
