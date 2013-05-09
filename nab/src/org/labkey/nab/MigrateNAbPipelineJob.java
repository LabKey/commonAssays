/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.CoreSchema;
import org.labkey.api.data.ExcelWriter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.study.DataSet;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.Study;
import org.labkey.api.study.StudyService;
import org.labkey.api.study.TimepointType;
import org.labkey.api.study.assay.AssayFileWriter;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayPublishKey;
import org.labkey.api.study.assay.AssayPublishService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.assay.dilution.DilutionDataHandler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: May 7, 2012
 */
public class MigrateNAbPipelineJob extends PipelineJob
{
    private final String _description;
    private final int _protocolId;
    /** Non-standard NAb run field values that may be supplied by the admin who initiates the migration */
    private final Map<String, String> _params;

    public MigrateNAbPipelineJob(ViewBackgroundInfo info, int protocolId, PipeRoot root, Map<String, String> params) throws SQLException
    {
        super("NAbUpgrade", info, root);
        _protocolId = protocolId;
        _params = params;
        _description = "Migrate NAb runs to " + getAssayDesign().getName();

        // Create a unique file name for the log file
        String logFileName = "NAbMigration-" + DateUtil.formatDateTime(new Date(), "yyyy-MM-dd-HH-mm-ss") + ".log";
        setLogFile(root.resolvePath(logFileName));

        getLogger().info(_description);
    }

    private ExpProtocol getAssayDesign()
    {
        return ExperimentService.get().getExpProtocol(_protocolId);
    }

    public ActionURL getStatusHref()
    {
        return null;
    }

    public String getDescription()
    {
        return _description;
    }

    public void run()
    {
        if (!setStatus("MIGRATING"))
        {
            return;
        }
        int failedRuns = 0;

        ExpProtocol protocol = getAssayDesign();
        NabAssayProvider provider = (NabAssayProvider)AssayService.get().getProvider(protocol);

        boolean completeStatus = false;
        try
        {
            UserSchema schema = QueryService.get().getUserSchema(getInfo().getUser(), getInfo().getContainer(), "Plate");
            if (schema != null)
            {
                TableInfo tableInfo = schema.getTable("Plate");
                if (tableInfo != null)
                {
                    // We just need the rowIds and the Created and CreatedBy data for all the runs in this container at this point
                    Map<String, Object>[] rows = new TableSelector(tableInfo, PageFlowUtil.set("RowId", "CreatedBy", "Created"), null, null).getArray(Map.class);
                    getLogger().info("Found " + rows.length + " runs in " + getInfo().getContainer().getPath());

                    // Create the directory the file will live in
                    File migratedNAbDir = getPipeRoot().resolvePath("migratedNAb");
                    if (!NetworkDrive.exists(migratedNAbDir))
                    {
                        migratedNAbDir.mkdirs();
                    }
                    if (!migratedNAbDir.isDirectory())
                    {
                        throw new IOException("Unable to create directory " + migratedNAbDir);
                    }

                    Map<String, Integer> legacyLSIDsToAssayRowIds = new HashMap<String, Integer>();

                    // Iterate over all of the runs and migrate them
                    for (Map<String, Object> row : rows)
                    {
                        int rowId = ((Number)row.get("RowId")).intValue();
                        int createdBy = ((Number)row.get("CreatedBy")).intValue();
                        Date created = (Date)row.get("Created");
                        try
                        {
                            legacyLSIDsToAssayRowIds.putAll(migrateRunData(protocol, rowId, createdBy, created, provider, migratedNAbDir));
                        }
                        catch (Exception e)
                        {
                            failedRuns++;
                            getLogger().error("Failed to migrate run " + rowId, e);
                        }
                    }

                    migrateDatasetData(protocol, legacyLSIDsToAssayRowIds, provider);
                }
                else
                {
                    throw new IllegalStateException("No plate query found in " + getInfo().getContainer().getPath());
                }
            }
            else
            {
                throw new IllegalStateException("No plate schema found in " + getInfo().getContainer().getPath());
            }

            if (failedRuns > 0)
            {
                throw new IllegalStateException("Failed to migrate " + failedRuns + " runs. See above for errors");
            }
            setStatus(PipelineJob.COMPLETE_STATUS);
            completeStatus = true;
        }
        catch (Throwable e)
        {
            getLogger().error("NAb migration failed", e);
        }
        finally
        {
            if (!completeStatus)
            {
                setStatus(PipelineJob.ERROR_STATUS);
            }
        }
    }

