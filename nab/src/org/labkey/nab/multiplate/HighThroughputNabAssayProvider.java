/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
import org.labkey.api.assay.plate.PlateBasedRunCreator;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.assay.query.ResultsQueryView;
import org.labkey.api.assay.query.RunListQueryView;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.assay.actions.AssayRunUploadForm;
import org.labkey.api.assay.AssayDataType;
import org.labkey.api.assay.AssayRunCreator;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.SampleMetadataInputFormat;
import org.labkey.api.study.assay.ThawListResolverType;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.query.NabProtocolSchema;
import org.springframework.validation.BindException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: brittp
 * Date: Aug 27, 2010 10:02:15 AM
 */
public abstract class HighThroughputNabAssayProvider extends NabAssayProvider
{
    public HighThroughputNabAssayProvider(String protocolLSIDPrefix, String runLSIDPrefix, String resultLSIDPrefix, AssayDataType dataType)
    {
        super(protocolLSIDPrefix, runLSIDPrefix, resultLSIDPrefix, dataType);
    }

    @Override
    public abstract String getName();
    @Override
    public abstract String getResourceName();

    @Override
    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return HtmlView.of("The high-throughput NAb data file is a specially formatted file with a .csv or .xls extension.");
    }

    @Override
    public abstract DilutionDataHandler getDataHandler();

    @Override
    protected void addPassThroughRunProperties(Domain runDomain)
    {
        // add no extra properties
    }

    @Override
    public void registerLsidHandler()
    {
        // don't register parent's handler
    }

    @Override
    protected SampleMetadataInputFormat getDefaultMetadataInputFormat()
    {
        return SampleMetadataInputFormat.FILE_BASED;
    }

    @Override
    public SampleMetadataInputFormat[] getSupportedMetadataInputFormats()
    {
        return new SampleMetadataInputFormat[]{SampleMetadataInputFormat.FILE_BASED, SampleMetadataInputFormat.COMBINED};
    }

    @Override
    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Arrays.asList(new ParticipantVisitLookupResolverType(), new SpecimenIDLookupResolverType(), new ParticipantDateLookupResolverType(), new ThawListResolverType());
    }

    @Override
    public boolean supportsMultiVirusPlate()
    {
        // for now only the single plate version supports more than one virus per plate
        return false;
    }

    @Override
    public AssayRunCreator<?> getRunCreator()
    {
        return new PlateBasedRunCreator<>(this);
    }


    protected NabProtocolSchema generateAlternateProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return new NabProtocolSchema(user, container, this, protocol, targetStudy)
        {
            final Map<String, Object> _extraParams = new HashMap<>();

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
}
