/*
 * Copyright (c) 2009-2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.luminex;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.TsvDataExchangeHandler;
import org.labkey.api.qc.TsvDataSerializer;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.util.Pair;
import org.labkey.luminex.model.Titration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds analyte and titration info for transform scripts
 * User: klum
 * Date: Apr 21, 2009
 */
public class LuminexDataExchangeHandler extends TsvDataExchangeHandler
{
    public static final String ANALYTE_DATA_PROP_NAME = "analyteData";
    public static final String TITRATION_DATA_PROP_NAME = "titrationData";

    private DataSerializer _serializer = new LuminexDataSerializer();

    @Override
    public Pair<File, Set<File>> createTransformationRunInfo(AssayRunUploadContext<? extends AssayProvider> context, ExpRun run, File scriptDir, Map<DomainProperty, String> runProperties, Map<DomainProperty, String> batchProperties) throws Exception
    {
        LuminexRunContext form = (LuminexRunContext)context;
        List<Map<String, Object>> analytes = new ArrayList<>();

        for (String analyteName : form.getAnalyteNames())
        {
            Map<String, Object> row = new HashMap<>();
            row.put("Name", analyteName);
            // get the analyte domain property values
            for (Map.Entry<DomainProperty, String> entry : form.getAnalyteProperties(analyteName).entrySet())
            {
                row.put(entry.getKey().getName(), entry.getValue());
            }
            // get the analyte table column property values
            for (Map.Entry<ColumnInfo, String> entry : form.getAnalyteColumnProperties(analyteName).entrySet())
            {
                row.put(entry.getKey().getName(), entry.getValue());
            }
            // TODO - What delimiter is safest to use?
            row.put("titrations", StringUtils.join(form.getTitrationsForAnalyte(analyteName), ","));
            analytes.add(row);
        }
        addSampleProperties(ANALYTE_DATA_PROP_NAME, analytes);

        List<Map<String, Object>> titrations = new ArrayList<>();
        for (Titration titration : form.getTitrations())
        {
            Map<String, Object> titrationRow = new HashMap<>();
            titrationRow.put("Name", titration.getName());
            titrationRow.put("QCControl", titration.isQcControl());
            titrationRow.put("Standard", titration.isStandard());
            titrationRow.put("Unknown", titration.isUnknown());
            titrationRow.put("OtherControl", titration.isOtherControl());
            titrations.add(titrationRow);
        }
        addSampleProperties(TITRATION_DATA_PROP_NAME, titrations);

        return super.createTransformationRunInfo(context, run, scriptDir, runProperties, batchProperties);
    }

    @Override
    public DataSerializer getDataSerializer()
    {
        return _serializer;
    }

    private static class LuminexDataSerializer extends TsvDataSerializer
    {
        @Override
        public List<Map<String, Object>> importRunData(ExpProtocol protocol, File runData) throws Exception
        {
            return _importRunData(protocol, runData, false);
        }
    }
}
