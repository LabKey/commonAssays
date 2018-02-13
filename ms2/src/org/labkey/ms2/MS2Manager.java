/*
 * Copyright (c) 2005-2016 Fred Hutchinson Cancer Research Center
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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.fhcrc.cpas.exp.xml.ExperimentArchiveDocument;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.TextAnchor;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.cache.StringKeyCache;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.BeanObjectFactory;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DatabaseCache;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.Filter;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.AbstractFileXarSource;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.XarSource;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.Formats;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.PepXMLFileType;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.AbstractMS2SearchTask;
import org.labkey.ms2.pipeline.MS2ImportPipelineJob;
import org.labkey.ms2.pipeline.TPPTask;
import org.labkey.ms2.pipeline.mascot.MascotImportPipelineJob;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.fasta.FastaFile;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.reader.ITraqProteinQuantitation;
import org.labkey.ms2.reader.LibraQuantResult;
import org.labkey.ms2.reader.MascotDatLoader;
import org.labkey.ms2.reader.PeptideProphetSummary;
import org.labkey.ms2.reader.RandomAccessMzxmlIterator;
import org.labkey.ms2.reader.RandomAccessMzxmlIteratorFactory;
import org.labkey.ms2.reader.RelativeQuantAnalysisSummary;
import org.labkey.ms2.reader.SimpleScan;

import javax.xml.stream.XMLStreamException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: arauch
 * Date: Mar 23, 2005
 * Time: 9:58:17 PM
 */
public class MS2Manager
{
    private static Logger _log = Logger.getLogger(MS2Manager.class);

    private static PeptideIndexCache _peptideIndexCache = new PeptideIndexCache();

    private static final String FRACTION_CACHE_PREFIX = "MS2Fraction/";
    private static final StringKeyCache<MS2Fraction> FRACTION_CACHE = CacheManager.getSharedCache();
    private static final String PEPTIDE_PROPHET_SUMMARY_CACHE_PREFIX = "PeptideProphetSummary/";
    private static final StringKeyCache<PeptideProphetSummary> PEPTIDE_PROPHET_SUMMARY_CACHE = CacheManager.getSharedCache();
    private static final String RUN_CACHE_PREFIX = "MS2Run/";
    private static final StringKeyCache<MS2Run> RUN_CACHE = CacheManager.getSharedCache();

