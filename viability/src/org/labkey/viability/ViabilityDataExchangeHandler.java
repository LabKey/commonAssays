/*
 * Copyright (c) 2009-2011 LabKey Corporation
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

package org.labkey.viability;

import org.labkey.api.dataiterator.DataIteratorBuilder;
import org.labkey.api.dataiterator.MapDataIterator;
import org.labkey.api.exp.ExperimentDataHandler;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.qc.TsvDataExchangeHandler;
import org.labkey.api.qc.TsvDataSerializer;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayService;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * User: klum
 * Date: Nov 17, 2009
 */
public class ViabilityDataExchangeHandler extends TsvDataExchangeHandler
{
    private final DataSerializer _serializer = new ViabilityDataSerializer();

    @Override
    public DataSerializer getDataSerializer()
    {
        return _serializer;
    }

    private static class ViabilityDataSerializer extends TsvDataSerializer
    {
        @Override
        public DataIteratorBuilder importRunData(ExpProtocol protocol, File runData) throws Exception
        {
            AssayProvider provider = AssayService.get().getProvider(protocol);
            ExpData data = ExperimentService.get().createData(protocol.getContainer(), ViabilityTsvDataHandler.DATA_TYPE);
            ExperimentDataHandler handler = data.findDataHandler();

            if (handler instanceof ViabilityAssayDataHandler viabilityHandler)
            {
                ViabilityAssayDataHandler.Parser parser = viabilityHandler.getParser(provider.getRunDomain(protocol),
                        provider.getResultsDomain(protocol), runData);
                List<Map<String, Object>> rows = parser.getResultData();
                return MapDataIterator.of(rows);
            }
            return MapDataIterator.of(Collections.emptyList());
        }
    }
}
