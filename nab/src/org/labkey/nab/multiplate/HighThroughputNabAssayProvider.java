/*
 * Copyright (c) 2010-2013 LabKey Corporation
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
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabDataHandler;

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

    public abstract NabDataHandler getDataHandler();

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
    protected boolean isSampleMetadataFileBased()
    {
        return true;
    }
}
