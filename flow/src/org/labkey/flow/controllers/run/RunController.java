/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

package org.labkey.flow.controllers.run;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.attachments.Attachment;
import org.labkey.api.attachments.AttachmentParent;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.attachments.BaseDownloadAction;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.collections.RowMapFactory;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.TSVMapWriter;
import org.labkey.api.data.TSVWriter;
import org.labkey.api.exp.api.ExpRunAttachmentParent;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusUrls;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.security.RequiresLogin;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.ConfigurationException;
import org.labkey.api.util.FileNameUniquifier;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.GUID;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.api.util.URLHelper;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.writer.FileSystemFile;
import org.labkey.api.writer.PrintWriters;
import org.labkey.api.writer.VirtualFile;
import org.labkey.api.writer.ZipFile;
import org.labkey.flow.FlowModule;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.SampleIdMap;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.data.FlowCompensationMatrix;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowFCSAnalysis;
import org.labkey.flow.data.FlowFCSFile;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowWell;
import org.labkey.flow.persist.AnalysisSerializer;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.view.ExportAnalysisForm;
import org.labkey.flow.view.ExportAnalysisManifest;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.labkey.api.util.FileUtil.getBaseName;
import static org.labkey.api.util.FileUtil.getTimestamp;
import static org.labkey.flow.controllers.run.StatusJsonHelper.getStatusUrl;

public class RunController extends BaseFlowController
{
    private static final Logger _log = Logger.getLogger(RunController.class);

    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(RunController.class);

    public RunController()
    {
        setActionResolver(_actionResolver);
    }


