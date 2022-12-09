/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
package org.labkey.nab.multiplate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.data.Container;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.assay.plate.PlateTemplate;
import org.labkey.api.assay.plate.PlateSamplePropertyHelper;
import org.labkey.api.study.assay.SampleMetadataInputFormat;
import org.labkey.api.assay.query.ResultsQueryView;
import org.labkey.api.assay.query.RunListQueryView;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabRunUploadForm;
import org.labkey.nab.query.NabProtocolSchema;
import org.springframework.validation.BindException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: klum
 * Date: 2/24/13
 */
public class SinglePlateDilutionNabAssayProvider extends HighThroughputNabAssayProvider
{
    private static final String NAB_RUN_LSID_PREFIX = "SinglePlateDilutionNabAssayRun";
    private static final String NAB_ASSAY_PROTOCOL = "SinglePlateDilutionNabAssayProtocol";

    public SinglePlateDilutionNabAssayProvider()
    {
        super(NAB_ASSAY_PROTOCOL, NAB_RUN_LSID_PREFIX, null, SinglePlateDilutionNabDataHandler.SINGLE_PLATE_DILUTION_DATA_TYPE);
    }

    @Override
    public String getName()
    {
        return "TZM-bl Neutralization (NAb), High-throughput (Single Plate Dilution)";
    }

    @Override
    public String getResourceName()
    {
        return "SinglePlateDilutionNAb";
    }

    @Override
    public String getDescription()
    {
        return "Imports a specially formatted CSV or XLS file that contains data from multiple plates.  This high-throughput NAb " +
                "assay differs from the standard NAb assay in that samples are identical across plates but with a different virus per plate. " +
                "Dilutions are assumed to occur within a single plate.  Both NAb assay types measure neutralization in TZM-bl cells as a function of a " +
                "reduction in Tat-induced luciferase (Luc) reporter gene expression after a single round of infection. Montefiori, D.C. 2004";
    }

    @Override
    public DilutionDataHandler getDataHandler()
    {
        return new SinglePlateDilutionNabDataHandler();
    }

    @Override
    protected void addPassThroughSampleWellGroupProperties(Container c, Domain sampleWellGroupDomain)
    {
        super.addPassThroughSampleWellGroupProperties(c, sampleWellGroupDomain);
        addProperty(sampleWellGroupDomain, NabAssayProvider.VIRUS_NAME_PROPERTY_NAME, NabAssayProvider.VIRUS_NAME_PROPERTY_NAME, PropertyType.STRING).setRequired(true);
    }

    @Override
    protected Map<String, Set<String>> getRequiredDomainProperties()
    {
        Map<String, Set<String>> domainMap = super.getRequiredDomainProperties();

        if (!domainMap.containsKey(NabAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP))
            domainMap.put(NabAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP, new HashSet<>());

        Set<String> sampleProperties = domainMap.get(NabAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);

        sampleProperties.add(NabAssayProvider.VIRUS_NAME_PROPERTY_NAME);

        return domainMap;
    }

    @Override
    protected PlateSamplePropertyHelper createSampleFilePropertyHelper(Container c, ExpProtocol protocol, List<? extends DomainProperty> sampleProperties, PlateTemplate template, SampleMetadataInputFormat inputFormat)
    {
        if (inputFormat == SampleMetadataInputFormat.MANUAL)
            return new PlateSamplePropertyHelper(sampleProperties, template);
        else
            return new SinglePlateDilutionSamplePropertyHelper(c, protocol, sampleProperties, template, inputFormat);
    }

    @Override
    public NabProtocolSchema createProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return generateAlternateProtocolSchema(user, container, protocol, targetStudy);
    }

    @Override
    public ActionURL getUploadWizardCompleteURL(NabRunUploadForm form, ExpRun run)
    {
        ActionURL url = super.getUploadWizardCompleteURL(form, run);

        url.addParameter("maxSamplesPerGraph", 20).
            addParameter("graphWidth", 550).
            addParameter("graphHeight", 600).
            addParameter("graphsPerRow", 1);

        return url;
    }
}
