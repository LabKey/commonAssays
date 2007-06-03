package org.labkey.flow.controllers.executescript;

import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionError;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.ACL;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.view.*;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.data.*;
import org.labkey.flow.script.AddRunsJob;
import org.labkey.flow.script.AnalyzeJob;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.flow.script.FlowPipelineProvider;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

@Jpf.Controller(messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")})
public class AnalysisScriptController extends BaseFlowController<AnalysisScriptController.Action>
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

        uploadWorkspace,

        showRefreshKeywords,
        refreshKeywords,
    }

    @Jpf.Action
    protected Forward begin() throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        FlowScript script = FlowScript.fromURL(getViewURLHelper(), getRequest());
        ScriptOverview overview = new ScriptOverview(getUser(), getContainer(), script);
        return includeView(new HomeTemplate(getViewContext(), new HtmlView(overview.toString()), getNavTrailConfig(script, null, Action.begin)));
    }

    protected Page getPage(String name) throws Exception
    {
        Page ret = (Page) getFlowPage(name);
        ret.setScript(getScript());
        return ret;
    }

    @Jpf.Action
    protected Forward chooseRunsToAnalyze(ChooseRunsToAnalyzeForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        HttpView view = FormPage.getView(AnalysisScriptController.class, form, "chooseRunsToAnalyze.jsp");
        NavTrailConfig ntc = getNavTrailConfig(form.getProtocol(), "Choose runs", Action.chooseRunsToAnalyze);
        return includeView(new HomeTemplate(getViewContext(), view, ntc));
    }

    @Jpf.Action
    protected Forward analyzeSelectedRuns(ChooseRunsToAnalyzeForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        int[] runIds = form.getSelectedRunIds();
        if (runIds.length == 0)
        {
            addError("Please select at least one run to analyze.");
            return chooseRunsToAnalyze(form);
        }
        String experimentLSID = form.getAnalysisLSID();
        if (experimentLSID == null)
        {
            return chooseAnalysisName(form);
        }
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
        return executeScript(job, analysis);
    }

    protected Forward chooseAnalysisName(ChooseRunsToAnalyzeForm form) throws Exception
    {
        HttpView view = FormPage.getView(AnalysisScriptController.class, form, "chooseAnalysisName.jsp");
        NavTrailConfig ntc = getNavTrailConfig(form.getProtocol(), "Choose new analysis name", Action.chooseRunsToAnalyze);
        HomeTemplate template = new HomeTemplate(getViewContext(), view, ntc);
        template.getModel().setFocus("forms[0].ff_analysisName");
        return includeView(template);
    }


    protected Map<String, String> getNewPaths(ChooseRunsToUploadForm form) throws Exception
    {
        PipelineService service = PipelineService.get();
        PipeRoot root = service.findPipelineRoot(getContainer());
        if (root == null)
        {
            addError("The pipeline root is not set.");
            return Collections.EMPTY_MAP;
        }

        String displayPath;
        if (StringUtils.isEmpty(form.path))
        {
            displayPath = "this directory";
        }
        else
        {
            displayPath = "'" + form.path + "'";
        }
        File directory = StringUtils.isEmpty(form.path) ? root.getRootPath() : new File(root.getRootPath(), form.path);
        if (!root.isUnderRoot(directory))
        {
            addError("The path " + displayPath + " is invalid.");
            return Collections.EMPTY_MAP;
        }

        if (!directory.isDirectory())
        {
            addError(displayPath + " is not a directory.");
            return Collections.EMPTY_MAP;
        }
        List<File> files = new ArrayList();
        files.add(directory);
        File[] dirFiles = directory.listFiles();
        if (dirFiles != null)
        {
            files.addAll(Arrays.asList(dirFiles));
        }

        Set<String> usedPaths = new HashSet();
        for (FlowRun run : FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords))
        {
            usedPaths.add(run.getExperimentRun().getFilePathRoot());
        }

        Map<String, String> ret = new TreeMap();
        boolean anyFCSDirectories = false;
        for (File file : files)
        {
            if (FlowAnalyzer.isFCSDirectory(file))
            {
                anyFCSDirectories = true;
                if (!usedPaths.contains(file.toString()))
                {
                    String relativePath = root.relativePath(file);
                    String displayName;
                    if (file.equals(directory))
                    {
                        displayName = "This Directory";
                    }
                    else
                    {
                        displayName = file.getName();
                    }
                    ret.put(relativePath, displayName);
                }
            }
        }
        if (ret.isEmpty())
        {
            if (anyFCSDirectories)
            {
                addError("All of the directories in " + displayPath + " have already been uploaded.");
            }
            else
            {
                addError("No FCS files were found in " + displayPath + " or its children.");
            }
        }
        return ret;

    }

    @Jpf.Action
    protected Forward chooseRunsToUpload(ChooseRunsToUploadForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
        root.requiresPermission(getContainer(), getUser(), ACL.PERM_INSERT);

        HttpView view = FormPage.getView(AnalysisScriptController.class, form, "chooseRunsToUpload.jsp");

        NavTrailConfig ntc = getNavTrailConfig(null, "Choose Runs To Upload", Action.chooseRunsToUpload);
        form.setNewPaths(getNewPaths(form));


        return includeView(new HomeTemplate(getViewContext(), view, ntc));
    }

    @Jpf.Action
    protected Forward uploadRuns(ChooseRunsToUploadForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
        root.requiresPermission(getContainer(), getUser(), ACL.PERM_INSERT);
        if (form.ff_path == null || form.ff_path.length == 0)
        {
            addError("You did not select any runs.");
            return chooseRunsToUpload(form);
        }
        List<File> paths = new ArrayList();
        List<String> skippedPaths = new ArrayList();
        for (String path : form.ff_path)
        {
            File file = root.resolvePath(path);
            if (file == null)
            {
                skippedPaths.add(path);
                continue;
            }

            paths.add(file);
        }

        ViewBackgroundInfo vbi = getViewBackgroundInfo();
        if (paths.size() > 0)
        {
            vbi = PipelineService.get().getJobBackgroundInfo(vbi, paths.get(0));
        }
        AddRunsJob job = new AddRunsJob(vbi, FlowProtocol.ensureForContainer(getUser(), vbi.getContainer()), paths);
        for (String path : skippedPaths)
        {
            job.addStatus("Skipping path '" + path + "' because it is invalid.");
        }
        return executeScript(job, null);
    }

    @Jpf.Action
    protected Forward showUploadRuns() throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        ViewURLHelper forward = PipelineService.get().urlBrowse(getContainer());
        forward.addParameter("referer", FlowPipelineProvider.NAME);
        return new ViewForward(forward);
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

    protected boolean addError(String error)
    {
        PageFlowUtil.getActionErrors(getRequest(), true).add("main", new ActionError("Error", error));
        return true;
    }

    @Jpf.Action
    protected Forward uploadWorkspace(UploadWorkspaceResultsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        if (isPost())
        {
            try
            {
                Forward forward = doUploadWorkspace(form);
                if (forward != null)
                    return forward;
            }
            catch (Throwable t)
            {
                ExceptionUtil.logExceptionToMothership(getRequest(), t);
                addError(t);
            }
        }
        return renderInTemplate(FormPage.getView(AnalysisScriptController.class, form, "uploadWorkspace.jsp"), getContainer(), getNavTrailConfig(null, "Upload Flow Jo Workspace Analysis Results", Action.uploadWorkspace));
    }


    protected Forward doUploadWorkspace(UploadWorkspaceResultsForm form) throws Exception
    {
        String path = form.getPath();
        PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
        File workspaceFile = root.resolvePath(path);
        FlowJoWorkspace workspace = FlowJoWorkspace.readWorkspace(new FileInputStream(workspaceFile));
        File runFilePathRoot = workspaceFile;
        for (FlowJoWorkspace.SampleInfo sampleInfo : workspace.getSamples())
        {
            File sampleFile = new File(workspaceFile.getParent(), sampleInfo.getLabel());
            if (sampleFile.exists())
            {
                runFilePathRoot = workspaceFile.getParentFile();
                break;
            }
        }
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

        FlowRun[] existing = experiment.findRun(runFilePathRoot, null);
        if (existing.length != 0)
        {
            addError("This analysis already contains this path.");
            return null;
        }

        FlowRun run = workspace.createExperimentRun(getUser(), getContainer(), experiment, workspaceFile, runFilePathRoot);
        return new ViewForward(run.urlShow());
    }
}