    @RequiresNoPermission
    public class BeginAction extends SimpleViewAction<RunForm>
    {
        public ModelAndView getView(RunForm runForm, BindException errors) throws Exception
        {
            return HttpView.redirect(urlFor(RunController.ShowRunAction.class));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowRunAction extends SimpleViewAction<RunForm>
    {
        FlowRun run;

        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            run = form.getRun();
            if (run == null)
            {
                throw new NotFoundException("Run not found: " + PageFlowUtil.filter(form.getRunId()));
            }

            run.checkContainer(getContainer(), getUser(), getActionURL());

            return new JspView<>(RunController.class, "showRun.jsp", form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String label = run != null ? null : "Run not found";
            return appendFlowNavTrail(getPageConfig(), root, run, label);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowRunsAction extends SimpleViewAction<RunsForm>
    {
        FlowExperiment experiment;
//        FlowScript script;

        public ModelAndView getView(RunsForm form, BindException errors) throws Exception
        {
            experiment = form.getExperiment();
//            script = form.getScript();

            checkContainer(experiment);

            return new JspView<>(RunController.class, "showRuns.jsp", form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            if (experiment == null)
                root.addChild("All Analysis Folders", new ActionURL(ShowRunsAction.class, getContainer()));
            else
                root.addChild(experiment.getLabel(), experiment.urlShow());

//            if (script != null)
//                root.addChild(script.getLabel(), script.urlShow());

            root.addChild("Runs");
            return root;
        }
    }


    @RequiresLogin
    @RequiresPermission(ReadPermission.class)
    public class DownloadAction extends SimpleViewAction<DownloadRunForm>
    {
        private FlowRun _run;
        private Map<String, File> _files = new TreeMap<>();
        private List<File> _missing = new LinkedList<>();

        @Override
        public void validate(DownloadRunForm form, BindException errors)
        {
            _run = form.getRun();
            if (_run == null)
            {
                errors.reject(ERROR_MSG, "run not found");
                return;
            }

            FlowWell[] wells = _run.getWells(true);
            if (wells.length == 0)
            {
                errors.reject(ERROR_MSG, "no wells in run: " + _run.getName());
                return;
            }

            for (FlowWell well : wells)
            {
                URI uri = well.getFCSURI();
                File file = new File(uri);
                if (file.exists() && file.canRead())
                    _files.put(file.getName(), file);
                else
                    _missing.add(file);
            }

            if (_missing.size() > 0 && !form.isSkipMissing())
            {
                errors.reject(ERROR_MSG, "files missing from run: " + _run.getName());
            }
        }

        public ModelAndView getView(DownloadRunForm form, BindException errors) throws Exception
        {
            if (errors.hasErrors())
            {
                return new JspView<>("/org/labkey/flow/controllers/run/download.jsp", new DownloadRunBean(_run, _files, _missing), errors);
            }
            else
            {
                HttpServletResponse response = getViewContext().getResponse();
                try (ZipFile zipFile = new ZipFile(response, _run.getName() + ".zip"))
                {
                    exportFCSFiles(zipFile, null, _run, form.getEventCount() == null ? 0 : form.getEventCount());
                }

                return null;
            }
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Download Run");
            return root;
        }
    }

    protected SampleIdMap<String> exportFCSFiles(VirtualFile dir, @Nullable String dirName, FlowRun run, int eventCount)
            throws Exception
    {
        FlowWell[] wells = run.getWells(true);
        return exportFCSFiles(dir, dirName, Arrays.asList(wells), eventCount);
    }

    protected SampleIdMap<String> exportFCSFiles(VirtualFile parentDir, @Nullable String dirName, Collection<FlowWell> wells, int eventCount)
            throws Exception
    {
        VirtualFile dir = dirName == null ? parentDir : parentDir.getDir(dirName);
        SampleIdMap<String> files = new SampleIdMap<>();
        FileNameUniquifier uniquifier = new FileNameUniquifier(true);

        // Issue 13557: The list of wells may contain both FlowFCSFile and FlowFCSAnalysis wells representing the same FCS file URI.
        // Keep track of which ones we've already seen during export.
        Set<String> seen = new CaseInsensitiveHashSet();

        byte[] buffer = new byte[524288];
        for (FlowWell well : wells)
        {
            URI uri = well.getFCSURI();
            if (uri == null)
                continue;

            // CONSIDER: If we see the same URI again but for a different well, we may want to write out a row to the catalog tsv file for the well
            String uriString = uri.toString();
            if (seen.contains(uriString))
                continue;

            seen.add(uriString);

            File file = new File(uri);
            if (file.canRead())
            {
                String fileName = uniquifier.uniquify(FileUtil.makeLegalName(file.getName()));
                OutputStream os = dir.getOutputStream(fileName);
                InputStream is;
                if (eventCount == 0)
                {
                    is = new FileInputStream(file);
                }
                else
                {
                    is = new ByteArrayInputStream(FCSAnalyzer.get().getFCSBytes(uri, eventCount));
                }
                int cb;
                while((cb = is.read(buffer)) > 0)
                {
                    os.write(buffer, 0, cb);
                }
                os.close();

                String path = dirName == null ? fileName : dirName + "/" + fileName;
                files.put(String.valueOf(well.getRowId()), well.getName(), path);
            }
        }

        return files;
    }

    void writeFCSFileCatalog(VirtualFile dir, SampleIdMap<String> files) throws IOException
    {
        List<String> columns = new ArrayList<>();
        columns.add(AnalysisSerializer.FCSFilesColumnName.ID.toString());
        columns.add(AnalysisSerializer.FCSFilesColumnName.Sample.toString());
        columns.add(AnalysisSerializer.FCSFilesColumnName.Path.toString());
        RowMapFactory<Object> rowMapFactory = new RowMapFactory<>(columns);

        List<Map<String, Object>> rows = new ArrayList<>();

        for (String id : files.idSet())
        {
            String name = files.getNameForId(id);
            String path = files.getById(id);

            rows.add(rowMapFactory.getRowMap(Arrays.asList(id, name, path)));
        }

        // write out the catalog file
        OutputStream tsv = dir.getOutputStream("FCSFiles.tsv");
        try (PrintWriter pw = PrintWriters.getPrintWriter(tsv); TSVWriter writer = new TSVMapWriter(columns, rows))
        {
            writer.write(pw);
        }
    }

    @RequiresLogin
    @RequiresPermission(ReadPermission.class)
    public class ExportAnalysis extends FormViewAction<ExportAnalysisForm>
    {
        private static final String MANIFEST_FILENAME = "manifest.json";
        List<FlowRun> _runs = null;
        List<FlowWell> _wells = null;
        boolean _success = false;

        URLHelper _successURL = null;
        private String _exportToScriptPath;
        private String _exportToScriptCommandLine;
        private String _exportToScriptLocation;
        private String _exportToScriptFormat;
        private Integer _exportToScriptTimeout;
        private boolean _exportToScriptDeleteOnComplete = true;

        private String _guid;


        @Override
        public void validateCommand(ExportAnalysisForm form, Errors errors)
        {
            if (form.getSendTo() == ExportAnalysisForm.SendTo.PipelineFiles || form.getSendTo() == ExportAnalysisForm.SendTo.PipelineZip)
            {
                PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
                if (root == null || !root.isValid())
                {
                    throw new NotFoundException("No valid pipeline root found");
                }
            }
            else if (form.getSendTo() == ExportAnalysisForm.SendTo.Script)
            {
                FlowModule module = ModuleLoader.getInstance().getModule(FlowModule.class);
                _exportToScriptPath = module.getExportToScriptPath(getContainer());
                if (_exportToScriptPath == null)
                    throw new ConfigurationException("Export script path must be configured in the folder settings");
                if (!new File(_exportToScriptPath).exists())
                    throw new ConfigurationException("Export script not found: " + _exportToScriptPath);

                _exportToScriptCommandLine = module.getExportToScriptCommandLine(getContainer());
                if (_exportToScriptCommandLine == null)
                    throw new ConfigurationException("Export script command line must be configured in the folder settings");

                _exportToScriptLocation = module.getExportToScriptLocation(getContainer());
                if (_exportToScriptLocation == null)
                    throw new ConfigurationException("Export location must be configured in the folder settings");
                if (!new File(_exportToScriptLocation).exists())
                    throw new ConfigurationException("Export location not found: " + _exportToScriptLocation);

                _exportToScriptFormat = module.getExportToScriptFormat(getContainer());
                String exportToScriptTimeout = module.getExportToScriptTimeout(getContainer());
                if (exportToScriptTimeout != null)
                {
                    try
                    {
                        _exportToScriptTimeout = Integer.parseInt(exportToScriptTimeout);
                    }
                    catch (NumberFormatException ex) { }
                }

                String exportToScriptDeleteOnComplete = module.getExportToScriptDeleteOnComplete(getContainer());
                if (exportToScriptDeleteOnComplete != null)
                    _exportToScriptDeleteOnComplete = Boolean.parseBoolean(exportToScriptDeleteOnComplete);
            }

            int[] runId = form.getRunId();
            int[] wellId = form.getWellId();

            // If no run or well IDs were in the request, check for selected rows.
            if ((runId == null || runId.length == 0) && (wellId == null || wellId.length == 0))
            {
                Set<String> selection = DataRegionSelection.getSelected(getViewContext(), form.getDataRegionSelectionKey(), true, true);
                if (form.getSelectionType() == null || form.getSelectionType().equals("runs"))
                    runId = PageFlowUtil.toInts(selection);
                else
                    wellId = PageFlowUtil.toInts(selection);
            }

            if (runId != null && runId.length > 0)
            {
                List<FlowRun> runs = new ArrayList<>();
                for (int id : runId)
                {
                    FlowRun run = FlowRun.fromRunId(id);
                    if (run == null)
                        throw new NotFoundException("Flow run not found");

                    runs.add(run);
                }
                _runs = runs;
            }
            else if (wellId != null && wellId.length > 0)
            {
                List<FlowWell> wells = new ArrayList<>();
                for (int id : wellId)
                {
                    FlowWell well = FlowWell.fromWellId(id);
                    if (well == null)
                        throw new NotFoundException("Flow well not found");

                    wells.add(well);
                }
                _wells = wells;
            }
            else
            {
                throw new NotFoundException("Flow run or well ids required");
            }
        }

        @Override
        public ModelAndView getView(ExportAnalysisForm form, boolean reshow, BindException errors) throws Exception
        {
            if (_success)
            {
                return null;
            }
            else
            {
                if (errors.hasErrors())
                {
                    return new SimpleErrorView(errors);
                }

                form._renderForm = true;
                return new JspView<>("/org/labkey/flow/view/exportAnalysis.jsp", form, errors);
            }
        }

        @Override
        public boolean handlePost(ExportAnalysisForm form, BindException errors) throws Exception
        {
            _guid = GUID.makeGUID();

            String fcsDirName = StringUtils.trimToNull(form.getFcsDirName());
            if (fcsDirName != null)
                fcsDirName = FileUtil.makeLegalName(fcsDirName);

            if (_runs != null && _runs.size() > 0)
            {
                String zipName = "ExportedRuns";
                if (_runs.size() == 1)
                {
                    FlowRun run = _runs.get(0);
                    zipName = getBaseName(run.getName());
                }

                // Uniquify run names if the same workspace has been imported twice and is now being exported.
                // CONSIDER: Unfortunately, the original run name will be lost -- consider adding a id column to the export format containing the lsid of the run.
                FileNameUniquifier uniquifier = new FileNameUniquifier(false);

                try (VirtualFile vf = createVirtualFile(form, zipName))
                {
                    SampleIdMap<String> files = null;
                    for (FlowRun run : _runs)
                    {
                        SampleIdMap<AttributeSet> keywords = new SampleIdMap<>();
                        SampleIdMap<AttributeSet> analysis = new SampleIdMap<>();
                        SampleIdMap<CompensationMatrix> matrices = new SampleIdMap<>();
                        getAnalysis(Arrays.asList(run.getWells()), keywords, analysis, matrices, form.isIncludeKeywords(), form.isIncludeGraphs(), form.isIncludeCompensation(), form.isIncludeStatistics());

                        String dirName = getBaseName(run.getName());
                        dirName = uniquifier.uniquify(dirName);

                        VirtualFile dir = vf.getDir(dirName);
                        AnalysisSerializer writer = new AnalysisSerializer(_log, dir);
                        if (form.isIncludeFCSFiles())
                        {
                            files = exportFCSFiles(dir, fcsDirName, run, 0);
                            writeFCSFileCatalog(dir, files);
                        }

                        writer.writeAnalysis(keywords, analysis, matrices, EnumSet.of(form.getExportFormat()));

                        List<Attachment> attachments = run.getAttachments();
                        AttachmentParent parent = new ExpRunAttachmentParent(run.getExperimentRun());
                        if (attachments != null && !attachments.isEmpty())
                        {
                            VirtualFile attachmentsDir = dir.getDir("attachments");
                            for (Attachment attachment : attachments)
                            {
                                try (InputStream is = AttachmentService.get().getInputStream(parent, attachment.getName());
                                     OutputStream os = attachmentsDir.getOutputStream(attachment.getName()))
                                {
                                    FileUtil.copyData(is, os);
                                }
                                catch (FileNotFoundException e)
                                {
                                    throw new FileNotFoundException("attachment file not found: " + attachment.getName());
                                }
                            }
                        }
                    }

                    _successURL = onExportComplete(form, vf, files);
                }
            }
            else if (_wells != null && _wells.size() > 0)
            {
                String zipName = "data";

                SampleIdMap<AttributeSet> keywords = new SampleIdMap<>();
                SampleIdMap<AttributeSet> analysis = new SampleIdMap<>();
                SampleIdMap<CompensationMatrix> matrices = new SampleIdMap<>();
                getAnalysis(_wells, keywords, analysis, matrices, form.isIncludeKeywords(), form.isIncludeGraphs(), form.isIncludeCompensation(), form.isIncludeStatistics());

                try (VirtualFile vf = createVirtualFile(form, zipName))
                {
                    VirtualFile dir = vf.getDir(zipName);
                    AnalysisSerializer writer = new AnalysisSerializer(_log, dir);

                    SampleIdMap<String> files = null;
                    if (form.isIncludeFCSFiles())
                    {
                        files = exportFCSFiles(dir, fcsDirName, _wells, 0);
                        writeFCSFileCatalog(dir, files);
                    }

                    writer.writeAnalysis(keywords, analysis, matrices, EnumSet.of(form.getExportFormat()));

                    _successURL = onExportComplete(form, vf, files);
                }
            }

            return _success = true;
        }

        private void writeManifest(String manifestJson, String dir) throws IOException
        {
            if (manifestJson == null || manifestJson.isEmpty())
                return;


            File file = new File(dir,MANIFEST_FILENAME);
            FileOutputStream statisticsFile = new FileOutputStream(file);

            try (PrintWriter pw = PrintWriters.getPrintWriter(statisticsFile))
            {
                pw.write(manifestJson);
            }
        }

        @NotNull
        private ExportAnalysisManifest buildExportAnalysisManifest(ExportAnalysisForm form, SampleIdMap<String> files)
        {
            ExportAnalysisManifest analysisManifest = new ExportAnalysisManifest();
            analysisManifest.setExportedBy(getUser().getDisplayName(getUser()));
            analysisManifest.setExportedDatetime(new Date());
            analysisManifest.setLabel(form.getLabel());
            analysisManifest.setSampleIdMap(files);
            analysisManifest.setExportFormat(form.getSendTo().name());
            analysisManifest.setGuid(_guid);
            return analysisManifest;
        }

        VirtualFile createVirtualFile(ExportAnalysisForm form, String name) throws IOException
        {
            switch (form.getSendTo())
            {
                case PipelineFiles:
                {
                    PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
                    File exportDir = new File(root.resolvePath(PipelineService.EXPORT_DIR), name);
                    exportDir.mkdirs();
                    return new FileSystemFile(exportDir);
                }

                case PipelineZip:
                {
                    PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
                    File exportDir = root.resolvePath(PipelineService.EXPORT_DIR);
                    exportDir.mkdir();
                    return new ZipFile(exportDir, FileUtil.makeFileNameWithTimestamp(name, "zip"));
                }

                case Script:
                {
                    File dir;
                    if (_exportToScriptLocation != null)
                        dir = new File(_exportToScriptLocation);
                    else
                        dir = new File(FileUtil.getTempDirectory(), "flow-export-to-script");

                    if ("zip".equalsIgnoreCase(_exportToScriptFormat))
                    {
                        File exportToScriptDir = new File(_exportToScriptLocation + "/" +  FileUtil.makeFileNameWithTimestamp(name));
                        return new ZipFile(exportToScriptDir, "export.zip");
                    }
                    else
                    {
                        File child = new File(dir, FileUtil.makeLegalName(name + "_" + getTimestamp()));
                        child.mkdirs();
                        return new FileSystemFile(child);
                    }
                }

                case Browser:
                default:
                    return new ZipFile(getViewContext().getResponse(), name + ".zip");
            }
        }

        URLHelper onExportComplete(ExportAnalysisForm form, VirtualFile vf, SampleIdMap<String> files) throws IOException
        {
            switch (form.getSendTo())
            {
                case PipelineFiles:
                case PipelineZip:
                    return PageFlowUtil.urlProvider(PipelineUrls.class).urlBrowse(getContainer(), null, PipelineService.EXPORT_DIR);

                case Script:
                    // after exporting the files, execute script as a pipeline job
                    File location = new File(vf.getLocation());
                    PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
                    ViewBackgroundInfo vbi = new ViewBackgroundInfo(getContainer(), getUser(), null);

                    ExportAnalysisManifest analysisManifest = buildExportAnalysisManifest(form, files);
                    writeManifest(analysisManifest.toJSON(), vf.getLocation());

                    PipelineJob job = new ExportToScriptJob(_guid, _exportToScriptPath, _exportToScriptCommandLine, _exportToScriptFormat, form.getLabel(), location, _exportToScriptTimeout, _exportToScriptDeleteOnComplete, vbi, root);
                    String jobGuid = null;
                    try
                    {
                        PipelineService.get().queueJob(job);
                        jobGuid = job.getJobGUID();
                    }
                    catch (PipelineValidationException e)
                    {
                        UnexpectedException.rethrow(e);
                    }

                    Integer jobId = null;
                    if (jobGuid != null)
                        jobId = PipelineService.get().getJobId(getUser(), getContainer(), jobGuid);

                    PipelineStatusUrls urls = PageFlowUtil.urlProvider(PipelineStatusUrls.class);
                    return jobId != null ? urls.urlDetails(getContainer(), jobId) : urls.urlBegin(getContainer());

                case Browser:
                default:
                    return null;
            }
        }

        @Override
        public URLHelper getSuccessURL(ExportAnalysisForm exportAnalysisForm)
        {
            return _successURL;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, null, "Export Analysis");
        }

    }

    private static class ExportToScriptJob extends PipelineJob
    {
        private final String _guid;
        private final String _exportToScriptPath;
        private final String _exportToScriptCommandLine;
        private final String _exportToScriptFormat;
        private final String _label;
        private final File _location;
        private final Integer _timeout;
        private final boolean _deleteOnComplete;

        public ExportToScriptJob(String guid, String exportToScriptPath, String exportToScriptCommandLine, String exportToScriptFormat, String label, File location, Integer timeout, boolean deleteOnComplete, ViewBackgroundInfo info, @NotNull PipeRoot root)
        {
            super(null, info, root);
            _guid = guid;
            _exportToScriptPath = exportToScriptPath;
            _exportToScriptCommandLine = exportToScriptCommandLine;
            _exportToScriptFormat = exportToScriptFormat;
            _label = label;
            _location = location;
            _timeout = timeout;
            _deleteOnComplete = deleteOnComplete;

            // setup the log file
            File logFile = new File(root.getLogDirectory(), FileUtil.makeFileNameWithTimestamp("export-to-script", "log"));
            setLogFile(logFile);
        }

        @Override
        public URLHelper getStatusHref()
        {
            URLHelper urlHelper = null;
            try
            {
                File log = getLogFile();
                List<String> lines = FileUtils.readLines(log, StringUtilsLabKey.DEFAULT_CHARSET);
                String url = getStatusUrl(lines);
                if(url != null)
                {
                    urlHelper = new URLHelper(url);
                }
            }
            catch (Exception e)
            {
                urlHelper = null;
            }
            return urlHelper;
        }

        @Override
        public String getDescription()
        {
            return "Export to script";
        }

        private List<String> parse(String command, Map<String, String> env)
        {
            List<String> ret = new ArrayList<>();

            String[] parts = command.split(" ");
            for (String part : parts)
            {
                part = part.trim();
                if (part.length() == 0)
                    continue;

                String arg = part;
                if (part.startsWith("${") && part.endsWith("}"))
                {
                    String key = part.substring("${".length(), part.length() - "}".length());
                    arg = env.getOrDefault(key, part);
                }

                if (arg != null)
                    ret.add(arg);
            }

            return ret;
        }

        @Override
        public void run()
        {
            setStatus(TaskStatus.running);

            Map<String, String> env = new CaseInsensitiveHashMap<>();
            env.put("guid", _guid);
            env.put("scriptPath", _exportToScriptPath);
            env.put("label", _label);
            env.put("location", _location.toString());
            env.put("timeout", Objects.toString(_timeout, ""));
            env.put("exportFormat", _exportToScriptFormat);
            List<String> params = parse(_exportToScriptCommandLine, env);

            ProcessBuilder pb = new ProcessBuilder(params);
            info("Executing script: " + StringUtils.join(pb.command(), " "));

            try
            {
                if (_timeout == null)
                    runSubProcess(pb, _location);
                else
                    runSubProcess(pb, _location, null, 0, false, _timeout, TimeUnit.SECONDS);

                if (getErrors() == 0 && _deleteOnComplete)
                {
                    debug("Deleting export directory: " + _location);
                    FileUtil.deleteDir(_location);
                }

                setStatus(TaskStatus.complete);
            }
            catch (PipelineJobException e)
            {
                error("Error running export script", e);
                setStatus(TaskStatus.error);
            }
        }
    }

    public void getAnalysis(List<FlowWell> wells,
                            SampleIdMap<AttributeSet> keywordAttrs,
                            SampleIdMap<AttributeSet> analysisAttrs,
                            SampleIdMap<CompensationMatrix> matrices,
                            boolean includeKeywords, boolean includeGraphBytes, boolean includeCompMatrices, boolean includeStatistics)
    {
        // CONSIDER: Uniquify well names if the same well has been imported into two different runs and is now being exported.
        for (FlowWell well : wells)
        {
            String id = String.valueOf(well.getRowId());
            String name = well.getName();

            if (well instanceof FlowFCSAnalysis && (includeStatistics || includeGraphBytes))
            {
                FlowFCSAnalysis analysis = (FlowFCSAnalysis) well;
                AttributeSet attrs = analysis.getAttributeSet(includeGraphBytes);
                analysisAttrs.put(id, name, attrs);
            }

            if (includeKeywords)
            {
                FlowFCSFile file = well.getFCSFileInput();
                AttributeSet attrs = file.getAttributeSet();
                keywordAttrs.put(id, name, attrs);
            }

            if (includeCompMatrices)
            {
                FlowCompensationMatrix flowCompMatrix = well.getCompensationMatrix();
                if (flowCompMatrix != null)
                {
                    CompensationMatrix matrix = flowCompMatrix.getCompensationMatrix();
                    if (matrix != null)
                        matrices.put(id, name, matrix);
                }
            }
        }
    }


    public static class AttachmentForm extends RunForm
    {
        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        private String name;
    }

    @RequiresPermission(ReadPermission.class)
    public class DownloadAttachmentAction extends BaseDownloadAction<AttachmentForm>
    {
        @Override
        public @Nullable Pair<AttachmentParent, String> getAttachment(AttachmentForm form)
        {
            final FlowRun run = form.getRun();
            if (null == run)
            {
                throw new NotFoundException();
            }

            return new Pair<>(new ExpRunAttachmentParent(run.getExperimentRun()), form.getName());
        }
    }
}
