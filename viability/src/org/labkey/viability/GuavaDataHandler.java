/*
 * Copyright (c) 2009 LabKey Corporation
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

import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.settings.AppProps;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * User: kevink
 * Date: Sep 22, 2009
 */
public class GuavaDataHandler extends ViabilityAssayDataHandler
{
    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (DATA_TYPE.matches(lsid))
        {
            File f = data.getFile();
            if (f.getName().toLowerCase().endsWith(".csv"))
                return Priority.HIGH;
        }
        return null;
    }

    protected Parser getParser(Domain runDomain, Domain resultsDomain, File dataFile)
    {
        return new Parser(runDomain, resultsDomain, dataFile);
    }

    public static class Parser extends ViabilityAssayDataHandler.Parser
    {
        public Parser(Domain runDomain, Domain resultsDomain, File dataFile)
        {
            super(runDomain, resultsDomain, dataFile);
        }

        protected void _parse() throws IOException, ExperimentException
        {
            _runData = new HashMap<DomainProperty, Object>();

            BufferedReader reader = null;
            try
            {
                reader = new BufferedReader(new FileReader(_dataFile));
                boolean foundBlankLine = false;
                String[] groupHeaders = null;
                String[] headers = null;
                int count = 0;
                String line;
                while (null != (line = reader.readLine()))
                {
                    String[] parts = line.split(",", -1); // include empty cells

                    if (!foundBlankLine)
                    {
                        if (line.length() == 0 || parts.length == 0 || parts[0].length() == 0)
                        {
                            foundBlankLine = true;
                        }
                        else if (_runDomain != null)
                        {
                            String firstCell = parts.length > 0 ? parts[0] : line;
                            String[] runMeta = firstCell.split(" - ", 2);
                            if (runMeta.length == 2 && runMeta[0].length() > 0 && runMeta[1].length() > 0)
                            {
                                DomainProperty property = _runDomain.getPropertyByName(runMeta[0].trim());
                                if (property != null)
                                {
                                    Object value = convert(property, runMeta[1].trim());
                                    if (value != null)
                                        _runData.put(property, value);
                                }
                            }
                        }
                    }
                    else
                    {
                        // skip blank lines after the first
                        if (line.length() == 0 || parts.length == 0)
                            continue;

                        if (groupHeaders == null)
                        {
                            groupHeaders = parts;
                        }
                        else if (headers == null)
                        {
                            headers = parts;
                            // found both header lines.
                            break;
                        }
                        else
                        {
                            assert false : "should have stopped parsing";
                        }
                    }

                    count++;
                }

                if (groupHeaders == null || headers == null)
                    throw new ExperimentException("Failed to find header rows in guava file");

                final int COL_SAMPLE_NUM = 0;
                final int COL_SAMPLE_ID = 1;
                final int COL_VIABLE = 11;
                final int COL_TOTAL_VIABLE = 42;
                final int COL_TOTAL_CELLS = 45;

                ColumnDescriptor[] columns = new ColumnDescriptor[headers.length];
                for (int i = 0; i < headers.length; i++)
                {
                    ColumnDescriptor cd = new ColumnDescriptor();
                    String expectHeader = null;
                    switch (i)
                    {
                        case COL_SAMPLE_NUM:
                            cd.name = ViabilityAssayProvider.SAMPLE_NUM_PROPERTY_NAME;
                            expectHeader = "Sample No.";
                            cd.clazz = Integer.class;
                            break;
                        case COL_SAMPLE_ID:
                            cd.name = ViabilityAssayProvider.POOL_ID_PROPERTY_NAME;
                            expectHeader = "Sample ID";
                            cd.clazz = String.class;
                            break;
                        case COL_VIABLE:
                            cd.name = "Viability"; // XXX: add constant to ViabilityAssayProvider?
                            expectHeader = "Viable";
                            cd.clazz = Double.class;
                            break;
                        case COL_TOTAL_VIABLE:
                            cd.name = ViabilityAssayProvider.VIABLE_CELLS_PROPERTY_NAME;
                            expectHeader = "Total Viable Cells in Original Sample";
                            cd.clazz = Double.class;
                            break;
                        case COL_TOTAL_CELLS:
                            cd.name = ViabilityAssayProvider.TOTAL_CELLS_PROPERTY_NAME;
                            expectHeader = "Total Cells in Original Sample";
                            cd.clazz = Double.class;
                            break;
                        default:
                            cd.load = false;
                    }

                    if (expectHeader != null)
                    {
                        assert cd.load;
                        if (headers[i] == null || !expectHeader.equals(headers[i].trim()))
                            throw new ExperimentException("Expected '" + expectHeader + "' in column " + i + " of header line; found '" + headers[i].trim() + "' instead.");
                    }

                    columns[i] = cd;
                }

                String expectHeader = "% of Total Information";
                if (groupHeaders[COL_VIABLE] == null || !expectHeader.equals(groupHeaders[COL_VIABLE].trim()))
                    throw new ExperimentException("Expected '" + expectHeader + "' in column " + COL_VIABLE + " of group headers line");

                TabLoader tl = new TabLoader(reader, false);
                tl.setColumns(columns);
                tl.setScanAheadLineCount(count);
                tl.parseAsCSV();
                _resultData = tl.load();
            }
            finally
            {
                if (reader != null) { try { reader.close(); } catch (IOException ioe) { } }
            }
        }
    }

    public static class TestCase extends junit.framework.TestCase
    {
        public TestCase() { super(); }
        public TestCase(String name) { super(name); }
        public static Test suite() { return new TestSuite(TestCase.class); }

        public void testGuava() throws Exception
        {
            AppProps props = AppProps.getInstance();
            String projectRootPath =  props.getProjectRoot();
            File projectRoot = new File(projectRootPath);

            File viabilityFiles = new File(projectRoot, "sampledata/viability");
            assertTrue("Expected to find viability test files: " + viabilityFiles.getAbsolutePath(), viabilityFiles.exists());

            GuavaDataHandler.Parser parser = new GuavaDataHandler.Parser(null, null, new File(viabilityFiles, /*"040609thaw.VIA.CSV"*/ "small.VIA.CSV"));

            List<Map<String, Object>> rows = parser.getResultData();
            assertEquals("Expected 6 rows", 6, rows.size());

            Map<String, Object> row = rows.get(0);
            assertEquals(7, row.size());
            assertEquals(1, row.get(ViabilityAssayProvider.SAMPLE_NUM_PROPERTY_NAME));
            assertEquals("160450533-5", row.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME));
            assertEquals("160450533", row.get(ViabilityAssayProvider.PARTICIPANTID_PROPERTY_NAME));
            assertEquals(5.0, row.get(ViabilityAssayProvider.VISITID_PROPERTY_NAME));
            assertEquals(84.5, row.get("Viability"));
            assertEquals(31268270.5, row.get(ViabilityAssayProvider.VIABLE_CELLS_PROPERTY_NAME));
            assertEquals(37003872.5, row.get(ViabilityAssayProvider.TOTAL_CELLS_PROPERTY_NAME));

            row = rows.get(rows.size()-1);
            assertEquals(5, row.size()); // can't extract participant and visit from pool
            assertEquals(34, row.get(ViabilityAssayProvider.SAMPLE_NUM_PROPERTY_NAME));
            assertEquals("159401872v5", row.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME));
            assertEquals(null, row.get(ViabilityAssayProvider.PARTICIPANTID_PROPERTY_NAME));
            assertEquals(null, row.get(ViabilityAssayProvider.VISITID_PROPERTY_NAME));
            assertEquals(95.4, row.get("Viability"));
            assertEquals(25878380.0, row.get(ViabilityAssayProvider.VIABLE_CELLS_PROPERTY_NAME));
            assertEquals(27126184.0, row.get(ViabilityAssayProvider.TOTAL_CELLS_PROPERTY_NAME));
        }
    }
}
