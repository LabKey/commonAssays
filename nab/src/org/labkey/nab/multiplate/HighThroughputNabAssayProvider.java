/*
 * Copyright (c) 2010-2014 LabKey Corporation
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

import org.labkey.api.exp.property.Domain;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayRunCreator;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.PlateBasedRunCreator;
import org.labkey.api.study.assay.SampleMetadataInputFormat;
import org.labkey.api.study.assay.ThawListResolverType;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.nab.NabAssayProvider;

import java.util.Arrays;
import java.util.List;

/**
 * User: brittp
 * Date: Aug 27, 2010 10:02:15 AM
 */
public abstract class HighThroughputNabAssayProvider extends NabAssayProvider
{
    public HighThroughputNabAssayProvider(String protocolLSIDPrefix, String runLSIDPrefix, AssayDataType dataType)
    {
        super(protocolLSIDPrefix, runLSIDPrefix, dataType);
    }

    public abstract String getName();
    public abstract String getResourceName();

    @Override
    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("The high-throughput NAb data file is a specially formatted file with a .csv or .xls extension.");
    }

    public abstract DilutionDataHandler getDataHandler();

    @Override
    protected void addPassThroughRunProperties(Domain runDomain)
    {
        // add no extra properties
    }

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
    public AssayRunCreator getRunCreator()
    {
        return new PlateBasedRunCreator(this);
    }

}
