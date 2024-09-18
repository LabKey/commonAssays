/*
 * Copyright (c) 2009-2013 LabKey Corporation
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

import org.apache.commons.vfs2.FileObject;
import org.labkey.api.dataiterator.DataIteratorBuilder;
import org.labkey.api.dataiterator.MapDataIterator;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.DataLoaderSettings;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayService;
import org.apache.commons.beanutils.converters.StringArrayConverter;
import org.apache.commons.beanutils.Converter;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * User: kevink
 * Date: Sep 20, 2009
 */
public class ViabilityTsvDataHandler extends ViabilityAssayDataHandler
{
    public static final DataType DATA_TYPE = new DataType("ViabilityAssay-TsvData");

    @Override
    public DataType getDataType()
    {
        return DATA_TYPE;
    }

    @Override
    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (DATA_TYPE.matches(lsid) || OLD_DATA_TYPE.matches(lsid))
        {
            File f = data.getFile();
            if (f != null)
            {
                String lowerName = f.getName().toLowerCase();
                if (lowerName.endsWith(".tsv") || lowerName.endsWith(".txt"))
                    return Priority.HIGH;
            }
            return Priority.MEDIUM;
        }
        return null;
    }

    @Override
    public Parser getParser(Domain runDomain, Domain resultsDomain, File dataFile)
    {
        return new Parser(runDomain, resultsDomain, dataFile);
    }

    public static class Parser extends ViabilityAssayDataHandler.Parser
    {
        public Parser(Domain runDomain, Domain resultsDomain, File dataFile)
        {
            super(runDomain, resultsDomain, dataFile);
        }

        @Override
        protected void _parse() throws IOException
        {
            try (TabLoader tl = new TabLoader(_dataFile, true))
            {
                tl.setParseQuotes(true);

                ColumnDescriptor[] columns = tl.getColumns();
                for (ColumnDescriptor cd : columns)
                {
                    if (cd.name.equals(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME))
                    {
                        // parses a comma separated list as List<String>
                        cd.converter = new Converter()
                        {
                            @Override
                            public Object convert(Class type, Object value)
                            {
                                Converter c = new StringArrayConverter();
                                Object o = c.convert(type, value);
                                if (o != null && o instanceof String[])
                                    return Arrays.asList((String[]) o);
                                return null;
                            }
                        };
                        break;
                    }
                }

                if (_runDomain != null)
                {
                    Map<String, String> comments = tl.getComments();
                    Map<DomainProperty, Object> runData = new HashMap<>(comments.size());
                    for (Map.Entry<String, String> comment : comments.entrySet())
                    {
                        DomainProperty property = _runDomain.getPropertyByName(comment.getKey());
                        if (property != null)
                        {
                            runData.put(property, comment.getValue());
                        }
                    }
                    _runData = runData;
                }
                _resultData = tl.load();
            }
        }
    }

    @Override
    public Map<DataType, DataIteratorBuilder> getValidationDataMap(ExpData data, FileObject dataFile, ViewBackgroundInfo info, Logger log, XarContext context, DataLoaderSettings settings) throws ExperimentException
    {
        assert dataFile.getName().getExtension().equals("tsv") || dataFile.getName().getExtension().endsWith(".TSV");
        
        // Uck.  The TsvDataExchangeHandler writes out GuavaDataHandler.getValidationDataMap() bfore running the transform script.
        // After the transform has run, this method is called to read that output back in.
        Map<DataType, DataIteratorBuilder> result = new HashMap<>();
        ExpRun run = data.getRun();
        ExpProtocol protocol = run.getProtocol();
        AssayProvider provider = AssayService.get().getProvider(protocol);
        Domain runDomain = provider.getRunDomain(protocol);
        Domain resultsDomain = provider.getResultsDomain(protocol);

        Parser parser = getParser(runDomain, resultsDomain, dataFile.getPath().toFile());
        List<Map<String, Object>> dataMap = parser.getResultData();
        result.put(ViabilityTsvDataHandler.DATA_TYPE, MapDataIterator.of(dataMap));

        return result;
    }

}
