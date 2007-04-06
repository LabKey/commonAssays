/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2;

import org.apache.log4j.Logger;
import org.labkey.api.data.*;
import org.labkey.api.data.Container;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.ms2.pipeline.MS2ImportPipelineJob;
//wch: mascotdev
import org.labkey.ms2.pipeline.MascotImportPipelineJob;
//END-wch: mascotdev
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.labkey.common.tools.MS2Modification;
import org.labkey.common.tools.PeptideProphetSummary;
import org.labkey.common.tools.RelativeQuantAnalysisSummary;
import org.labkey.api.util.Cache;
import org.labkey.api.util.Formats;
import org.labkey.api.view.Portal;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.WebPartFactory;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.TextAnchor;

import javax.xml.stream.XMLStreamException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
import java.util.List;

/**
 * User: arauch
 * Date: Mar 23, 2005
 * Time: 9:58:17 PM
 */
public class MS2Manager
{
    private static Logger _log = Logger.getLogger(MS2Manager.class);

    private static PeptideIndexCache _peptideIndexCache = new PeptideIndexCache();

    private static final String _ms2RunPrefix = "MS2Run/";
    private static final String _peptideProphetSummaryPrefix = "PeptideProphetSummary/";
    protected static final String _ms2RunPackage = MS2Run.class.getPackage().getName() + ".";

    public static DbSchema getSchema()
    {
        return DbSchema.get("ms2");
    }


    public static SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }


    // NOTE: DataRegion names are different from Table names.  We renamed all the tables to remove the MS2 prefix, but we want
    // old filters and saved views that refer to MS2* to continue to work.
    public static String getDataRegionNameProteins()
    {
        return "MS2Proteins";
    }

    public static String getDataRegionNameCompare()
    {
        return "MS2Compare";
    }

    public static String getDataRegionNamePeptides()
    {
        return "MS2Peptides";
    }

    public static String getDataRegionNameRuns()
    {
        return "MS2Runs";
    }

    public static String getDataRegionNameExperimentRuns()
    {
        return "MS2ExperimentRuns";
    }

    public static String getDataRegionNameProteinGroups()
    {
        return "ProteinGroupsWithQuantitation";
    }

    public static TableInfo getTableInfoCompare()
    {
        return getSchema().getTable("Compare");
    }

    public static TableInfo getTableInfoRuns()
    {
        return getSchema().getTable("Runs");
    }

    public static TableInfo getTableInfoExperimentRuns()
    {
        return getSchema().getTable("ExperimentRuns");
    }

    public static TableInfo getTableInfoProteinGroups()
    {
        return getSchema().getTable("ProteinGroups");
    }

    public static TableInfo getTableInfoProteinGroupsWithQuantitation()
    {
        return getSchema().getTable("ProteinGroupsWithQuantitation");
    }

    public static TableInfo getTableInfoProteinProphetFiles()
    {
        return getSchema().getTable("ProteinProphetFiles");
    }

    public static TableInfo getTableInfoQuantitation()
    {
        return getSchema().getTable("Quantitation");
    }

    public static TableInfo getTableInfoProteinQuantitation()
    {
        return getSchema().getTable("ProteinQuantitation");
    }

    public static TableInfo getTableInfoFractions()
    {
        return getSchema().getTable("Fractions");
    }

    public static TableInfo getTableInfoProteins()
    {
        return getSchema().getTable("Proteins");
    }

    public static TableInfo getTableInfoModifications()
    {
        return getSchema().getTable("Modifications");
    }

    public static TableInfo getTableInfoHistory()
    {
        return getSchema().getTable("History");
    }

    public static TableInfo getTableInfoPeptidesData()
    {
        return getSchema().getTable("PeptidesData");
    }

    public static TableInfo getTableInfoPeptides()
    {
        return getSchema().getTable("Peptides");
    }

    public static TableInfo getTableInfoSimplePeptides()
    {
        return getSchema().getTable("SimplePeptides");
    }

    public static TableInfo getTableInfoPeptideMemberships()
    {
        return getSchema().getTable("PeptideMemberships");
    }

    public static TableInfo getTableInfoSpectraData()
    {
        return getSchema().getTable("SpectraData");
    }

    public static TableInfo getTableInfoSpectra()
    {
        return getSchema().getTable("Spectra");
    }

    public static TableInfo getTableInfoProteinGroupMemberships()
    {
        return getSchema().getTable("ProteinGroupMemberships");
    }

    public static TableInfo getTableInfoPeptideProphetSummaries()
    {
        return getSchema().getTable("PeptideProphetSummaries");
    }

    public static TableInfo getTableInfoPeptideProphetData()
    {
        return getSchema().getTable("PeptideProphetData");
    }

    public static TableInfo getTableInfoQuantSummaries()
    {
        return getSchema().getTable("QuantSummaries");
    }

    public static Sort getRunsBaseSort()
    {
        return new Sort("-Run");
    }

    public static MS2Run getRun(int runId)
    {
        return getRun(String.valueOf(runId));
    }

    public static MS2Run getRunByFileName(String path, String fileName, Container c)
    {
        path = path.replace('\\', '/');
        MS2Run[] runs = getRuns("Path = ? AND runs.FileName = ? AND Deleted = ? AND Container = ?", path, fileName, Boolean.FALSE, c.getId());
        if (null == runs || runs.length == 0)
        {
            return null;
        }
        if (runs.length == 1)
        {
            return runs[0];
        }
        throw new IllegalStateException("There is more than one non-deleted MS2Run for " + path + "/" + fileName);
    }


    public static List<Integer> getRunIds(List<MS2Run> runs)
    {
        List<Integer> runIds = new ArrayList<Integer>(runs.size());

        for (MS2Run run : runs)
            runIds.add(run.getRun());

        return runIds;
    }

    public static ProteinProphetFile getProteinProphetFile(File f, Container c) throws SQLException, IOException
    {
        String sql = "SELECT " +
                getTableInfoProteinProphetFiles() + ".* FROM " +
                getTableInfoProteinProphetFiles() + ", " +
                getTableInfoRuns() + " WHERE " +
                getTableInfoProteinProphetFiles() + ".FilePath = ? AND " +
                getTableInfoProteinProphetFiles() + ".Run = " + getTableInfoRuns() + ".Run AND " +
                getTableInfoRuns() + ".Container = ? AND " +
                getTableInfoRuns() + ".Deleted = ?";

        ProteinProphetFile[] files = Table.executeQuery(getSchema(), sql, new Object[] { f.getCanonicalPath(), c.getId(), Boolean.FALSE }, ProteinProphetFile.class);
        if (files.length == 0)
        {
            return null;
        }
        if (files.length == 1)
        {
            return files[0];
        }
        throw new IllegalStateException("Expected a zero or one matching ProteinProphetFiles");
    }

    public static ProteinProphetFile getProteinProphetFileByRun(int runId) throws SQLException
    {
        return lookupProteinProphetFile(runId, "Run");
    }

    public static ProteinProphetFile getProteinProphetFile(int proteinProphetFileId) throws SQLException
    {
        return lookupProteinProphetFile(proteinProphetFileId, "RowId");
    }

    private static ProteinProphetFile lookupProteinProphetFile(int id, String columnName)
        throws SQLException
    {
        String sql = "SELECT " +
                getTableInfoProteinProphetFiles() + ".* FROM " +
                getTableInfoProteinProphetFiles() + ", " +
                getTableInfoRuns() + " WHERE " +
                getTableInfoProteinProphetFiles() + "." + columnName + " = ? AND " +
                getTableInfoProteinProphetFiles() + ".Run = " + getTableInfoRuns() + ".Run AND " +
                getTableInfoRuns() + ".Deleted = ?";

        ProteinProphetFile[] files = Table.executeQuery(getSchema(), sql, new Object[] { id, Boolean.FALSE }, ProteinProphetFile.class);
        if (files.length == 0)
        {
            return null;
        }
        if (files.length == 1)
        {
            return files[0];
        }
        throw new IllegalStateException("Expected a zero or one matching ProteinProphetFiles");
    }

    private static MS2Run[] getRuns(String whereClause, Object... params)
    {
        ResultSet rs = null;

        try
        {
            rs = Table.executeQuery(getSchema(),
                    "SELECT Container, Run, Description, Path, runs.FileName, Type, SearchEngine, MassSpecType, SearchEnzyme, runs.FastaId, ff.FileName AS FastaFileName, Loaded, Status, StatusId, Deleted, HasPeptideProphet, ExperimentRunLSID, PeptideCount, SpectrumCount, NegativeHitCount FROM " + getTableInfoRuns() + " runs LEFT OUTER JOIN " + ProteinManager.getTableInfoFastaFiles() + " ff ON runs.FastaId = ff.FastaId WHERE " + whereClause,
                    params);

            List<MS2Run> runs = new ArrayList<MS2Run>();

            while (rs.next())
            {
                String type = rs.getString("Type");
                Class<MS2Run> c;

                try
                {
                    c = (Class<MS2Run>)Class.forName(_ms2RunPackage + type + "Run");
                }
                catch (ClassNotFoundException e)
                {
                    _log.debug("Class not found (" + type + "Run) in MS2Manager.getRuns()");
                    return null;
                }

                BeanObjectFactory<MS2Run> bof = new BeanObjectFactory<MS2Run>(c);
                runs.add(bof.handle(rs));
            }

            return runs.toArray(new MS2Run[runs.size()]);
        }
        catch (SQLException e)
        {
            _log.error("getRuns", e);
            return null;
        }
        finally
        {
            if (rs != null)
            {
                try { rs.close(); } catch(SQLException e) { _log.error("Error closing ResultSet", e); }
            }
        }
    }