    private void migrateDatasetData(ExpProtocol protocol, Map<String, Integer> legacyLSIDsToAssayRowId, NabAssayProvider provider) throws SQLException
    {
        getLogger().info("Starting to re-copy migrated data to study datasets");
        // Find all the legacy NAB datasets
        SQLFragment datasetSQL = new SQLFragment("SELECT Container, DatasetId FROM study.dataset WHERE Name='NAB' AND ProtocolId IS NULL AND KeyPropertyName = ?", OldNabManager.PlateProperty.VirusId.name());
        Map<String, Object>[] datasets = new SqlSelector(CoreSchema.getInstance().getSchema().getScope(), datasetSQL).getArray(Map.class);

        getLogger().info("Found " + datasets.length + " potential legacy NAb datasets");

        for (Map<String, Object> dataset : datasets)
        {
            String containerId = (String)dataset.get("Container");
            Container container = ContainerManager.getForId(containerId);
            int datasetId = ((Number)dataset.get("DatasetId")).intValue();

            List<Map<String, Object>> datasetRows = findMatchingDatasetRows(container, datasetId, new ArrayList<String>(legacyLSIDsToAssayRowId.keySet()));

            getLogger().info("Found " + datasetRows.size() + " rows that had been copied to NAb dataset in " + container.getPath());

            if (!datasetRows.isEmpty())
            {
                // Found some dataset rows. Need to recopy, using the same Participant and SequenceNum/Date combination
                Study study = StudyService.get().getStudy(container);
                Map<Integer, AssayPublishKey> publishKeys = new HashMap<Integer, AssayPublishKey>(datasetRows.size());
                for (Map<String, Object> row : datasetRows)
                {
                    String participantId = (String)row.get(study.getSubjectColumnName());
                    String sourceLSID = (String)row.get(AssayPublishService.SOURCE_LSID_PROPERTY_NAME);
                    Integer newRowId = legacyLSIDsToAssayRowId.get(sourceLSID);
                    if (newRowId == null)
                    {
                        throw new IllegalStateException("Could not find a new RowId for SourceLSID " + sourceLSID);
                    }

                    AssayPublishKey publishKey;
                    if (study.getTimepointType() == TimepointType.VISIT)
                    {
                        Number sequenceNum = (Number)row.get(AssayPublishService.SEQUENCENUM_PROPERTY_NAME);
                        publishKey = new AssayPublishKey(study.getContainer(), participantId, sequenceNum.floatValue(), newRowId);
                    }
                    else
                    {
                        Date date = (Date)row.get(AssayPublishService.DATE_PROPERTY_NAME);
                        publishKey = new AssayPublishKey(study.getContainer(), participantId, date, newRowId);
                    }
                    publishKeys.put(newRowId, publishKey);
                }

                List<String> errors = new ArrayList<String>();
                provider.copyToStudy(getUser(), getContainer(), protocol, study.getContainer(), publishKeys, errors);
                if (!errors.isEmpty())
                {
                    getLogger().error("Errors when copying to study in " + container.getPath());
                    getLogger().error(errors);
                    throw new IllegalStateException("Errors when copying to study");
                }
            }
        }
    }

