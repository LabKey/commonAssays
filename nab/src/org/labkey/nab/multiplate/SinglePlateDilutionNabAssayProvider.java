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
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;
import org.labkey.api.study.assay.SampleMetadataInputFormat;
import org.labkey.api.study.query.ResultsQueryView;
import org.labkey.api.study.query.RunListQueryView;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.nab.NabAssayController;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabRunUploadForm;
import org.labkey.nab.query.NabProtocolSchema;
import org.springframework.validation.BindException;

import java.io.IOException;
import java.io.Writer;
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
        super(NAB_ASSAY_PROTOCOL, NAB_RUN_LSID_PREFIX, SinglePlateDilutionNabDataHandler.SINGLE_PLATE_DILUTION_DATA_TYPE);
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

    public String getDescription()
    {
        return "Imports a specially formatted CSV or XLS file that contains data from multiple plates.  This high-throughput NAb " +
                "assay differs from the standard NAb assay in that samples are identical across plates but with a different virus per plate. " +
                "Dilutions are assumed to occur within a single plate.  Both NAb assay types measure neutralization in TZM-bl cells as a function of a " +
                "reduction in Tat-induced luciferase (Luc) reporter gene expression after a single round of infection. Montefiori, D.C. 2004" +
                PageFlowUtil.helpPopup("NAb", "<a href=\"http://www.ncbi.nlm.nih.gov/pubmed/18432938\">" +
                        "Evaluating neutralizing antibodies against HIV, SIV and SHIV in luciferase " +
                        "reporter gene assays</a>.  Current Protocols in Immunology, (Coligan, J.E., " +
                        "A.M. Kruisbeek, D.H. Margulies, E.M. Shevach, W. Strober, and R. Coico, eds.), John Wiley & Sons, 12.11.1-12.11.15.", true);
    }

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

    protected Map<String, Set<String>> getRequiredDomainProperties()
    {
        Map<String, Set<String>> domainMap = super.getRequiredDomainProperties();

        if (!domainMap.containsKey(NabAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP))
            domainMap.put(NabAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP, new HashSet<String>());

        Set<String> sampleProperties = domainMap.get(NabAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);

        sampleProperties.add(NabAssayProvider.VIRUS_NAME_PROPERTY_NAME);

        return domainMap;
    }

    @Override
    protected PlateSamplePropertyHelper createSampleFilePropertyHelper(Container c, ExpProtocol protocol, List<? extends DomainProperty> sampleProperties, PlateTemplate template, SampleMetadataInputFormat inputFormat)
    {
        return new SinglePlateDilutionSamplePropertyHelper(c, protocol, sampleProperties, template, inputFormat);
    }

    @Override
    public NabProtocolSchema createProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return new NabProtocolSchema(user, container, this, protocol, targetStudy)
        {
            Map<String, Object> _extraParams = new HashMap<>();

            @Override
            protected RunListQueryView createRunsQueryView(ViewContext context, QuerySettings settings, BindException errors)
            {
                NabRunListQueryView queryView = new NabRunListQueryView(this, settings);
                queryView.setExtraDetailsUrlParams(getDetailUrlParams());

                return queryView;
            }

            @Override
            protected ResultsQueryView createDataQueryView(ViewContext context, QuerySettings settings, BindException errors)
            {
                NabResultsQueryView queryView = new NabResultsQueryView(getProtocol(), context, settings);
                queryView.setExtraDetailsUrlParams(getDetailUrlParams());

                return queryView;
            }

            private Map<String, Object> getDetailUrlParams()
            {
                if (_extraParams.isEmpty())
                {
                    _extraParams.put("maxSamplesPerGraph", 20);
                    _extraParams.put("graphWidth", 600);
                    _extraParams.put("graphHeight", 550);
                    _extraParams.put("graphsPerRow", 1);
                    _extraParams.put("sampleNoun", "Virus");
                }
                return _extraParams;
            }
        };
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
