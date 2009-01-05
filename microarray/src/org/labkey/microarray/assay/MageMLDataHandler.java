/*
 * Copyright (c) 2008 LabKey Corporation
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

import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.Lsid;
import org.labkey.api.study.assay.AbstractAssayTsvDataHandler;
import org.labkey.microarray.MicroarrayModule;
import org.labkey.common.tools.SimpleXMLStreamReader;
import org.labkey.common.tools.TabLoader;
import org.labkey.common.tools.ColumnDescriptor;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * User: jeckels
 * Date: Jan 3, 2008
 */
public class MageMLDataHandler extends AbstractAssayTsvDataHandler
{
    public Priority getPriority(ExpData data)
    {
        if (MicroarrayModule.MAGE_ML_DATA_TYPE.matches(new Lsid(data.getLSID())))
        {
            return Priority.HIGH;
        }
        return null;
    }

    protected boolean allowEmptyData()
    {
        return true;
    }

    protected Map<String, Object>[] loadFileData(PropertyDescriptor[] columns, File dataFile) throws IOException, ExperimentException
    {
        if (columns.length == 0)
        {
            return new Map[0];
        }
        FileInputStream fIn = null;
        try
        {
            fIn = new FileInputStream(dataFile);
            SimpleXMLStreamReader reader = new SimpleXMLStreamReader(fIn);
            List<String> columnNames = new ArrayList<String>();
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
                        Map<String, Class> expectedColumns = new HashMap<String, Class>(columns.length);
                        for (PropertyDescriptor col : columns)
                            expectedColumns.put(col.getName().toLowerCase(), col.getPropertyType().getJavaType());
                        for (PropertyDescriptor col : columns)
                        {
                            if (col.getLabel() != null && !expectedColumns.containsKey(col.getLabel().toLowerCase()))
                            {
                                expectedColumns.put(col.getLabel().toLowerCase(), col.getPropertyType().getJavaType());
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
                        return (Map<String, Object>[]) loader.load();
                    }
                    finally
                    {
                        try { tsvIn.close(); } catch (IOException e) {}
                    }
                }
            }
        }
        catch (XMLStreamException e)
        {
            throw new ExperimentException(e);
        }
        finally
        {
            if (fIn != null) { try { fIn.close(); } catch (IOException e) {} }
        }
        throw new UnsupportedOperationException();
    }
}