    public static DbSchema getSchema()
    {
        return DbSchema.get("ms2", DbSchemaType.Module);
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

    public static TableInfo getTableInfoFastaRunMapping()
    {
        return getSchema().getTable("FastaRunMapping");
    }

    public static TableInfo getTableInfoRuns()
    {
        return getSchema().getTable("Runs");
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

    public static TableInfo getTableInfoITraqPeptideQuantitation()
    {
        return getSchema().getTable("iTraqPeptideQuantitation");
    }

    public static TableInfo getTableInfoITraqProteinQuantitation()
    {
        return getSchema().getTable("iTraqProteinQuantitation");
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

    public static TableInfo getTableInfoExpressionData()
    {
        return getSchema().getTable("ExpressionData");
    }

    public static MS2Run getRun(int runId)
    {
        MS2Run run = _getRunFromCache(runId);

        if (null != run)
            return run;

        MS2Run[] runs = getRuns("Run = ? AND deleted = ?", runId, false);

        if (runs != null && runs.length == 1)
        {
            run = runs[0];
            // Cache only successfully imported files so message updates as run is imported
            if (run.getStatusId() == MS2Importer.STATUS_SUCCESS)
                _addRunToCache(runId, run);
        }

        return run;
    }

    public static MS2Run getRunByFileName(String path, String fileName, Container c)
    {
        path = path.replace('\\', '/');
        MS2Run[] runs = getRuns("LOWER(Path) = LOWER(?) AND LOWER(runs.FileName) = LOWER(?) AND Deleted = ? AND Container = ?", path, fileName, Boolean.FALSE, c.getId());
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

    public static ExpRun ensureWrapped(MS2Run run, User user) throws ExperimentException
    {
        ExpRun expRun;
        if (run.getExperimentRunLSID() != null)
        {
            expRun = ExperimentService.get().getExpRun(run.getExperimentRunLSID());
            if (expRun != null && expRun.getContainer().equals(run.getContainer()))
            {
                return expRun;
            }
        }
        return wrapRun(run, user);
    }

    private static ExpRun wrapRun(MS2Run run, User user) throws ExperimentException
    {
        try (DbScope.Transaction transaction = ExperimentService.get().getSchema().getScope().ensureTransaction())
        {
            Container container = run.getContainer();
            final File pepXMLFile = new File(run.getPath(), run.getFileName());

            // Check if this 
            ExpData existingPepXmlData = ExperimentService.get().getExpDataByURL(pepXMLFile, container);
            if (existingPepXmlData == null)
            {
                // Maybe the casing on one of the parent directories has changed
                existingPepXmlData = ExperimentService.get().getExpDataByURL(pepXMLFile.toURI().toString(), container);
            }
            if (existingPepXmlData != null)
            {
                ExpRun existingRun = existingPepXmlData.getRun();
                if (existingRun != null)
                {
                    // There's an existing experiment run but somehow it got disconnected
                    run.setExperimentRunLSID(existingRun.getLSID());
                    MS2Manager.updateRun(run, user);
                    return existingRun;
                }
            }

            // Make sure that we have a protocol in this folder
            Lsid lsid = new Lsid("Protocol.Folder-" + container.getRowId(), MS2Schema.IMPORTED_SEARCH_PROTOCOL_OBJECT_PREFIX);
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(lsid.toString());
            if (protocol == null)
            {
                protocol = ExperimentService.get().createExpProtocol(container, ExpProtocol.ApplicationType.ProtocolApplication, "MS2 Import", lsid.toString());
                protocol.setMaxInputMaterialPerInstance(0);
                protocol = ExperimentService.get().insertSimpleProtocol(protocol, user);
            }

            ExpRun expRun = ExperimentService.get().createExperimentRun(container, run.getDescription());
            expRun.setProtocol(protocol);
            Map<ExpData, String> inputDatas = new HashMap<>();
            Map<ExpData, String> outputDatas = new HashMap<>();
            XarSource source = new AbstractFileXarSource("Wrap MS2 Run", container, user)
            {
                public File getLogFile() throws IOException
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public File getRoot()
                {
                    return pepXMLFile.getParentFile(); 
                }

                @Override
                public ExperimentArchiveDocument getDocument() throws XmlException, IOException
                {
                    throw new UnsupportedOperationException();
                }
            };

            for (int fastaId : run.getFastaIds())
            {
                FastaFile fastaFile = ProteinManager.getFastaFile(fastaId);
                File file = new File(fastaFile.getFilename());
                ExpData fastaData = ExperimentService.get().getExpDataByURL(file, container);
                if (fastaData == null)
                {
                    fastaData = ExperimentService.get().createData(file.toURI(), source);
                }
                inputDatas.put(fastaData, AbstractMS2SearchTask.FASTA_INPUT_ROLE);
            }

            for (MS2Fraction fraction : run.getFractions())
            {
                if (fraction.getMzXmlURL() != null)
                {
                    URI mzXMLURI = new URI(fraction.getMzXmlURL());
                    ExpData mzXmlData = ExperimentService.get().getExpDataByURL(new File(mzXMLURI), container);
                    if (mzXmlData == null)
                    {
                        mzXmlData = ExperimentService.get().createData(mzXMLURI, source);
                    }
                    inputDatas.put(mzXmlData, AbstractMS2SearchTask.SPECTRA_INPUT_ROLE);
                }
            }

            ExpData pepXMLData = ExperimentService.get().getExpDataByURL(pepXMLFile, container);
            if (pepXMLData == null)
            {
                pepXMLData = ExperimentService.get().createData(pepXMLFile.toURI(), source);
            }
            outputDatas.put(pepXMLData, TPPTask.PEP_XML_INPUT_ROLE);

            if (run.hasProteinProphet())
            {
                ProteinProphetFile proteinProphet = run.getProteinProphetFile();
                File protXMLFile = new File(proteinProphet.getFilePath());
                ExpData protXMLData = ExperimentService.get().getExpDataByURL(protXMLFile, container);
                if (protXMLData == null)
                {
                    protXMLData = ExperimentService.get().createData(protXMLFile.toURI(), source);
                }
                else
                {
                    // If it's somehow connected with an existing run, break the old association
                    protXMLData.setSourceApplication(null);
                }
                outputDatas.put(protXMLData, TPPTask.PROT_XML_INPUT_ROLE);
            }

            expRun.setFilePathRoot(pepXMLFile.getParentFile());
            ViewBackgroundInfo info = new ViewBackgroundInfo(container, user, null);
            expRun = ExperimentService.get().saveSimpleExperimentRun(expRun, Collections.emptyMap(), inputDatas, Collections.emptyMap(),
                    outputDatas, Collections.emptyMap(), info, _log, false);

            // Set the MS2 run to point at this experiment run
            run.setExperimentRunLSID(expRun.getLSID());
            MS2Manager.updateRun(run, user);

            // Ensure that any custom data handlers have a chance to do their magic
            pepXMLData.findDataHandler().importFile(pepXMLData, pepXMLFile, info, _log, source.getXarContext());

            transaction.commit();
            return expRun;
        }
        catch (URISyntaxException e)
        {
            throw new ExperimentException(e);
        }
    }

    public static List<Integer> getRunIds(List<MS2Run> runs)
    {
        List<Integer> runIds = new ArrayList<>(runs.size());

        for (MS2Run run : runs)
            runIds.add(run.getRun());

        return runIds;
    }

    public static ProteinProphetFile getProteinProphetFile(File f, Container c)
    {
        String sql = "SELECT " +
                getTableInfoProteinProphetFiles() + ".* FROM " +
                getTableInfoProteinProphetFiles() + ", " +
                getTableInfoRuns() + " WHERE " +
                getTableInfoProteinProphetFiles() + ".FilePath = ? AND " +
                getTableInfoProteinProphetFiles() + ".Run = " + getTableInfoRuns() + ".Run AND " +
                getTableInfoRuns() + ".Container = ? AND " +
                getTableInfoRuns() + ".Deleted = ?";

        String path;
        try
        {
            path = f.getCanonicalPath();
        }
        catch (IOException e)
        {
            path = f.getAbsolutePath();
        }

        return new SqlSelector(getSchema(), sql, path, c, Boolean.FALSE).getObject(ProteinProphetFile.class);
    }

    public static ProteinProphetFile getProteinProphetFileByRun(int runId)
    {
        return lookupProteinProphetFile(runId, "Run");
    }

    public static ProteinProphetFile getProteinProphetFile(int proteinProphetFileId)
    {
        return lookupProteinProphetFile(proteinProphetFileId, "RowId");
    }

    private static ProteinProphetFile lookupProteinProphetFile(int id, String columnName)
    {
        String sql = "SELECT " +
                getTableInfoProteinProphetFiles() + ".* FROM " +
                getTableInfoProteinProphetFiles() + ", " +
                getTableInfoRuns() + " WHERE " +
                getTableInfoProteinProphetFiles() + "." + columnName + " = ? AND " +
                getTableInfoProteinProphetFiles() + ".Run = " + getTableInfoRuns() + ".Run AND " +
                getTableInfoRuns() + ".Deleted = ?";

        return new SqlSelector(getSchema(), sql, id, Boolean.FALSE).getObject(ProteinProphetFile.class);
    }

    private static MS2Run[] getRuns(String whereClause, Object... params)
    {
        List<MS2Run> runs = new ArrayList<>();

        try (ResultSet rs = new SqlSelector(getSchema(),
                    "SELECT Container, Run, Description, Path, runs.FileName, Type, SearchEngine, MassSpecType, SearchEnzyme, Status, StatusId, Deleted, HasPeptideProphet, ExperimentRunLSID, PeptideCount, SpectrumCount, NegativeHitCount, MascotFile, DistillerRawFile FROM " + getTableInfoRuns() + " runs WHERE " + whereClause,
                    params).getResultSet())
        {
            while (rs.next())
            {
                String type = rs.getString("Type");

                MS2RunType runType = MS2RunType.lookupType(type, null);
                if (runType != null)
                {
                    BeanObjectFactory<MS2Run> bof = new BeanObjectFactory<>((Class<MS2Run>)runType.getRunClass());
                    runs.add(bof.handle(rs));
                }
                else
                {
                    _log.debug("MS2RunType \"" + type + "\" not found");
                    return null;
                }
            }
        }
        catch (SQLException e)
        {
            _log.error("getRuns", e);
            throw new RuntimeSQLException(e);
        }

        return runs.toArray(new MS2Run[runs.size()]);
    }

    public static MS2Importer.RunInfo addMascotRunToQueue(ViewBackgroundInfo info,
                                                          File file,
                                                          String description, PipeRoot root) throws SQLException, IOException
    {
        MS2Importer importer = createImporter(file, info, description, null, new XarContext(description, info.getContainer(), info.getUser()));
        MS2Importer.RunInfo runInfo = importer.prepareRun(false);
        MascotImportPipelineJob job = new MascotImportPipelineJob(info, file, description, runInfo, root);
        try
        {
            PipelineService.get().queueJob(job);
        }
        catch (PipelineValidationException e)
        {
            throw new IOException(e);
        }
        return runInfo;
    }

    public static MS2Importer.RunInfo addRunToQueue(ViewBackgroundInfo info,
                                                    File file,
                                                    String description, PipeRoot root) throws SQLException, IOException
    {
        MS2Importer importer = createImporter(file, info, description, null, new XarContext(description, info.getContainer(), info.getUser()));
        MS2Importer.RunInfo runInfo = importer.prepareRun(false);
        MS2ImportPipelineJob job = new MS2ImportPipelineJob(info, file, description, runInfo, root);
        try
        {
            PipelineService.get().queueJob(job);
        }
        catch (PipelineValidationException e)
        {
            throw new IOException(e);
        }
        return runInfo;
    }

    public static MS2Run addRun(ViewBackgroundInfo info, Logger log,
                             File file,
                             boolean restart, XarContext context) throws SQLException, IOException, XMLStreamException
    {
        MS2Importer importer = createImporter(file, info, file.getName() + (context.getJobDescription() != null ? " - " + context.getJobDescription() : ""), log, context);
        MS2Importer.RunInfo runInfo = importer.prepareRun(restart);

        return importRun(info, log, file, runInfo, context);
    }

    // help deal with fact that TPP treats xml.gz as a native format
    public static boolean endsWithExtOrExtDotGZ(String name,String ext)
    {  
        return name.endsWith(ext) || name.endsWith(ext+".gz");
    }
    
    public static MS2Run importRun(ViewBackgroundInfo info, Logger log,
                                   File file,
                                   MS2Importer.RunInfo runInfo,
                                   XarContext context) throws IOException, XMLStreamException
    {
        MS2Importer importer = createImporter(file, info, file.getName() + (context.getJobDescription() != null ? file.getName() + " (" + context.getJobDescription() + ")" : ""), log, context);
        return importer.upload(runInfo);
    }

    private static MS2Importer createImporter(File file, ViewBackgroundInfo info, String description, Logger log, XarContext context) throws IOException
    {
        Container c = info.getContainer();

        String fileName = file.getPath();
        if (endsWithExtOrExtDotGZ(fileName,".xml") || fileName.endsWith(".pepXML"))
            return new PepXmlImporter(info.getUser(), c, description, fileName, log, context);
        else if (fileName.toLowerCase().endsWith(".dat"))
            return new MascotDatImporter(info.getUser(), c, description, fileName, log, context);
        else
            throw new IOException("Unable to import file type '" + file + "'.");
    }

    public static MS2Run getRun(String runId)
    {
        try
        {
            return getRun(Integer.parseInt(runId));
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    public static RelativeQuantAnalysisSummary getQuantSummaryForRun(int runId)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("run"), runId);

        return new TableSelector(getTableInfoQuantSummaries(), filter, null).getObject(RelativeQuantAnalysisSummary.class);
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
            summary = new TableSelector(getTableInfoPeptideProphetSummaries()).getObject(runId, PeptideProphetSummary.class);
            _addPeptideProphetSummaryToCache(summary);
        }

        return summary;
    }


    // We've already verified INSERT permission on newContainer
    // Now, verify DELETE permission on old container(s) and move runs to the new container
    public static void moveRuns(final User user, List<MS2Run> runList, Container newContainer) throws UnauthorizedException
    {
        List<Integer> runIds = new ArrayList<>(runList.size());

        for (MS2Run run : runList)
            runIds.add(run.getRun());

        SQLFragment selectSQL = new SQLFragment("SELECT DISTINCT Container FROM " + getTableInfoRuns() + " ");
        SimpleFilter inClause = new SimpleFilter();
        inClause.addInClause(FieldKey.fromParts("Run"), runIds);
        SQLFragment runSQL = inClause.getSQLFragment(getSqlDialect());
        selectSQL.append(runSQL);

        // Check for DELETE permission on all containers holding the requested runs
        // UI only allows moving from containers with DELETE permissions, but one could hack the request
        new SqlSelector(getSchema(), selectSQL).forEach(new Selector.ForEachBlock<String>()
        {
            @Override
            public void exec(String containerId) throws SQLException
            {
                Container c = ContainerManager.getForId(containerId);  // TODO: Switch to ForEachBlock<Container>
                if (!c.hasPermission(user, DeletePermission.class))
                    throw new UnauthorizedException();
            }
        }, String.class);

        SQLFragment updateSQL = new SQLFragment("UPDATE " + getTableInfoRuns() + " SET Container=? ", newContainer.getId());
        updateSQL.append(runSQL);
        new SqlExecutor(getSchema()).execute(updateSQL);

        _removeRunsFromCache(runIds);
    }

    public static void renameRun(int runId, String newDescription)
    {
        if (newDescription == null || newDescription.length() == 0)
            return;

        new SqlExecutor(getSchema()).execute("UPDATE " + getTableInfoRuns() + " SET Description=? WHERE Run = ?", newDescription, runId);

        _removeRunsFromCache(Arrays.asList(runId));
    }

    // For safety, simply mark runs as deleted.  This allows them to be (manually) restored.
    public static void markAsDeleted(List<Integer> runIds, Container c, User user)
    {
        if (runIds.isEmpty())
            return;

        // Save these to delete after we've deleted the runs
        List<ExpRun> experimentRunsToDelete = new ArrayList<>();

        for (Integer runId : runIds)
        {
            MS2Run run = getRun(runId.intValue());
            if (run != null)
            {
                File file = new File(run.getPath(), run.getFileName());
                ExpData data = ExperimentService.get().getExpDataByURL(file, c);
                if (data != null)
                {
                    ExpRun expRun = data.getRun();
                    if (expRun != null)
                    {
                        experimentRunsToDelete.add(expRun);
                    }
                }
            }
        }

        markDeleted(runIds, c);

        for (ExpRun run : experimentRunsToDelete)
        {
            run.delete(user);
        }
    }

    public static void markAsDeleted(Container c, User user)
    {
        List<Integer> runIds = new SqlSelector(getSchema(), "SELECT Run FROM " + getTableInfoRuns() + " WHERE Container=?", c).getArrayList(Integer.class);
        markAsDeleted(runIds, c, user);
    }

    // pulled out into separate method so could be called by itself from data handlers
    public static void markDeleted(List<Integer> runIds, Container c)
    {
        SQLFragment markDeleted = new SQLFragment("UPDATE " + getTableInfoRuns() + " SET ExperimentRunLSID = NULL, Deleted=?, Modified=? ", Boolean.TRUE, new Date());
        SimpleFilter where = SimpleFilter.createContainerFilter(c);
        where.addInClause(FieldKey.fromParts("Run"), runIds);
        markDeleted.append(where.getSQLFragment(getSqlDialect()));

        new SqlExecutor(getSchema()).execute(markDeleted);
        _removeRunsFromCache(runIds);
        invalidateBasicStats();
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
    public static void purgeDeleted(int days)
    {
        // Status will be non-null if a purge thread is running; in that case, ignore the purge request.
        if (null != _purgeStatus)
            return;

        Calendar cutOff = Calendar.getInstance();
        cutOff.add(Calendar.DAY_OF_MONTH, -days);
        Date date = cutOff.getTime();

        Integer[] runIds = new SqlSelector(getSchema(), "SELECT Run FROM " + getTableInfoRuns() + " WHERE Deleted = ? AND Modified <= ?", Boolean.TRUE, date).getArray(Integer.class);

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
        if (runs != null && runs.length == 2)
        {
            // Check if we have both an intermediate file and a result file
            String name1 = runs[0].getFileName().toLowerCase();
            String name2 = runs[1].getFileName().toLowerCase();
            String rawSuffix = AbstractMS2SearchPipelineJob.getRawPepXMLSuffix().toLowerCase();
            if (endsWithExtOrExtDotGZ(name1,rawSuffix) && !endsWithExtOrExtDotGZ(name2,rawSuffix))
            {
                return runs[1];
            }
            if (endsWithExtOrExtDotGZ(name2,rawSuffix) && !endsWithExtOrExtDotGZ(name1,rawSuffix))
            {
                return runs[0];
            }

            // Check if we have both an dat file and pepXML file
            PepXMLFileType ft = new PepXMLFileType();
            if (name1.endsWith(".dat") && ft.isType(runs[1].getFileName()))
            {
                // Prefer the pepXML
                return runs[1];
            }
            if (name2.endsWith(".dat") && ft.isType(runs[0].getFileName()))
            {
                // Prefer the pepXML
                return runs[0];
            }
        }
        return null;
    }

    public static List<Protein> getProteinsForGroup(int rowId, int groupNumber, int indistinguishableCollectionId)
    {
        String sql = "SELECT seq.SeqId, seq.ProtSequence AS Sequence, seq.Mass, seq.Description, seq.BestName, seq.BestGeneName, fs.LookupString FROM " +
            getTableInfoProteinGroupMemberships() + " pgm," +
            getTableInfoProteinGroups() + " pg, " +
            getTableInfoProteinProphetFiles() + " ppf, " +
            getTableInfoRuns() + " r, " +
            getTableInfoFastaRunMapping() + " frm, " +
            ProteinManager.getTableInfoFastaSequences() + " fs, " +
            ProteinManager.getTableInfoSequences() + " seq " +
            "WHERE pg.RowId = pgm.ProteinGroupId " +
            "AND seq.SeqId = pgm.SeqId " +
            "AND pg.ProteinProphetFileId = ppf.RowId " +
            "AND ppf.Run = r.Run " +
            "AND fs.FastaId = frm.FastaId " +
            "AND frm.Run = r.Run " +
            "AND fs.SeqId = seq.SeqId " +
            "AND pg.GroupNumber = ? " +
            "AND pg.IndistinguishableCollectionId = ? " +
            "AND pg.ProteinProphetFileId = ?";

        return new SqlSelector(getSchema(), sql, groupNumber, indistinguishableCollectionId, rowId).getArrayList(Protein.class);
    }

    public static ProteinGroupWithQuantitation getProteinGroup(int proteinGroupId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("RowId"), proteinGroupId);

        return new TableSelector(getTableInfoProteinGroupsWithQuantitation(), filter, null).getObject(ProteinGroupWithQuantitation.class);
    }

    public static Collection<ProteinGroupWithQuantitation> getProteinGroupsWithPeptide(MS2Peptide peptide)
    {
        SQLFragment sql = new SQLFragment("SELECT pg.* FROM ");
        sql.append(getTableInfoProteinGroupsWithQuantitation(), "pg");
        sql.append(" WHERE pg.RowId IN (SELECT ProteinGroupId FROM ");
        sql.append(getTableInfoPeptideMemberships(), "pm");
        sql.append(" WHERE PeptideId = ?)");
        sql.add(peptide.getRowId());

        return new SqlSelector(getSchema(), sql).getCollection(ProteinGroupWithQuantitation.class);
    }

    public static ProteinGroupWithQuantitation getProteinGroup(int proteinProphetFileId, int groupNumber, int indistinguishableCollectionId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("ProteinProphetFileId"), proteinProphetFileId);
        filter.addCondition(FieldKey.fromParts("GroupNumber"), groupNumber);
        filter.addCondition(FieldKey.fromParts("IndistinguishableCollectionId"), indistinguishableCollectionId);

        return new TableSelector(getTableInfoProteinGroupsWithQuantitation(), filter, null).getObject(ProteinGroupWithQuantitation.class);
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
            finally
            {
                clearPurgeStatus();
            }
        }
    }


    // Clear all the data in a run, then delete the run record itself
    public static void purgeRun(int run)
    {
        clearRun(run);
        new SqlExecutor(getSchema()).execute("DELETE FROM " + getTableInfoRuns() + " WHERE Run = ?", run);
    }


    // Clear contents of a single run, but not the run itself.  Used to re-import after a failed attempt and to purge runs.
    public static void clearRun(int run)
    {
        Object[] params = new Object[] {run};
        purgeProteinProphetFiles("IN (SELECT RowId FROM " + getTableInfoProteinProphetFiles() + " WHERE Run = ?" + ")", new Object[] {run});

        String runWhere = " WHERE Run = ?";
        String fractionWhere = " WHERE Fraction IN (SELECT Fraction FROM " + getTableInfoFractions() + runWhere + ")";
        String peptideFKWhere = " WHERE PeptideId IN (SELECT RowId FROM " + getTableInfoPeptidesData() + fractionWhere + ")";

        SqlExecutor executor = new SqlExecutor(getSchema());
        executor.execute("DELETE FROM " + getTableInfoSpectraData() + fractionWhere, params);
        executor.execute("DELETE FROM " + getTableInfoQuantSummaries() + runWhere, params);
        executor.execute("DELETE FROM " + getTableInfoQuantitation() + peptideFKWhere, params);
        executor.execute("DELETE FROM " + getTableInfoITraqPeptideQuantitation() + peptideFKWhere, params);
        executor.execute("DELETE FROM " + getTableInfoPeptideProphetData() + peptideFKWhere, params);
        executor.execute("DELETE FROM " + getTableInfoPeptidesData() + fractionWhere, params);
        executor.execute("DELETE FROM " + getTableInfoPeptideProphetSummaries() + runWhere, params);
        executor.execute("DELETE FROM " + getTableInfoModifications() + runWhere, params);
        executor.execute("DELETE FROM " + getTableInfoFractions() + runWhere, params);
        executor.execute("DELETE FROM " + getTableInfoFastaRunMapping() + runWhere, params);
    }


    public static void purgeProteinProphetFile(int rowId)
    {
        purgeProteinProphetFiles("= ?", new Object[]{rowId});
    }

    private static void purgeProteinProphetFiles(String rowIdComparison, Object[] params)
    {
        String proteinProphetFilesWhere = " WHERE ProteinProphetFileId " + rowIdComparison;
        String proteinGroupsWhere = " WHERE ProteinGroupId IN (SELECT RowId FROM " + getTableInfoProteinGroups() + proteinProphetFilesWhere + ")";

        SqlExecutor executor = new SqlExecutor(getSchema());
        executor.execute("DELETE FROM " + getTableInfoProteinQuantitation() + proteinGroupsWhere, params);
        executor.execute("DELETE FROM " + getTableInfoITraqProteinQuantitation() + proteinGroupsWhere, params);
        executor.execute("DELETE FROM " + getTableInfoPeptideMemberships() + proteinGroupsWhere, params);
        executor.execute("DELETE FROM " + getTableInfoProteinGroupMemberships() + proteinGroupsWhere, params);
        executor.execute("DELETE FROM " + getTableInfoProteinGroups() + proteinProphetFilesWhere, params);
        executor.execute("DELETE FROM " + getTableInfoProteinProphetFiles() + " WHERE RowId " + rowIdComparison, params);
    }

    public static MS2Fraction[] getFractions(int runId)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("run"), runId);

