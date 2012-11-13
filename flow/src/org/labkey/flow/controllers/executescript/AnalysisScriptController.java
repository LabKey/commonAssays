/*
 * Copyright (c) 2006-2012 LabKey Corporation
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

package org.labkey.flow.controllers.executescript;

import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.action.*;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.study.assay.AssayFileWriter;
import org.labkey.api.util.*;
import org.labkey.api.view.*;
import org.labkey.api.view.template.PageConfig;
import org.labkey.api.writer.ZipUtil;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.FlowSettings;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.FCS;
import org.labkey.flow.analysis.model.PCWorkspace;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.WorkspaceData;
import org.labkey.flow.controllers.executescript.ImportAnalysisForm.SelectFCSFileOption;
import org.labkey.flow.data.*;
import org.labkey.flow.persist.AnalysisSerializer;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.script.*;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.zip.ZipEntry;

public class AnalysisScriptController extends BaseFlowController
{
    private static final Logger _log = Logger.getLogger(AnalysisScriptController.class);

    public enum Action
    {
        chooseRunsToAnalyze,
        chooseAnalysisName,
        analyzeSelectedRuns,
    }

    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(AnalysisScriptController.class);

    public AnalysisScriptController() throws Exception
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        FlowScript script;

        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            script = FlowScript.fromURL(getActionURL(), getRequest());
            if (script == null)
            {
                return HttpView.redirect(new ActionURL(FlowController.BeginAction.class, getContainer()));
            }
            FlowPreference.showRuns.updateValue(getRequest());
            return new JspView<FlowScript>(AnalysisScriptController.class, "showScript.jsp", script, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, script, null);
        }
    }

    protected Page getPage(String name) throws Exception
    {
        Page ret = (Page) getFlowPage(name);
        ret.setScript(getScript());
        return ret;
    }

    public abstract class BaseAnalyzeRunsAction extends SimpleViewAction<ChooseRunsToAnalyzeForm>
    {
        FlowScript script;
        Pair<String, Action> nav;

        protected ModelAndView chooseRunsToAnalyze(ChooseRunsToAnalyzeForm form, BindException errors)
        {
            nav = new Pair<String, Action>("Choose runs", Action.chooseRunsToAnalyze);
            form.populate(errors);
            return new JspView<ChooseRunsToAnalyzeForm>(AnalysisScriptController.class, "chooseRunsToAnalyze.jsp", form, errors);
        }

        protected ModelAndView chooseAnalysisName(ChooseRunsToAnalyzeForm form, BindException errors)
        {
            nav = new Pair<String, Action>("Choose new analysis name", Action.chooseAnalysisName);
            return new JspView<ChooseRunsToAnalyzeForm>(AnalysisScriptController.class, "chooseAnalysisName.jsp", form, errors);
        }

        protected ModelAndView analyzeRuns(ChooseRunsToAnalyzeForm form, BindException errors,
                                         int[] runIds, String experimentLSID)
                throws Exception
        {
            nav = new Pair<String, Action>(null, Action.analyzeSelectedRuns);
            DataRegionSelection.clearAll(getViewContext());

            FlowExperiment experiment = FlowExperiment.fromLSID(experimentLSID);
            String experimentName = form.ff_analysisName;
            if (experiment != null)
            {
                experimentName = experiment.getName();
            }
            FlowScript analysis = form.getProtocol();
            AnalyzeJob job = new AnalyzeJob(getViewBackgroundInfo(), experimentName, experimentLSID, FlowProtocol.ensureForContainer(getUser(), getContainer()), analysis, form.getProtocolStep(), runIds, PipelineService.get().findPipelineRoot(getContainer()));
            if (form.getCompensationMatrixId() != 0)
            {
                job.setCompensationMatrix(FlowCompensationMatrix.fromCompId(form.getCompensationMatrixId()));
            }
            job.setCompensationExperimentLSID(form.getCompensationExperimentLSID());
            return HttpView.redirect(executeScript(job));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, script, nav.first);
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class ChooseRunsToAnalyzeAction extends BaseAnalyzeRunsAction
    {
        FlowScript script;

        public ModelAndView getView(ChooseRunsToAnalyzeForm form, BindException errors) throws Exception
        {
            script = form.getProtocol();
            return chooseRunsToAnalyze(form, errors);
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class AnalyzeSelectedRunsAction extends BaseAnalyzeRunsAction
    {
        public ModelAndView getView(ChooseRunsToAnalyzeForm form, BindException errors) throws Exception
        {
            script = form.getProtocol();
            int[] runIds = form.getSelectedRunIds();
            if (runIds.length == 0)
            {
                errors.reject(ERROR_MSG, "Please select at least one run to analyze.");
                return chooseRunsToAnalyze(form, errors);
            }
            String experimentLSID = form.getAnalysisLSID();
            if (experimentLSID == null)
            {
                return chooseAnalysisName(form, errors);
            }
            return analyzeRuns(form, errors, runIds, experimentLSID);
        }
    }

    protected void collectNewPaths(ImportRunsForm form, Errors errors) throws Exception
    {
        PipelineService service = PipelineService.get();
        PipeRoot root = service.findPipelineRoot(getContainer());
        if (root == null)
        {
            errors.reject(ERROR_MSG, "The pipeline root is not set.");
            return;
        }

        File directory;
        String displayPath;
        if (StringUtils.isEmpty(form.getPath()) || "./".equals(form.getPath()))
        {
            displayPath = "root directory";
            directory = root.getRootPath();
        }
        else
        {
            displayPath = "'" + PageFlowUtil.decode(form.getPath()) + "'";
            directory = root.resolvePath(form.getPath());
        }
        form.setDisplayPath(displayPath);

        if (directory == null)
        {
            errors.reject(ERROR_MSG, "The path " + displayPath + " is invalid.");
            return;
        }

        if (!directory.isDirectory())
        {
            errors.reject(ERROR_MSG, displayPath + " is not a directory.");
            return;
        }

        List<File> files = new ArrayList<File>();
        if (form.isCurrent())
        {
            files.add(directory);
        }
        else if (form.getFile() == null || form.getFile().length == 0)
        {
            File[] dirFiles = directory.listFiles((java.io.FileFilter)DirectoryFileFilter.INSTANCE);
            if (dirFiles != null)
                files.addAll(Arrays.asList(dirFiles));
        }
        else
        {
            files.addAll(form.getValidatedFiles(getContainer()));
        }

        Set<File> usedPaths = new HashSet<File>();
        for (FlowRun run : FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords))
        {
            // skip FlowJo workspace imported runs
            if (run.getWorkspace() == null)
                usedPaths.add(run.getExperimentRun().getFilePathRoot());
        }

        Map<String, String> ret = new TreeMap<String, String>();
        boolean anyFCSDirectories = false;
        for (File file : files)
        {
            File[] fcsFiles = file.listFiles((java.io.FileFilter)FCS.FCSFILTER);
            if (fcsFiles.length > 0)
            {
                anyFCSDirectories = true;
                if (!usedPaths.contains(file))
                {
                    String displayName;
                    String relativeFile;
                    if (file.equals(directory))
                    {
                        displayName = "Current Directory (" + fcsFiles.length + " fcs files)";
                        relativeFile = "";
                    }
                    else
                    {
                        displayName = file.getName() + " (" + fcsFiles.length + " fcs files)";

                        // make relative to the path parameter
                        URI relativeURI = URIUtil.relativize(directory.toURI(), file.toURI());
                        if (relativeURI == null)
                        {
                            errors.reject(ERROR_MSG, file.getName() + " is not under '" + displayPath + "'");
                            continue;
                        }

                        relativeFile = relativeURI.getPath();
                        if (relativeFile.endsWith("/"))
                            relativeFile = relativeFile.substring(0, relativeFile.length()-1);
                        if (relativeFile.length() == 0 || relativeFile.contains("..") || relativeFile.contains("/") || relativeFile.contains("\\"))
                        {
                            errors.reject(ERROR_MSG, relativeFile + " is not under '" + displayPath + "'");
                            continue;
                        }
                    }

                    ret.put(relativeFile, displayName);
                }
            }
        }

        if (ret.isEmpty())
        {
            if (anyFCSDirectories)
                errors.reject(ERROR_MSG, "All of the directories in " + displayPath + " have already been uploaded.");
            else
                errors.reject(ERROR_MSG, "No FCS files were found in " + displayPath + " or its children.");
            return;
        }

        form.setNewPaths(ret);
    }

    public abstract class ImportRunsBaseAction extends SimpleViewAction<ImportRunsForm>
    {
        protected void validatePipeline()
        {
            PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
            root.requiresPermission(getContainer(), getUser(), InsertPermission.class);
        }

        protected ModelAndView confirmRuns(ImportRunsForm form, BindException errors) throws Exception
        {
            validatePipeline();

            collectNewPaths(form, errors);
            return new JspView<PipelinePathForm>(AnalysisScriptController.class, "confirmRunsToImport.jsp", form, errors);
        }

        protected ModelAndView uploadRuns(ImportRunsForm form, BindException errors) throws Exception
        {
            if (!form.isConfirm())
            {
                URLHelper url = form.getReturnURLHelper();
                if (url == null)
                    url = new ActionURL(BeginAction.class, getContainer());
                return HttpView.redirect(url);
            }

            validatePipeline();
            List<File> files;
            PipeRoot pr = PipelineService.get().findPipelineRoot(getContainer());
            if (form.isCurrent())
            {
                files = Collections.singletonList(pr.resolvePath(form.getPath()));
            }
            else
                files = form.getValidatedFiles(form.getContainer());

            ViewBackgroundInfo vbi = getViewBackgroundInfo();
            KeywordsJob job = new KeywordsJob(vbi, FlowProtocol.ensureForContainer(getUser(), vbi.getContainer()), files, pr);
            return HttpView.redirect(executeScript(job));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, null, "Import Flow FCS Files");
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class ConfirmImportRunsAction extends ImportRunsBaseAction
    {
        public ModelAndView getView(ImportRunsForm form, BindException errors) throws Exception
        {
            return confirmRuns(form, errors);
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class ImportRunsAction extends ImportRunsBaseAction
    {
        public ModelAndView getView(ImportRunsForm form, BindException errors) throws Exception
        {
            return uploadRuns(form, errors);
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class ShowUploadRunsAction extends RedirectAction
    {
        public ActionURL getSuccessURL(Object o)
        {
            return PageFlowUtil.urlProvider(PipelineUrls.class).urlBrowse(getContainer(),
                FlowPipelineProvider.NAME);
        }

        public boolean doAction(Object o, BindException errors) throws Exception
        {
            return true;
        }

        public void validateCommand(Object target, Errors errors)
        {
        }
    }

    abstract static public class Page extends FlowPage
    {
        FlowScript _analysisScript;
        public void setScript(FlowScript script)
        {
            _analysisScript = script;
        }
        public FlowScript getScript()
        {
            return _analysisScript;
        }
    }

    public enum ImportAnalysisStep
    {
        SELECT_ANALYSIS("Select Analysis"),
        SELECT_FCSFILES("Select FCS Files"),
        REVIEW_SAMPLES("Review Samples"),
        ANALYSIS_ENGINE("Analysis Engine"),
        ANALYSIS_OPTIONS("Analysis Options"),
        CHOOSE_ANALYSIS("Analysis Folder"),
        CONFIRM("Confirm");

        String title;

        ImportAnalysisStep(String title)
        {
            this.title = title;
        }

        public String getTitle()
        {
            return title;
        }

        public int getNumber()
        {
            return ordinal()+1;
        }

        public static ImportAnalysisStep fromNumber(int number)
        {
            if (number <= 0)
                return SELECT_ANALYSIS;

            for (ImportAnalysisStep step : values())
                if (step.getNumber() == number)
                    return step;
            return SELECT_ANALYSIS;
        }
    }

    /**
     * This action acts as a bridge between FlowPipelineProvider and ImportAnalysisAction
     * by setting the 'workspace.path' parameter and POSTs to the first wizard step.
     */
    @RequiresPermissionClass(UpdatePermission.class)
    public class ImportAnalysisFromPipelineAction extends SimpleViewAction<PipelinePathForm>
    {
        @Override
        public ModelAndView getView(PipelinePathForm form, BindException errors) throws Exception
        {
            File f = form.getValidatedSingleFile(getContainer());
            PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
            String workspacePath = "/" + root.relativePath(f).replace('\\', '/');

            ActionURL url = new ActionURL(ImportAnalysisAction.class, getContainer());

            List<Pair<String, String>> inputs = new ArrayList<Pair<String, String>>();
            inputs.add(Pair.of("workspace.path", workspacePath));
            inputs.add(Pair.of("step", String.valueOf(ImportAnalysisStep.SELECT_ANALYSIS.getNumber())));

            getPageConfig().setTemplate(PageConfig.Template.None);
            return new HttpPostRedirectView(url.getLocalURIString(), inputs);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }


    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class ImportAnalysisAction extends FormViewAction<ImportAnalysisForm>
    {
        String title;
        PipeRoot root;
        boolean foundRoot;

        public void validateCommand(ImportAnalysisForm form, Errors errors)
        {
            getWorkspace(form, errors);
            if (errors.hasErrors())
                form.setWizardStep(ImportAnalysisStep.SELECT_ANALYSIS);
        }

        public ModelAndView getView(ImportAnalysisForm form, boolean reshow, BindException errors) throws Exception
        {
            // When entering the wizard from the pipeline browser "Import Workspace" button,
            // we aren't POST'ing and so haven't parsed or validated the workspace yet.
            if ("GET".equals(getRequest().getMethod()) && form.getWorkspace().getWorkspaceObject() == null && form.getWizardStep() == ImportAnalysisStep.SELECT_FCSFILES)
                validateCommand(form, errors);

            title = form.getWizardStep().getTitle();
            return new JspView<ImportAnalysisForm>(AnalysisScriptController.class, "importAnalysis.jsp", form, errors);
        }

        public boolean handlePost(ImportAnalysisForm form, BindException errors) throws Exception
        {
            if (form.getWizardStep() == null)
            {
                form.setWizardStep(ImportAnalysisStep.SELECT_ANALYSIS);
            }
            else
            {
                // wizard step is the last step shown to the user.
                // Handle the post and setup the form for the next wizard step.
                switch (form.getWizardStep())
                {
                    case SELECT_ANALYSIS:
                        stepSelectAnalysis(form, errors);
                        break;

                    case SELECT_FCSFILES:
                        stepSelectFCSFiles(form, errors);
                        break;

                    case REVIEW_SAMPLES:
                        stepReviewSamples(form, errors);
                        break;

                    case ANALYSIS_ENGINE:
                        stepAnalysisEngine(form, errors);
                        break;

                    case ANALYSIS_OPTIONS:
                        stepAnalysisOptions(form, errors);
                        break;

                    case CHOOSE_ANALYSIS:
                        stepChooseAnalysis(form, errors);
                        break;

                    case CONFIRM:
                        stepConfirm(form, errors);
                        break;
                }
            }

            title = form.getWizardStep().getTitle();

            return false;
        }

        private PipeRoot getPipeRoot()
        {
            if (foundRoot)
                return root;
            foundRoot = true;
            root = PipelineService.get().findPipelineRoot(getContainer());
            return root;
        }

        // Saves uploaded "workspace.file" to pipeline root
        // or reads "workspace.path" from pipeline.
        private void getWorkspace(ImportAnalysisForm form, Errors errors)
        {
            WorkspaceData workspace = form.getWorkspace();
            Map<String, MultipartFile> files = getFileMap();
            MultipartFile file = files.get("workspace.file");
            if (file != null && StringUtils.isNotEmpty(file.getOriginalFilename()))
            {
                // ensure the pipeline root exists
                PipeRoot root = getPipeRoot();
                if (root == null)
                {
                    errors.reject(ERROR_MSG, "Please configure the pipeline root for this folder");
                    return;
                }

                try
                {
                    // save the uploaded workspace
                    AssayFileWriter writer = new AssayFileWriter();
                    File dir = writer.ensureUploadDirectory(getContainer());
                    File uploadedFile = AssayFileWriter.findUniqueFileName(file.getOriginalFilename(), dir);
                    file.transferTo(uploadedFile);

                    String uploadedPath = root.relativePath(uploadedFile);
                    form.getWorkspace().setPath(uploadedPath);
                }
                catch (Exception e)
                {
                    errors.reject(ERROR_MSG, "Error saving uploaded workspace to pipeline: " + e.getMessage());
                    return;
                }
            }

            workspace.validate(getContainer(), errors, getRequest());
        }

        private FlowRun getExistingKeywordRun(ImportAnalysisForm form, Errors errors)
        {
            int keywordRunId = form.getExistingKeywordRunId();
            if (keywordRunId > 0 && form.getKeywordDir() != null && form.getKeywordDir().length > 0)
            {
                errors.reject(ERROR_MSG, "Can't select both an existing run and a file path.");
                return null;
            }

            if (keywordRunId > 0)
                return FlowRun.fromRunId(keywordRunId);
            return null;
        }

        // path may be:
        // - absolute (run path)
        // - a file-browser path (absolute but relative to pipe root)
        // - a file-browser path (relative to pipe root)
        private File getDir(String path, Errors errors)
        {
            PipeRoot root = getPipeRoot();
            File dir = new File(path);
            if (!dir.isAbsolute() || !root.isUnderRoot(dir))
                dir = root.resolvePath(path);

            if (dir == null)
            {
                errors.reject(ERROR_MSG, "The directory containing FCS files wasn't found.");
                return null;
            }
            if (!root.isUnderRoot(dir))
            {
                errors.reject(ERROR_MSG, "The directory isn't under the current pipeline root");
                return null;
            }
            if (!dir.isDirectory())
            {
                errors.reject(ERROR_MSG, "The path specified must be a directory containing FCS files.");
                return null;
            }
            return dir;
        }

        // Get the directory to use as the file path root of the flow analysis run.
        private File getRunPathRoot(FlowRun keywordRun, List<File> keywordDirs, Map<String, FlowFCSFile> resolvedFCSFiles, File workspacePath, Errors errors) throws Exception
        {
            if (keywordRun != null && keywordRun.getPath() != null)
            {
                File dir = getDir(keywordRun.getPath(), errors);
                if (errors.hasErrors())
                    return null;

                return dir;
            }

            if (keywordDirs != null && !keywordDirs.isEmpty())
            {
                // CONSIDER: Use common parent path of all keywordDir paths.  For now, just use first path.
                return keywordDirs.get(0);
            }

            if (resolvedFCSFiles != null && !resolvedFCSFiles.isEmpty())
            {
                // CONSIDER: Use common parent path of all resolvedFCSFile paths.  For now, just use first path.
                for (Map.Entry<String, FlowFCSFile> entry : resolvedFCSFiles.entrySet())
                {
                    FlowFCSFile fcsFile = entry.getValue();
                    if (fcsFile != null)
                    {
                        FlowRun flowRun = fcsFile.getRun();
                        ExpRun expRun = flowRun != null ? flowRun.getExperimentRun() : null;
                        if (expRun != null)
                            return expRun.getFilePathRoot();
                    }
                }
            }

//            if (workspacePath != null) // XXX: not the same as previous behavior
//                return workspacePath.getParentFile();

            return null;
        }

        // Get the path to either the previously imported keyword run or
        // to the selected pipeline browser directory under the pipeline root.
        private List<File> getKeywordDirs(ImportAnalysisForm form, Errors errors) throws Exception
        {
            FlowRun keywordRun = getExistingKeywordRun(form, errors);
            if (errors.hasErrors())
                return null;

            String path = null;
            if (keywordRun != null)
            {
                String keywordRunPath = keywordRun.getPath();
                if (keywordRunPath == null)
                {
                    assert false; // form shouldn't allow user to select a keyword run without a path
                    errors.reject(ERROR_MSG, "Selected FCS File run doesn't have a path.");
                    return null;
                }
                PipeRoot root = getPipeRoot();
                File keywordRunFile = new File(keywordRunPath);
                if (!root.isUnderRoot(keywordRunFile))
                {
                    errors.reject(ERROR_MSG, "Selected FCS File run isn't under the current pipeline root.");
                    return null;
                }
                path = root.relativePath(keywordRunFile);
                if (path == null)
                {
                    errors.reject(ERROR_MSG, "Couldn't relativize the selected FCS File run path");
                    return null;
                }
            }
            else if (form.getKeywordDir() != null && form.getKeywordDir().length > 0)
            {
                // UNDONE: Currently, only a single keyword directory is supported.
                path = PageFlowUtil.decode(form.getKeywordDir()[0]);
            }

            if (path != null)
            {
                File keywordDir = getDir(path, errors);
                if (errors.hasErrors())
                    return null;

                return Collections.singletonList(keywordDir);
            }
            return null;
        }

        // Returns a map of workspace sample label -> FlowFCSFile from either
        // the resolved FCSFiles previously imported or from an existing keyword run.
        private Map<String, FlowFCSFile> getSelectedFCSFiles(ImportAnalysisForm form, Errors errors) throws Exception
        {
            WorkspaceData workspaceData = form.getWorkspace();
            Workspace workspace = workspaceData.getWorkspaceObject();
            Map<String, SelectedSamples.ResolvedSample> rows = form.getSelectedSamples().getRows();
            if (rows.size() == 0)
                return null;

            Map<String, FlowFCSFile> fcsFiles = new HashMap<String, FlowFCSFile>(rows.size());
            int keywordRunId = form.getExistingKeywordRunId();
            if (keywordRunId > 0)
            {
                // Get the FlowFCSFile for the exising keywords run
                FlowRun keywordRun = getExistingKeywordRun(form, errors);
                if (errors.hasErrors())
                    return null;

                // XXX: do we need to remap by workspace sample label ?
                for (FlowWell well : keywordRun.getWells())
                {
                    if (well instanceof FlowFCSFile)
                    {
                        FlowFCSFile file = (FlowFCSFile)well;
                        if (file.isOriginalFCSFile())
                            fcsFiles.put(well.getName(), (FlowFCSFile)well);
                    }
                }
            }
            else
            {
                if (form.isResolving() && form.getKeywordDir() != null && form.getKeywordDir().length > 0 && StringUtils.isNotEmpty(form.getKeywordDir()[0]))
                {
                    errors.reject(ERROR_MSG, "Can't select directory of FCS files and resolve existing FCS files");
                    return null;
                }

                // Get the FlowFCSFile for the resolved samples and remap by sample label.
                for (Map.Entry<String, SelectedSamples.ResolvedSample> entry : rows.entrySet())
                {
                    SelectedSamples.ResolvedSample resolvedSample = entry.getValue();
                    if (resolvedSample.isSelected())
                    {
                        FlowFCSFile file = null;
                        if (form.isResolving())
                        {
                            file = (FlowFCSFile)FlowWell.fromWellId(resolvedSample.getMatchedFile());
                            if (file == null)
                            {
                                errors.reject(ERROR_MSG, "Failed to find resolved FCS file with rowid '" + resolvedSample.getMatchedFile() + "'");
                                return null;
                            }

                            if (!file.isOriginalFCSFile())
                            {
                                errors.reject(ERROR_MSG, "Resolved FCS file '" + file.getName() + "' is a FCS files created from importing an external analysis.");
                                return null;
                            }
                        }

                        Workspace.SampleInfo sampleInfo = workspace.getSample(entry.getKey());
                        if (sampleInfo == null)
                            continue;
                        fcsFiles.put(sampleInfo.getLabel(), file);
                    }
                }
            }

            return fcsFiles;
        }

        private AnalysisEngine getAnalysisEngine(ImportAnalysisForm form, Errors errors) throws Exception
        {
            // UNDONE: validate pipeline root is available for rEngine
            if (form.getSelectAnalysisEngine() != null)
                return form.getSelectAnalysisEngine();
            return AnalysisEngine.FlowJoWorkspace;
        }

        private void stepSelectAnalysis(ImportAnalysisForm form, BindException errors) throws Exception
        {
            WorkspaceData workspaceData = form.getWorkspace();
            Workspace workspace = workspaceData.getWorkspaceObject();
            List<Workspace.SampleInfo> samples = workspace.getAllSamples();
            if (samples.size() == 0)
            {
                errors.reject(ERROR_MSG, "The workspace doesn't contain samples");
                return;
            }

            PipeRoot root = null;
            String path = workspaceData.getPath();
            File workspaceFile = null;
            if (path != null)
            {
                root = getPipeRoot();
                workspaceFile = root.resolvePath(path);
            }

            // CONSIDER: Simplify -- if there are *any* keyword runs, use SelectFCSFileOption.Previous.

            // First, try to find an existing run in the same directory as the workspace.
            // Default to selecting a previous run if there are any keyword runs.
            if (workspaceFile != null && form.getExistingKeywordRunId() == 0)
            {
//                FlowRun[] keywordRuns = FlowRun.getRunsForPath(getContainer(), FlowProtocolStep.keywords, workspaceFile.getParentFile());
//                if (keywordRuns != null && keywordRuns.length > 0)
//                {
//                    form.setSelectFCSFilesOption(SelectFCSFileOption.Previous);
//                    for (FlowRun keywordRun : keywordRuns)
//                    {
//                        FlowExperiment experiment = keywordRun.getExperiment();
//                        if (experiment != null && experiment.isKeywords())
//                        {
//                            form.setExistingKeywordRunId(keywordRun.getRunId());
//                            //form.setRunFilePathRoot(keywordRun.getPath());
//                            break;
//                        }
//                    }
//                }
                int fcsRunCount = FlowManager.get().getFCSRunCount(getContainer());
                if (fcsRunCount > 0)
                    form.setSelectFCSFilesOption(SelectFCSFileOption.Previous);
            }

            // Next, guess the FCS files are in the same directory as the workspace.
            if (form.getExistingKeywordRunId() == 0 && form.getKeywordDir() == null)
            {
                if (workspaceFile != null)
                {
                    File keywordDir = null;
                    for (Workspace.SampleInfo sampleInfo : samples)
                    {
                        File sampleFile = new File(workspaceFile.getParent(), sampleInfo.getLabel());
                        if (sampleFile.exists())
                        {
                            keywordDir = workspaceFile.getParentFile();
                            break;
                        }
                    }

                    if (keywordDir != null)
                    {
                        String relPath = root.relativePath(keywordDir);
                        if (relPath != null)
                        {
                            String[] parts = StringUtils.split(relPath, File.separatorChar);
                            String keywordPath = StringUtils.join(parts, "/");
                            form.setKeywordDir(new String[] { keywordPath });
                            //form.setRunFilePathRoot(keywordPath);
                        }
                        form.setSelectFCSFilesOption(SelectFCSFileOption.Browse);
                    }
                }
            }

            form.setWizardStep(ImportAnalysisStep.SELECT_FCSFILES);
        }

        private void stepSelectFCSFiles(ImportAnalysisForm form, BindException errors) throws Exception
        {
            SelectFCSFileOption fcsFilesOption = form.getSelectFCSFilesOption();

            // Disallow other select options if there are FCS files included in the archive.
            if (fcsFilesOption != SelectFCSFileOption.Included && form.getWorkspace().isIncludesFCSFiles())
            {
                errors.reject(ERROR_MSG, "Can't select option other than Inclded if FCS files are already included");
                return;
            }

            if (fcsFilesOption == SelectFCSFileOption.None)
            {
                // Don't associate FCS files with the workspace.
                WorkspaceData workspaceData = form.getWorkspace();
                Workspace workspace = workspaceData.getWorkspaceObject();
                List<Workspace.SampleInfo> sampleInfos = workspace.getAllSamples();
                Map<String, SelectedSamples.ResolvedSample> rows = new HashMap<String, SelectedSamples.ResolvedSample>();
                for (Workspace.SampleInfo sampleInfo : sampleInfos)
                {
                    SelectedSamples.ResolvedSample resolvedSample = new SelectedSamples.ResolvedSample(true, 0, null);
                    rows.put(sampleInfo.getSampleId(), resolvedSample);
                }

                SelectedSamples samples = form.getSelectedSamples();
                samples.setSamples(sampleInfos);
                samples.setKeywords(workspace.getKeywords());
                samples.setRows(rows);

                //// Skip Analysis engine step.  Analysis engine can only be selected when no FCS files are associated with the run
                form.setWizardStep(ImportAnalysisStep.REVIEW_SAMPLES);
            }
            else if (fcsFilesOption == SelectFCSFileOption.Included)
            {
                if (!form.getWorkspace().isIncludesFCSFiles())
                {
                    assert false; // form shouldn't allow user to select 'Included' if there are no included FCS files.
                    errors.reject(ERROR_MSG, "FCS files are not included.");
                    return;
                }

                // UNDONE: Extract FCSFiles into pipeline if needed...

                form.setWizardStep(ImportAnalysisStep.REVIEW_SAMPLES);
            }
            else if (fcsFilesOption == SelectFCSFileOption.Previous)
            {
                if (form.getKeywordDir() != null && form.getKeywordDir().length > 0)
                {
                    errors.reject(ERROR_MSG, "Can't select directory from pipeline and use previously imported files.");
                    return;
                }

                // Resolve samples from workspace with previously imported FCSFiles
                form.setResolving(true);
                resolveSamples(form);

                form.setWizardStep(ImportAnalysisStep.REVIEW_SAMPLES);
            }
            else if (fcsFilesOption == SelectFCSFileOption.Browse)
            {
                WorkspaceData workspaceData = form.getWorkspace();
                List<File> keywordDirs = getKeywordDirs(form, errors);
                if (keywordDirs == null || keywordDirs.size() == 0)
                    errors.reject(ERROR_MSG, "No directory selected");

                if (keywordDirs != null && keywordDirs.size() > 1)
                    errors.reject(ERROR_MSG, "Only a single keyword directoy can currently be imported.");

                if (errors.hasErrors())
                    return;

                File keywordDir = keywordDirs.get(0);

                if (form.getExistingKeywordRunId() == 0)
                {
                    // Translate selected keyword directory into a existing keyword run if possible.
                    FlowRun[] keywordRuns = FlowRun.getRunsForPath(getContainer(), FlowProtocolStep.keywords, keywordDir);
                    if (keywordRuns != null && keywordRuns.length > 0)
                    {
                        for (FlowRun keywordRun : keywordRuns)
                        {
                            if (keywordRun.getExperiment().isKeywords())
                            {
                                form.setExistingKeywordRunId(keywordRun.getRunId());
                                form.setKeywordDir(null);
                                break;
                            }
                        }
                    }
                }

                if (form.getExistingKeywordRunId() > 0)
                {
                    // Resolve workspace samples against the previously imported FCS files.
                    form.setResolving(true);
                    resolveSamples(form);
                }
                else
                {
                    // We don't have an existing keyword run, check that at least one sample in the
                    // selected directory exists and mark those samples as selected for import.
                    boolean found = false;
                    Workspace workspace = workspaceData.getWorkspaceObject();
                    List<Workspace.SampleInfo> sampleInfos = workspace.getAllSamples();
                    Map<String, SelectedSamples.ResolvedSample> rows = new HashMap<String, SelectedSamples.ResolvedSample>();
                    for (Workspace.SampleInfo sampleInfo : sampleInfos)
                    {
                        File sampleFile = new File(keywordDir, sampleInfo.getLabel());
                        boolean exists = sampleFile.exists();
                        if (exists)
                            found = true;
                        SelectedSamples.ResolvedSample resolvedSample = new SelectedSamples.ResolvedSample(exists, 0, null);
                        rows.put(sampleInfo.getSampleId(), resolvedSample);
                    }

                    if (!found)
                    {
                        String msg = "None of the samples used by the workspace were found in the selected directory '" + form.getKeywordDir()[0] + "'.";
                        errors.reject(ERROR_MSG, msg);
                        return;
                    }

                    SelectedSamples samples = form.getSelectedSamples();
                    samples.setSamples(sampleInfos);
                    samples.setKeywords(workspace.getKeywords());
                    samples.setRows(rows);

                    form.setResolving(false);
                }

                form.setWizardStep(ImportAnalysisStep.REVIEW_SAMPLES);
            }
            else
            {
                assert false;
                errors.reject(ERROR_MSG, "Unexpected option");
            }
        }

        private void resolveSamples(ImportAnalysisForm form)
        {
            WorkspaceData workspaceData = form.getWorkspace();
            Workspace workspace = workspaceData.getWorkspaceObject();
            List<Workspace.SampleInfo> sampleInfos = workspace.getAllSamples();

            SelectedSamples selectedSamples = form.getSelectedSamples();
            selectedSamples.setSamples(sampleInfos);
            selectedSamples.setKeywords(workspace.getKeywords());

            // If this is the intial visit, resolve the FCS files otherwise use the form POSTed resolved data.
            if (selectedSamples.getRows().isEmpty())
            {
                List<FlowFCSFile> files;
                if (form.getExistingKeywordRunId() > 0)
                {
                    FlowRun run = FlowRun.fromRunId(form.getExistingKeywordRunId());
                    files = Arrays.asList(run.getFCSFiles());
                }
                else
                {
                    files = FlowFCSFile.fromName(getContainer(), null);
                }

                Map<Workspace.SampleInfo, Pair<FlowFCSFile, List<FlowFCSFile>>> resolved = resolveSamples(sampleInfos, files);

                Map<String, SelectedSamples.ResolvedSample> rows = new HashMap<String, SelectedSamples.ResolvedSample>();
                for (Workspace.SampleInfo sample : sampleInfos)
                {
                    SelectedSamples.ResolvedSample resolvedSample = null;
                    Pair<FlowFCSFile, List<FlowFCSFile>> matches = resolved.get(sample);
                    if (matches != null)
                    {
                        FlowFCSFile perfectMatch = matches.first;
                        int perfectMatchId = perfectMatch != null ? perfectMatch.getRowId() : 0;
                        List<FlowFCSFile> candidates = matches.second;

                        if (perfectMatchId != 0 || (candidates != null && candidates.size() > 0))
                        {
                            resolvedSample = new SelectedSamples.ResolvedSample(true, perfectMatchId, candidates);
                        }
                    }

                    if (resolvedSample == null)
                        resolvedSample = new SelectedSamples.ResolvedSample(false, 0, null);

                    rows.put(sample.getSampleId(), resolvedSample);
                }
                selectedSamples.setRows(rows);
            }
        }

        private Map<Workspace.SampleInfo, Pair<FlowFCSFile, List<FlowFCSFile>>> resolveSamples(List<Workspace.SampleInfo> samples, List<FlowFCSFile> files)
        {
            if (files.isEmpty())
                return Collections.emptyMap();

            Map<Workspace.SampleInfo, Pair<FlowFCSFile, List<FlowFCSFile>>> resolved = new LinkedHashMap<Workspace.SampleInfo, Pair<FlowFCSFile, List<FlowFCSFile>>>();

            MultiHashMap<String, FlowFCSFile> filesByName = new MultiHashMap<String, FlowFCSFile>();
            MultiHashMap<String, FlowFCSFile> filesByGUID = new MultiHashMap<String, FlowFCSFile>();
            for (FlowFCSFile file : files)
            {
                // Don't include FCSFile wells created for attaching extra keywords.
                if (!file.isOriginalFCSFile())
                    continue;

                filesByName.put(file.getName(), file);

                String guidKeyword = file.getKeyword("GUID");
                if (guidKeyword != null)
                    filesByGUID.put(guidKeyword, file);
            }

            // Now, attempt to find an exact match and a list of possible candidates.
            for (Workspace.SampleInfo sample : samples)
            {
                FlowFCSFile perfectMatch = null;
                LinkedHashSet<FlowFCSFile> candidates = new LinkedHashSet<FlowFCSFile>();

                // First, try the most specific keyword
                Collection<FlowFCSFile> dupes = null;
                if (sample.getKeywords().containsKey("GUID"))
                {
                    dupes = filesByGUID.get(sample.getKeywords().get("GUID"));
                    if (dupes != null && dupes.size() == 1)
                        perfectMatch = dupes.iterator().next();
                    candidates.addAll(dupes);
                }

                // Next, try well name
                dupes = filesByName.get(sample.getSampleName());
                if (dupes != null)
                {
                    if (perfectMatch == null && dupes.size() == 1)
                        perfectMatch = dupes.iterator().next();
                    candidates.addAll(dupes);
                }

                // Next, try $FIL keyword
                dupes = filesByName.get(sample.getFilename());
                if (dupes != null)
                {
                    if (perfectMatch == null && dupes.size() == 1)
                        perfectMatch = dupes.iterator().next();
                    candidates.addAll(dupes);
                }

                if (perfectMatch != null || !candidates.isEmpty())
                {
                    resolved.put(sample, Pair.<FlowFCSFile, List<FlowFCSFile>>of(perfectMatch, new ArrayList<FlowFCSFile>(candidates)));
                }
            }

            return resolved;
        }

        private void stepReviewSamples(ImportAnalysisForm form, BindException errors) throws Exception
        {
            WorkspaceData workspaceData = form.getWorkspace();
            Workspace workspace = workspaceData.getWorkspaceObject();

            // Populate resolved samples data for error reshow
            form.getSelectedSamples().setSamples(workspace.getAllSamples());
            form.getSelectedSamples().setKeywords(workspace.getKeywords());

            // Verify resolved FCSFiles
            SelectedSamples resolvedData = form.getSelectedSamples();
            Map<String, SelectedSamples.ResolvedSample> rows = resolvedData.getRows();
            if (rows == null || rows.isEmpty())
            {
                errors.reject(ERROR_MSG, "No selected samples.");
                return;
            }

            // Verify all all selected files have a match.
            Set<String> selectedWithoutMatch = new LinkedHashSet<String>();
            boolean hasSelected = false;
            boolean hasMatched = false;
            for (Map.Entry<String, SelectedSamples.ResolvedSample> entry : rows.entrySet())
            {
                String sampleId = entry.getKey();
                SelectedSamples.ResolvedSample resolvedSample = entry.getValue();
                if (resolvedSample.isSelected())
                {
                    hasSelected = true;
                    if (!resolvedSample.hasMatchedFile())
                        selectedWithoutMatch.add(sampleId);
                }

                if (resolvedSample.hasMatchedFile())
                    hasMatched = true;
            }

            if (!hasSelected)
                errors.reject(ERROR_MSG, "Please select at least one sample to import.");

            if (form.isResolving())
            {
                if (!selectedWithoutMatch.isEmpty())
                    errors.reject(ERROR_MSG, "All selected rows must be matched to a previously imported FCS file.");

                if (!hasMatched)
                    errors.reject(ERROR_MSG, "Please select a previously imported FCS file to associate with the imported samples");
            }

            if (errors.hasErrors())
                return;

            // R Engine can only be used on Mac FlowJo workspaces currently so skip to Analysis Folder step.
            if (workspaceData.getWorkspaceObject() instanceof PCWorkspace)
                form.setWizardStep(ImportAnalysisStep.CHOOSE_ANALYSIS);
            else
                form.setWizardStep(ImportAnalysisStep.ANALYSIS_ENGINE);
        }

        private void stepAnalysisEngine(ImportAnalysisForm form, BindException errors) throws Exception
        {
            WorkspaceData workspaceData = form.getWorkspace();
            List<File> keywordDirs = getKeywordDirs(form, errors);
            if (errors.hasErrors())
                return;

            // Map of workspace sample label -> FlowFCSFile
            Map<String, FlowFCSFile> selectedFCSFiles = getSelectedFCSFiles(form, errors);
            if (errors.hasErrors())
                return;

            AnalysisEngine analysisEngine = getAnalysisEngine(form, errors);
            if (errors.hasErrors())
                return;

            if (analysisEngine.requiresPipeline())
            {
                if (workspaceData.getPath() == null)
                {
                    errors.reject(ERROR_MSG, "Selecting the '" + analysisEngine + "' engine requires using a workspace from the pipeline.");
                    return;
                }

                if (keywordDirs == null && form.getExistingKeywordRunId() == 0 && selectedFCSFiles == null)
                {
                    errors.reject(ERROR_MSG, "You must select FCS Files before selecting the '" + analysisEngine + "' engine.");
                    return;
                }

            }

            // Skip to choose analysis folder step.
            form.setWizardStep(ImportAnalysisStep.CHOOSE_ANALYSIS);

            if (analysisEngine == AnalysisEngine.R)
            {
                // R Engine can only be used on Mac FlowJo workspaces currently.
                if (workspaceData.getWorkspaceObject() instanceof PCWorkspace)
                {
                    errors.reject(ERROR_MSG, "PC/Java FlowJo workspaces can only be analyzed by the LabKey engine.");
                    return;
                }

                // Currently, only the R engine has any analysis options and only if normalization is turned on.
                if (FlowSettings.isNormalizationEnabled())
                    form.setWizardStep(ImportAnalysisStep.ANALYSIS_OPTIONS);
            }
        }

        private void stepAnalysisOptions(ImportAnalysisForm form, BindException errors) throws Exception
        {
            WorkspaceData workspaceData = form.getWorkspace();
            List<File> keywordDirs = getKeywordDirs(form, errors);
            if (errors.hasErrors())
                return;

            AnalysisEngine analysisEngine = getAnalysisEngine(form, errors);
            if (errors.hasErrors())
                return;

            if (AnalysisEngine.R == analysisEngine && (form.isrEngineNormalization() != null && form.isrEngineNormalization().booleanValue()))
            {
                if (!FlowSettings.isNormalizationEnabled())
                {
                    errors.reject(ERROR_MSG, "Normalization must be enabled in the admin console > flow cytometry settings");
                    return;
                }

                if (form.getrEngineNormalizationReference() == null || form.getrEngineNormalizationReference().length() == 0)
                {
                    errors.reject(ERROR_MSG, "You must select a normalization reference sample.");
                    return;
                }

                if (form.getrEngineNormalizationSubsets() == null || form.getrEngineNormalizationSubsets().length() == 0)
                {
                    errors.reject(ERROR_MSG, "You must select at least one subset to normalize.");
                    return;
                }

                if (form.getrEngineNormalizationParameters() == null || form.getrEngineNormalizationParameters().length() == 0)
                {
                    errors.reject(ERROR_MSG, "You must select at least one parameter to normalize.");
                    return;
                }

                Workspace workspace = workspaceData.getWorkspaceObject();
                String[] parameters = workspace.getParameters();
                List<String> params = form.getrEngineNormalizationParameterList();
                for (String param : params)
                {
                    param = StringUtils.trim(param);
                    if (param.startsWith(CompensationMatrix.PREFIX) && param.endsWith(CompensationMatrix.SUFFIX))
                        param = param.substring(1, param.length()-1);

                    int index = ArrayUtils.indexOf(parameters, param);
                    if (index == -1)
                    {
                        errors.reject(ERROR_MSG, "Parameter '" + param + "' does not exist in the workspace");
                        return;
                    }
                }

                // All samples in group should have the same staining panel for normalization to succeed.
                List<Workspace.SampleInfo> sampleInfos = new ArrayList<Workspace.SampleInfo>();
                SelectedSamples selectedSamples = form.getSelectedSamples();
                for (String sampleId : selectedSamples.getRows().keySet())
                {
                    Workspace.SampleInfo sampleInfo = workspace.getSample(sampleId);
                    if (sampleInfo != null)
                        sampleInfos.add(sampleInfo);
                }

                if (sampleInfos.size() <= 1)
                {
                    errors.reject(ERROR_MSG, "More than one sample is needed to perform normalization.  Please select a different group to import.");
                    return;
                }

                Workspace.SampleInfo referenceSample = sampleInfos.get(0);
                Analysis referenceAnalysis = workspace.getSampleAnalysis(referenceSample);
                for (int i = 1; i < sampleInfos.size(); i++)
                {
                    Analysis analysis = workspace.getSampleAnalysis(sampleInfos.get(i));
                    if (!analysis.isSimilar(referenceAnalysis))
                    {
                        String msg = "All samples selected for import must have similar gating to perform normalization. Try selecting a different set of samples to import.";
                        errors.reject(ERROR_MSG, msg);
                        return;
                    }
                }
            }

            form.setWizardStep(ImportAnalysisStep.CHOOSE_ANALYSIS);
        }

        private void stepChooseAnalysis(ImportAnalysisForm form, BindException errors) throws Exception
        {
            List<File> keywordDirs = getKeywordDirs(form, errors);
            if (errors.hasErrors())
                return;

            FlowExperiment experiment;
            if (form.isCreateAnalysis())
            {
                if (StringUtils.isEmpty(form.getNewAnalysisName()))
                {
                    errors.reject(ERROR_MSG, "Missing analysis folder name");
                    return;
                }
                experiment = FlowExperiment.getForName(getUser(), getContainer(), form.getNewAnalysisName());
                if (experiment != null)
                {
                    // just use the existing analysis instead
                    form.setExistingAnalysisId(experiment.getExperimentId());
                    form.setCreateAnalysis(false);
                }
                else
                {
                    form.setExistingAnalysisId(0);
                }
            }
            else
            {
                experiment = FlowExperiment.fromExperimentId(form.getExistingAnalysisId());
                if (experiment == null)
                {
                    errors.reject(ERROR_MSG, "Analysis folder for id '" + form.getExistingAnalysisId() + "' doesn't exist.");
                    return;
                }
                form.setNewAnalysisName(null);
            }

            if (experiment != null)
            {
                if (!experiment.getContainer().equals(getContainer()))
                    throw new IllegalArgumentException("Wrong container");

                if (keywordDirs != null)
                {
                    for (File keywordDir : keywordDirs)
                        if (experiment.hasRun(keywordDir, null))
                        {
                            errors.reject(ERROR_MSG, "The '" + experiment.getName() + "' analysis folder already contains the FCS files from '" + keywordDir + "'.");
                            return;
                        }
                }
            }

            form.setWizardStep(ImportAnalysisStep.CONFIRM);
        }

        private void stepConfirm(ImportAnalysisForm form, BindException errors) throws Exception
        {
            FlowRun keywordRun = getExistingKeywordRun(form, errors);
            if (errors.hasErrors())
                return;

            List<File> keywordDirs = getKeywordDirs(form, errors);
            if (errors.hasErrors())
                return;

            // Only allow importing keyword directories if there doesn't already exist a keyword run.
            assert keywordRun == null || keywordDirs != null;

            // Get the list of selected samples and, if resolving, the resolved FlowFCSFile wells.
            // Map of workspace sample label -> FlowFCSFile (if resolving)
            Map<String, FlowFCSFile> selectedFCSFiles = getSelectedFCSFiles(form, errors);
            if (errors.hasErrors())
                return;

//            // Either we are: not associating any FCSFiles, importing keyword directories, or we have resolved existing FCS files.
//            assert form.getSelectFCSFilesOption() == SelectFCSFileOption.None ||
//                    (form.getSelectFCSFilesOption() == SelectFCSFileOption.Previous && resolvedFCSFiles != null && keywordDirs == null) ||
//                    (form.getSelectFCSFilesOption() == SelectFCSFileOption.Browse && resolvedFCSFiles == null && keywordDirs != null);

            FlowExperiment experiment;
            if (form.isCreateAnalysis())
            {
                if (StringUtils.isEmpty(form.getNewAnalysisName()))
                {
                    errors.reject(ERROR_MSG, "Missing analysis folder name");
                    return;
                }
                experiment = FlowExperiment.createForName(getUser(), getContainer(), form.getNewAnalysisName());
            }
            else
            {
                experiment = FlowExperiment.fromExperimentId(form.getExistingAnalysisId());
                if (experiment == null)
                {
                    errors.reject(ERROR_MSG, "Analysis folder for id '" + form.getExistingAnalysisId() + "' doesn't exist.");
                    return;
                }
            }
            if (!experiment.getContainer().equals(getContainer()))
                throw new IllegalArgumentException("Wrong container");

            WorkspaceData workspaceData = form.getWorkspace();
            File pipelineFile = null;
            ViewBackgroundInfo info = getViewBackgroundInfo();
            if (getPipeRoot() == null)
            {
                // root-less pipeline job for workapce uploaded via the browser
                info.setURL(null);
            }
            else
            {
                if (workspaceData.getPath() != null)
                    pipelineFile = getPipeRoot().resolvePath(workspaceData.getPath());
            }

            // Choose a run path root for the imported analysis based upon the input FCS files.
            File runFilePathRoot = getRunPathRoot(keywordRun, keywordDirs, selectedFCSFiles, pipelineFile, errors);

            AnalysisEngine analysisEngine = getAnalysisEngine(form, errors);
            if (errors.hasErrors())
                return;

            FlowJob job = null;
            if (AnalysisEngine.FlowJoWorkspace == analysisEngine)
            {
                job = new WorkspaceJob(info, getPipeRoot(), experiment,
                        workspaceData, pipelineFile, runFilePathRoot,
                        keywordDirs,
                        selectedFCSFiles,
                        false);
            }
            /*
            else if (AnalysisEngine.LabKey == analysisEngine)
            {
            }
            */
            else if (AnalysisEngine.R == analysisEngine)
            {
                job = new RScriptJob(info, getPipeRoot(), experiment,
                        workspaceData, pipelineFile, runFilePathRoot,
                        keywordDirs,
                        selectedFCSFiles,
                        form.isrEngineNormalization(),
                        form.getrEngineNormalizationReference(),
                        form.getrEngineNormalizationSubsetList(),
                        form.getrEngineNormalizationParameterList(),
                        false);
            }
            else
            {
                errors.reject(ERROR_MSG, "Analysis engine not recognized: " + analysisEngine);
                return;
            }

            throw new RedirectException(executeScript(job));
        }

        public ActionURL getSuccessURL(ImportAnalysisForm form)
        {
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String display = "Import Analysis";
            if (title != null)
                display += ": " + title;
            return root.addChild(display);
        }
    }



    // Called from pipeline import panel
    @RequiresPermissionClass(UpdatePermission.class)
    public class ImportAnalysisResultsAction extends SimpleViewAction<PipelinePathForm>
    {
        @Override
        public ModelAndView getView(PipelinePathForm form, BindException errors) throws Exception
        {
            File pipelineFile = form.getValidatedSingleFile(getContainer());

            PipeRoot root = form.getPipeRoot(getContainer());
            File statisticsFile = findStatisticsFile(errors, pipelineFile, FlowSettings.getWorkingDirectory());
            if (statisticsFile == null && !errors.hasErrors())
                errors.reject(ERROR_MSG, "No statistics file found.");

            if (errors.hasErrors())
                return new SimpleErrorView(errors);

            // UNDONE: set runFilePathRoot to path containing FCS files
            File runFilePathRoot = statisticsFile.getParentFile();
            File analysisPathRoot = statisticsFile.getParentFile();

            // Get an experiment based on the parent folder name
            int suffix = 0;
            String runName;
            if (pipelineFile.getName().equalsIgnoreCase(AnalysisSerializer.STATISTICS_FILENAME))
                runName = FileUtil.getBaseName(runFilePathRoot);
            else
                runName = FileUtil.getBaseName(pipelineFile);
            while (true)
            {
                String experimentName = runName + (suffix == 0 ? "" : suffix);
                FlowExperiment experiment = FlowExperiment.getForName(getUser(), getContainer(), experimentName);
                if (experiment == null)
                    break;

                if (!experiment.hasRun(runFilePathRoot, FlowProtocolStep.analysis))
                    break;

                suffix++;
            }

            FlowExperiment experiment = FlowExperiment.createForName(getUser(), getContainer(), runName + (suffix == 0 ? "" : suffix));

            // UNDONE: resolve FCS files from archive
            Map<String, FlowFCSFile> resolvedFCSFiles = null;

            ViewBackgroundInfo info = getViewBackgroundInfo();
            if (root == null)
            {
                // root-less pipeline job for analysis results uploaded via the browser
                info.setURL(null);
            }

            List<File> keywordDirs = null;
            boolean failOnError = true;

            ImportResultsJob job = new ImportResultsJob(
                    info, root, experiment, AnalysisEngine.Archive,
                    analysisPathRoot, pipelineFile, runFilePathRoot,
                    keywordDirs, resolvedFCSFiles, runName, failOnError);
            throw new RedirectException(executeScript(job));
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Import Analysis");
        }
    }

    public File findStatisticsFile(BindException errors, File pipelineFile, File tempDir) throws Exception
    {
        File statisticsFile = null;

        if (pipelineFile.getName().equalsIgnoreCase(AnalysisSerializer.STATISTICS_FILENAME))
        {
            statisticsFile = pipelineFile;
        }
        else if (pipelineFile.getName().endsWith(".zip"))
        {
            // NOTE: Duplicated code in Main
            java.util.zip.ZipFile zipFile;
            try
            {
                zipFile = new java.util.zip.ZipFile(pipelineFile);
            }
            catch (IOException e)
            {
                errors.reject(ERROR_MSG, "Import failed: Could not read zip file: " + e.getMessage());
                return null;
            }

            String zipBaseName = FileUtil.getBaseName(pipelineFile);
            ZipEntry zipEntry = zipFile.getEntry(AnalysisSerializer.STATISTICS_FILENAME);
            if (zipEntry == null)
                zipEntry = zipFile.getEntry(zipBaseName + "/" + AnalysisSerializer.STATISTICS_FILENAME);

            if (zipEntry == null)
            {
                errors.reject(ERROR_MSG, "Import failed: Couldn't find '" + AnalysisSerializer.STATISTICS_FILENAME + "' or '" + zipBaseName + "/" + AnalysisSerializer.STATISTICS_FILENAME + "' in the zip archive.");
                return null;
            }

            File importDir = File.createTempFile(zipBaseName, null, tempDir);
            if (importDir.exists() && !FileUtil.deleteDir(importDir))
            {
                errors.reject(ERROR_MSG, "Import failed: Could not delete the directory \"" + importDir + "\"");
                return null;
            }

            try
            {
                ZipUtil.unzipToDirectory(pipelineFile, importDir, _log);
                statisticsFile = new File(importDir, zipEntry.getName());
            }
            catch (IOException e)
            {
                errors.reject(ERROR_MSG, "Import failed: Could not extract zip archive: " + e.getMessage());
                return null;
            }
        }

        return statisticsFile;
    }
}
