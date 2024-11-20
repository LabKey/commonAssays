/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SimpleRedirectAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.assay.AssayFileWriter;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.jsp.JspBase;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.study.Study;
import org.labkey.api.study.StudyService;
import org.labkey.api.usageMetrics.SimpleMetricsService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.URIUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.BadRequestException;
import org.labkey.api.view.HttpPostRedirectView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.template.PageConfig;
import org.labkey.flow.FlowModule;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.analysis.model.ExternalAnalysis;
import org.labkey.flow.analysis.model.FCS;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.flow.analysis.model.ISampleInfo;
import org.labkey.flow.analysis.model.IWorkspace;
import org.labkey.flow.analysis.model.SampleIdMap;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.WorkspaceData;
import org.labkey.flow.controllers.executescript.ImportAnalysisForm.SelectFCSFileOption;
import org.labkey.flow.data.FlowCompensationMatrix;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowFCSFile;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.data.FlowWell;
import org.labkey.flow.script.AnalyzeJob;
import org.labkey.flow.script.FlowJob;
import org.labkey.flow.script.ImportResultsJob;
import org.labkey.flow.script.KeywordsJob;
import org.labkey.flow.script.WorkspaceJob;
import org.labkey.flow.util.SampleUtil;
import org.labkey.vfs.FileLike;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.labkey.api.assay.AssayFileWriter.DIR_NAME;

public class AnalysisScriptController extends BaseFlowController
{
    private static final Logger _log = LogHelper.getLogger(AnalysisScriptController.class, "Flow analysis API controller");

    private static final String ACCELERATION_ISSUE_MSG = "Couldn't accelerate the import process, the co-located .fcs files don't match the samples in the selected .wsp file.";

    public enum Action
    {
        chooseRunsToAnalyze,
        chooseAnalysisName,
        analyzeSelectedRuns,
    }

    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(AnalysisScriptController.class);

