/*
 * Copyright (c) 2009-2016 LabKey Corporation
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

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.log4j.Logger;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.DataLoaderSettings;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.AssayUploadXarContext;
import org.labkey.api.util.FileType;
import org.labkey.api.view.ViewBackgroundInfo;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: kevink
 * Date: Sep 22, 2009
 */
public class GuavaDataHandler extends ViabilityAssayDataHandler implements TransformDataHandler
{
    public static final String NAMESPACE = "ViabilityAssay-GuavaData";
    private static final AssayDataType DATA_TYPE = new AssayDataType(NAMESPACE, new FileType(".csv"));

    @Override
    public DataType getDataType()
    {
        return DATA_TYPE;
    }

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (DATA_TYPE.matches(lsid) || OLD_DATA_TYPE.matches(lsid))
        {
            File f = data.getFile();
            if (f != null && f.getName() != null)
            {
                String lowerName = f.getName().toLowerCase();
                if (lowerName.endsWith(".csv"))
                    return Priority.HIGH;
            }
            return Priority.MEDIUM;
        }
        return null;
    }

    public Parser getParser(Domain runDomain, Domain resultsDomain, File dataFile)
    {
        return new Parser(runDomain, resultsDomain, dataFile);
    }

    public static class Parser extends ViabilityAssayDataHandler.Parser
    {
        private boolean shouldSplitPoolID = true;

        public Parser(Domain runDomain, Domain resultsDomain, File dataFile)
        {
            super(runDomain, resultsDomain, dataFile);
        }

        @Override
        protected boolean shouldSplitPoolID()
        {
            return shouldSplitPoolID;
        }

        protected void _parse() throws IOException, ExperimentException
        {
            _runData = new HashMap<>();

            boolean foundBlankLine = false;
            Map<String, String> runHeaders = new CaseInsensitiveHashMap<>();
            String[] groupHeaders = null;
            String[] headers = null;
            int count = 0;
            String line;

            try (BufferedReader reader = new BufferedReader(new FileReader(_dataFile)))
            {
                while (null != (line = reader.readLine()))
                {
                    String[] parts = line.split(",", -1); // include empty cells

                    if (!foundBlankLine)
                    {
                        if (line.length() == 0 || parts.length == 0 || parts[0].length() == 0)
                        {
                            foundBlankLine = true;
                        }
                        else
                        {
                            String firstCell = parts.length > 0 ? parts[0] : line;
                            String[] runMeta = firstCell.split(" - ", 2);
                            if (runMeta.length == 2 && runMeta[0].length() > 0 && runMeta[1].length() > 0)
                            {
                                String runHeaderKey = runMeta[0].trim();
                                String runHeaderValue = runMeta[1].trim();
                                runHeaders.put(runHeaderKey, runHeaderValue);

                                if (_runDomain != null)
                                {
                                    DomainProperty property = _runDomain.getPropertyByName(runHeaderKey);
                                    if (property == null && runHeaderKey.equalsIgnoreCase("Current Date"))
                                        property = _runDomain.getPropertyByName("Date");
                                    if (property != null)
                                    {
                                        Object value = convert(property, runHeaderValue);
                                        if (value != null)
                                            _runData.put(property, value);
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        // skip blank or entirely empty lines after the first
                        if (line.length() == 0 || parts.length == 0 || Arrays.stream(parts).allMatch(StringUtils::isEmpty))
                            continue;

                        // skip line containing disclaimer
                        if (parts.length > 2 && parts[1].contains("For Research Use Only."))
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

                if (runHeaders.isEmpty() || groupHeaders == null || headers == null)
                    throw new ExperimentException("Failed to find header rows in guava file");

                String softwareName = runHeaders.get("Software Name");
                if (softwareName == null)
                    throw new ExperimentException("Expected 'Software Name' to appear in first lines of guava file");

                ColumnDescriptor[] columns = null;
                if (softwareName.equals("ViaCount"))
                {
                    columns = createViaCountColumns(groupHeaders, headers);
                }
                else if (softwareName.equalsIgnoreCase("Count & Viability"))
                {
                    columns = createCountAndViabilityColumns(groupHeaders, headers);
                }
                else if (softwareName.equals("ExpressPlus"))
                {
                    // FIXME: The Letvin lab never uses visit based studies and have participant ids that contain '-' characters.
                    shouldSplitPoolID = false;
                    columns = createExpressPlusColumns(groupHeaders, headers);
                }

                TabLoader tl = new TabLoader(reader, false);
                tl.setColumns(columns);
                tl.setScanAheadLineCount(count);
                tl.parseAsCSV();

                if (softwareName.equals("ExpressPlus"))
                    _resultData = loadExpressPlus(tl.load());
                else
                    _resultData = tl.load();
            }
        }

        private ColumnDescriptor[] createColumns(
                String[] groupHeaders, String[] headers,
                final String sampleNumExpectedHeader,
                final int groupCol, final String groupExpectedHeader,
                final int viableCol, final String viableExpectedHeader,
                final int totalViableCol, final String totalViableExpectedHeader,
                final int totalCellsCol, final String totalCellsExpectedHeader,
                final int originalVolumeCol, final String originalVolumeExpectedHeader)
            throws ExperimentException
        {
            final int COL_SAMPLE_NUM = 0;
            final int COL_SAMPLE_ID = 1;

            String groupHeader = groupHeaders[groupCol];
            if (groupHeader == null || !groupExpectedHeader.equals(groupHeader.trim()))
                throw new ExperimentException("Expected '" + groupExpectedHeader + "' in column " + groupCol + " of group headers line; found '" + groupHeader.trim() + "' instead.");

            ColumnDescriptor[] columns = new ColumnDescriptor[headers.length];
            for (int i = 0; i < headers.length; i++)
            {
                ColumnDescriptor cd = new ColumnDescriptor();
                String expectHeader = null;

                if (i == COL_SAMPLE_NUM)
                {
                    cd.name = ViabilityAssayProvider.SAMPLE_NUM_PROPERTY_NAME;
                    expectHeader = sampleNumExpectedHeader;
                    cd.clazz = Integer.class;
                }
                else if (i == COL_SAMPLE_ID)
                {
                    cd.name = ViabilityAssayProvider.POOL_ID_PROPERTY_NAME;
                    expectHeader = "Sample ID";
                    cd.clazz = String.class;
                }
                else if (i == viableCol)
                {
                    cd.name = ViabilityAssayProvider.VIABILITY_PROPERTY_NAME;
                    expectHeader = viableExpectedHeader;
                    cd.clazz = Double.class;
                    cd.converter = new Converter()
                    {
                        public Object convert(Class type, Object value)
                        {
                            Double d = (Double) ConvertUtils.convert((String)value, Double.class);
                            if (d != null)
                                return d.doubleValue() / 100;
                            return null;
                        }
                    };
                }
                else if (i == totalViableCol)
                {
                    cd.name = ViabilityAssayProvider.VIABLE_CELLS_PROPERTY_NAME;
                    expectHeader = totalViableExpectedHeader;
                    cd.clazz = Double.class;
                }
                else if (i == totalCellsCol)
                {
                    cd.name = ViabilityAssayProvider.TOTAL_CELLS_PROPERTY_NAME;
                    expectHeader = totalCellsExpectedHeader;
                    cd.clazz = Double.class;
                }
                else if (i == originalVolumeCol)
                {
                    // NOTE: Currently not imported into the domain -- just used for calculations.
                    cd.name = "OriginalVolume";
                    expectHeader = originalVolumeExpectedHeader;
                    cd.clazz = Double.class;
                }
                else
                {
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

            return columns;
        }

        private ColumnDescriptor[] createViaCountColumns(String[] groupHeaders, String[] headers)
                throws ExperimentException
        {
            final int COL_GROUP = 11; // cell 'L'
            final int COL_VIABLE = 11; // cell 'L'
            final int COL_TOTAL_VIABLE = 42; // cell 'AQ'
            final int COL_TOTAL_CELLS = 45; // cell 'AT'

            return createColumns(groupHeaders, headers,
                    "Sample No.",
                    COL_GROUP, "% of Total Information",
                    COL_VIABLE, "Viable",
                    COL_TOTAL_VIABLE, "Total Viable Cells in Original Sample",
                    COL_TOTAL_CELLS, "Total Cells in Original Sample",
                    -1, null);
        }

        private ColumnDescriptor[] createCountAndViabilityColumns(String[] groupHeaders, String[] headers)
                throws ExperimentException
        {
            final int COL_GROUP = 9; // cell 'J'
            final int COL_VIABLE = 9; // cell 'J'
            final int COL_TOTAL_VIABLE = 30; // cell 'AE'
            final int COL_TOTAL_CELLS = 32; // cell 'AG'

            return createColumns(groupHeaders, headers,
                    "Sample No.",
                    COL_GROUP, "% of Total Information",
                    COL_VIABLE, "Viable",
                    COL_TOTAL_VIABLE, "Total Viable Cells in Original Sample",
                    COL_TOTAL_CELLS, "Total Cells in Original Sample",
                    -1, null);
        }

        // NOTE: These columns are hard coded to the Letvin sample file.  There doesn't seem to be any
        // indication in the file which group contains the actual viability information.
        private ColumnDescriptor[] createExpressPlusColumns(String[] groupHeaders, String[] headers)
                throws ExperimentException
        {
            final int COL_GROUP = 154; // cell 'EY'
            final int COL_VIABLE = COL_GROUP+2; // cell 'FA'
            final int COL_TOTAL_VIABLE = COL_GROUP+1; // cell 'EZ', need to multiply by original volume in cell 'G'
            // not in the spreadsheet: total cells = total viable / %viable
            //final int COL_TOTAL_CELLS = COL_GROUP+1;
            final int COL_ORIGINAL_VOLUME = 6; // cell 'G'

            return createColumns(groupHeaders, headers,
                    "Sample Number",
                    COL_GROUP, "Quad Stat - UpperLeft Region (Plot 3)",
                    COL_VIABLE, "% of Total Cells",
                    COL_TOTAL_VIABLE, "Cells/mL",
                    -1, null,
                    COL_ORIGINAL_VOLUME, "Original Volume");
        }

        // Adjust the total viable cells (based on volume) and calculate the total cells.
        private List<Map<String, Object>> loadExpressPlus(List<Map<String, Object>> rows)
        {
            List<Map<String, Object>> newRows = new ArrayList<>();
            for (Map<String, Object> row : rows)
            {
                Map<String, Object> newRow = new HashMap<>(row);
                Double volume = (Double)row.get("OriginalVolume");
                Double viableCells = volume * (Double)row.get(ViabilityAssayProvider.VIABLE_CELLS_PROPERTY_NAME);

                Double viability = (Double)row.get(ViabilityAssayProvider.VIABILITY_PROPERTY_NAME);
                Double totalCells = viableCells / viability;

                newRow.put(ViabilityAssayProvider.VIABLE_CELLS_PROPERTY_NAME, viableCells);
                newRow.put(ViabilityAssayProvider.TOTAL_CELLS_PROPERTY_NAME, totalCells);
                newRows.add(newRow);
            }

            return newRows;
        }

    }

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context, DataLoaderSettings settings) throws ExperimentException
    {
        Map<DataType, List<Map<String, Object>>> result = new HashMap<>();
        if (context instanceof AssayUploadXarContext)
        {
            List<Map<String, Object>> rows;
            AssayRunUploadContext<ViabilityAssayProvider> uploadContext = ((AssayUploadXarContext) context).getContext();
            if (uploadContext instanceof ViabilityAssayRunUploadForm)
            {
                // Importing via standard assay import wizard
                ViabilityAssayRunUploadForm form = (ViabilityAssayRunUploadForm) ((AssayUploadXarContext) context).getContext();
                rows = form.getResultProperties(null);
            }
            else
            {
                // Importing via ImportRunApiAction
                ViabilityAssayProvider provider = uploadContext.getProvider();
                ExpProtocol protocol = uploadContext.getProtocol();
                Domain runDomain = provider.getRunDomain(protocol);
                Domain resultsDomain = provider.getResultsDomain(protocol);

                // Parse with either Guava or TSV parser
                ViabilityAssayDataHandler.Parser parser = ViabilityAssayDataHandler.createParser(dataFile, runDomain, resultsDomain);
                rows = parser.getResultData();
                ViabilityAssayDataHandler.validateData(rows, false);
            }

            // Use the .tsv DATA_TYPE so the results will be read back in by the ViabilityTsvDataHandler after transormation
            result.put(ViabilityTsvDataHandler.DATA_TYPE, rows);
        }
        else
        {
            throw new ExperimentException("Unsupported import type: " + context);
        }
        return result;
    }


}