    /** @return all rows in the requested dataset that have been copied from the set of legacy NAb LSIDs */
    private List<Map<String, Object>> findMatchingDatasetRows(Container container, int datasetId, List<String> legacyLSIDs)
            throws SQLException
    {
        DataSet set = StudyService.get().getDataSet(container, datasetId);
        TableInfo datasetTableInfo = set.getTableInfo(getUser(), false);

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        // Batch it up to stay below any limits for IN clauses or JDBC parameters
        final int batchSize = 200;

        for (int i = 0; i < legacyLSIDs.size(); i += batchSize)
        {
            List<String> subList = legacyLSIDs.subList(i, Math.min(i + batchSize, legacyLSIDs.size()));
            SimpleFilter filter = new SimpleFilter(new SimpleFilter.InClause(AssayPublishService.SOURCE_LSID_PROPERTY_NAME, subList));
            result.addAll(Arrays.asList((Map<String, Object>[])Table.select(datasetTableInfo, Table.ALL_COLUMNS, filter, null, Map.class)));
        }

        return result;
    }

    /**
     * Perform the conversion for a single run
     * @return legacy LSID (as would be present as SourceLSID in study datasets) -> new RowId that would be copied to a study dataset
     */
    private Map<String, Integer> migrateRunData(ExpProtocol protocol, int rowId, int createdBy, Date created, NabAssayProvider provider, File migratedNAbDir) throws Exception
    {
        LegacyNAbUploadContext context = new LegacyNAbUploadContext(protocol, provider, rowId, getInfo(), _params);
        getLogger().info("Starting to migrate run " + rowId + ": " + context.getLegacyRun().getName());
        InputStream in = null;
        try
        {
            String fileName = context.getLegacyRun().getName();
            File dataFile = AssayFileWriter.findUniqueFileName(fileName, migratedNAbDir);

            // Legacy NAb stores the Excel file as an Attachment (BLOB), so grab it and write it to disk
            try
            {
                in = AttachmentService.get().getInputStream(context.getLegacyRun().getPlate(), fileName);
                writeFile(dataFile, in);
            }
            catch (FileNotFoundException e)
            {
                getLogger().warn("Could not find original data file for run id " + rowId + " (" + fileName + "), creating a new file");
                extractNAbFileFromDatabase(rowId, context, dataFile);
            }

            context.setFile(dataFile);

            ExpRun run = AssayService.get().createExperimentRun(fileName, getInfo().getContainer(), protocol, dataFile);
            provider.getRunCreator().saveExperimentRun(context, null, run, false);

            // Set the Created and CreatedBy values to their values in the legacy run. Do this via direct SQL
            // since all of the normal APIs set them based on the current user and time.
            SQLFragment runFixupSQL = new SQLFragment("UPDATE " + ExperimentService.get().getTinfoExperimentRun() +
                    " SET Created = ?, CreatedBy = ? WHERE RowId = ?");
            runFixupSQL.add(created);
            runFixupSQL.add(createdBy);
            runFixupSQL.add(run.getRowId());
            new SqlExecutor(ExperimentService.get().getSchema()).execute(runFixupSQL);

            // Get the mapping from specimen name ("Specimen 1", "Specimen 2", etc) to LSID that would have been
            // copied to a study dataset
            Map<String, String> legacyLSIDs = context.getSpecimensToLSIDs();

            // Now figure out the mapping from specimen name to Id for the newly created run
            AssayProtocolSchema schema = provider.createProtocolSchema(getUser(), getContainer(), protocol, null);
            TableInfo resultsTableInfo = schema.createDataTable(false);
            FieldKey runFK = provider.getTableMetadata(protocol).getRunRowIdFieldKeyFromResults();
            FieldKey rowIdFK = provider.getTableMetadata(protocol).getResultRowIdFieldKey();
            FieldKey specimenNameFK = FieldKey.fromParts("Properties", DilutionDataHandler.WELLGROUP_NAME_PROPERTY);

            // Do the query to get the new run's info
            Map<FieldKey, ColumnInfo> cols = QueryService.get().getColumns(resultsTableInfo, Arrays.asList(rowIdFK, specimenNameFK));
            ColumnInfo rowIdCol = cols.get(rowIdFK);
            ColumnInfo specimenNameCol = cols.get(specimenNameFK);
            Map<String, Object>[] newDataRows = new TableSelector(resultsTableInfo, cols.values(), new SimpleFilter(runFK.toString(), run.getRowId()), null).getArray(Map.class);

            // Figure out the RowId for the new assay run
            Map<String, Integer> result = new HashMap<String, Integer>();

            for (Map.Entry<String, String> entry : legacyLSIDs.entrySet())
            {
                String name = entry.getKey();
                String legacyLsid = entry.getValue();
                Integer newRowId = findRowId(newDataRows, name, specimenNameCol, rowIdCol);
                if (newRowId != null)
                {
                    result.put(legacyLsid, newRowId);
                }
                else
                {
                    throw new IllegalStateException("Could not find new RowId for LSID " + legacyLsid);
                }
            }

            return result;
        }
        finally
        {
            if (in != null) { try { in.close(); } catch (IOException ignored) {} }
        }
    }