//wch: mascotdev
    public static MS2Importer.RunInfo addMascotRunToQueue(ViewBackgroundInfo info,
                                                    File file,
                                                    String description,
                                                    boolean appendLog) throws SQLException, IOException
    {
        MS2Importer importer = createImporter(file, info, description, null, new XarContext());
        MS2Importer.RunInfo runInfo = importer.prepareRun(false);
        MascotImportPipelineJob job = new MascotImportPipelineJob(info, file, description, runInfo, appendLog);
        PipelineService.get().queueJob(job);
        return runInfo;
    }
//END-wch: mascotdev

    public static MS2Importer.RunInfo addRunToQueue(ViewBackgroundInfo info,
                                                    File file,
                                                    String description,
                                                    boolean appendLog) throws SQLException, IOException
    {
        MS2Importer importer = createImporter(file, info, description, null, new XarContext());
        MS2Importer.RunInfo runInfo = importer.prepareRun(false);
        MS2ImportPipelineJob job = new MS2ImportPipelineJob(info, file, description, runInfo, appendLog);
        PipelineService.get().queueJob(job);
        return runInfo;
    }

    public static int addRun(PipelineJob job,
                             File file,
                             boolean restart, XarContext context) throws SQLException, IOException, XMLStreamException
    {
        MS2Importer importer = createImporter(file, job.getInfo(), file.getName() + " (" + job.getDescription() + ")", job.getLogger(), context);
        MS2Importer.RunInfo runInfo = importer.prepareRun(restart);

        return addRun(job, file, runInfo, context);
    }

    public static int addRun(PipelineJob job,
                             File file,
                             MS2Importer.RunInfo runInfo,
                             XarContext context) throws SQLException, IOException, XMLStreamException
    {
        ViewBackgroundInfo info = job.getInfo();
//        ensureWebParts(info);

        MS2Importer importer = createImporter(file, info, file.getName() + " (" + job.getDescription() + ")", job.getLogger(), context);
        return importer.upload(runInfo);
    }

    private static void ensureWebParts(ViewBackgroundInfo info)
            throws SQLException
    {
        Container c = info.getContainer();
        String path = info.getUrlHelper().getExtraPath();

        Portal.WebPart[] parts;
        Portal.WebPart partRuns = null;
        String partName = "MS2 Runs";
        int partIndex = 0;

        if (null == c)
        {
            // Try creating the container.
            c = ContainerManager.ensureContainer(path);
            if (null == c)
                throw new IllegalArgumentException("Unable to create container " + c);
        }
        else
        {
            parts = Portal.getParts(c.getId());
            for (final Portal.WebPart part : parts)
            {
                if (partName.equals(part.getName()))
                    partRuns = part;
                else if ("Pipeline Status".equals(part.getName()))
                    partIndex = 1;
            }
        }

        if (partRuns == null)
        {
            // Try to put MS2 Runs on the portal page.
            WebPartFactory desc = Portal.getPortalPart("MS2 Runs");
            if (null != desc)
            {
                Portal.addPart(c, desc, partIndex);
            }
        }
    }

    private static MS2Importer createImporter(File file, ViewBackgroundInfo info, String description, Logger log, XarContext context)
    {
        Container c = info.getContainer();

        String fileName = file.getPath();
        if (fileName.endsWith(".xml") || fileName.endsWith(".pepXML"))
            return new PepXmlImporter(info.getUser(), c, description, fileName, log, context);
        else
            return new CometReader(info.getUser(), c, description, fileName, log, context);
    }


    public static MS2Run getRun(String runId)
    {
        MS2Run run = _getRunFromCache(runId);

        if (null != run)
            return run;

        MS2Run[] runs = getRuns("Run = ?", runId);

        if (runs != null && runs.length == 1)
        {
            run = runs[0];
            // Cache only successfully loaded files so message updates as run loads
            if (run.getStatusId() == MS2Importer.STATUS_SUCCESS)
                _addRunToCache(runId, run);
        }

        return run;
    }

    public static RelativeQuantAnalysisSummary getQuantSummary(int quantId)
    {
        return Table.selectObject(getTableInfoQuantSummaries(), quantId, RelativeQuantAnalysisSummary.class);
    }

    public static RelativeQuantAnalysisSummary getQuantSummaryForRun(int runId)
    {
        SimpleFilter filter = new SimpleFilter("run", new Integer(runId));
        RelativeQuantAnalysisSummary[] summaries;
        try
        {
            summaries = Table.select(getTableInfoQuantSummaries(), Table.ALL_COLUMNS, filter, null, RelativeQuantAnalysisSummary.class);
        }
        catch (SQLException e)
        {
            _log.error("Error in getQuantSummaryForRun(" + runId + ")", e);
            return null;
        }
            
        if (null == summaries || summaries.length <= 0)
            return null;
        if (summaries.length > 1)
            _log.warn("Found more than one quantitation summary for run " + runId + "; using first");
        return summaries[0];
    }

    /**
     * Return the analysis type for relative quantitation in a given run.
     */
    public static String getQuantAnalysisType(int runId)
    {
        RelativeQuantAnalysisSummary summary = getQuantSummaryForRun(runId);
        return (null == summary ? null : summary.getAnalysisType());
    }

    /**
     * Return the algorithm used for relative quantitation in a given run.
     */
    public static String getQuantAnalysisAlgorithm(int runId)
    {
        RelativeQuantAnalysisSummary summary = getQuantSummaryForRun(runId);
        return (null == summary ? null : summary.getAnalysisAlgorithm());
    }

    public static PeptideProphetSummary getPeptideProphetSummary(int runId)
    {
        PeptideProphetSummary summary = _getPeptideProphetSummaryFromCache(runId);

        if (null == summary)
        {
            summary = Table.selectObject(getTableInfoPeptideProphetSummaries(), runId, PeptideProphetSummary.class);
            _addPeptideProphetSummaryToCache(summary);
        }

        return summary;
    }


    // We've already verified INSERT permission on newContainer
    // Now, verify DELETE permission on old container(s) and move runs to the new container
    public static void moveRuns(User user, List<MS2Run> runList, Container newContainer) throws UnauthorizedException
    {
        ResultSet rs = null;

        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (MS2Run run : runList)
        {
            sb.append(separator);
            sb.append(run.getRun());
            separator = ",";
        }
        String runListString = sb.toString();

        try
        {
            rs = Table.executeQuery(getSchema(), "SELECT DISTINCT Container FROM " + getTableInfoRuns() + " WHERE Run IN (" + runListString + ")", null);

            // Check for DELETE permission on all containers holding the requested runs
            // UI only allows moving from containers with DELETE permissions, but one could hack the request
            while (rs.next())
            {
                Container c = ContainerManager.getForId(rs.getString(1));
                if (!c.hasPermission(user, ACL.PERM_DELETE))
                    throw new UnauthorizedException();
            }

            Table.execute(getSchema(), "UPDATE " + getTableInfoRuns() + " SET Container=? WHERE Run IN (" + runListString + ")", new Object[]{newContainer.getId()});
        }
        catch (SQLException e)
        {
            _log.error("moveRuns: " + e);
        }
        finally
        {
            try
            {
                if (null != rs) rs.close();
            }
            catch (SQLException e)
            {
            }
        }

        _removeRunsFromCache(runListString);
    }

    public static void renameRun(int runId, String newDescription)
    {
        if (newDescription == null || newDescription.length() == 0)
            return;

        try
        {
            Table.execute(getSchema(), "UPDATE " + getTableInfoRuns() + " SET Description=? WHERE Run = ?",
                    new Object[]{newDescription, new Integer(runId)});
        }
        catch (SQLException e)
        {
            _log.error("renameRun", e);
        }

        _removeRunsFromCache(Integer.toString(runId));
    }

    // For safety, simply mark runs as deleted.  This allows them to be (manually) restored.
    public static void markAsDeleted(Integer[] runIds, Container c)
    {
        if (runIds.length == 0)
            return;

        StringBuffer runList = new StringBuffer();
        // Save these to delete after we've deleted the runs
        List<Integer> experimentRunsToDelete = new ArrayList<Integer>();

        for (int i = 0; i < runIds.length; i++)
        {
            if (i > 0) runList.append(",");
            runList.append(runIds[i]);

            MS2Run run = getRun(runIds[i]);
            if (run != null)
            {
                try
                {
                    File file = new File(run.getPath(), run.getFileName());
                    Data data = ExperimentService.get().getDataByURL(file, c);
                    if (data != null && data.getRunId() != null)
                    {
                        experimentRunsToDelete.add(data.getRunId());
                    }
                }
                catch (SQLException e)
                {
                    _log.error("markAsDeleted", e);
                }
                catch (MalformedURLException e)
                {
                    _log.error("markAsDeleted", e);
                }
                catch (IOException e)
                {
                    _log.error("markAsDeleted", e);
                }
            }
        }

        String authorizedRuns = "WHERE Container=? AND Run IN (" + runList + ")";

        try
        {
            Table.execute(getSchema(), "UPDATE " + getTableInfoRuns() + " SET Deleted=?, Modified=? " + authorizedRuns, new Object[]{Boolean.TRUE, new Date(), c.getId()});
        }
        catch (SQLException e)
        {
            _log.error("markAsDeleted", e);
        }

        _removeRunsFromCache(runList.toString());
        computeBasicMS2Stats();  // Update runIds/peptides statistics

        for (Integer experimentRunId : experimentRunsToDelete)
        {
            try
            {
                ExperimentService.get().deleteExperimentRun(experimentRunId, c);
            }
            catch (SQLException e)
            {
                _log.error("markAsDeleted", e);
            }
            catch (ExperimentException e)
            {
                _log.error("markAsDeleted", e);
            }
        }
    }


    public static void markAsDeleted(Container c)
    {
        try
        {
            Integer[] runIds = Table.executeArray(getSchema(), "SELECT Run FROM " + getTableInfoRuns() + " WHERE Container=?", new Object[]{c.getId()}, Integer.class);
            markAsDeleted(runIds, c);
        }
        catch (SQLException e)
        {
            _log.error("markAsDeleted", e);
        }
    }


    public static void markAsDeleted(String[] runIds, Container c)
    {
        Integer[] deleteRunIds = new Integer[runIds.length];
        for (int i = 0; i < runIds.length; i++)
        {
            deleteRunIds[i] = Integer.parseInt(runIds[i]);
        }
        markAsDeleted(deleteRunIds, c);
    }


    private static String _purgeStatus = null;

    public static String getPurgeStatus()
    {
        return _purgeStatus;
    }


    private static void setPurgeStatus(int complete, int count)
    {
        _purgeStatus = "In the process of purging runs: " + complete + " out of " + count + " runs complete (" + Formats.percent.format(((double)complete)/(double)count) + ").";
    }


    private static void clearPurgeStatus()
    {
        _purgeStatus = null;
    }


    // Purge all data associated with runs marked as deleted that were modified the specified number
    //    of days ago.  For example, purgeDeleted(14) purges all runs modified 14 days ago or
    //    before; purgeDeleted(0) purges ALL deleted runs.
    public static void purgeDeleted(int days) throws SQLException
    {
        // Status will be non-null if a purge thread is running; in that case, ignore the purge request.
        if (null != _purgeStatus)
            return;

        Calendar cutOff = Calendar.getInstance();
        cutOff.add(Calendar.DAY_OF_MONTH, -days);
        Date date = cutOff.getTime();

        Integer[] runIds = Table.executeArray(getSchema(), "SELECT Run FROM " + getTableInfoRuns() + " WHERE Deleted=? AND Modified <= ?", new Object[]{Boolean.TRUE, date}, Integer.class);

        // Don't bother with the thread if there are no runs to delete... prevents "0 runs to purge" status message
        if (0 == runIds.length)
            return;

        Thread thread = new Thread(new MS2Purger(runIds), "MS2Purger");
        thread.start();
    }

    public static MS2Run getRunByExperimentRunLSID(String lsid)
    {
        MS2Run[] runs = getRuns("ExperimentRunLSID = ? AND Deleted = ?", lsid, Boolean.FALSE);

        if (runs != null && runs.length == 1)
        {
            return runs[0];
        }
        return null;
    }

    public static Protein[] getProteinsForGroup(int rowId, int groupNumber, int indistinguishableCollectionId) throws SQLException
    {
        String sql = "SELECT seq.SeqId, seq.ProtSequence AS Sequence, seq.Mass, seq.Description, seq.BestName, seq.BestGeneName, fs.LookupString FROM " +
            getTableInfoProteinGroupMemberships() + " pgm," +
            getTableInfoProteinGroups() + " pg, " +
            getTableInfoProteinProphetFiles() + " ppf, " +
            getTableInfoRuns() + " r, " +
            ProteinManager.getTableInfoFastaSequences() + " fs, " +
            ProteinManager.getTableInfoSequences() + " seq " +
            "WHERE pg.RowId = pgm.ProteinGroupId " +
            "AND seq.SeqId = pgm.SeqId " +
            "AND pg.ProteinProphetFileId = ppf.RowId " +
            "AND ppf.Run = r.Run " +
            "AND fs.FastaId = r.FastaId " +
            "AND fs.SeqId = seq.SeqId " +
            "AND pg.GroupNumber = ? " +
            "AND pg.IndistinguishableCollectionId = ? " +
            "AND pg.ProteinProphetFileId = ?";
        return Table.executeQuery(getSchema(), sql, new Object[] {groupNumber, indistinguishableCollectionId, rowId}, Protein.class);
    }

    public static ProteinGroupWithQuantitation getProteinGroup(int proteinGroupId) throws SQLException
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition("RowId", proteinGroupId);
        ProteinGroupWithQuantitation[] groups = Table.select(getTableInfoProteinGroupsWithQuantitation(), Table.ALL_COLUMNS, filter, null, ProteinGroupWithQuantitation.class);
        if (groups.length == 0)
        {
            return null;
        }
        if (groups.length == 1)
        {
            return groups[0];
        }
        throw new IllegalStateException("Expected zero or one protein groups for rowId=" + proteinGroupId);

    }

    public static ProteinGroupWithQuantitation getProteinGroup(int proteinProphetFileId, int groupNumber, int indistinguishableCollectionId) throws SQLException
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition("ProteinProphetFileId", proteinProphetFileId);
        filter.addCondition("GroupNumber", groupNumber);
        filter.addCondition("IndistinguishableCollectionId", indistinguishableCollectionId);
        ProteinGroupWithQuantitation[] groups = Table.select(getTableInfoProteinGroupsWithQuantitation(), Table.ALL_COLUMNS, filter, null, ProteinGroupWithQuantitation.class);
        if (groups.length == 0)
        {
            return null;
        }
        if (groups.length == 1)
        {
            return groups[0];
        }
        throw new IllegalStateException("Expected zero or one protein groups for proteinProphetFileId=" + proteinProphetFileId + ", groupNumber=" + groupNumber + ", indistinguishableCollectionId=" + indistinguishableCollectionId);
    }


    private static class MS2Purger implements Runnable
    {
        private Integer[] _runIds;

        private MS2Purger(Integer[] runIds)
        {
            _runIds = runIds;
            setPurgeStatus(0, _runIds.length);
        }

        public void run()
        {
            try
            {
                int complete = 0;

                for (Integer runId : _runIds)
                {
                    purgeRun(runId);
                    complete++;
                    setPurgeStatus(complete, _runIds.length);
                }
            }
            catch(SQLException e)
            {
                _log.error("Error purging runs", e);
            }
            finally
            {
                clearPurgeStatus();
            }
        }
    }


    // Clear all the data in a run, then delete the run record itself
    public static void purgeRun(int run) throws SQLException
    {
        clearRun(run);
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoRuns() + " WHERE Run = ?", new Object[] {run});
    }


    // Clear contents of a single run, but not the run itself.  Used to reload after a failed attempt and to purge runs.
    public static void clearRun(int run) throws SQLException
    {
        Object[] params = new Object[] {run};
        purgeProteinProphetFiles("IN (SELECT RowId FROM " + getTableInfoProteinProphetFiles() + " WHERE Run = ?" + ")", new Object[] {run});

        String runWhere = " WHERE Run = ?";
        String fractionWhere = " WHERE Fraction IN (SELECT Fraction FROM " + getTableInfoFractions() + runWhere + ")";
        String peptideFKWhere = " WHERE PeptideId IN (SELECT RowId FROM " + getTableInfoPeptidesData() + fractionWhere + ")";

        Table.execute(getSchema(), "DELETE FROM " + getTableInfoSpectraData() + fractionWhere, params);
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoQuantSummaries() + runWhere, params);
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoQuantitation() + peptideFKWhere, params);
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoPeptideProphetData() + peptideFKWhere, params);
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoPeptidesData() + fractionWhere, params);
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoPeptideProphetSummaries() + runWhere, params);
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoModifications() + runWhere, params);
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoFractions() + runWhere, params);
    }


    public static void purgeProteinProphetFile(int rowId) throws SQLException
    {
        purgeProteinProphetFiles("= ?", new Object[]{rowId});
    }

    private static void purgeProteinProphetFiles(String rowIdComparison, Object[] params) throws SQLException
    {
        String proteinProphetFilesWhere = " WHERE ProteinProphetFileId " + rowIdComparison;
        String proteinGroupsWhere = " WHERE ProteinGroupId IN (SELECT RowId FROM " + getTableInfoProteinGroups() + proteinProphetFilesWhere + ")";

        Table.execute(getSchema(), "DELETE FROM " + getTableInfoProteinQuantitation() + proteinGroupsWhere, params);
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoPeptideMemberships() + proteinGroupsWhere, params);
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoProteinGroupMemberships() + proteinGroupsWhere, params);
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoProteinGroups() + proteinProphetFilesWhere, params);
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoProteinProphetFiles() + " WHERE RowId " + rowIdComparison, params);
    }

    public static MS2Fraction[] getFractions(int runId)
    {
        SimpleFilter filter = new SimpleFilter("run", new Integer(runId));

        try
        {
            return Table.select(getTableInfoFractions(), Table.ALL_COLUMNS, filter, null, MS2Fraction.class);
        }
        catch (SQLException e)
        {
            _log.error("getFractions", e);
            return new MS2Fraction[0];
        }
    }

