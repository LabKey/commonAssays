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

import org.apache.commons.lang.StringUtils;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.qc.TsvDataExchangeHandler;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.DomainProperty;

import java.io.File;
import java.util.Collections;
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
    public static final String TITRATION_DATA_PROP_NAME = "titrationData";

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
            // TODO - What delimeter is safest to use?
            row.put("titrations", StringUtils.join(form.getTitrationsForAnalyte(analyteName), ","));
            analytes.add(row);
        }
        addSampleProperties(ANALYTE_DATA_PROP_NAME, analytes);

        List<Map<String, Object>> titrations = new ArrayList<Map<String, Object>>();
        for (String titration : form.getParser().getTitrations())
        {
            titrations.add(Collections.<String, Object>singletonMap("Name", titration));
        }
        addSampleProperties(TITRATION_DATA_PROP_NAME, titrations);

        return super.createValidationRunInfo(context, run, scriptDir);
    }

    public Map<DomainProperty, String> getRunProperties(AssayRunUploadContext context) throws ExperimentException
    {
        LuminexRunUploadForm form = (LuminexRunUploadForm)context;

        Map<DomainProperty, String> result = super.getRunProperties(context);
        result.putAll(form.getParser().getExcelRunProps());
        return result;
    }
}