    private void extractNAbFileFromDatabase(int rowId, LegacyNAbUploadContext context, File dataFile)
            throws SQLException, IOException
    {
        // Pull the plate (with its well data) from the database
        Plate plate = PlateService.get().getPlate(context.getContainer(), rowId);
        Workbook workbook = ExcelWriter.ExcelDocumentType.xls.createWorkbook();
        // Create a mostly empty first sheet, since NAb expects the well data on the second sheet
        Sheet firstSheet = workbook.createSheet("List ; Plates 1 - 1");
        Cell infoCell = firstSheet.createRow(0).createCell(0, Cell.CELL_TYPE_STRING);
        infoCell.setCellValue(message(rowId, context));

        Sheet dataSheet = workbook.createSheet("Plate");
        Cell infoCell2 = dataSheet.createRow(0).createCell(0, Cell.CELL_TYPE_STRING);
        infoCell2.setCellValue(message(rowId, context));
        // NAb expects the data to start at A7 (column 0, row 6)
        for (int rowNum = 0; rowNum < plate.getRows(); rowNum++)
        {
            Row row = dataSheet.createRow(rowNum + OldNabManager.START_ROW);
            for (int colNum = 0; colNum < plate.getColumns(); colNum++)
            {
                Cell cell = row.createCell(colNum + OldNabManager.START_COL, Cell.CELL_TYPE_NUMERIC);
                cell.setCellValue(plate.getWell(rowNum, colNum).getValue());
            }
        }
        // Write it to disk
        FileOutputStream fOut = new FileOutputStream(dataFile);
        try
        {
            workbook.write(fOut);
        }
        finally
        {
            fOut.close();
        }
    }

    private String message(int rowId, LegacyNAbUploadContext context)
    {
        return "File reconstructed by extracting NAb run data from " + context.getContainer().getPath() +", legacy RunId " + rowId + ". This is not the original instrument output file.";
    }

    private Integer findRowId(Map<String, Object>[] newDataRows, String name, ColumnInfo specimenNameCol, ColumnInfo rowIdCol)
    {
        for (Map<String, Object> newDataRow : newDataRows)
        {
            if (name.equals(newDataRow.get(specimenNameCol.getAlias())))
            {
                return ((Number)newDataRow.get(rowIdCol.getAlias())).intValue();
            }
        }
        return null;
    }

    /** Write the content of the input stream to the file on disk */
    private void writeFile(File file, InputStream in) throws IOException
    {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        try
        {
            byte[] bytes = new byte[4096];
            int i;
            while ((i = in.read(bytes)) != -1)
            {
                out.write(bytes, 0, i);
            }
        }
        finally
        {
            try { out.close(); } catch (IOException ignored) {}
        }
    }
}
