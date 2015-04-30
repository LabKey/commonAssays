/*
 * Copyright (c) 2015 LabKey Corporation
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


package org.labkey.ms2.matrix;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Lookup;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayPipelineProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayTableMetadata;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.matrix.ColumnMappingProperty;
import org.labkey.api.util.FileType;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Module;
import org.labkey.ms2.protein.query.ProteinUserSchema;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProteinExpressionMatrixAssayProvider extends AbstractAssayProvider
{
    public static final String NAME = "Protein Expression Matrix";
    public static final String RESOURCE_NAME = "ProteinExpressionMatrix";

    public static final String LSID_PREFIX = "ProteinExpressionMatrix";
    public static final AssayDataType DATA_TYPE = new AssayDataType(LSID_PREFIX, new FileType(".tsv"));

    public static final String PROTEIN_SEQUENCE_SET_PROPERTY_NAME = "fastaFormatProteinSequences";

    public static final ColumnMappingProperty PROTEIN_SEQUENCE_SET = new ColumnMappingProperty(PROTEIN_SEQUENCE_SET_PROPERTY_NAME, "Fasta/Uniprot File", true);
//    public static final ColumnMappingProperty IMPORT_VALUES_COLUMN = new ColumnMappingProperty("importValues", "Import Values", false);

    public ProteinExpressionMatrixAssayProvider()
    {
        super(LSID_PREFIX, LSID_PREFIX, DATA_TYPE, ModuleLoader.getInstance().getModule(MS2Module.class));
    }

    @Override @NotNull
    public AssayTableMetadata getTableMetadata(@NotNull ExpProtocol protocol)
    {
        return new AssayTableMetadata(this, protocol, null, FieldKey.fromParts("Run"), FieldKey.fromParts("RowId"));
    }

    @Override
    public ExpData getDataForDataRow(Object dataRowId, ExpProtocol protocol)
    {
        return null;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getResourceName()
    {
        return RESOURCE_NAME;
    }

    @Override
    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("");
    }

    @Override
    protected Pair<Domain, Map<DomainProperty, Object>> createBatchDomain(Container c, User user)
    {
        return super.createBatchDomain(c, user, false);
    }

    @Override
    public AssayProtocolSchema createProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return new ProteinExpressionMatrixProtocolSchema(user, container, this, protocol, targetStudy);
    }

    @Override
    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Collections.emptyList();
    }

    @Override
    public Domain getResultsDomain(ExpProtocol protocol)
    {
        return null;
    }

    @Override
    public PipelineProvider getPipelineProvider()
    {
        return new AssayPipelineProvider(MS2Module.class,
                new PipelineProvider.FileTypesEntryFilter(getDataType().getFileType()),
                this, "Import Protein Expression Matrix");
    }

    @Override
    public String getDescription()
    {
        return "Import a matrix-like TSV file of protein sequence/sample values.";
    }

    @Override
    public List<NavTree> getHeaderLinks(ViewContext viewContext, ExpProtocol protocol, ContainerFilter containerFilter)
    {
        List<NavTree> result = super.getHeaderLinks(viewContext, protocol, containerFilter);
        result.add(new NavTree("manage protein annotations", new ActionURL(MS2Controller.InsertAnnotsAction.class, viewContext.getContainer())));
        result.add(new NavTree("manage samples", PageFlowUtil.urlProvider(ExperimentUrls.class).getShowSampleSetListURL(viewContext.getContainer())));
        return result;
    }

    @Override
    protected Pair<Domain, Map<DomainProperty, Object>> createRunDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result = super.createRunDomain(c, user);
        Domain runDomain = result.getKey();

        DomainProperty proteinSeqSet = addProperty(runDomain, PROTEIN_SEQUENCE_SET.getName(), PROTEIN_SEQUENCE_SET.getLabel(), PropertyType.INTEGER);
        proteinSeqSet.setLookup(new Lookup(null, ProteinUserSchema.NAME, ProteinUserSchema.FASTA_FILE_TABLE_NAME));
        proteinSeqSet.setShownInInsertView(true);
        proteinSeqSet.setShownInUpdateView(false);
        proteinSeqSet.setRequired(true);

//        DomainProperty importValues = addProperty(runDomain, IMPORT_VALUES_COLUMN.getName(), IMPORT_VALUES_COLUMN.getLabel(), PropertyType.BOOLEAN);
//        importValues.setShownInInsertView(true);
//        importValues.setShownInUpdateView(false);
//        importValues.setShownInDetailsView(false);
//        importValues.setHidden(true);
//        importValues.setRequired(false);

        // Default the ImportValues column as true by default
//        Map<DomainProperty, Object> defaultValues = new HashMap<>();
//        defaultValues.put(importValues, Boolean.TRUE);

//        result.setValue(defaultValues);

        return result;
    }

    @Override
    protected Map<String, Set<String>> getRequiredDomainProperties()
    {
        Map<String, Set<String>> domainMap = super.getRequiredDomainProperties();
        Set<String> runProperties = domainMap.get(ExpProtocol.ASSAY_DOMAIN_RUN);

        if (runProperties == null)
        {
            runProperties = new HashSet<>();
            domainMap.put(ExpProtocol.ASSAY_DOMAIN_RUN, runProperties);
        }

        runProperties.add(PROTEIN_SEQUENCE_SET.getName());
//        runProperties.add(IMPORT_VALUES_COLUMN.getName());

        return domainMap;
    }


}
