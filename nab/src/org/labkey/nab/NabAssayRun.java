/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.assay.dilution.DilutionMaterialKey;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.api.assay.nab.Luc5Assay;
import org.labkey.api.data.*;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.CustomView;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.SchemaKey;
import org.labkey.api.security.User;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewContext;
import org.labkey.nab.query.NabProviderSchema;
import org.labkey.nab.query.NabRunDataTable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/*
 * User: brittp
 * Date: Dec 9, 2008
 * Time: 5:43:01 PM
 */

public abstract class NabAssayRun extends DilutionAssayRun
{
    public NabAssayRun(DilutionAssayProvider provider, ExpRun run,
                       User user, List<Integer> cutoffs, DilutionCurve.FitType renderCurveFitType)
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
            List<SampleResult> sampleResults = new ArrayList<SampleResult>();

            DilutionDataHandler handler = _provider.getDataHandler();
            DataType dataType = handler.getDataType();

            ExpData[] outputDatas = _run.getOutputDatas(null); //handler.getDataType());
            ExpData outputObject = null;
            if (outputDatas.length == 1 && outputDatas[0].getDataType() == dataType)
            {
                outputObject = outputDatas[0];
            }
            else if (outputDatas.length > 1)
            {
                // If there is a transformed dataType, use that
                ExpData dataWithHandlerType = null;
                ExpData dataWithTransformedType = null;
                for (ExpData expData : outputDatas)
                {
                    if (SinglePlateNabDataHandler.NAB_TRANSFORMED_DATA_TYPE.getNamespacePrefix().equalsIgnoreCase(expData.getLSIDNamespacePrefix()))
                    {
                        if (null != dataWithTransformedType)
                            throw new IllegalStateException("Expected a single data file output for this NAb run. Found at least 2 transformed expDatas and a total of " + outputDatas.length);
                        dataWithTransformedType = expData;
                    }
                    else if (dataType.equals(expData.getDataType()))
                    {
                        if (null != dataWithHandlerType)
                            throw new IllegalStateException("Expected a single data file output for this NAb run. Found at least 2 expDatas with the expected datatype and a total of " + outputDatas.length);
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
                throw new IllegalStateException("Expected a single data file output for this NAb run, but none matching the expected datatype found. Found a total of " + outputDatas.length);

            Map<String, DilutionResultProperties> allProperties = getSampleProperties(outputObject);
            Set<String> captions = new HashSet<String>();
            boolean longCaptions = false;

            for (DilutionSummary summary : getSummaries())
            {
                if (!summary.isBlank())
                {
                    DilutionMaterialKey key = summary.getMaterialKey();
                    String shortCaption = key.getDisplayString(false);
                    if (captions.contains(shortCaption))
                        longCaptions = true;
                    captions.add(shortCaption);

                    DilutionResultProperties props = allProperties.get(getSampleKey(summary));
                    sampleResults.add(new SampleResult(_provider, outputObject, summary, key, props.getSampleProperties(), props.getDataProperties()));
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

    private Map<String, DilutionResultProperties> getSampleProperties(ExpData outputData)
    {
        Map<String, DilutionResultProperties> samplePropertyMap = new HashMap<String, DilutionResultProperties>();

        Collection<ExpMaterial> inputs = _run.getMaterialInputs().keySet();
        Domain sampleDomain = _provider.getSampleWellGroupDomain(_protocol);
        DomainProperty[] sampleDomainProperties = sampleDomain.getProperties();

        NabProviderSchema nabProviderSchema = (NabProviderSchema)_provider.createProviderSchema(_user, _run.getContainer(), null);
        NabRunDataTable nabRunDataTable = nabProviderSchema.createDataRowTable(_protocol);

        for (ExpMaterial material : inputs)
        {
            Map<PropertyDescriptor, Object> sampleProperties = new TreeMap<PropertyDescriptor, Object>(new PropertyDescriptorComparator());
            for (DomainProperty dp : sampleDomainProperties)
            {
                PropertyDescriptor property = dp.getPropertyDescriptor();
                sampleProperties.put(property, material.getProperty(property));
            }

            // in addition to the properties saved on the sample object, we'll add the properties associated with each sample's
            // "output" data object.
            Map<PropertyDescriptor, Object> dataProperties = new TreeMap<PropertyDescriptor, Object>(new PropertyDescriptorComparator());
            String wellGroupName = getWellGroupName(material);
            String dataRowLsid = getDataHandler().getDataRowLSID(outputData, wellGroupName, sampleProperties).toString();
            Set<Double> cutoffValues = new HashSet<Double>();
            for (Integer value : DilutionDataHandler.getCutoffFormats(_protocol, _run).keySet())
                cutoffValues.add(value.doubleValue());
            List<PropertyDescriptor> propertyDescriptors = NabProviderSchema.getExistingDataProperties(_protocol, cutoffValues);
            NabManager.get().getDataPropertiesFromNabRunData(nabRunDataTable, dataRowLsid, _run.getContainer(), propertyDescriptors, dataProperties);
            samplePropertyMap.put(getSampleKey(material), new DilutionResultProperties(sampleProperties,  dataProperties));
        }
        return samplePropertyMap;
    }
}
