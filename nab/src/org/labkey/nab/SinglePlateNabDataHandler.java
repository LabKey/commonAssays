/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.assay.dilution.DilutionAssayProvider;
import org.labkey.api.assay.dilution.DilutionAssayRun;
import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.assay.dilution.SampleProperty;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.DataLoaderSettings;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.DataLoaderService;
import org.labkey.api.reader.ExcelFactory;
import org.labkey.api.reader.ExcelLoader;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.security.User;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.Position;
import org.labkey.api.study.WellData;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.plate.PlateUtils;
import org.labkey.api.util.FileType;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.nab.query.NabVirusDomainKind;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: brittp
 * Date: Sep 21, 2007
 * Time: 3:21:18 PM
 */
public class SinglePlateNabDataHandler extends NabDataHandler implements TransformDataHandler
{
    public static final AssayDataType NAB_DATA_TYPE = new AssayDataType("AssayRunNabData", new FileType(Arrays.asList(".xls", ".xlsx"), ".xls"));

    @Override
    protected String getPreferredDataFileExtension()
    {
        return "xls";
    }

    @Override
    public DataType getDataType()
    {
        return NAB_DATA_TYPE;
    }

    @Override
    protected DilutionAssayRun createDilutionAssayRun(DilutionAssayProvider provider, ExpRun run, List<Plate> plates, User user,
                                                      List<Integer> sortedCutoffs, StatsService.CurveFitType fit)
    {
        return new SinglePlateNabAssayRun(provider, run, plates.get(0), user, sortedCutoffs, fit);
    }

    protected double[][] getCellValues(final File dataFile, PlateTemplate nabTemplate) throws ExperimentException
    {
        final int expectedRows = nabTemplate.getRows();
        final int expectedCols = nabTemplate.getColumns();

        try
        {
            // Special case for excel - The ExcelLoader only returns data for a single sheet so we need to create a new ExcelLoader for each sheet
            if (ExcelLoader.isExcel(dataFile))
            {
                final Workbook workbook = ExcelFactory.create(dataFile);
                for (int i = 0, len = workbook.getNumberOfSheets(); i < len; i++)
                {
                    final int sheetNum = i;
                    Load load = new Load()
                    {
                        @Override
                        DataLoader createList()
                        {
                            return createLoader(true);
                        }

                        @Override
                        DataLoader createGrid()
                        {
                            return createLoader(false);
                        }

                        DataLoader createLoader(boolean hasColumnHeaders)
                        {
                            ExcelLoader loader = new ExcelLoader(workbook, hasColumnHeaders, null);
                            loader.setInferTypes(false);
                            loader.setIncludeBlankLines(true);
                            loader.setSheetIndex(sheetNum);
                            return loader;
                        }
                    };

                    double[][] values = parse(dataFile, load, expectedRows, expectedCols);
                    if (values != null)
                        return values;
                }
            }
            else
            {
                Load load = new Load()
                {
                    @Override
                    DataLoader createList() throws IOException, ExperimentException
                    {
                        return createLoader(dataFile, true);
                    }

                    @Override
                    DataLoader createGrid() throws IOException, ExperimentException
                    {
                        return createLoader(dataFile, false);
                    }
                };
                double[][] values = parse(dataFile, load, expectedRows, expectedCols);
                if (values != null)
                    return values;
            }
        }
        catch (InvalidFormatException | IOException e)
        {
            throw createParseError(dataFile, e.getMessage(), e);
        }

        throw createParseError(dataFile, dataFile.getName() + " does not appear to be a valid data file: no plate data was found.");
    }

    // Excel can only handle single sheets so this helper class abstracts away creating the loader for a specific sheet.
    // In addition we need to configure the loaders differently for parsing a list of data versus a grid of data without headers.
    protected abstract class Load
    {
        abstract DataLoader createList() throws IOException, ExperimentException;
        abstract DataLoader createGrid() throws IOException, ExperimentException;
    }

    protected double[][] parse(File dataFile, Load load, int expectedRows, int expectedCols) throws ExperimentException, IOException
    {
        // First, attempt to parse list-style data using column headers.
        DataLoader loader = load.createList();
        ColumnDescriptor[] columns = loader.getColumns();
        List<Map<String, Object>> rows = loader.load();
        if (columns != null && columns.length > 0)
        {
            // Last column is the results column -- only attempt parsing if it has a non-default name, e.g. "column5"
            String resultColumnHeader = columns[columns.length-1].name;
            if (!resultColumnHeader.equals("column" + (columns.length-1)))
            {
                List<ExperimentException> errors = new ArrayList<>();
                List<double[][]> plates = parseList(dataFile, rows, "Well", resultColumnHeader, 1, expectedRows, expectedCols, errors);
                if (!errors.isEmpty())
                {
                    LOG.warn("Unable to parse list style data from file (retrying using grid method) : " + errors.get(0).getMessage());
                }

                if (plates != null && !plates.isEmpty())
                {
                    return plates.get(0);
                }
            }

        }

        // Next, attempt to parse grid-style data without column headers.
        loader = load.createGrid();
        rows = loader.load();
        double[][] matrix = PlateUtils.parseGrid(dataFile, rows, expectedRows, expectedCols, null);
        if (matrix != null)
            return matrix;

        return null;
    }

