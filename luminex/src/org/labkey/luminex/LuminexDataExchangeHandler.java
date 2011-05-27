/*
 * Copyright (c) 2009-2011 LabKey Corporation
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

import org.labkey.api.exp.ExperimentException;
import org.labkey.api.qc.TsvDataExchangeHandler;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.DomainProperty;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * User: klum
 * Date: Apr 21, 2009
 */
public class LuminexDataExchangeHandler extends TsvDataExchangeHandler
{
    public static final String ANALYTE_DATA_PROP_NAME = "analyteData";

    @Override
    public File createValidationRunInfo(AssayRunUploadContext context, ExpRun run, File scriptDir) throws Exception
    {
        LuminexRunUploadForm form = (LuminexRunUploadForm)context;
        List<Map<String, Object>> analytes = new ArrayList<Map<String, Object>>();

        for (String analyteName : form.getAnalyteNames())
        {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("Name", analyteName);
            for (Map.Entry<DomainProperty, String> entry : form.getAnalyteProperties(analyteName).entrySet())
            {
                row.put(entry.getKey().getName(), entry.getValue());
            }
            analytes.add(row);
        }
        addSampleProperties(ANALYTE_DATA_PROP_NAME, analytes);
        return super.createValidationRunInfo(context, run, scriptDir);
    }

    public Map<DomainProperty, String> getRunProperties(AssayRunUploadContext context) throws ExperimentException
    {
        Map<DomainProperty, String> result = super.getRunProperties(context);
        try
        {
            LuminexDataHandler.LuminexDataFileParser parser = new LuminexExcelDataHandler.LuminexExcelParser(context.getProtocol(), context.getUploadedData().get(AssayDataCollector.PRIMARY_FILE));
            result.putAll(parser.getExcelRunProps());
            return result;
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
    }
}
