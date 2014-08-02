/*
 * Copyright (c) 2008-2014 LabKey Corporation
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
package org.labkey.nab;

import org.labkey.api.assay.dilution.DilutionAssayProvider;
import org.labkey.api.assay.dilution.DilutionAssayRun;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.assay.dilution.DilutionMaterialKey;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.api.assay.nab.view.RunDetailOptions;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.query.CustomView;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.SchemaKey;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.view.ViewContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * User: brittp
 * Date: Dec 9, 2008
 * Time: 5:43:01 PM
 */

public abstract class NabAssayRun extends DilutionAssayRun
{
    public NabAssayRun(DilutionAssayProvider provider, ExpRun run,
                       User user, List<Integer> cutoffs, StatsService.CurveFitType renderCurveFitType)
    {
        super(provider, run, user, cutoffs, renderCurveFitType);
    }

    @Override
    protected CustomView getRunsCustomView(ViewContext context)
    {
        CustomView runView = QueryService.get().getCustomView(context.getUser(), context.getContainer(), context.getUser(),
                SchemaKey.fromParts("assay", getProvider().getResourceName(), getProtocol().getName()).toString(), AssayProtocolSchema.RUNS_TABLE_NAME, NabAssayProvider.CUSTOM_DETAILS_VIEW_NAME);

        if (runView == null)
        {
            // Try with the old schema/query name
            runView = QueryService.get().getCustomView(context.getUser(), context.getContainer(), context.getUser(),
                    AssaySchema.NAME, AssaySchema.getLegacyProtocolTableName(getProtocol(), AssayProtocolSchema.RUNS_TABLE_NAME), NabAssayProvider.CUSTOM_DETAILS_VIEW_NAME);
        }
        return runView;
    }

    @Override
    public List<SampleResult> getSampleResults()
    {
        if (_sampleResults == null)
        {
            List<SampleResult> sampleResults = new ArrayList<>();

            DilutionDataHandler handler = _provider.getDataHandler();
            DataType dataType = handler.getDataType();

            List<? extends ExpData> outputDatas = _run.getOutputDatas(null); //handler.getDataType());
            ExpData outputObject = null;
            if (outputDatas.size() == 1 && outputDatas.get(0).getDataType() == dataType)
            {
                outputObject = outputDatas.get(0);
            }
            else if (outputDatas.size() > 1)
            {
                // If there is a transformed dataType, use that
                ExpData dataWithHandlerType = null;
                ExpData dataWithTransformedType = null;
                for (ExpData expData : outputDatas)
                {
                    if (SinglePlateNabDataHandler.NAB_TRANSFORMED_DATA_TYPE.getNamespacePrefix().equalsIgnoreCase(expData.getLSIDNamespacePrefix()))
                    {
                        if (null != dataWithTransformedType)
                            throw new IllegalStateException("Expected a single data file output for this NAb run. Found at least 2 transformed expDatas and a total of " + outputDatas.size());
                        dataWithTransformedType = expData;
                    }
                    else if (dataType.equals(expData.getDataType()))
                    {
                        if (null != dataWithHandlerType)
                            throw new IllegalStateException("Expected a single data file output for this NAb run. Found at least 2 expDatas with the expected datatype and a total of " + outputDatas.size());
                        dataWithHandlerType = expData;
                    }
                }
                if (null != dataWithTransformedType)
                {
                    outputObject = dataWithTransformedType;
                }
                else if (null != dataWithHandlerType)
                {
                    outputObject = dataWithHandlerType;
                }
            }
            if (null == outputObject)
                throw new IllegalStateException("Expected a single data file output for this NAb run, but none matching the expected datatype found. Found a total of " + outputDatas.size());

            AssayProtocolSchema schema = _provider.createProtocolSchema(_user, _run.getContainer(), _protocol, null);
            TableInfo virusTable = schema.createTable(DilutionManager.VIRUS_TABLE_NAME);

            Map<String, Map<PropertyDescriptor, Object>> samplePropertiesMap = getSampleProperties();
            DilutionSummary[] dilutionSummaries = getSummaries();
            Map<String, DilutionResultProperties> allProperties = getSampleProperties(outputObject, dilutionSummaries, samplePropertiesMap);
            Set<String> captions = new HashSet<>();
            boolean longCaptions = false;

            for (DilutionSummary summary : dilutionSummaries)
            {
                if (!summary.isBlank())
                {
                    DilutionMaterialKey key = summary.getMaterialKey();
                    String shortCaption = key.getDisplayString(RunDetailOptions.DataIdentifier.DefaultFormat);
                    if (captions.contains(shortCaption))
                        longCaptions = true;
                    captions.add(shortCaption);

                    Map<PropertyDescriptor, Object> sampleProperties = samplePropertiesMap.get(getSampleKey(summary));
                    String sampleKeyWithVirus = getSampleKeyForResultPoperties(summary, virusTable);
                    DilutionResultProperties props = allProperties.get(sampleKeyWithVirus);
                    sampleResults.add(new SampleResult(_provider, outputObject, summary, key, sampleProperties, props));
                }
            }

            if (longCaptions)
            {
                for (SampleResult result : sampleResults)
                    result.setLongCaptions(true);
            }

            _sampleResults = sampleResults;
        }
        return _sampleResults;
    }
}