        return new TableSelector(getTableInfoFractions(), filter, null).getArray(MS2Fraction.class);
    }

    public static List<MS2Modification> getModifications(MS2Run run)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("run"), run.getRun());

        return new TableSelector(getTableInfoModifications(), filter, null).getArrayList(MS2Modification.class);
    }

    public static MS2Peptide getPeptide(long peptideId)
    {
        Filter filter = new SimpleFilter(FieldKey.fromParts("RowId"), peptideId);

        return new TableSelector(getTableInfoPeptides(), filter, null).getObject(MS2Peptide.class);
    }

    public static PeptideQuantitation getQuantitation(long peptideId)
    {
        return new TableSelector(getTableInfoQuantitation()).getObject(peptideId, PeptideQuantitation.class);
    }

    public static LibraQuantResult getLibraQuantResult(long peptideId)
    {
        return new TableSelector(getTableInfoITraqPeptideQuantitation()).getObject(peptideId, LibraQuantResult.class);
    }

    public static ITraqProteinQuantitation getITraqProteinQuantitation(int groupId)
    {
        return new TableSelector(getTableInfoITraqProteinQuantitation()).getObject(groupId, ITraqProteinQuantitation.class);
    }

    public static int verifyRowIndex(long[] index, int rowIndex, long peptideId)
    {
        if (rowIndex < 0 || rowIndex >= index.length)
            logRowIndexError("RowIndex out of bounds " + rowIndex);
        else if (index[rowIndex] != peptideId)
            logRowIndexError("Wrong peptideId found at rowIndex " + rowIndex + " in cached peptide index");
        else
            return rowIndex;

        for (int i=0; i<index.length; i++)
            if (index[i] == peptideId)
                return i;

        logRowIndexError("Can't find peptideId " + peptideId + " in peptide index");
        return -1;
    }


    private static void logRowIndexError(String error)
    {
        ViewContext ctx = HttpView.currentContext();
        String url = ctx.getActionURL().getLocalURIString();
        String referrer = StringUtils.trimToEmpty(ctx.getRequest().getHeader("referer"));
        String userAgent = StringUtils.trimToEmpty(ctx.getRequest().getHeader("User-Agent"));
        _log.warn(error + "(url=" + url + ", referrer=" + referrer + ", browser=" + userAgent + ")");
    }


    public static Pair<float[], float[]> getSpectrum(int fractionId, int scan) throws SpectrumException
    {
        byte[] spectrumBytes = new SqlSelector(getSchema(), "SELECT Spectrum FROM " + getTableInfoSpectraData() + " WHERE Fraction = ? AND Scan = ?", fractionId, scan).getObject(byte[].class);

        if (null != spectrumBytes)
            return SpectrumImporter.byteArrayToFloatArrays(spectrumBytes);
        else
            return getSpectrumFromFile(fractionId, scan);
    }

    public static Pair<float[], float[]> getSpectrumFromFile(int fractionId, int scan) throws SpectrumException
    {
        MS2Fraction fraction = MS2Manager.getFraction(fractionId);
        if (null == fraction)
            throw new SpectrumException("Can't locate fraction.");
        if (StringUtils.endsWithIgnoreCase(fraction.getFileName(), ".dat"))
            return getSpectrumFromDat(fraction, scan);
        else
            return getSpectrumFromMzXML(fraction, scan);
    }

    public static Pair<float[], float[]> getSpectrumFromMzXML(MS2Fraction fraction, int scan) throws SpectrumException
    {
        if (null == fraction || fraction.getMzXmlURL() == null)
            throw new SpectrumException("Can't locate spectrum file.");

        URL url;
        File f = null;
        try
        {
            url = new URL(fraction.getMzXmlURL());
            URI uri = url.toURI();
            if (uri.getAuthority() == null)
            {
                f = new File(uri);
            }
        }
        catch (Exception e)
        {
            // Treat exceptions and null file identically below
        }

        if (null == f)
            throw new SpectrumException("Invalid mzXML URL: " + fraction.getMzXmlURL());

        if (!NetworkDrive.exists(f))
            throw new SpectrumException("Spectrum file not found.\n" + f.getAbsolutePath());

        RandomAccessMzxmlIterator iter = null;

        try
        {
            iter = RandomAccessMzxmlIteratorFactory.newIterator(f, 2, scan);
            if (iter.hasNext())
            {
                SimpleScan sscan = iter.next();
                float[][] data = sscan.getData();
                if (data != null)
                {
                    return new Pair<>(data[0], data[1]);
                }
                else
                {
                    throw new SpectrumException("Could not find spectra for scan " + scan + " in " + f.getName());
                }
            }
            else
            {
                throw new SpectrumException("Could not find scan " + scan + " in " + f.getName());
            }
        }
        catch (IOException e)
        {
            throw new SpectrumException("Error reading mzXML file " + f.getName(), e);
        }
        finally
        {
            if (iter != null)
            {
                iter.close();
            }
        }
    }

    private static Pair<float[], float[]> getSpectrumFromDat(@NotNull MS2Fraction fraction, int scan) throws SpectrumException
    {
        File f = new File(getRun(fraction.getRun()).getPath(), fraction.getFileName());
        NetworkDrive.ensureDrive(f.getPath());
        try (MascotDatLoader loader = new MascotDatLoader(f, _log))
        {
            return loader.loadSpectrum(scan);
        }
        catch (IOException | XMLStreamException e)
        {
            throw new SpectrumException("Can't read .dat file", e);
        }
    }

    private static void _addRunToCache(int runId, MS2Run run)
    {
        RUN_CACHE.put(RUN_CACHE_PREFIX + runId, run);
    }


    private static MS2Run _getRunFromCache(int runId)
    {
        return RUN_CACHE.get(RUN_CACHE_PREFIX + runId);
    }

    private static void _removeRunsFromCache(List<Integer> runIds)
    {
        for (Integer runId : runIds)
            RUN_CACHE.remove(RUN_CACHE_PREFIX + runId);
    }


    private static void _addPeptideProphetSummaryToCache(PeptideProphetSummary summary)
    {
        PEPTIDE_PROPHET_SUMMARY_CACHE.put(PEPTIDE_PROPHET_SUMMARY_CACHE_PREFIX + summary.getRun(), summary);
    }


    private static PeptideProphetSummary _getPeptideProphetSummaryFromCache(int runId)
    {
        return PEPTIDE_PROPHET_SUMMARY_CACHE.get(PEPTIDE_PROPHET_SUMMARY_CACHE_PREFIX + runId);
    }


    private static DecimalFormat df = new DecimalFormat("#,##0");

    public static Map<String, String> getStats(int days)
    {
        Map<String, String> stats = new HashMap<>(20);

        addStats(stats, "successful", "Deleted = ? AND StatusId = 1", new Object[]{Boolean.FALSE});
        addStats(stats, "failed", "Deleted = ? AND StatusId = 2", new Object[]{Boolean.FALSE});
        addStats(stats, "deleted", "Deleted = ?", new Object[]{Boolean.TRUE});

        // For in-process runs, actually count the current number of peptides & spectra; counts in Runs table aren't filled in until import is complete
        addStatsWithCounting(stats, "inProcess", "Deleted = ? AND StatusId = 0", new Object[]{Boolean.FALSE});

        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_MONTH, -days);
        Date date = now.getTime();

        addStats(stats, "purged", "Deleted = ? AND Modified <= ?", new Object[]{Boolean.TRUE, date});

        return stats;
    }

    private static void addStats(Map<String, String> stats, String prefix, String whereSql, Object[] params)
    {
        // TODO: Use getMap() instead of getResultSet()
        try (ResultSet rs = new SqlSelector(getSchema(), "SELECT COUNT(*) AS Runs, COALESCE(SUM(PeptideCount),0) AS Peptides, COALESCE(SUM(SpectrumCount),0) AS Spectra FROM " + getTableInfoRuns() + " WHERE " + whereSql, params).getResultSet())
        {
            rs.next();

            stats.put(prefix + "Runs", df.format(rs.getObject(1)));
            stats.put(prefix + "Peptides", df.format(rs.getObject(2)));
            stats.put(prefix + "Spectra", df.format(rs.getObject(3)));
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }


    private static void addStatsWithCounting(Map<String, String> stats, String prefix, String whereSql, Object[] params)
    {
        Long inProcessRuns = new SqlSelector(getSchema(), "SELECT COUNT(*) FROM " + getTableInfoRuns() + " WHERE " + whereSql, params).getObject(Long.class);
        Long inProcessPeptides = new SqlSelector(getSchema(), "SELECT COUNT(*) FROM " + getTableInfoPeptides() + " WHERE Run IN (SELECT Run FROM " + getTableInfoRuns() + " WHERE " + whereSql + ")", params).getObject(Long.class);
        Long inProcessSpectra = new SqlSelector(getSchema(), "SELECT COUNT(*) FROM " + getTableInfoSpectra() + " WHERE Run IN (SELECT Run FROM " + getTableInfoRuns() + " WHERE " + whereSql + ")", params).getObject(Long.class);

        stats.put(prefix + "Runs", df.format(inProcessRuns));
        stats.put(prefix + "Peptides", df.format(inProcessPeptides));
        stats.put(prefix + "Spectra", df.format(inProcessSpectra));
    }


    public static void recomputeBasicStats()
    {
        _basicStats = computeBasicStats();

        try
        {
            Map<String, String> stats = getBasicStats();
            long runs = df.parse(stats.get("Runs")).longValue();
            long peptides = df.parse(stats.get("Peptides")).longValue();
            insertStats(runs, peptides);
        }
        catch (ParseException e)
        {
            throw new UnexpectedException(e);
        }
    }


    // Cache the basic stats for the MS2 stats web part
    private static Map<String, String> _basicStats = null;

    public static synchronized Map<String, String> getBasicStats()
    {
        if (null == _basicStats)
            _basicStats = computeBasicStats();

        return _basicStats;
    }

    public static synchronized void invalidateBasicStats()
    {
        _basicStats = null;
    }

    private static Map<String, String> computeBasicStats()
    {
        Map<String, String> stats = new HashMap<>();
        addStats(stats, "", "DELETED = ? AND StatusId = 1", new Object[]{Boolean.FALSE});
        return stats;
    }

    private static void insertStats(long runs, long peptides)
    {
        Map<String, Object> m = new HashMap<>();
        m.put("date", new Date());
        m.put("runs", runs);
        m.put("peptides", peptides);

        Table.insert(null, getTableInfoHistory(), m);
    }


    public static MS2Fraction getFraction(int fractionId)
    {
        MS2Fraction fraction = _getFractionFromCache(fractionId);

        if (null == fraction)
        {
            fraction = new TableSelector(getTableInfoFractions()).getObject(fractionId, MS2Fraction.class);
            if (fraction != null)
            {
                _addFractionToCache(fractionId, fraction);
            }
        }

        return fraction;
    }


    private static void _addFractionToCache(int fractionId, MS2Fraction fraction)
    {
        FRACTION_CACHE.put(FRACTION_CACHE_PREFIX + fractionId, fraction);
    }


    private static MS2Fraction _getFractionFromCache(int fractionId)
    {
        return FRACTION_CACHE.get(FRACTION_CACHE_PREFIX + fractionId);
    }


    private static void _removeFractionFromCache(int fractionId)
    {
        FRACTION_CACHE.remove(FRACTION_CACHE_PREFIX + fractionId);
    }

    public static void clearFractionCache()
    {
        FRACTION_CACHE.clear();
    }

    public static void clearRunCache()
    {
        RUN_CACHE.clear();
    }

    public static MS2Fraction writeHydro(MS2Fraction fraction, Map updateMap)
    {
        Table.update(null, MS2Manager.getTableInfoFractions(), updateMap, fraction.getFraction());
        _removeFractionFromCache(fraction.getFraction());

        return MS2Manager.getFraction(fraction.getFraction());
    }


    public static void updateRun(MS2Run run, User user)
    {
        Table.update(user, getTableInfoRuns(), run, run.getRun());
    }


    public static void cachePeptideIndex(String key, long[] index)
    {
        _peptideIndexCache.put(key, index);
    }


    public static long[] getPeptideIndex(String key)
    {
        return _peptideIndexCache.get(key);
    }


    private static class PeptideIndexCache extends DatabaseCache<long[]>
    {
        private static int CACHE_SIZE = 10;

        public PeptideIndexCache()
        {
            super(getSchema().getScope(), CACHE_SIZE, CacheManager.HOUR, "Peptide Index");
        }
    }


    public static long getRunCount(Container c)
    {
        return new SqlSelector(getSchema(), "SELECT COUNT(*) FROM " + getTableInfoRuns() + " WHERE Deleted = ? AND Container = ?", Boolean.FALSE, c).getObject(Long.class);
    }

    public static final String NEGATIVE_HIT_PREFIX = "rev_";

    public static class XYSeriesROC extends XYSeries
    {
        private List<XYAnnotation> annotations = new ArrayList<>();

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
            if (text == null)
                return;
            
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
                                                double increment, double percentAACorrect, int limitFalsePs,
                                                double[] marks, boolean markFdr)
    {
        String negHitPrefix = NEGATIVE_HIT_PREFIX;

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
                    // TODO: Use getMap() instead of getResultSet()
                    try (ResultSet rs = new SqlSelector(getSchema(),
                                                    "SELECT Protein, " + discriminate + " as Expression, " +
                                                            " CASE substring(Protein, 1, 4) WHEN 'rev_' THEN 1 ELSE 0 END as FP " +
                                                    "FROM " + getTableInfoPeptides().getSelectName() + " " +
                                                    "WHERE Run = ? " +
                                                    "ORDER BY Expression, FP",
                                                    run.getRun()).getResultSet())
                    {

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
                                    if (iMark < marks.length &&
                                        ((!markFdr && falsePositives == marks[iMark]) ||
                                         (markFdr && falsePositives > (marks[iMark] / 100.0) * ((100.0 - percentAACorrect) / 100.00) * rows)))
                                    {
                                        series.addFirstFalseAnnotation(rs.getString("Expression"),
                                                falsePositives, rows - falsePositives);

                                        iMark++;
                                    }
                                    // If FDR dips below mark, back-up and mark again.
                                    else if (iMark > 0 && (markFdr && falsePositives <= (marks[iMark-1] / 100.0) * ((100.0 - percentAACorrect) / 100.00) * rows))
                                    {
                                        iMark--;
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
                }

                collection.addSeries(series);
            }
        }

        return collection;
    }

    public static XYSeriesCollection getROCDataProt(int[] runIds, double increment,
                                                    boolean[][] discriminateFlags,
                                                    double percentAACorrect, int limitFalsePs,
                                                    double[] marks, boolean markFdr)
    {
        String negHitPrefix = NEGATIVE_HIT_PREFIX;

        XYSeriesCollection collection = new XYSeriesCollection();
        for (int i = 0; i < runIds.length; i++)
        {
            MS2Run run = getRun(runIds[i]);
            if (run == null)
                continue;

            // Only show runs for which at least one discriminate flag is showing.
            boolean showRun = false;
            for (boolean discriminateFlag : discriminateFlags[i])
                showRun = showRun || discriminateFlag;
            if (!showRun)
                continue;

            long runRows = new SqlSelector(getSchema(),
                "SELECT COUNT(*) " +
                    "FROM " + getTableInfoRuns().getSelectName() + " r " +
                        "inner join " + getTableInfoProteinProphetFiles().getSelectName() + " f on r.Run = f.Run " +
                        "inner join " + getTableInfoProteinGroups().getSelectName() + " g on f.RowId = g.ProteinProphetFileId " +
                    "WHERE r.Run = ? ",
                    run.getRun()).getObject(Integer.class).longValue();

            String key = run.getDescription();
            XYSeriesROC series = new XYSeriesROC(key);

            if (run.statusId == 0)
                series.setKey(series.getKey() + " (Loading)");
            else
            {
                // TODO: Use getMap() instead of getResultSet()
                try (ResultSet rs = new SqlSelector(getSchema(),
                                "SELECT GroupNumber, -max(GroupProbability) as Expression, min(BestName) as Protein, " +
                                        " CASE substring(min(BestName), 1, 4) WHEN 'rev_' THEN 1 ELSE 0 END as FP " +
                                "FROM " + getTableInfoRuns().getSelectName() + " r " +
                                    "inner join " + getTableInfoProteinProphetFiles().getSelectName() + " f on r.Run = f.Run " +
                                    "inner join " + getTableInfoProteinGroups().getSelectName() + " g on f.RowId = g.ProteinProphetFileId " +
                                    "inner join " + getTableInfoProteinGroupMemberships().getSelectName() + " m on g.RowId = m.ProteinGroupId " +
                                    "inner join " + ProteinManager.getTableInfoSequences().getSelectName() + " s on m.SeqId = s.SeqId " +
                                "WHERE r.Run = ? " +
                                "GROUP BY GroupNumber " +
                                "ORDER BY Expression, FP",
                                run.getRun()).getResultSet())
                {
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
                                if (iMark < marks.length &&
                                    ((!markFdr && falsePositives == marks[iMark]) ||
                                     (markFdr && falsePositives > (marks[iMark] / 100.0) * ((100.0 - percentAACorrect) / 100.00) * rows)))
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
            }

            collection.addSeries(series);
        }

        return collection;
    }

    public static XYSeriesCollection getDiscriminateROCData(int runId,
                                                            String[] expressions,
                                                            double increment,
                                                            int limitFalsePs,
                                                            int[] marks
    )
    {
        String negHitPrefix = NEGATIVE_HIT_PREFIX;

        XYSeriesCollection collection = new XYSeriesCollection();
        MS2Run run = getRun(runId);
        if (run != null && run.statusId != 0)
        {
            long runRows = run.getPeptideCount();

            String key = run.getDescription();
            XYSeriesROC series = new XYSeriesROC(key);

            try (ResultSet rs = new SqlSelector(getSchema(),
                                            "SELECT Protein, " +
                                                " case" +
                                                    " when Charge = 1 then " + expressions[0] +
                                                    " when Charge = 2 then " + expressions[1] +
                                                    " else " + expressions[2] +
                                                " end as Expression " +
                                            "FROM " + getTableInfoPeptides().getSelectName() + " " +
                                            "WHERE Run = ? " +
                                            "ORDER BY Expression DESC",
                                            runId).getResultSet())
            {
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

            collection.addSeries(series);
        }

        return collection;
    }

    public static XYSeriesCollection getDiscriminateData(int runId,
                                                         int charge,
                                                         double percentAACorrect,
                                                         final String expression,
                                                         double bucket,
                                                         int scaleFactor
    )
    {
        String negHitPrefix = NEGATIVE_HIT_PREFIX;

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

            // TODO: Use getMap() instead of getResultSet()
            try (ResultSet rs = new SqlSelector(getSchema(),
                        "SELECT Fraction, Scan, Charge, Protein, " + expression + " as Expression " +
                        "FROM " + getTableInfoPeptides().getSelectName() + " " +
                        "WHERE Run = ? " +
                        "ORDER BY Expression",
                        runId).getResultSet())
            {
                if (!rs.next())
                    return collection;

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

            collection.addSeries(seriesFP);
            collection.addSeries(seriesCorrect);
        }

        return collection;
    }

    public static void validateRuns(List<MS2Run> runs, boolean requireSameType, User user) throws UnauthorizedException, RunListException
    {
        String type = null;
        Set<String> errors = new LinkedHashSet<>();

        for (MS2Run run : runs)
        {
            if (null == run)
            {
                errors.add("Run not found");
                continue;
            }

            // Authorize this run
            Container c = run.getContainer();

            if (!c.hasPermission(user, ReadPermission.class))
            {
                if (user.isGuest())
                {
                    throw new UnauthorizedException();
                }

                errors.add("Run " + run.getRun() + ": Not authorized");
            }

            if (run.getStatusId() == MS2Importer.STATUS_RUNNING)
            {
                errors.add(run.getDescription() + " is still importing");
            }

            if (run.getStatusId() == MS2Importer.STATUS_FAILED)
            {
                errors.add(run.getDescription() + " did not import successfully");
            }

            if (requireSameType)
            {
                if (null == type)
                    type = run.getType();
                else if (!type.equals(run.getType()))
                {
                    errors.add("Can't mix " + type + " and " + run.getType() + " runs.");
                }
            }
        }

        if (!errors.isEmpty())
        {
            throw new RunListException(errors);
        }
    }

    public static void deleteExpressionData(Container c)
    {
        // Purge ms2.ExpressionData using the associated ExpData's container
        TableInfo expressionData = getTableInfoExpressionData();
        SQLFragment deleteFrag = new SQLFragment();
        deleteFrag.append("DELETE FROM ").append(expressionData).append(" WHERE DataId IN (");
        deleteFrag.append(" SELECT RowId FROM ").append(ExperimentService.get().getTinfoData(), "d").append(" WHERE d.container = ?");
        deleteFrag.add(c.getEntityId());
        deleteFrag.append(")");
        new SqlExecutor(expressionData.getSchema()).execute(deleteFrag);
    }

    public static List<Map> getExpressionDataDistinctSamples(ExpProtocol protocol)
    {
        SQLFragment frag = new SQLFragment("SELECT SampleId, Name FROM ");
        frag.append("(SELECT DISTINCT SampleId FROM ");
        frag.append(getTableInfoExpressionData(), "e");
        frag.append(", ");
        frag.append(ExperimentService.get().getTinfoData(), "d");
        frag.append(", ");
        frag.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        frag.append(" WHERE e.DataId = d.RowId\n");
        frag.append("   AND d.RunId = r.RowId\n");
        frag.append("   AND d.container=?\n").add(protocol.getContainer());
        frag.append("   AND r.ProtocolLSID = ?\n").add(protocol.getLSID());
        frag.append(") as fd, ");
        frag.append(ExperimentService.get().getTinfoMaterial(), "m");
        frag.append(" WHERE fd.SampleId = m.RowId");

        SqlSelector selector = new SqlSelector(getSchema(), frag);

        return selector.getArrayList(Map.class);
    }

    public static Map<String, Integer> getFastaFileSeqIds(int fastaFileId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("FastaId"), fastaFileId);

        TableSelector seqIdSelector = new TableSelector(ProteinManager.getTableInfoFastaSequences(), PageFlowUtil.set("LookupString", "SeqId"), filter, null);
        return seqIdSelector.fillValueMap(new CaseInsensitiveHashMap<>());
    }

    private static final float DEFAULT_SCORE_THRESHOLD = 13.1f;
    private static final float DEFAULT_P_VALUE = .05f;

    public static class DecoySummaryBean
    {
        private int targetCount;
        private int decoyCount;
        private float scoreThreshold;
        private float fdr = 1.0f;
        private boolean result = false;
        private Float desiredFdr;
        private float fdrAtDefaultPvalue;
        private float pValue;
        private final List<Float> fdrOptions = new ArrayList(Arrays.asList(.001f, .002f, .01f, .02f, .025f, .05f, .1f));
        private final Map<Float, Float> fdrOptionToThresholdMap = new HashMap<Float, Float>(fdrOptions.size() + 1);  // may add one option during processing

        public int getTargetCount()
        {
            return targetCount;
        }

        public void setTargetCount(int targetCount)
        {
            this.targetCount = targetCount;
        }

        public int getDecoyCount()
        {
            return decoyCount;
        }

        public void setDecoyCount(int decoyCount)
        {
            this.decoyCount = decoyCount;
        }

        public float getScoreThreshold()
        {
            return scoreThreshold;
        }

        public void setScoreThreshold(float scoreThreshold)
        {
            this.scoreThreshold = scoreThreshold;
        }

        public float getFdr()
        {
            return fdr;
        }

        public void setFdr(float fdr)
        {
            this.fdr = fdr;
        }

        public boolean isResult()
        {
            return result;
        }

        public void setResult(boolean result)
        {
            this.result = result;
        }

        public Float getDesiredFdr()
        {
            return desiredFdr;
        }

        public void setDesiredFdr(Float desiredFdr)
        {
            this.desiredFdr = desiredFdr;
        }

        public float getFdrAtDefaultPvalue()
        {
            return fdrAtDefaultPvalue;
        }

        public void setFdrAtDefaultPvalue(float fdrAtDefaultPvalue)
        {
            this.fdrAtDefaultPvalue = fdrAtDefaultPvalue;
        }

        public float getpValue()
        {
            return pValue;
        }

        public void setpValue(float pValue)
        {
            this.pValue = pValue;
        }

        void setValues(DecoySummaryBean dsb, boolean useStandardScore)
        {
            setTargetCount(dsb.getTargetCount());
            setDecoyCount(dsb.getDecoyCount());
            setFdr(dsb.getFdr());
            if (useStandardScore)
            {
                setScoreThreshold(DEFAULT_SCORE_THRESHOLD);
                setpValue(DEFAULT_P_VALUE);
            }
            else
            {
                setScoreThreshold(dsb.getScoreThreshold());
                calcPvalue();
            }

            setResult(true);
        }

        private void calcPvalue()
        {
            pValue = (float)Math.pow(10, (scoreThreshold / -10 ));
        }

        public List<Float> getFdrOptions()
        {
            return fdrOptions;
        }

        public Map<Float, Float> getFdrOptionToThresholdMap()
        {
            return fdrOptionToThresholdMap;
        }
    }



    public static DecoySummaryBean getDecoySummaryForRun(int run, Float desiredFdr)
    {
        DbSchema schema = getSchema();
        SQLFragment sql = new SQLFragment("SELECT coalesce(t.targets, 0) targetCount, coalesce(d.decoys,0) decoyCount, coalesce(t.score1, d.score1, 0) scoreThreshold FROM\n")
                .append("(SELECT count(rowId) targets, score1 \n")
                .append("FROM ms2.fractions f join ms2.peptidesdata p on f.fraction = p.fraction\n")
                .append("WHERE  f.run = ? and p.decoy = ").append(schema.getSqlDialect().getBooleanFALSE()).append("\n")
                .append(" and p.hitrank = 1")
                .append("GROUP BY p.score1) t\n")
                .append("FULL OUTER JOIN\n")
                .append("(SELECT count(rowId) decoys, score1 \n")
                .append("FROM ms2.fractions f join ms2.peptidesdata p on f.fraction = p.fraction\n")
                .append("WHERE f.run = ? and p.decoy = ").append(schema.getSqlDialect().getBooleanTRUE()).append("\n")
                .append(" and p.hitrank = 1")
                .append("GROUP BY p.score1) d ON t.score1 = d.score1\n")
                .append("ORDER BY coalesce(t.score1, d.score1) DESC");
        sql.add(run);
        sql.add(run);

        // For debugging, this block of code calculates the FDR for every score value, both starting the cume totals
        // from the highest identity (probably correct) and starting from the lowest (probably incorrect).
        // This can be removed once we're 100% certain we've implemented the correct algorithm.
//        Map<Float, Pair<String, String>> fullMap = new LinkedHashMap<>();
//        NumberFormat defaultFormat = NumberFormat.getPercentInstance();
//        defaultFormat.setMinimumFractionDigits(2);
//        int tempTargetTotal = 0;
//        int tempDecoyTotal = 0;
//        List<DecoySummaryBean> identities = new SqlSelector(schema.getScope(), sql).getArrayList(DecoySummaryBean.class);
//        for (DecoySummaryBean row : identities)
//        {
//            tempTargetTotal += row.getTargetCount();
//            tempDecoyTotal += row.getDecoyCount();
//            float fullFdr = (float)tempDecoyTotal / (tempTargetTotal + tempDecoyTotal);
//            Pair<String, String> fdrs = new Pair<>(defaultFormat.format(fullFdr), null);
//
//            fullMap.put(row.getScoreThreshold(), fdrs);
//        }
//        Collections.reverse(identities);
//        tempDecoyTotal = 0;
//        tempTargetTotal = 0;
//        for (DecoySummaryBean row : identities)
//        {
//            tempTargetTotal += row.getTargetCount();
//            tempDecoyTotal += row.getDecoyCount();
//            float fullFdr = (float)tempDecoyTotal / (tempTargetTotal + tempDecoyTotal);
//            fullMap.get(row.getScoreThreshold()).setValue(defaultFormat.format(fullFdr)) ;
//        }

        int targetTotal = 0;
        int decoyTotal = 0;

        List<DecoySummaryBean> scores = new SqlSelector(schema.getScope(), sql).getArrayList(DecoySummaryBean.class);

        if (scores.isEmpty())
            return null;

        // Calculate the target & decoy cumulative totals, and fdr, for every row
        for (DecoySummaryBean row : scores)
        {
            targetTotal += row.getTargetCount();
            decoyTotal += row.getDecoyCount();
            float fdr = (float) decoyTotal / targetTotal;

            row.setTargetCount(targetTotal);
            row.setDecoyCount(decoyTotal);
            row.setFdr(fdr);
        }

        if (decoyTotal == 0)
            return null;

        DecoySummaryBean defaultPvalueResult = new DecoySummaryBean();
        int rowCount = scores.size();

        // Find the FDR at the default threshold
        // No matter the desired FDR value, the FDR at default p Value is constant.
        // We'll use it on the initial page load and as an option in the dropdown
        for (int i = 0; i < rowCount; i++)
        {
            int scoreComparison = Float.compare(scores.get(i).getScoreThreshold(), DEFAULT_SCORE_THRESHOLD);
            if (i == 0 && scoreComparison < 1) // We're already below cutoff, the FDR is 100%
            {
                break;
            }
            else if (i == rowCount - 1 && scoreComparison > 0) // We got to the end without getting to the score cutoff, use this result
                defaultPvalueResult = scores.get(i);
            else if (scoreComparison < 1 || i == rowCount - 1) // We want the one before this
            {
                defaultPvalueResult = scores.get(i - 1);
                break;
            }
        }

        DecoySummaryBean result = new DecoySummaryBean();

        result.fdrOptions.add(defaultPvalueResult.getFdr());
        Collections.sort(result.fdrOptions);
        boolean useDefaultFdr = (null == desiredFdr) || (Float.compare(desiredFdr, new Float(defaultPvalueResult.getFdr())) == 0);  // no selection, or default FDR

        if (useDefaultFdr)
        {
            result.setValues(defaultPvalueResult, true);
        }
        for (Float fdrOption : result.fdrOptions)  // need to look at all FDR options for ionCutoff (even though result only uses at most one) to fill in hash map properly
        {
            DecoySummaryBean tempResult = new DecoySummaryBean();
            DecoySummaryBean bestResultOverThreshold = new DecoySummaryBean();
            tempResult.setFdrAtDefaultPvalue(defaultPvalueResult.getFdr());

            for (DecoySummaryBean row : scores)
            {
                if (row.getDecoyCount() > 0) // TODO: FDR == 0 allowed? Maybe if target count is sufficiently high?
                {
                    if (Float.compare(row.getFdr(), fdrOption) < 1) // We're still getting closer to the highest FDR we'll allow
                    {
                        if (!tempResult.isResult() || Float.compare(row.getFdr(), tempResult.getFdr()) > 0)
                        {
                            tempResult.setValues(row, false);
                        }
                    }
                    else if (tempResult.isResult()) // We went one too far, the previous row was the best result without going over
                        break;
                        // Also keep track of the best we've seen that exceeds the desired FDR
                    else if (Float.compare(row.getFdr(), bestResultOverThreshold.getFdr()) < 0 || !bestResultOverThreshold.isResult())
                    {
                        bestResultOverThreshold.setValues(row, false);
                    }
                }
            }

            if (!tempResult.isResult() && bestResultOverThreshold.isResult())
                tempResult.setValues(bestResultOverThreshold, false);

            // done processing, so add threshold to hash map and (possibly) values to returned result

            if (Float.compare(fdrOption, new Float(defaultPvalueResult.getFdr())) == 0)
                result.getFdrOptionToThresholdMap().put(fdrOption, DEFAULT_SCORE_THRESHOLD);
            else
                result.getFdrOptionToThresholdMap().put(fdrOption, tempResult.getScoreThreshold());
            if (!useDefaultFdr && (Float.compare(fdrOption, desiredFdr) == 0))
                // if not using default FDR, and fdrOption is the selected value, then populate result for real
            {
                result.setValues(tempResult, false);
                result.setDesiredFdr(desiredFdr);
            }
        }

        return result;
    }
}
