/*
 * Copyright (c) 2006-2010 LabKey Corporation
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

import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.study.assay.AssayFileWriter;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

/**
 * User: jeckels
 * Date: May 7, 2012
 */
public class MigrateNAbPipelineJob extends PipelineJob
{
    private final String _description;
    private final int _protocolId;

    public MigrateNAbPipelineJob(ViewBackgroundInfo info, int protocolId, PipeRoot root) throws SQLException
    {
        super("NAbUpgrade", info, root);
        _protocolId = protocolId;
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
                    // TODO - do we need to filter to avoid plate templates, Elispot runs, etc?
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

                    // Iterate over all of the runs and migrate them
                    for (Map<String, Object> row : rows)
                    {
                        int rowId = ((Number)row.get("RowId")).intValue();
                        int createdBy = ((Number)row.get("CreatedBy")).intValue();
                        Date created = (Date)row.get("Created");
                        try
                        {
                            migrateRun(protocol, rowId, createdBy, created, provider, migratedNAbDir);
                        }
                        catch (Exception e)
                        {
                            getLogger().error("Failed to migrate run " + rowId, e);
                        }
                    }
                }
                else
                {
                    getLogger().info("No plate query found in " + getInfo().getContainer().getPath());
                }
            }
            else
            {
                getLogger().info("No plate schema found in " + getInfo().getContainer().getPath());
            }
            setStatus(PipelineJob.COMPLETE_STATUS);
            completeStatus = true;
        }
        catch (Exception e)
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

    /** Perform the conversion for a single run */
    private void migrateRun(ExpProtocol protocol, int rowId, int createdBy, Date created, NabAssayProvider provider, File migratedNAbDir) throws Exception
    {
        LegacyNAbUploadContext context = new LegacyNAbUploadContext(protocol, provider, rowId, getInfo());
        getLogger().info("Starting to migrate run " + rowId + ": " + context.getLegacyRun().getName());
        InputStream in = null;
        try
        {
            String fileName = context.getLegacyRun().getName();
            // Legacy NAb stores the Excel file as an Attachment (BLOB), so grab it and write it to disk 
            in = AttachmentService.get().getInputStream(context.getLegacyRun().getPlate(), fileName);
            if (in == null)
            {
                getLogger().error("Could not migrate run id " + rowId + " (" + fileName + " because data file is not available");
            }
            else
            {
                File dataFile = AssayFileWriter.findUniqueFileName(fileName, migratedNAbDir);
                writeFile(dataFile, in);
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
                Table.execute(ExperimentService.get().getSchema(), runFixupSQL);
            }
        }
        finally
        {
            if (in != null) { try { in.close(); } catch (IOException e) {} }
        }
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