    @NotNull
    protected DataLoader createLoader(File dataFile, boolean hasColumnHeaders) throws IOException, ExperimentException
    {
        final String fileName = dataFile.getName().toLowerCase();

        DataLoader loader = null;
        DataLoaderService svc = DataLoaderService.get();
        if (svc != null)
            loader = svc.createLoader(dataFile, null, hasColumnHeaders, null, ExcelLoader.FILE_TYPE);
        else
        {
            // BUGBUG: DataLoaderService isn't available when running JunitTest directly from IDE
            if (fileName.endsWith(".tsv") || fileName.endsWith(".txt"))
                loader = new TabLoader(dataFile, hasColumnHeaders, null);
            else if (fileName.endsWith(".csv"))
            {
                TabLoader csv = new TabLoader(dataFile, hasColumnHeaders, null);
                csv.parseAsCSV();
                loader = csv;
            }
            else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx"))
                loader = new ExcelLoader(dataFile, hasColumnHeaders);
        }

        if (loader == null)
            throw new ExperimentException("Unsupported plate format: " + fileName);

        // Don't infer types or convert values -- we will convert to numbers manually
        loader.setInferTypes(false);

        // Include blank lines so we can find the Nth row even if there are blank lines
        loader.setIncludeBlankLines(true);

        return loader;
    }


    @Override
    protected void prepareWellGroups(List<WellGroup> groups, ExpMaterial sampleInput, Map<String, DomainProperty> properties) throws ExperimentException
    {
        if (groups.size() != 1)
            throw new IllegalStateException("Expected exactly 1 well group per material for single-plate NAb runs.  Found " + groups.size());
        WellGroup group = groups.get(0);
        for (DomainProperty property : properties.values())
            group.setProperty(property.getName(), sampleInput.getProperty(property));

        List<? extends WellData> wells = group.getWellData(true);
        boolean reverseDirection = Boolean.parseBoolean((String) group.getProperty(SampleProperty.ReverseDilutionDirection.name()));

        // for the multi virus case, we need to produce dilution summaries over the intersection of the
        // sample and virus well groups
        Set<WellGroup> virusGroups = group.getOverlappingGroups(WellGroup.Type.VIRUS);
        if (!virusGroups.isEmpty())
        {
            Set<WellGroup> replicates = group.getOverlappingGroups(WellGroup.Type.REPLICATE);
            Map<String, WellGroup> replicateToVirusGroup = new HashMap<>();

            for (WellGroup replicateGroup : replicates)
            {
                for (Position p : replicateGroup.getPositions())
                {
                    for (WellGroup virusGroup : virusGroups)
                    {
                        if (virusGroup.contains(p))
                        {
                            // need to ensure replicate groups are completely contained
                            if (replicateToVirusGroup.containsKey(replicateGroup.getName()))
                            {
                                if (!replicateToVirusGroup.get(replicateGroup.getName()).equals(virusGroup))
                                    throw new ExperimentException("The replicate group: " + replicateGroup + " spans more than one virus group");
                            }
                            else
                                replicateToVirusGroup.put(replicateGroup.getName(), virusGroup);
                        }
                    }
                }
            }

            // loop over the replicate groups
            for (WellGroup virusGroup : virusGroups)
            {
                List<WellData> virusData = new ArrayList<>();
                for (WellData well : wells)
                {
                    if (well instanceof WellGroup)
                    {
                        String groupName = ((WellGroup)well).getName();
                        if (virusGroup.equals(replicateToVirusGroup.get(groupName)))
                            virusData.add(well);
                    }
                }
                applyDilution(virusData, sampleInput, properties, reverseDirection);
            }
        }
        else
            applyDilution(wells, sampleInput, properties, reverseDirection);
    }

    public void importTransformDataMap(ExpData data, AssayRunUploadContext context, ExpRun run, List<Map<String, Object>> dataMap) throws ExperimentException
    {
        importRows(data, run, context.getProtocol(), dataMap, context.getUser());
    }

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context, DataLoaderSettings settings) throws ExperimentException
    {
        DilutionDataFileParser parser = getDataFileParser(data, dataFile, info);

        Map<DataType, List<Map<String, Object>>> datas = new HashMap<>();
        datas.put(NAB_TRANSFORMED_DATA_TYPE, parser.getResults());

        return datas;
    }

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (NAB_DATA_TYPE.matches(lsid) || NAB_TRANSFORMED_DATA_TYPE.matches(lsid))
        {
            return Priority.HIGH;
        }
        return null;
    }

    @Override
    public void beforeDeleteData(List<ExpData> datas) throws ExperimentException
    {
        try
        {
            NabManager.get().deleteRunData(datas);
            for (ExpData data : datas)
            {
                ExpRun run = data.getRun();
                if (run != null)
                {
                    ExpProtocol protocol = data.getRun().getProtocol();
                    AssayProvider provider = AssayService.get().getProvider(protocol);
                    AssayProtocolSchema protocolSchema = provider.createProtocolSchema(null, protocol.getContainer(), protocol, null);
                    TableInfo virusTable = protocolSchema.createTable(DilutionManager.VIRUS_TABLE_NAME);
                    if (virusTable instanceof FilteredTable)
                    {
                        if (virusTable.getColumn(FieldKey.fromParts(NabVirusDomainKind.DATLSID_COLUMN_NAME)) != null)
                        {
                            TableInfo table = ((FilteredTable) virusTable).getRealTable();
                            SimpleFilter dataLsidFilter = new SimpleFilter(FieldKey.fromString(NabVirusDomainKind.DATLSID_COLUMN_NAME), data.getLSID());

                            // delete the rows in the virus table associated with this run
                            Table.delete(table, dataLsidFilter);
                        }
                    }
                }
            }
        }
        catch(SQLException e)
        {
            throw new ExperimentException(e);
        }
    }
}
