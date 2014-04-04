/*
 * Copyright (c) 2009-2014 LabKey Corporation
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

package org.labkey.elispot;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.PlateBasedDataExchangeHandler;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ViewContext;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: klum
 * Date: Apr 14, 2009
 * Time: 5:24:15 PM
 */
public class ElispotDataExchangeHandler extends PlateBasedDataExchangeHandler
{
    public static final String ANTIGEN_DATA_PROP_NAME = "antigenData";

    @Override
    public Pair<File, Set<File>> createTransformationRunInfo(AssayRunUploadContext<? extends AssayProvider> context, ExpRun run, File scriptDir, Map<DomainProperty, String> runProperties, Map<DomainProperty, String> batchProperties) throws Exception
    {
        ElispotRunUploadForm form = (ElispotRunUploadForm)context;

        ElispotAssayProvider provider = form.getProvider();
        PlateTemplate template = provider.getPlateTemplate(form.getContainer(), form.getProtocol());

        // add in the specimen information, the data will be serialized to a tsv and the file
        // location will be added to the run properties file.

        addSampleProperties(SAMPLE_DATA_PROP_NAME, GROUP_COLUMN_NAME, form.getSampleProperties(), template, WellGroup.Type.SPECIMEN);
        addSampleProperties(ANTIGEN_DATA_PROP_NAME, GROUP_COLUMN_NAME, form.getAntigenProperties(), template, WellGroup.Type.ANTIGEN);

        return super.createTransformationRunInfo(context, run, scriptDir, runProperties, batchProperties);
    }

    @Override
    public void createSampleData(@NotNull ExpProtocol protocol, ViewContext viewContext, File scriptDir) throws Exception
    {
        AssayProvider provider = AssayService.get().getProvider(protocol);
        if (provider instanceof ElispotAssayProvider)
        {
            ElispotAssayProvider plateProvider = (ElispotAssayProvider)provider;
            PlateTemplate template = plateProvider.getPlateTemplate(viewContext.getContainer(), protocol);
            if (template != null)
            {
                List<? extends DomainProperty> props = plateProvider.getSampleWellGroupDomain(protocol).getProperties();
                List<? extends DomainProperty> antigenProps = plateProvider.getAntigenWellGroupDomain(protocol).getProperties();

                Map<String, Map<DomainProperty, String>>specimens = createTestSampleProperties(props, template, WellGroup.Type.SPECIMEN);
                Map<String, Map<DomainProperty, String>>antigens = createTestSampleProperties(antigenProps, template, WellGroup.Type.ANTIGEN);

                addSampleProperties(SAMPLE_DATA_PROP_NAME, GROUP_COLUMN_NAME, specimens, template, WellGroup.Type.SPECIMEN);
                addSampleProperties(ANTIGEN_DATA_PROP_NAME, GROUP_COLUMN_NAME, antigens, template, WellGroup.Type.ANTIGEN);
            }
        }
        super.createSampleData(protocol, viewContext, scriptDir);
    }
}