    public AnalysisScriptController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction<Object>
    {
        FlowScript script;

        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            script = FlowScript.fromURL(getActionURL(), getRequest(), getContainer(), getUser());
            if (script == null)
            {
                return HttpView.redirect(new ActionURL(FlowController.BeginAction.class, getContainer()));
            }
            FlowPreference.showRuns.updateValue(getRequest());
            return new JspView<>("/org/labkey/flow/controllers/executescript/showScript.jsp", script, errors);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addFlowNavTrail(getPageConfig(), root, script, null);
        }
    }

    public abstract class BaseAnalyzeRunsAction extends SimpleViewAction<ChooseRunsToAnalyzeForm>
    {
        FlowScript script;
        Pair<String, Action> nav;

        protected ModelAndView chooseRunsToAnalyze(ChooseRunsToAnalyzeForm form, BindException errors)
        {
            nav = new Pair<>("Choose runs", Action.chooseRunsToAnalyze);
            form.populate(errors);
            return new JspView<>("/org/labkey/flow/controllers/executescript/chooseRunsToAnalyze.jsp", form, errors);
        }

        protected ModelAndView chooseAnalysisName(ChooseRunsToAnalyzeForm form, BindException errors)
        {
            nav = new Pair<>("Choose new analysis name", Action.chooseAnalysisName);
            if (form.getProtocol() == null || form.getProtocolStep() == null)
            {
                throw new BadRequestException("Invalid wizard state");
            }
            return new JspView<>("/org/labkey/flow/controllers/executescript/chooseAnalysisName.jsp", form, errors);
        }

        protected ModelAndView analyzeRuns(ChooseRunsToAnalyzeForm form, int[] runIds, String experimentLSID)
                throws Exception
        {
            nav = new Pair<>(null, Action.analyzeSelectedRuns);
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

        @Override
        public void addNavTrail(NavTree root)
        {
            addFlowNavTrail(getPageConfig(), root, script, nav.first);
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class ChooseRunsToAnalyzeAction extends BaseAnalyzeRunsAction
    {
        FlowScript script;

        @Override
        public ModelAndView getView(ChooseRunsToAnalyzeForm form, BindException errors)
        {
            script = form.getProtocol();
            return chooseRunsToAnalyze(form, errors);
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class AnalyzeSelectedRunsAction extends BaseAnalyzeRunsAction
    {
        @Override
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
            return analyzeRuns(form, runIds, experimentLSID);
        }
    }

    protected void collectNewPaths(ImportRunsForm form, Errors errors)
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

        List<File> files = new ArrayList<>();
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

        Set<File> usedPaths = new HashSet<>();
        for (FlowRun run : FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords))
        {
            // skip FlowJo workspace imported runs
            if (run.getWorkspace() == null)
                usedPaths.add(run.getExperimentRun().getFilePathRoot());
        }

        Map<String, String> ret = new TreeMap<>();
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
                        if (relativeFile.isEmpty() || relativeFile.contains("..") || relativeFile.contains("/") || relativeFile.contains("\\"))
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
            PipeRoot root = Objects.requireNonNull(PipelineService.get().findPipelineRoot(getContainer()));
            root.requiresPermission(getContainer(), getUser(), InsertPermission.class);
        }

        protected ModelAndView confirmRuns(ImportRunsForm form, BindException errors)
        {
            validatePipeline();

            collectNewPaths(form, errors);
            return new JspView<PipelinePathForm>("/org/labkey/flow/controllers/executescript/confirmRunsToImport.jsp", form, errors);
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
                files = Collections.singletonList(pr.resolvePath(form.getPath()));
            else
                files = form.getValidatedFiles(form.getContainer());

            // validate target study
            Container targetStudy = getTargetStudy(form.getTargetStudy(), errors);
            if (errors.hasErrors())
                return confirmRuns(form, errors);

            ViewBackgroundInfo vbi = getViewBackgroundInfo();
            KeywordsJob job = new KeywordsJob(vbi, FlowProtocol.ensureForContainer(getUser(), vbi.getContainer()), files, targetStudy, pr);
            return HttpView.redirect(executeScript(job));
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addFlowNavTrail(getPageConfig(), root, null, "Import Flow FCS Files");
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class ConfirmImportRunsAction extends ImportRunsBaseAction
    {
        @Override
        public ModelAndView getView(ImportRunsForm form, BindException errors)
        {
            return confirmRuns(form, errors);
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class ImportRunsAction extends ImportRunsBaseAction
    {
        @Override
        public ModelAndView getView(ImportRunsForm form, BindException errors) throws Exception
        {
            return uploadRuns(form, errors);
        }
    }

    @RequiresPermission(InsertPermission.class)
    public static class ShowUploadRunsAction extends SimpleRedirectAction<Object>
    {
        @Override
        public URLHelper getRedirectURL(Object o)
        {
            return urlProvider(PipelineUrls.class).urlBrowse(getContainer(), null);
        }
    }

    abstract static public class Page extends JspBase
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

    private Container getTargetStudy(String targetStudyId, Errors errors)
    {
        Container targetStudy = null;
        if (targetStudyId != null && !targetStudyId.isEmpty())
        {
            targetStudy = ContainerManager.getForId(targetStudyId);
            if (targetStudy == null)
            {
                errors.reject(ERROR_MSG, "TargetStudy container '" + targetStudyId + "' doesn't exist.");
                return null;
            }

            if (!targetStudy.hasPermission(getUser(), ReadPermission.class))
            {
                errors.reject(ERROR_MSG, "You don't have read permission to the TargetStudy container '" + targetStudyId + "'.");
                return null;
            }

            Set<Study> studies = StudyService.get().findStudy(targetStudy, getUser());
            if (studies.isEmpty())
            {
                errors.reject(ERROR_MSG, "No study found in TargetStudy container '" + targetStudy.getPath() + "'.");
                return null;
            }

            if (studies.size() > 1)
            {
                errors.reject(ERROR_MSG, "Found more than one study for TargetStudy container '" + targetStudy.getPath() + "'");
                return null;
            }
        }

        return targetStudy;
    }

    // static js steps to take for the Back buttons in the wizard
    public final static String BACK_BUTTON_ACTION = "document.forms['importAnalysis'].elements['goBack'].value = true; return true;";

    public enum ImportAnalysisStep
    {
        SELECT_ANALYSIS("Select Workspace", "/org/labkey/flow/controllers/executescript/importAnalysisSelectAnalysis.jsp"),
        SELECT_FCSFILES("Select FCS Files", "/org/labkey/flow/controllers/executescript/importAnalysisSelectFCSFiles.jsp"),
        REVIEW_SAMPLES("Review Samples", "/org/labkey/flow/controllers/executescript/importAnalysisReviewSamples.jsp"),
        CHOOSE_ANALYSIS("Analysis Folder", "/org/labkey/flow/controllers/executescript/importAnalysisChooseAnalysis.jsp"),
        CONFIRM("Confirm", "/org/labkey/flow/controllers/executescript/importAnalysisConfirm.jsp");

        final String title;
        final String jspPath;

        ImportAnalysisStep(String title, String jspPath)
        {
            this.title = title;
            this.jspPath = jspPath;
        }

        public JspView<ImportAnalysisForm> getJspView(ImportAnalysisForm form)
        {
            return new JspView<>(jspPath, form);
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
    @RequiresPermission(UpdatePermission.class)
    public class ImportAnalysisFromPipelineAction extends SimpleViewAction<PipelinePathForm>
    {
        @Override
        public ModelAndView getView(PipelinePathForm form, BindException errors)
        {
            Path f = form.getValidatedSinglePath(getContainer());
            PipeRoot root = Objects.requireNonNull(PipelineService.get().findPipelineRoot(getContainer()));
            String workspacePath = "/" + root.relativePath(f).replace('\\', '/');

            ActionURL url = new ActionURL(ImportAnalysisAction.class, getContainer());

            List<Pair<String, String>> inputs = new ArrayList<>();
            inputs.add(Pair.of("workspace.path", workspacePath));
            inputs.add(Pair.of("step", String.valueOf(ImportAnalysisStep.SELECT_ANALYSIS.getNumber())));

            getPageConfig().setTemplate(PageConfig.Template.None);
            return new HttpPostRedirectView(url.getLocalURIString(), inputs);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class ImportAnalysisAction extends FormViewAction<ImportAnalysisForm>
    {
        String title;
        PipeRoot root;
        boolean foundRoot;

        @Override
        public void validateCommand(ImportAnalysisForm form, Errors errors)
        {
            getWorkspace(form, errors);
            if (form.isGoBack() && form.getStep() == ImportAnalysisStep.SELECT_ANALYSIS.getNumber())
                errors.reject(ERROR_MSG, "If you wish to cancel the import wizard, please use the Cancel button");

            if (errors.hasErrors())
                form.setWizardStep(ImportAnalysisStep.SELECT_ANALYSIS);
        }

        @Override
        public ModelAndView getView(ImportAnalysisForm form, boolean reshow, BindException errors)
        {
            // When entering the wizard from the pipeline browser "Import Workspace" button,
            // we aren't POST'ing and so haven't parsed or validated the workspace yet.
            if ("GET".equals(getRequest().getMethod()) && form.getWorkspace().getWorkspaceObject() == null && form.getWizardStep() == ImportAnalysisStep.SELECT_FCSFILES)
                validateCommand(form, errors);

            title = form.getWizardStep().getTitle();
            return new JspView<>("/org/labkey/flow/controllers/executescript/importAnalysis.jsp", form, errors);
        }

        @Override
        public boolean handlePost(ImportAnalysisForm form, BindException errors) throws Exception
        {
            if (form.getWizardStep() == null)
            {
                form.setWizardStep(ImportAnalysisStep.SELECT_ANALYSIS);
            }
            else
            {
                // wizard step is the last step shown to the user.
                // Handle the post and set up the form for the next wizard step.
                if (form.isGoBack())
                    handleBack(form, errors);
                else
                    handleNext(form, errors);
            }

            title = form.getWizardStep().getTitle();

            return false;
        }

        private void handleNext(ImportAnalysisForm form, BindException errors) throws Exception
        {
            // wizard step is the last step shown to the user.
            // Handle the post and set up the form for the next wizard step.
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

                case CHOOSE_ANALYSIS:
                    stepChooseAnalysis(form, errors);
                    break;

                case CONFIRM:
                    stepConfirm(form, errors);
                    break;
            }
        }

        private void handleBack(@NotNull ImportAnalysisForm form, BindException errors)
        {
            // wizard step is the last step shown to the user.
            // Handle the post and set up the form for the previous wizard step.
            switch (form.getWizardStep())
            {
                case SELECT_ANALYSIS:
                    stepSelectAnalysis(form, errors);
                    break;

                case SELECT_FCSFILES:
                    form.setWizardStep(ImportAnalysisStep.SELECT_ANALYSIS);
                    break;

                case REVIEW_SAMPLES:
                    // Retain the previous radio button selection
                    SelectFCSFileOption selected = form.getSelectFCSFilesOption();
                    form.setSelectFCSFilesOption(null);
                    stepSelectAnalysis(form, errors);
                    form.setSelectFCSFilesOption(selected);
                    break;

                case CHOOSE_ANALYSIS:
                    stepSelectFCSFiles(form, errors);
                    break;

                case CONFIRM:
                    stepReviewSamples(form, errors);
                    break;
            }
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
                    FileLike path = AssayFileWriter.getUploadDirectoryPath(getContainer(), DIR_NAME);
                    FileLike dir = AssayFileWriter.ensureUploadDirectoryPath(path);
                    FileLike uploadedFile = AssayFileWriter.findUniqueFileName(file.getOriginalFilename(), dir);
                    file.transferTo(uploadedFile.toNioPathForWrite().toFile());

                    String uploadedPath = root.relativePath(uploadedFile.toNioPathForRead().toFile());
                    form.getWorkspace().setPath(uploadedPath);
                }
                catch (Exception e)
                {
                    errors.reject(ERROR_MSG, "Error saving uploaded workspace to pipeline: " + e.getMessage());
                    return;
                }
            }

            workspace.validate(getUser(), getContainer(), errors, getRequest());
        }

        // path may be:
        // - absolute (run path)
        // - a file-browser path (relative to pipe root but starts with '/')
        // - a file-browser path (relative to pipe root and doesn't start with '/')
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
        private File getRunPathRoot(List<File> keywordDirs, SampleIdMap<FlowFCSFile> resolvedFCSFiles)
        {
            if (keywordDirs != null && !keywordDirs.isEmpty())
            {
                // CONSIDER: Use common parent path of all keywordDir paths.  For now, just use first path.
                return keywordDirs.get(0);
            }

            if (resolvedFCSFiles != null && !resolvedFCSFiles.isEmpty())
            {
                // CONSIDER: Use common parent path of all resolvedFCSFile paths.  For now, just use first path.
                for (String id : resolvedFCSFiles.idSet())
                {
                    FlowFCSFile fcsFile = resolvedFCSFiles.getById(id);
                    assert fcsFile != null;
                    if (!fcsFile.isUnmapped())
                    {
                        FlowRun flowRun = fcsFile.getRun();
                        ExpRun expRun = flowRun != null ? flowRun.getExperimentRun() : null;
                        if (expRun != null)
                            return expRun.getFilePathRoot();
                    }
                }
            }

            return null;
        }

        // Get the path to either the previously imported keyword run or
        // to the selected pipeline browser directory under the pipeline root.
        private List<File> getKeywordDirs(ImportAnalysisForm form, Errors errors)
        {
            String path = null;
            if (form.getKeywordDir() != null && form.getKeywordDir().length > 0)
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

        // Returns a map of workspace sample ID -> FlowFCSFile from either
        // the resolved FCSFiles previously imported or from an existing keyword run.
        private SampleIdMap<FlowFCSFile> getSelectedFCSFiles(ImportAnalysisForm form, Errors errors)
        {
            WorkspaceData workspaceData = form.getWorkspace();
            IWorkspace workspace = workspaceData.getWorkspaceObject();
            Map<String, SelectedSamples.ResolvedSample> rows = form.getSelectedSamples().getRows();
            if (rows.isEmpty())
                return null;

            if (form.isResolving() && form.getKeywordDir() != null && form.getKeywordDir().length > 0 && StringUtils.isNotEmpty(form.getKeywordDir()[0]))
            {
                errors.reject(ERROR_MSG, "Can't select directory of FCS files and resolve existing FCS files");
                return null;
            }

            // Get the FlowFCSFile for the resolved samples and remap by sample id.
            SampleIdMap<FlowFCSFile> fcsFiles = new SampleIdMap<>();
            for (Map.Entry<String, SelectedSamples.ResolvedSample> entry : rows.entrySet())
            {
                SelectedSamples.ResolvedSample resolvedSample = entry.getValue();
                if (resolvedSample.isSelected())
                {
                    ISampleInfo sampleInfo = workspace.getSampleById(entry.getKey());
                    if (sampleInfo == null)
                    {
                        errors.reject(ERROR_MSG, "FCS file not found for sample id '" + entry.getKey() + "'");
                        return null;
                    }

                    if (form.isResolving())
                    {
                        FlowFCSFile file = (FlowFCSFile)FlowWell.fromWellId(resolvedSample.getMatchedFile());
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

                        fcsFiles.put(sampleInfo, file);
                    }
                    else
                    {
                        // place maker value into the selected sample map
                        fcsFiles.put(sampleInfo, FlowFCSFile.UNMAPPED);
                    }
                }
            }

            return fcsFiles;
        }

        private AnalysisEngine getAnalysisEngine(@NotNull ImportAnalysisForm form)
        {
            // UNDONE: validate pipeline root is available for rEngine
            if (form.getSelectAnalysisEngine() != null)
                return form.getSelectAnalysisEngine();
            return AnalysisEngine.FlowJoWorkspace;
        }

        private enum RunType {
            Local,
            Existing,
            None
        };

        private RunType hasExistingRuns(File workspaceFile)
        {
            PipeRoot root = getPipeRoot();
            String workspaceDir = root.relativePath(workspaceFile.getParentFile());

            // First, try to find an existing run in the same container as the workspace.
            // Default to selecting a previous run if there are any keyword runs.
            List<FlowRun> allKeywordRuns = FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords);
            RunType rt = RunType.None;
            for (FlowRun keywordRun : allKeywordRuns)
            {
                if (keywordRun.getPath() == null)
                    continue;

                FlowExperiment experiment = keywordRun.getExperiment();
                if (experiment != null && (experiment.isWorkspace() || experiment.isAnalysis()))
                    continue;

                File keywordRunFile = new File(keywordRun.getPath());
                if (keywordRunFile.exists())
                {
                    String keywordRunPath = root.relativePath(keywordRunFile);
                    if (keywordRunPath != null && keywordRun.hasRealWells())
                    {
                        rt = RunType.Existing;  // Order of selection preference Local > Existing > None
                        if (workspaceDir.equals(keywordRunPath) && keywordRun.hasRealWells())
                            // If we already have a run for local fcs files return;
                            return RunType.Local;
                    }
                }
            }

            return rt;
        }


        private void stepSelectAnalysis(ImportAnalysisForm form, BindException errors)
        {
            WorkspaceData workspaceData = form.getWorkspace();
            IWorkspace workspace = workspaceData.getWorkspaceObject();
            List<? extends ISampleInfo> samples = workspace.getSamples();
            if (samples.isEmpty())
            {
                errors.reject(ERROR_MSG, String.format("%s doesn't contain samples", workspace.getKindName()));
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

            RunType rt = RunType.None;
            if (workspaceFile != null && form.getSelectFCSFilesOption() == null && form.getKeywordDir() == null)
            {
                rt = hasExistingRuns(workspaceFile);
                if (rt != RunType.None)
                {
                    // If there are existing and or existing Local runs
                    form.setKeywordRunsExist(true);
                    form.setSelectFCSFilesOption(SelectFCSFileOption.Previous);
                }
                else
                {
                    // Next, guess the FCS files are in the same directory as the workspace, but not analyzed yet.
                    File keywordDir = null;
                    for (ISampleInfo sampleInfo : samples)
                    {
                        File sampleFile = FileUtil.appendName(new File(workspaceFile.getParent()),sampleInfo.getLabel());
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
                            String keywordPath = "./" + StringUtils.join(parts, "/");
                            form.setKeywordDir(new String[] { keywordPath });
                        }
                        form.setSelectFCSFilesOption(SelectFCSFileOption.Browse);
                    }
                }
            }

            // If there are no existing FCS files present
            if (form.getSelectFCSFilesOption() == null)
                form.setSelectFCSFilesOption(SelectFCSFileOption.None);

            // If import folder has a 1:1 fcs file for each sample, then skip ahead to the analysis
            if (!errors.hasErrors() && canAccelerate(form, errors) && !form.isGoBack())
                accelerateWizard(form, errors, rt);
            else
                form.setWizardStep(ImportAnalysisStep.SELECT_FCSFILES);
        }

        private void accelerateWizard(ImportAnalysisForm form, BindException errors, RunType rt)
        {
            // If there are already existing runs, but not one for the local files prefer the local files
            if (rt == RunType.Existing)
                form.setSelectFCSFilesOption(SelectFCSFileOption.None); //Setting to None will allow the selection of local files in next step

            // Set the data folder if we aren't using an existing run
            if (SelectFCSFileOption.None == form.getSelectFCSFilesOption())
                form.setKeywordDir(new String[]{getWorkspaceFolder(form).toString()});

            // Select FCS file (by default we already select all fcs files in working folder)
            stepSelectFCSFiles(form, errors);
            // Map samples to data files (by default we map by names)
            stepReviewSamples(form, errors);
            // advance wizard to choosing the Analysis/Study folders
            form.setWizardStep(ImportAnalysisStep.CHOOSE_ANALYSIS);
        }

        @NotNull
        private Path getWorkspaceFolder(ImportAnalysisForm form)
        {
            return Objects.requireNonNull(getPipeRoot().resolveToNioPath(form.getWorkspace().getPath())).getParent();
        }

        /**
         * For now we will only accelerate the wizard iff there is a 1:1 mapping of workspace samples and data files
         * @param form   wizard form
         * @param errors error collection
         * @return true iff there is a 1:1 mapping of workspace samples and data files
         */
        private boolean canAccelerate(ImportAnalysisForm form, BindException errors)
        {
            List<String> sampleNames = form.getWorkspace().getWorkspaceObject().getSamples().stream().map(ISampleInfo::getSampleName).toList();
            Set<String> fileNames;

            Path dataFolder = getWorkspaceFolder(form);

            try (Stream<Path> files = Files.list(dataFolder))
            {
                fileNames = files
                        .map(Path::toFile)
                        .filter(((IOFileFilter)FCS.FCSFILTER)::accept)
                        .map(File::getName)
                        .collect(Collectors.toSet());
            }
            catch (IOException e)
            {
                String msg = "Could not accelerate flow import, unable to resolve fcs files.";
                errors.reject(ERROR_MSG, msg);
                _log.debug(msg + ". DataFolder: " + dataFolder);
                return false;
            }

            String unmatchedSamples = sampleNames.stream().filter(s -> !fileNames.remove(s)).collect(Collectors.joining(", "));
            String unmatchedFiles = String.join(", ", fileNames);
            if (!PageFlowUtil.empty(unmatchedSamples) || !PageFlowUtil.empty(unmatchedFiles))
            {
                String msg = ACCELERATION_ISSUE_MSG;
                msg += !PageFlowUtil.empty(unmatchedFiles) ? String.format("\nUnmatched wsp samples: %1$s.", unmatchedSamples) : "";
                msg += !PageFlowUtil.empty(unmatchedFiles) ? String.format("\nAdditional fcs file(s) supplied: %1$s.", unmatchedFiles) : "";

                errors.reject(ERROR_MSG, msg);
                _log.debug(msg);
            }

            return !errors.hasErrors();
        }

        private void stepSelectFCSFiles(ImportAnalysisForm form, BindException errors)
        {
            SelectFCSFileOption fcsFilesOption = form.getSelectFCSFilesOption();

            // Disallow other select options if there are FCS files included in the archive.
            if (fcsFilesOption != SelectFCSFileOption.Included && form.getWorkspace().isIncludesFCSFiles())
            {
                errors.reject(ERROR_MSG, "Can't select option other than Included if FCS files are already included");
                return;
            }

            if (fcsFilesOption == SelectFCSFileOption.None)
            {
                // Don't associate FCS files with the workspace.
                WorkspaceData workspaceData = form.getWorkspace();
                IWorkspace workspace = workspaceData.getWorkspaceObject();
                List<? extends ISampleInfo> sampleInfos = workspace.getSamples();
                Map<String, SelectedSamples.ResolvedSample> rows = new HashMap<>();
                for (ISampleInfo sampleInfo : sampleInfos)
                {
                    SelectedSamples.ResolvedSample resolvedSample = new SelectedSamples.ResolvedSample(true, 0, null);
                    rows.put(sampleInfo.getSampleId(), resolvedSample);
                }

                SelectedSamples samples = form.getSelectedSamples();
                samples.setSamples(sampleInfos);
                samples.setKeywords(workspace.getKeywords());
                samples.setRows(rows);

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
                resolveSamples(form, null);

                form.setWizardStep(ImportAnalysisStep.REVIEW_SAMPLES);
            }
            else if (fcsFilesOption == SelectFCSFileOption.Browse)
            {
                WorkspaceData workspaceData = form.getWorkspace();
                List<File> keywordDirs = getKeywordDirs(form, errors);
                if (keywordDirs == null || keywordDirs.isEmpty())
                    errors.reject(ERROR_MSG, "No directory selected");

                if (keywordDirs != null && keywordDirs.size() > 1)
                    errors.reject(ERROR_MSG, "Only a single keyword directory can currently be imported.");

                if (errors.hasErrors())
                    return;

                File keywordDir = keywordDirs.get(0);

                // Translate selected keyword directory into a existing keyword run if possible.
                FlowRun existingKeywordRun = null;
                List<FlowRun> keywordRuns = FlowRun.getRunsForPath(getContainer(), FlowProtocolStep.keywords, keywordDir);
                if (!keywordRuns.isEmpty())
                {
                    for (FlowRun keywordRun : keywordRuns)
                    {
                        FlowExperiment experiment = keywordRun.getExperiment();
                        if (experiment != null && experiment.isKeywords())
                        {
                            existingKeywordRun = keywordRun;
                            form.setKeywordDir(null);
                            break;
                        }
                    }
                }

                if (existingKeywordRun != null)
                {
                    // Resolve workspace samples against the previously imported FCS files.
                    form.setResolving(true);
                    resolveSamples(form, existingKeywordRun);
                }
                else
                {
                    // We don't have an existing keyword run, check that at least one sample in the
                    // selected directory exists and mark those samples as selected for import.
                    boolean found = false;
                    IWorkspace workspace = workspaceData.getWorkspaceObject();
                    List<? extends ISampleInfo> sampleInfos = workspace.getSamples();
                    Map<String, SelectedSamples.ResolvedSample> rows = new HashMap<>();
                    for (ISampleInfo sampleInfo : sampleInfos)
                    {
                        File sampleFile = FileUtil.appendName(keywordDir, sampleInfo.getLabel());
                        boolean exists = sampleFile.exists();
                        if (exists)
                            found = true;
                        SelectedSamples.ResolvedSample resolvedSample = new SelectedSamples.ResolvedSample(exists, 0, null);
                        rows.put(sampleInfo.getSampleId(), resolvedSample);
                    }

                    if (!found)
                    {
                        String msg = String.format("None of the samples used by the %s were found in the selected directory '%s'.", workspace.getKindName(), form.getKeywordDir()[0]);
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

        private void resolveSamples(ImportAnalysisForm form, @Nullable FlowRun keywordRun)
        {
            WorkspaceData workspaceData = form.getWorkspace();
            IWorkspace workspace = workspaceData.getWorkspaceObject();
            List<? extends ISampleInfo> sampleInfos = workspace.getSamples();

            SelectedSamples selectedSamples = form.getSelectedSamples();
            selectedSamples.setSamples(sampleInfos);
            selectedSamples.setKeywords(workspace.getKeywords());

            // If this is the initial visit, resolve the FCS files otherwise use the form POSTed resolved data.
            if (selectedSamples.getRows().isEmpty())
            {
                List<FlowFCSFile> files;
                if (keywordRun != null)
                {
                    files = Arrays.asList(keywordRun.getFCSFiles());
                }
                else
                {
                    files = FlowFCSFile.fromName(getContainer(), null);
                }

                Map<ISampleInfo, Pair<FlowFCSFile, List<FlowFCSFile>>> resolved = SampleUtil.resolveSamples(sampleInfos, files);

                Map<String, SelectedSamples.ResolvedSample> rows = new HashMap<>();
                for (ISampleInfo sample : sampleInfos)
                {
                    SelectedSamples.ResolvedSample resolvedSample = null;
                    Pair<FlowFCSFile, List<FlowFCSFile>> matches = resolved.get(sample);
                    if (matches != null)
                    {
                        FlowFCSFile perfectMatch = matches.first;
                        int perfectMatchId = perfectMatch != null ? perfectMatch.getRowId() : 0;
                        List<FlowFCSFile> candidates = matches.second;

                        if (perfectMatchId != 0 || (candidates != null && !candidates.isEmpty()))
                        {
                            resolvedSample = new SelectedSamples.ResolvedSample(perfectMatchId > 0, perfectMatchId, candidates);
                        }
                    }

                    if (resolvedSample == null)
                        resolvedSample = new SelectedSamples.ResolvedSample(false, 0, null);

                    rows.put(sample.getSampleId(), resolvedSample);
                }
                selectedSamples.setRows(rows);
            }
        }


        private void stepReviewSamples(ImportAnalysisForm form, BindException errors)
        {
            WorkspaceData workspaceData = form.getWorkspace();
            IWorkspace workspace = workspaceData.getWorkspaceObject();

            // Populate resolved samples data for error reshow
            form.getSelectedSamples().setSamples(workspace.getSamples());
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
            Set<String> selectedWithoutMatch = new LinkedHashSet<>();
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

            if (!workspace.hasAnalysis() || workspace instanceof ExternalAnalysis)
            {
                // The current ExternalAnalysis archive format doesn't include any analysis definition
                form.setSelectAnalysisEngine(AnalysisEngine.Archive);
                form.setWizardStep(ImportAnalysisStep.CHOOSE_ANALYSIS);
            }
            else if (workspace instanceof FlowJoWorkspace)
            {
                // Default to using importing results from FlowJo
                form.setSelectAnalysisEngine(AnalysisEngine.FlowJoWorkspace);
                form.setWizardStep(ImportAnalysisStep.CHOOSE_ANALYSIS);
            }
            else
            {
                errors.reject(ERROR_MSG, "Unsupported workspace type: " + workspace.getKindName());
            }
        }

        private void stepChooseAnalysis(ImportAnalysisForm form, BindException errors)
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

            Container targetStudy = getTargetStudy(form.getTargetStudy(), errors);
            if (form.getStudyChanged())
            {
                // Estabilish & increment a metric to measure usage of the Study Linkage feature of the Flow data import process
                SimpleMetricsService.get().increment(FlowModule.NAME, "SampleImport", "StudyLinkChanged");
            }

            if (errors.hasErrors())
                return;

            if (targetStudy != null && (keywordDirs == null || keywordDirs.isEmpty()))
            {
                errors.reject(ERROR_MSG, "Target study can only be selected when also importing a directory of FCS files");
                return;
            }

            form.setWizardStep(ImportAnalysisStep.CONFIRM);
        }

        private void stepConfirm(ImportAnalysisForm form, BindException errors) throws Exception
        {
            List<File> keywordDirs = getKeywordDirs(form, errors);
            if (errors.hasErrors())
                return;

            // Get the list of selected samples.
            // When resolving, the map contains sample ID to the resolved FlowFCSFile well, otherwise FlowFCSFile.NONE
            SampleIdMap<FlowFCSFile> selectedFCSFiles = getSelectedFCSFiles(form, errors);
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
                // root-less pipeline job for workspace uploaded via the browser
                info.setURL(null);
            }
            else
            {
                if (workspaceData.getPath() != null)
                    pipelineFile = getPipeRoot().resolvePath(workspaceData.getPath());
            }

            // Choose a run path root for the imported analysis based upon the input FCS files.
            File runFilePathRoot = getRunPathRoot(keywordDirs, selectedFCSFiles);

            AnalysisEngine analysisEngine = getAnalysisEngine(form);
            if (errors.hasErrors())
                return;

            Container targetStudy = getTargetStudy(form.getTargetStudy(), errors);
            if (errors.hasErrors())
                return;

            if (targetStudy != null && (keywordDirs == null || keywordDirs.isEmpty()))
            {
                errors.reject(ERROR_MSG, "Target study can only be selected when also importing a directory of FCS files");
                return;
            }

            FlowJob job;
            if (analysisEngine == null || AnalysisEngine.FlowJoWorkspace == analysisEngine)
            {
                assert (workspaceData.getWorkspaceObject() instanceof Workspace);
                job = new WorkspaceJob(info, getPipeRoot(), experiment,
                        workspaceData, pipelineFile, runFilePathRoot,
                        keywordDirs,
                        selectedFCSFiles,
                        targetStudy,
                        false);
            }
            else if (AnalysisEngine.Archive == analysisEngine)
            {
                assert (workspaceData.getWorkspaceObject() instanceof ExternalAnalysis);
                File originalFile = pipelineFile;
                if (workspaceData.getOriginalPath() != null)
                    originalFile = root.resolvePath(workspaceData.getOriginalPath());
                job = new ImportResultsJob(info, getPipeRoot(), experiment,
                        AnalysisEngine.Archive, pipelineFile, originalFile,
                        runFilePathRoot,
                        keywordDirs,
                        selectedFCSFiles,
                        workspaceData.getWorkspaceObject().getName(),
                        targetStudy,
                        false);

            }
            else
            {
                errors.reject(ERROR_MSG, "Analysis engine not recognized: " + analysisEngine);
                return;
            }

            workspaceData.clearStashedWorkspace(getRequest());

            throw new RedirectException(executeScript(job));
        }

        @Override
        public ActionURL getSuccessURL(ImportAnalysisForm form)
        {
            return null;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            String display = "Import Analysis";
            if (title != null)
                display += ": " + title;
            root.addChild(display);
        }
    }
}