//    private static String[] suffixes = new String[] {".pep.tgz", ".cmt.tar.gz", ".gz"};

    public static String getMzXMLPath(MS2Fraction fraction)
    {
        String baseName = fraction.getFileName();
        //TODO: Assume we have base path for this document to include all analyses
        baseName = baseName.substring(0, baseName.indexOf('.'));
        return "xml/" + baseName + ".mzXML";
    }

    public static String getRawPath(MS2Fraction fraction)
    {
        String baseName = fraction.getFileName();
        //TODO: Assume we have base path for this document to include all analyses
        baseName = baseName.substring(0, baseName.indexOf('.'));
        //TODO: Not always .RAW extension. Check where file really is
        return "raw/" + baseName + ".RAW";
    }

    public static MS2Modification[] getModifications(int run)
    {
        SimpleFilter filter = new SimpleFilter("run", new Integer(run));

        try
        {
            return Table.select(getTableInfoModifications(), Table.ALL_COLUMNS, filter, null, MS2Modification.class);
        }
        catch (SQLException e)
        {
            _log.error("getModifications", e);
            return new MS2Modification[0];
        }
    }

    public static MS2Peptide getPeptide(long peptideId) throws SQLException
    {
        Filter filter = new SimpleFilter("RowId", peptideId);
        return Table.selectObject(getTableInfoPeptides(), filter, null, MS2Peptide.class);
    }

    public static Quantitation getQuantitation(long peptideId)
    {
        Object[] pk = new Object[]{new Long(peptideId)};
        return Table.selectObject(getTableInfoQuantitation(), pk, Quantitation.class);
    }

    public static int verifyRowIndex(long[] index, int rowIndex, long peptideId)
    {
        if (rowIndex < 0 || rowIndex >= index.length)
            _log.error("RowIndex out of bounds " + rowIndex);
        else if (index[rowIndex] != peptideId)
            _log.error("Wrong peptideId found at rowIndex " + rowIndex + " in cached peptide index");
        else
            return rowIndex;

        for (int i=0; i<index.length; i++)
            if (index[i] == peptideId)
                return i;

        _log.error("Can't find peptideId " + peptideId + " in peptide index");
        return -1;
    }


    public static byte[] getSpectrum(int fractionId, int scan)
    {
        try
        {
            return Table.executeSingleton(getSchema(), "SELECT Spectrum FROM " + getTableInfoSpectraData() + " WHERE Fraction=? AND Scan=?", new Object[]{new Integer(fractionId), new Integer(scan)}, byte[].class);
        }
        catch (SQLException e)
        {
            _log.error("getSpectrum", e);
            return null;
        }
    }

    private static void _addRunToCache(String runId, MS2Run run)
    {
        Cache.getShared().put(_ms2RunPrefix + runId, run);
    }


    private static MS2Run _getRunFromCache(String runId)
    {
        return (MS2Run) Cache.getShared().get(_ms2RunPrefix + runId);
    }


    private static void _removeRunFromCache(String runId)
    {
        Cache.getShared().remove(_ms2RunPrefix + runId);
    }


    private static void _removeRunsFromCache(String runList)
    {
        String[] runs = runList.split(",");

        for (String run : runs)
            _removeRunFromCache(run);
    }


    private static void _addPeptideProphetSummaryToCache(PeptideProphetSummary summary)
    {
        Cache.getShared().put(_peptideProphetSummaryPrefix + summary.getRun(), summary);
    }


    private static PeptideProphetSummary _getPeptideProphetSummaryFromCache(int runId)
    {
        return (PeptideProphetSummary) Cache.getShared().get(_peptideProphetSummaryPrefix + runId);
    }


    private static DecimalFormat df = new DecimalFormat("#,##0");

    public static Map getStats(int days) throws SQLException
    {
        Map<String, String> stats = new HashMap<String, String>(20);

        addStats(stats, "successful", "Deleted = ? AND StatusId = 1", new Object[]{Boolean.FALSE});
        addStats(stats, "failed", "Deleted = ? AND StatusId = 2", new Object[]{Boolean.FALSE});
        addStats(stats, "deleted", "Deleted = ?", new Object[]{Boolean.TRUE});

        // For in-process runs, actually count the current number of peptides & spectra; counts in MS2Runs table aren't filled in until run is done loading
        addStatsWithCounting(stats, "inProcess", "Deleted = ? AND StatusId = 0", new Object[]{Boolean.FALSE});

        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_MONTH, -days);
        Date date = now.getTime();

        addStats(stats, "purged", "Deleted = ? AND Modified <= ?", new Object[]{Boolean.TRUE, date});

        return stats;
    }

    public static void updateMS2Application(int ms2RunId, String LSID) throws SQLException
    {
        String sql = " UPDATE " + getTableInfoRuns() + " SET ExperimentRunLSID = ? "
                + " WHERE Run = ? ;";

        Table.execute(getSchema(), sql, new Object[]{LSID, ms2RunId});
    }

    private static void addStats(Map<String, String> stats, String prefix, String whereSql, Object[] params) throws SQLException
    {
        ResultSet rs = Table.executeQuery(getSchema(), "SELECT COUNT(*) AS Runs, COALESCE(SUM(PeptideCount),0) AS Peptides, COALESCE(SUM(SpectrumCount),0) AS Spectra FROM " + getTableInfoRuns() + " WHERE " + whereSql, params);
        rs.next();

        stats.put(prefix + "Runs", df.format(rs.getObject(1)));
        stats.put(prefix + "Peptides", df.format(rs.getObject(2)));
        stats.put(prefix + "Spectra", df.format(rs.getObject(3)));

        rs.close();
    }


    private static void addStatsWithCounting(Map<String, String> stats, String prefix, String whereSql, Object[] params) throws SQLException
    {
        Long inProcessRuns = Table.executeSingleton(getSchema(), "SELECT COUNT(*) FROM " + getTableInfoRuns() + " WHERE " + whereSql, params, Long.class);
        Long inProcessPeptides = Table.executeSingleton(getSchema(), "SELECT COUNT(*) FROM " + getTableInfoPeptides() + " WHERE Run IN (SELECT Run FROM " + getTableInfoRuns() + " WHERE " + whereSql + ")", params, Long.class);
        Long inProcessSpectra = Table.executeSingleton(getSchema(), "SELECT COUNT(*) FROM " + getTableInfoSpectra() + " WHERE Run IN (SELECT Run FROM " + getTableInfoRuns() + " WHERE " + whereSql + ")", params, Long.class);

        stats.put(prefix + "Runs", df.format(inProcessRuns));
        stats.put(prefix + "Peptides", df.format(inProcessPeptides));
        stats.put(prefix + "Spectra", df.format(inProcessSpectra));
    }


    public static void computeBasicMS2Stats()
    {
        _basicStats = null;

        try
        {
            Map<String, String> stats = getBasicStats();
            long runs = df.parse(stats.get("Runs")).longValue();
            long peptides = df.parse(stats.get("Peptides")).longValue();
            insertStats(runs, peptides);
        }
        catch (SQLException e)
        {
            _log.error("computeBasicMS2Stats", e);
        }
        catch (ParseException e)
        {
            _log.error("computeBasicMS2Stats", e);
        }
    }


    // Cache the basic stats for the MS2 stats web part
    private static Map<String, String> _basicStats = null;

    public static Map<String, String> getBasicStats() throws SQLException
    {
        if (null == _basicStats)
            _basicStats = computeBasicStats();

        return _basicStats;
    }


    private static Map<String, String> computeBasicStats() throws SQLException
    {
        Map<String, String> stats = new HashMap<String, String>();
        addStats(stats, "", "DELETED = ? AND StatusId = 1", new Object[]{Boolean.FALSE});
        return stats;
    }


    private static void insertStats(long runs, long peptides)
    {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("date", new Date());
        m.put("runs", runs);
        m.put("peptides", peptides);

        try
        {
            Table.insert(null, getTableInfoHistory(), m);
        }
        catch (SQLException e)
        {
            _log.error("insertStats", e);
        }
    }


    public static MS2Fraction getFraction(int fractionId)
    {
        return Table.selectObject(getTableInfoFractions(), fractionId, MS2Fraction.class);
    }


    public static void updateRun(MS2Run run, User user) throws SQLException
    {
        Table.update(user, getTableInfoRuns(), run, run.getRun(), null);
    }


    public static void cachePeptideIndex(String key, long[] index)
    {
        _peptideIndexCache.put(key, index);
    }


    public static long[] getPeptideIndex(String key)
    {
        return _peptideIndexCache.get(key);
    }


    public static int getQuantitationCount(int run) throws SQLException
    {
        return Table.executeSingleton(getSchema(), "SELECT COUNT(*) FROM " + getTableInfoQuantitation() + " WHERE PeptideId IN (SELECT RowId FROM " + getTableInfoPeptides() + " WHERE Run = ?)", new Object[] { run }, Integer.class ).intValue();
    }

    public static int getProteinQuantitationCount(int run) throws SQLException
    {
        return Table.executeSingleton(getSchema(), "SELECT COUNT(*) FROM " + getTableInfoProteinQuantitation() + " WHERE ProteinGroupId IN (SELECT RowId FROM " + getTableInfoProteinGroups() + " WHERE ProteinProphetFileId IN (SELECT RowId FROM " + getTableInfoProteinProphetFiles() + " WHERE Run = ?))", new Object[] { run }, Integer.class ).intValue();
    }


    private static class PeptideIndexCache extends DatabaseCache<long[]>
    {
        private static int CACHE_SIZE = 10;
        private static long TIME_OUT = HOUR;

        public PeptideIndexCache()
        {
            super(getSchema().getScope(), CACHE_SIZE, TIME_OUT);
        }
    }


    public static long getRunCount(Container c)
            throws SQLException
    {
        return Table.executeSingleton(getSchema(), "SELECT COUNT(*) FROM " + getTableInfoRuns() + " WHERE Deleted= ? AND Container = ?", new Object[]{Boolean.FALSE, c.getId()}, Long.class);
    }

    // TODO: Make this a property stored with the pipeline root.
    private static final String negativeHitPrefix = "rev_";

    public static String getNegativeHitPrefix(Container c)
    {
        return negativeHitPrefix;
    }

    public static class XYSeriesROC extends XYSeries
    {
        private List<XYAnnotation> annotations = new ArrayList<XYAnnotation>();

        public XYSeriesROC(Comparable key)
        {
            super(key);
        }

        public void addAnnotation(XYAnnotation annotation)
        {
            annotations.add(annotation);
        }

        public void addFirstFalseAnnotation(String text, double x, double y)
        {
            XYPointerAnnotation pointer = new XYPointerAnnotation(
                text, x, y, 9.0 * Math.PI / 4.0
            );
            pointer.setBaseRadius(35.0);
            pointer.setTipRadius(2.0);
            pointer.setFont(new Font("SansSerif", Font.PLAIN, 9));
            pointer.setTextAnchor(TextAnchor.HALF_ASCENT_LEFT);
            addAnnotation(pointer);
            add(x, y);
        }

        public List<XYAnnotation> getAnnotations()
        {
            return annotations;
        }

        public void plotAnnotations(XYPlot plot, Paint paint)
        {
            for (XYAnnotation annotation : getAnnotations())
            {
                if (!(annotation instanceof XYPointerAnnotation))
                    continue;

                final XYPointerAnnotation pointer = (XYPointerAnnotation) annotation;

//                pointer.setPaint(paint);
                pointer.setArrowPaint(paint);

                plot.addAnnotation(annotation);
            }
        }
    }

    public static XYSeriesCollection getROCData(int[] runIds, boolean[][] discriminateFlags,
                                                double increment, int limitFalsePs, int[] marks,
                                                Container c)
    {
        String negHitPrefix = getNegativeHitPrefix(c);

        XYSeriesCollection collection = new XYSeriesCollection();
        for (int i = 0; i < runIds.length; i++)
        {
            MS2Run run = getRun(runIds[i]);
            if (run == null)
                continue;

            long runRows = run.getPeptideCount();

            String[] discriminates = run.getDiscriminateExpressions().split("\\s*,\\s*");
            for (int j = 0; j < discriminates.length; j++)
            {
                if (discriminateFlags[i] == null ||
                        discriminateFlags[i].length <= j ||
                        !discriminateFlags[i][j])
                    continue;
                final String discriminate = discriminates[j];
                String key = run.getDescription();
                if (discriminates.length > 1)
                    key += " - " + discriminate;
                XYSeriesROC series = new XYSeriesROC(key);

                if (run.statusId == 0)
                    series.setKey(series.getKey() + " (Loading)");
                else
                {
                    ResultSet rs = null;
                    try
                    {
                        rs = Table.executeQuery(getSchema(),
                                                    "SELECT Protein, " + discriminate + " as Expression " +
                                                    "FROM " + getTableInfoPeptides().getFromSQL() + " " +
                                                    "WHERE Run = ? " +
                                                    "ORDER BY Expression",
                                                    new Object[] { run.getRun() });

                        int rows = 0;
                        int falsePositives = 0;
                        int iMark = 0;

                        series.add(0.0, 0.0);

                        points_loop:
                        for (int k = 1; falsePositives < limitFalsePs; k++)
                        {
                            double cutoff = k * increment / 100.0;
                            while ((((double) rows) / (double) runRows) < cutoff &&
                                    falsePositives < limitFalsePs)
                            {
                                if (!rs.next())
                                    break points_loop;
                                if (rs.getString("Protein").startsWith(negHitPrefix))
                                {
                                    // If this is the first false positive, create a point
                                    // with an annotation for it.
                                    if (iMark < marks.length && falsePositives == marks[iMark])
                                    {
                                        series.addFirstFalseAnnotation(rs.getString("Expression"),
                                                falsePositives, rows - falsePositives);

                                        iMark++;
                                    }
                                    falsePositives++;
                                }
                                rows++;
                            }
                            series.add(falsePositives, rows - falsePositives);
                        }
                    }
                    catch (SQLException e)
                    {
                        series.setKey(series.getKey() + " (Error)");
                        series.clear();
                        _log.error("Error getting ROC data.", e);
                    }
                    finally
                    {
                        if (rs != null)
                        {
                            try { rs.close(); } catch(SQLException e) { _log.error("Error closing ResultSet", e); }
                        }
                    }
                }

                collection.addSeries(series);
            }
        }

        return collection;
    }

    public static XYSeriesCollection getDiscriminateROCData(int runId,
                                                         String[] expressions,
                                                         double increment,
                                                         int limitFalsePs,
                                                         int[] marks,
                                                         Container c)
    {
        String negHitPrefix = getNegativeHitPrefix(c);

        XYSeriesCollection collection = new XYSeriesCollection();
        MS2Run run = getRun(runId);
        if (run != null && run.statusId != 0)
        {
            long runRows = run.getPeptideCount();

            String key = run.getDescription();
            XYSeriesROC series = new XYSeriesROC(key);

            ResultSet rs = null;
            try
            {
                rs = Table.executeQuery(getSchema(),
                                            "SELECT Protein, " +
                                                " case" +
                                                    " when Charge = 1 then " + expressions[0] +
                                                    " when Charge = 2 then " + expressions[1] +
                                                    " else " + expressions[2] +
                                                " end as Expression " +
                                            "FROM " + getTableInfoPeptides().getFromSQL() + " " +
                                            "WHERE Run = ? " +
                                            "ORDER BY Expression DESC",
                                            new Object[] { new Integer(runId) });
                if (!rs.next())
                    return collection;

                int rows = 0;
                int falsePositives = 0;
                int iMark = 0;

                series.add(0.0, 0.0);

                points_loop:
                for (int k = 1; falsePositives < limitFalsePs; k++)
                {
                    double cutoff = k * increment / 100.0;
                    while ((((double) rows) / (double) runRows) < cutoff &&
                            falsePositives < limitFalsePs)
                    {
                        if (!rs.next())
                            break points_loop;
                        if (rs.getString("Protein").startsWith(negHitPrefix))
                        {
                            if (iMark < marks.length && falsePositives == marks[iMark])
                            {
                                series.addFirstFalseAnnotation(rs.getString("Expression"),
                                        falsePositives, rows - falsePositives);

                                iMark++;
                            }
                            falsePositives++;
                        }
                        rows++;
                    }
                    series.add(falsePositives, rows - falsePositives);
                }
            }
            catch (SQLException e)
            {
                series.setKey(series.getKey() + " (Error)");
                series.clear();
                _log.error("Error getting ROC data.", e);
            }
            finally
            {
                if (rs != null)
                {
                    try { rs.close(); } catch(SQLException e) { _log.error("Error closing ResultSet", e); }
                }
            }

            collection.addSeries(series);
        }

        return collection;
    }

    public static XYSeriesCollection getDiscriminateData(int runId,
                                                         int charge,
                                                         double percentAACorrect,
                                                         final String expression,
                                                         double bucket,
                                                         int scaleFactor,
                                                         Container c)
    {
        String negHitPrefix = getNegativeHitPrefix(c);

        XYSeriesCollection collection = new XYSeriesCollection();
        MS2Run run = getRun(runId);
        if (run != null && run.statusId != 0)
        {
            String keyCorrect = "Correct";
            if (scaleFactor != 1)
                keyCorrect += " - " + scaleFactor + "x";
            XYSeries seriesCorrect = new XYSeries(keyCorrect);
            XYSeries seriesFP = new XYSeries("False-Positives");
            //Set<SpectrumId> seen = new HashSet<SpectrumId>();

            ResultSet rs = null;
            try
            {
                rs = Table.executeQuery(getSchema(),
                                            "SELECT Fraction, Scan, Charge, Protein, " + expression + " as Expression " +
                                            "FROM " + getTableInfoPeptides().getFromSQL() + " " +
                                            "WHERE Run = ? " +
                                            "ORDER BY Expression",
                                            new Object[] { new Integer(runId) });
                if (!rs.next())
                    return collection;

                int rows = 0;
                double startChart = rs.getDouble(5);

                double cutoffLast = startChart;
                int falsePositivesLast = 0;
                int correctIdsLast = 0;
                int k = 1;

                points_loop:
                for (;; k++)
                {
                    int falsePositives = 0;
                    int correctIds = 0;

                    double cutoff = startChart + (k * bucket);
                    while (rs.getDouble(5) < cutoff)
                    {
                        if (!rs.next())
                            break points_loop;

                        if (rs.getInt(3) != charge)
                            continue;

                        if (rs.getString(4).startsWith(negHitPrefix))
                            falsePositives++;
                        else
                            correctIds++;
                        rows++;
                    }
                    if (falsePositives > 0 || falsePositivesLast > 0)
                    {
                        if (falsePositivesLast <= 0)
                            seriesFP.add(cutoffLast, 0);
                        seriesFP.add(cutoff, falsePositives);
                    }
                    correctIds = (int) Math.max(0.0, correctIds - (falsePositives * percentAACorrect / 100.0));
                    if (correctIds > 0 || correctIdsLast > 0)
                    {
                        if (correctIdsLast <= 0)
                            seriesCorrect.add(cutoffLast, 0);
                        seriesCorrect.add(cutoff, correctIds * scaleFactor);
                    }

                    cutoffLast = cutoff;
                    falsePositivesLast = falsePositives;
                    correctIdsLast = correctIds;
                }

                if (falsePositivesLast > 0)
                    seriesFP.add(startChart + (k * bucket), 0);
                if (correctIdsLast > 0)
                    seriesCorrect.add(startChart + (k * bucket), 0);
            }
            catch (SQLException e)
            {
                seriesFP.setKey(seriesFP.getKey() + " (Error)");
                seriesFP.clear();
                seriesCorrect.setKey(seriesCorrect.getKey() + " (Error)");
                seriesCorrect.clear();
                _log.error("Error getting descriminate data.", e);
            }
            finally
            {
                if (rs != null)
                {
                    try { rs.close(); } catch(SQLException e) { _log.error("Error closing ResultSet", e); }
                }
            }

            collection.addSeries(seriesFP);
            collection.addSeries(seriesCorrect);
        }

        return collection;
    }
}
