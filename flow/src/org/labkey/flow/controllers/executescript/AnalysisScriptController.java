package org.labkey.flow.controllers.executescript;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.StringUtils;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.RedirectAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.browse.DefaultBrowseView;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.*;
import org.labkey.common.util.Pair;
import org.labkey.flow.FlowSettings;
import org.labkey.flow.analysis.model.FCS;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.SpringFlowController;
import org.labkey.flow.controllers.WorkspaceData;
import org.labkey.flow.data.*;
import org.labkey.flow.script.AddRunsJob;
import org.labkey.flow.script.AnalyzeJob;
import org.labkey.flow.script.FlowPipelineProvider;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.net.URI;
import java.util.*;

public class AnalysisScriptController extends SpringFlowController<AnalysisScriptController.Action>
{
    public enum Action
    {
        begin,

        showUploadRuns,
        chooseRunsToUpload,
        uploadRuns,

        chooseRunsToAnalyze,
        chooseAnalysisName,
        analyzeSelectedRuns,

        showUploadWorkspace,
        chooseAnalysis,
        uploadWorkspaceChooseAnalysis,
        uploadWorkspaceBrowse,
        browseForWorkspace,
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
            ScriptOverview overview = new ScriptOverview(getUser(), getContainer(), script);
            return new HtmlView(overview.toString());
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
            return HttpView.redirect(executeScript(job, analysis));
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
            return HttpView.redirect(executeScript(job, null));
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
    protected Class<Action> getActionClass()
    {
        return Action.class;
    }

    public abstract class BaseUploadWorkspaceAction extends FormViewAction<UploadWorkspaceResultsForm>
    {
        Pair<String, Action> nav;
        ActionURL successURL;

        public void validateCommand(UploadWorkspaceResultsForm form, Errors errors)
        {
            WorkspaceData workspace = form.getWorkspace();
            Map<String, MultipartFile> files = getFileMap();
            MultipartFile file = files.get("workspace.file");
            if (file != null)
                form.getWorkspace().setFile(file);
            workspace.validate(getContainer(), errors, getRequest());
        }

        public ActionURL getSuccessURL(UploadWorkspaceResultsForm form)
        {
            return successURL;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(root, null, nav.first, nav.second);
        }

        protected boolean uploadWorkspaceChooseAnalysis(UploadWorkspaceResultsForm form, BindException errors)
        {
            try
            {
                successURL = doUploadWorkspace(form, errors);
                if (successURL != null)
                    return true;
            }
            catch (Throwable t)
            {
                ExceptionUtil.logExceptionToMothership(getRequest(), t);
                errors.reject(ERROR_MSG, t.getMessage());
            }
            return false;
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
    public class UploadWorkspaceChooseAnalysisAction extends BaseUploadWorkspaceAction
    {
        public ModelAndView getView(UploadWorkspaceResultsForm form, boolean reshow, BindException errors) throws Exception
        {
            nav = new Pair<String, Action>("Upload FlowJo Workspace Analysis Results", Action.uploadWorkspaceChooseAnalysis);
            return new JspView<UploadWorkspaceResultsForm>(AnalysisScriptController.class, "uploadWorkspaceChooseAnalysis.jsp", form, errors);
        }

        public boolean handlePost(UploadWorkspaceResultsForm form, BindException errors) throws Exception
        {
            if (form.ff_confirm)
                return uploadWorkspaceChooseAnalysis(form, errors);
            return false;
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
    public class ShowUploadWorkspaceAction extends BaseUploadWorkspaceAction
    {
        public ModelAndView getView(UploadWorkspaceResultsForm form, boolean reshow, BindException errors) throws Exception
        {
            nav = new Pair<String, Action>("Upload FlowJo Results", Action.showUploadWorkspace);
            return new JspView<UploadWorkspaceResultsForm>(AnalysisScriptController.class, "showUploadWorkspace.jsp", form, errors);
        }

        public boolean handlePost(UploadWorkspaceResultsForm form, BindException errors) throws Exception
        {
            return uploadWorkspaceChooseAnalysis(form, errors);
        }
    }

    protected ActionURL doUploadWorkspace(UploadWorkspaceResultsForm form, BindException errors) throws Exception
    {
        WorkspaceData workspaceData = form.getWorkspace();

        String path = workspaceData.getPath();
        File workspaceFile = null;
        if (path != null)
        {
            PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
            workspaceFile = root.resolvePath(path);
        }
        FlowJoWorkspace workspace = workspaceData.getWorkspaceObject();
        File runFilePathRoot = null;
        FlowExperiment experiment;
        if (StringUtils.isEmpty(form.ff_newAnalysisName))
        {
            experiment = FlowExperiment.fromExperimentId(form.ff_existingAnalysisId);
        }
        else
        {
            experiment = FlowExperiment.createForName(getUser(), getContainer(), form.ff_newAnalysisName);
        }
        if (!experiment.getContainer().equals(getContainer()))
        {
            throw new IllegalArgumentException("Wrong container");
        }

        if (workspaceFile != null)
        {
            for (FlowJoWorkspace.SampleInfo sampleInfo : workspace.getSamples())
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
                FlowRun[] existing = experiment.findRun(runFilePathRoot, null);
                if (existing.length != 0)
                {
                    errors.reject(ERROR_MSG, "This analysis folder already contains this path.");
                    return null;
                }
            }
        }
        else
        {
            workspaceFile = new File(FlowSettings.getWorkingDirectory(), form.getWorkspace().getName());
        }

        FlowRun run = workspace.createExperimentRun(getUser(), getContainer(), experiment, workspaceFile, runFilePathRoot);
        return run.urlShow();
    }

    @RequiresPermission(ACL.PERM_UPDATE)
    public class BrowseForWorkspaceAction extends SimpleViewAction<BrowsePipelineForm>
    {
        public ModelAndView getView(BrowsePipelineForm form, BindException errors) throws Exception
        {
            return new DefaultBrowseView<BrowsePipelineForm>(form);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Browse for Workspace");
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
    public class UploadWorkspaceBrowseAction extends BrowseForWorkspaceAction
    {
        public ModelAndView getView(BrowsePipelineForm form, BindException errors) throws Exception
        {
            String[] files = form.getFile();
            if (files.length == 0)
            {
                errors.reject(ERROR_MSG, "You must select at least one file.");
                return super.getView(form, errors);
            }
            WorkspaceData wsData = new WorkspaceData();
            wsData.setPath(files[0]);
            wsData.validate(getContainer(), errors, getRequest());
            if (errors.hasErrors())
            {
                return super.getView(form, errors);
            }
            ActionURL url = getContainer().urlFor(Action.uploadWorkspaceChooseAnalysis);
            url.addParameter("workspace.path", files[0]);
            return HttpView.redirect(url);
        }
    }

}
