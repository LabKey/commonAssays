/*
 * Copyright (c) 2008-2016 LabKey Corporation
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

package org.labkey.microarray.assay;

import org.apache.log4j.Logger;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.DataLoaderSettings;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.query.ValidationException;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.SimpleXMLStreamReader;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.study.assay.AbstractAssayTsvDataHandler;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.microarray.MicroarrayModule;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Jan 3, 2008
 */
public class MageMLDataHandler extends AbstractAssayTsvDataHandler implements TransformDataHandler
{
    @Override
    public DataType getDataType()
    {
        return MicroarrayModule.MAGE_ML_INPUT_TYPE;
    }

    protected boolean allowEmptyData()
    {
        return true;
    }

    @Override
    protected boolean shouldAddInputMaterials()
    {
        return false;
    }

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context, DataLoaderSettings settings) throws ExperimentException
    {
        ExpProtocol protocol = data.getRun().getProtocol();
        AssayProvider provider = AssayService.get().getProvider(protocol);

        Domain dataDomain = provider.getResultsDomain(protocol);
        List<? extends DomainProperty> columns = dataDomain.getProperties();
        if (columns.isEmpty())
        {
            // Return a single, empty row of results so that we get an entry in the results table and can copy it to
            // a study, which is useful because it propagates the run and batch properties
            List<Map<String, Object>> dataRows = new ArrayList<>(1);
            dataRows.add(new HashMap<String, Object>());
            return Collections.singletonMap(MicroarrayModule.MAGE_ML_INPUT_TYPE, dataRows);
        }
        FileInputStream fIn = null;
        try
        {
            fIn = new FileInputStream(dataFile);
            SimpleXMLStreamReader reader = new SimpleXMLStreamReader(fIn);
            List<String> columnNames = new ArrayList<>();
            if (reader.skipToStart("QuantitationTypes_assnreflist"))
            {
                boolean endOfTypes = false;
                while (!endOfTypes)
                {
                    if (reader.isStartElement())
                    {
                        String identifier = reader.getAttributeValue(null, "identifier");
                        if (identifier != null)
                        {
                            if (identifier.startsWith("Local:QT:"))
                            {
                                identifier = identifier.substring("Local:QT:".length());
                            }
                            columnNames.add(identifier);
                        }
                    }
                    else if (reader.isEndElement() && reader.getLocalName().equals("QuantitationTypes_assnreflist"))
                    {
                        endOfTypes = true;
                    }
                    reader.next();
                }
            }

            if (reader.skipToStart("DataInternal"))
            {
                reader.next();
                int startingOffset = reader.getLocation().getCharacterOffset();
                if (reader.skipToEnd("DataInternal"))
                {
                    int endingOffset = reader.getLocation().getCharacterOffset();
                    reader.close();
                    InputStream tsvIn = new TrimmedFileInputStream(dataFile, startingOffset, endingOffset);
                    try
                    {
                        Map<String, Class> expectedColumns = new HashMap<>(columns.size());
                        for (DomainProperty col : columns)
                            expectedColumns.put(col.getName().toLowerCase(), col.getPropertyDescriptor().getPropertyType().getJavaType());
                        for (DomainProperty col : columns)
                        {
                            if (col.getLabel() != null && !expectedColumns.containsKey(col.getLabel().toLowerCase()))
                            {
                                expectedColumns.put(col.getLabel().toLowerCase(), col.getPropertyDescriptor().getPropertyType().getJavaType());
                            }
                        }
                        Reader fileReader = new InputStreamReader(tsvIn);

                        TabLoader loader = new TabLoader(fileReader, false);
                        ColumnDescriptor[] tabColumns = new ColumnDescriptor[columnNames.size()];
                        for (int i = 0; i < columnNames.size(); i++)
                        {
                            String name = columnNames.get(i);
                            Class colClass = expectedColumns.get(name.toLowerCase());
                            tabColumns[i] = new ColumnDescriptor(name, colClass);
                            if (colClass == null)
                            {
                                tabColumns[i].load = false;
                            }
                            tabColumns[i].errorValues = ERROR_VALUE;
                        }
                        loader.setColumns(tabColumns);
                        Map<DataType, List<Map<String, Object>>> datas = new HashMap<>();

                        datas.put(MicroarrayModule.MAGE_ML_INPUT_TYPE, loader.load());
                        return datas;
                    }
                    finally
                    {
                        try { tsvIn.close(); } catch (IOException e) {}
                    }
                }
            }
        }
        catch (IOException ioe)
        {
            throw new ExperimentException(ioe);
        }
        catch (XMLStreamException e)
        {
            throw new ExperimentException(e);
        }
        finally
        {
            if (fIn != null) { try { fIn.close(); } catch (IOException e) {} }
        }
        return Collections.emptyMap();
    }
}
