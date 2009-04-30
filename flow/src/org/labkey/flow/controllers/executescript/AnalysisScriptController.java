/*
 * Copyright (c) 2006-2009 LabKey Corporation
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
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.RedirectAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URIUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.*;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.analysis.model.FCS;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.SpringFlowController;
import org.labkey.flow.controllers.WorkspaceData;
import org.labkey.flow.data.*;
import org.labkey.flow.script.AddRunsJob;
import org.labkey.flow.script.AnalyzeJob;
import org.labkey.flow.script.FlowPipelineProvider;
import org.labkey.flow.script.WorkspaceJob;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.net.URI;
import java.util.*;

public class AnalysisScriptController extends SpringFlowController<AnalysisScriptController.Action>
{
    static Logger _log = Logger.getLogger(SpringActionController.class);

    public enum Action
    {
        begin,

        showUploadRuns,
        chooseRunsToUpload,
        uploadRuns,

        chooseRunsToAnalyze,
        chooseAnalysisName,
        analyzeSelectedRuns,
    }

    static SpringActionController.DefaultActionResolver _actionResolver = new SpringActionController.DefaultActionResolver(AnalysisScriptController.class);

    public AnalysisScriptController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleViewAction
    {
        FlowScript script;

        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            script = FlowScript.fromURL(getActionURL(), getRequest());
            if (script == null)
            {
                return HttpView.redirect(getContainer().urlFor(FlowController.Action.begin));
            }
            FlowPreference.showRuns.updateValue(getRequest());
            return new JspView<FlowScript>(AnalysisScriptController.class, "showScript.jsp", script, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(root, script, null, Action.begin);
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
            AnalyzeJob job = new AnalyzeJob(getViewBackgroundInfo(), experimentName, experimentLSID, FlowProtocol.ensureForContainer(getUser(), getContainer()), analysis, form.getProtocolStep(), runIds);
            if (form.getCompensationMatrixId() != 0)
            {
                job.setCompensationMatrix(FlowCompensationMatrix.fromCompId(form.getCompensationMatrixId()));
            }
            job.setCompensationExperimentLSID(form.getCompensationExperimentLSID());
            return HttpView.redirect(executeScript(job));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(root, script, nav.first, nav.second);
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class ChooseRunsToAnalyzeAction extends BaseAnalyzeRunsAction
    {
        FlowScript script;

        public ModelAndView getView(ChooseRunsToAnalyzeForm form, BindException errors) throws Exception
        {
            script = form.getProtocol();
            return chooseRunsToAnalyze(form, errors);
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
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

    protected Map<String, String> getNewPaths(ChooseRunsToUploadForm form, Errors errors) throws Exception
    {
        PipelineService service = PipelineService.get();
        PipeRoot root = service.findPipelineRoot(getContainer());
        if (root == null)
        {
            errors.reject(ERROR_MSG, "The pipeline root is not set.");
            return Collections.EMPTY_MAP;
        }

        String displayPath;
        if (StringUtils.isEmpty(form.path))
        {
            displayPath = "this directory";
        }
        else
        {
            displayPath = "'" + PageFlowUtil.decode(form.path) + "'";
        }

        URI uri = URIUtil.resolve(root.getUri(), form.path);
        if (null == uri)
        {
            errors.reject(ERROR_MSG, "The path " + displayPath + " is invalid.");
            return Collections.EMPTY_MAP;
        }
        File directory = new File(uri);
        if (!root.isUnderRoot(directory))
        {
            errors.reject(ERROR_MSG, "The path " + displayPath + " is invalid.");
            return Collections.EMPTY_MAP;
        }

        if (!directory.isDirectory())
        {
            errors.reject(ERROR_MSG, displayPath + " is not a directory.");
            return Collections.EMPTY_MAP;
        }
        List<File> files = new ArrayList<File>();
        files.add(directory);
        File[] dirFiles = directory.listFiles((java.io.FileFilter)DirectoryFileFilter.INSTANCE);
        if (dirFiles != null)
            files.addAll(Arrays.asList(dirFiles));

        Set<String> usedPaths = new HashSet<String>();
        for (FlowRun run : FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords))
        {
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
                if (!usedPaths.contains(file.toString()))
                {
                    String displayName;
                    if (file.equals(directory))
                    {
                        displayName = "This Directory (" + fcsFiles.length + " fcs files)";
                    }
                    else
                    {
                        displayName = file.getName() + " (" + fcsFiles.length + " fcs files)";
                    }
                    ret.put(URIUtil.relativize(root.getUri(), file.toURI()).toString(), displayName);
                }
            }
        }
        if (ret.isEmpty())
        {
            if (anyFCSDirectories)
            {
                errors.reject(ERROR_MSG, "All of the directories in " + displayPath + " have already been uploaded.");
            }
            else
            {
                errors.reject(ERROR_MSG, "No FCS files were found in " + displayPath + " or its children.");
            }
        }
        return ret;
    }

    public abstract class BaseUploadRunsAction extends SimpleViewAction<ChooseRunsToUploadForm>
    {
        Pair<String, Action> nav;

        protected ModelAndView chooseRunsToUpload(ChooseRunsToUploadForm form, BindException errors) throws Exception
        {
            nav = new Pair<String, Action>("Choose Runs to Upload", Action.chooseRunsToUpload);
            PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
            root.requiresPermission(getContainer(), getUser(), ACL.PERM_INSERT);

            JspView<ChooseRunsToUploadForm> view = new JspView<ChooseRunsToUploadForm>(AnalysisScriptController.class, "chooseRunsToUpload.jsp", form, errors);
            form.setNewPaths(getNewPaths(form, errors));
            form.setPipeRoot(root);
            return view;
        }

        protected ModelAndView uploadRuns(ChooseRunsToUploadForm form, BindException errors) throws Exception
        {
            nav = new Pair<String, Action>(null, Action.uploadRuns);
            PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
            root.requiresPermission(getContainer(), getUser(), ACL.PERM_INSERT);
            if (form.ff_path == null || form.ff_path.length == 0)
            {
                errors.reject(ERROR_MSG, "You did not select any runs.");
                return chooseRunsToUpload(form, errors);
            }
            List<File> paths = new ArrayList<File>();
            List<String> skippedPaths = new ArrayList<String>();
            for (String path : form.ff_path)
            {
                File file;
                if (path == null)
                {
                    file = root.getRootPath();
                }
                else
                {
                    file = new File(URIUtil.resolve(root.getUri(), path));
                }
                if (file == null)
                {
                    skippedPaths.add(path);
                    continue;
                }

                paths.add(file);
            }

            ViewBackgroundInfo vbi = getViewBackgroundInfo();
            AddRunsJob job = new AddRunsJob(vbi, FlowProtocol.ensureForContainer(getUser(), vbi.getContainer()), paths);
            for (String path : skippedPaths)
            {
                job.addStatus("Skipping path '" + path + "' because it is invalid.");
            }
            return HttpView.redirect(executeScript(job));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(root, null, nav.first, nav.second);
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class ChooseRunsToUploadAction extends BaseUploadRunsAction
    {
        public ModelAndView getView(ChooseRunsToUploadForm form, BindException errors) throws Exception
        {
            return chooseRunsToUpload(form, errors);
        }
    }


    @RequiresPermission(ACL.PERM_INSERT)
    public class UploadRunsAction extends BaseUploadRunsAction
    {
        public ModelAndView getView(ChooseRunsToUploadForm form, BindException errors) throws Exception
        {
            return uploadRuns(form, errors);
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
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
        INIT("Start"),
        UPLOAD_WORKSPACE("Upload Workspace"),
        ASSOCIATE_FCSFILES("Associate FCS Files"),
        CHOOSE_ANALYSIS("Choose Analysis Folder"),
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
            return ordinal();
        }

        public static ImportAnalysisStep fromNumber(int number)
        {
            for (ImportAnalysisStep step : values())
                if (step.ordinal() == number)
                    return step;
            return INIT;
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
    public class ImportAnalysisAction extends FormViewAction<ImportAnalysisForm>
    {
        String title;
        PipeRoot root;
        boolean foundRoot;

        public void validateCommand(ImportAnalysisForm form, Errors errors)
        {
            if (form.getWizardStep().getNumber() > ImportAnalysisStep.INIT.getNumber())
            {
                getWorkspace(form, errors);
                if (errors.hasErrors())
                    form.setWizardStep(ImportAnalysisStep.UPLOAD_WORKSPACE);
            }
        }

        public ModelAndView getView(ImportAnalysisForm form, boolean reshow, BindException errors) throws Exception
        {
            return new JspView<ImportAnalysisForm>(AnalysisScriptController.class, "importAnalysis.jsp", form, errors);
        }

        public boolean handlePost(ImportAnalysisForm form, BindException errors) throws Exception
        {
            if (form.getWizardStep() == ImportAnalysisStep.INIT)
            {
                form.setWizardStep(ImportAnalysisStep.UPLOAD_WORKSPACE);
            }
            else
            {
                // wizard step is the last step shown to the user.
                // Handle the post and setup the form for the next wizard step.
                switch (form.getWizardStep())
                {
                    case UPLOAD_WORKSPACE:
                        stepUploadWorkspace(form, errors);
                        break;

                    case ASSOCIATE_FCSFILES:
                        stepAssociateFCSFiles(form, errors);
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

        // reads uploaded workspace.file or workspace.path from pipeline
        private void getWorkspace(ImportAnalysisForm form, Errors errors)
        {
            WorkspaceData workspace = form.getWorkspace();
            Map<String, MultipartFile> files = getFileMap();
            MultipartFile file = files.get("workspace.file");
            if (file != null && StringUtils.isNotEmpty(file.getOriginalFilename()))
                form.getWorkspace().setFile(file);
            workspace.validate(getContainer(), errors, getRequest());
        }

        private FlowRun getExistingKeywordRun(ImportAnalysisForm form, Errors errors)
        {
            int keywordRunId = form.getExistingKeywordRunId();
            if (keywordRunId > 0 && form.getRunFilePathRoot() != null)
            {
                errors.reject(ERROR_MSG, "Can't select both an existing run and a file path.");
                return null;
            }

            if (keywordRunId > 0)
                return FlowRun.fromRunId(keywordRunId);
            return null;
        }

        // get the path to either the previously imported keyword run or
        // to the selected directory under the pipeline root.
        private File getRunPathRoot(ImportAnalysisForm form, Errors errors) throws Exception
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
            else if (form.getRunFilePathRoot() != null)
            {
                path = PageFlowUtil.decode(form.getRunFilePathRoot());
            }

            if (path != null)
            {
                PipeRoot root = getPipeRoot();
                File runFilePathRoot = root.resolvePath(path);

                if (runFilePathRoot == null)
                {
                    errors.reject(ERROR_MSG, "The directory containing FCS files wasn't found.");
                    return null;
                }
                if (!runFilePathRoot.isDirectory())
                {
                    errors.reject(ERROR_MSG, "The path specified must be a directory containing FCS files.");
                    return null;
                }
                return runFilePathRoot;
            }
            return null;
        }

        private void stepUploadWorkspace(ImportAnalysisForm form, BindException errors) throws Exception
        {
            WorkspaceData workspaceData = form.getWorkspace();
            FlowJoWorkspace workspace = workspaceData.getWorkspaceObject();
            List<FlowJoWorkspace.SampleInfo> samples = workspace.getSamples();
            if (samples.size() == 0)
            {
                errors.reject(ERROR_MSG, "The workspace doesn't have any samples");
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

            // first, try to find an existing run in the same directory as the workspace
            if (form.getExistingKeywordRunId() == 0)
            {
                FlowRun[] keywordRuns = FlowRun.getRunsForPath(getContainer(), FlowProtocolStep.keywords, workspaceFile.getParentFile());
                if (keywordRuns != null && keywordRuns.length > 0)
                {
                    for (FlowRun keywordRun : keywordRuns)
                    {
                        if (keywordRun.getExperiment().isKeywords())
                        {
                            form.setExistingKeywordRunId(keywordRun.getRunId());
                            break;
                        }
                    }
                }
            }

            // otherwise, guess the FCS files are in the same directory as the workspace
            if (form.getExistingKeywordRunId() == 0 && form.getRunFilePathRoot() == null)
            {
                if (workspaceFile != null)
                {
                    File runFilePathRoot = null;
                    for (FlowJoWorkspace.SampleInfo sampleInfo : samples)
                    {
                        File sampleFile = new File(workspaceFile.getParent(), sampleInfo.getLabel());
                        if (sampleFile.exists())
                        {
                            runFilePathRoot = workspaceFile.getParentFile();
                            break;
                        }
                    }

                    if (runFilePathRoot != null)
                    {
                        String relPath = root.relativePath(runFilePathRoot);
                        if (relPath != null)
                        {
                            String[] parts = StringUtils.split(relPath, File.separatorChar);
                            form.setRunFilePathRoot(StringUtils.join(parts, "/"));
                        }
                    }
                }
            }
            form.setWizardStep(ImportAnalysisStep.ASSOCIATE_FCSFILES);
        }

        private void stepAssociateFCSFiles(ImportAnalysisForm form, BindException errors) throws Exception
        {
            File runFilePathRoot = getRunPathRoot(form, errors);
            if (errors.hasErrors())
                return;

            if (runFilePathRoot != null)
            {
                if (form.getExistingKeywordRunId() == 0)
                {
                    // try to use an existing run for the path if possible
                    FlowRun[] keywordRuns = FlowRun.getRunsForPath(getContainer(), FlowProtocolStep.keywords, runFilePathRoot);
                    if (keywordRuns != null && keywordRuns.length > 0)
                    {
                        for (FlowRun keywordRun : keywordRuns)
                        {
                            if (keywordRun.getExperiment().isKeywords())
                            {
                                form.setExistingKeywordRunId(keywordRun.getRunId());
                                form.setRunFilePathRoot(null);
                                break;
                            }
                        }
                    }
                }

                boolean found = false;
                WorkspaceData workspaceData = form.getWorkspace();
                FlowJoWorkspace workspace = workspaceData.getWorkspaceObject();
                List<FlowJoWorkspace.SampleInfo> samples = workspace.getSamples();
                for (FlowJoWorkspace.SampleInfo sampleInfo : samples)
                {
                    File sampleFile = new File(runFilePathRoot, sampleInfo.getLabel());
                    if (sampleFile.exists())
                    {
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    String msg = "None of the samples used by the workspace were found in the selected directory '" + form.getRunFilePathRoot() + "'.";
                    errors.reject(ERROR_MSG, msg);
                    return;
                }
            }
            form.setWizardStep(ImportAnalysisStep.CHOOSE_ANALYSIS);
        }

        private void stepChooseAnalysis(ImportAnalysisForm form, BindException errors) throws Exception
        {
            File runFilePathRoot = getRunPathRoot(form, errors);
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

                if (runFilePathRoot != null && experiment.hasRun(runFilePathRoot, null))
                {
                    errors.reject(ERROR_MSG, "The '" + experiment.getName() + "' analysis folder already contains the FCS files from '" + runFilePathRoot + "'.");
                    return;
                }
            }

            form.setWizardStep(ImportAnalysisStep.CONFIRM);
        }

        private void stepConfirm(ImportAnalysisForm form, BindException errors) throws Exception
        {
            FlowRun keywordRun = getExistingKeywordRun(form, errors);
            if (errors.hasErrors())
                return;

            File runFilePathRoot = getRunPathRoot(form, errors);
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
            ViewBackgroundInfo info = getViewBackgroundInfo();
            if (root == null)
            {
                // root-less pipeline job for workapce uploaded via the browser
                info.setUrlHelper(null);
            }

            boolean createKeywordRun = keywordRun == null && runFilePathRoot != null;
            WorkspaceJob job = new WorkspaceJob(info, experiment,
                    workspaceData, runFilePathRoot, createKeywordRun, false);
            HttpView.throwRedirect(executeScript(job));
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

}
