package org.labkey.flow.controllers.executescript;

import org.labkey.flow.controllers.BaseFlowController;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.struts.action.ActionError;
import org.apache.commons.lang.StringUtils;
import org.labkey.flow.data.*;
import org.labkey.flow.script.*;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.ACL;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.*;

import java.io.File;
import java.net.URI;
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

    protected List<URI> getNewPaths(ChooseRunsToUploadForm form) throws Exception
    {
        PipelineService service = PipelineService.get();
        URI root = service.getPipelineRoot(getContainer());
        if (root == null)
        {
            addError("The pipeline root is not set.");
            return Collections.EMPTY_LIST;
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
        URI pathRoot = URIUtil.resolve(root, form.path);
        if (pathRoot == null)
        {
            addError("The path " + displayPath + " is invalid.");
            return Collections.EMPTY_LIST;
        }

        File fileRoot = new File(URIUtil.resolve(root, form.path));
        if (!fileRoot.isDirectory())
        {
            addError(displayPath + " is not a directory.");
        }
        List<File> files = new ArrayList();
        files.add(fileRoot);
        files.addAll(Arrays.asList(fileRoot.listFiles()));

        Set<String> usedPaths = new HashSet();
        for (FlowRun run : FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords))
        {
            usedPaths.add(run.getExperimentRun().getFilePathRoot());
        }

        List<URI> ret = new ArrayList();
        boolean anyFCSDirectories = false;
        for (File file : files)
        {
            if (FlowAnalyzer.isFCSDirectory(file))
            {
                anyFCSDirectories = true;
                if (!usedPaths.contains(file.toString()))
                {
                    ret.add(URIUtil.relativize(root, file.toURI()));
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

        HttpView view = FormPage.getView(AnalysisScriptController.class, form, "chooseRunsToUpload.jsp");

        NavTrailConfig ntc = getNavTrailConfig(null, "Choose Runs To Upload", Action.chooseRunsToUpload);
        form.setNewPaths(getNewPaths(form));


        return includeView(new HomeTemplate(getViewContext(), view, ntc));
    }

    @Jpf.Action
    protected Forward uploadRuns(ChooseRunsToUploadForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        if (form.ff_path == null || form.ff_path.length == 0)
        {
            addError("You did not select any runs.");
            return chooseRunsToUpload(form);
        }
        FlowScript analysisScript = null;
        if (form.ff_protocolId != 0)
        {
            analysisScript = FlowScript.fromScriptId(form.ff_protocolId);
        }
        else
        {
            List<FlowScript> protocols = FlowScript.getProtocolsWithStep(getContainer(), FlowProtocolStep.keywords);
            if (protocols.size() == 0)
            {
                analysisScript = null;
            }
            else if (protocols.size() == 1)
            {
                analysisScript = protocols.get(0);
            }
        }

        PipelineService service = PipelineService.get();
        List<File> paths = new ArrayList();
        URI root = service.getPipelineRoot(getContainer());
        for (String path : form.ff_path)
        {
            paths.add(new File(URIUtil.resolve(root, path)));
        }

        AddRunsJob job = new AddRunsJob(getViewBackgroundInfo(), FlowProtocol.ensureForContainer(getUser(), getContainer()), analysisScript, paths);
        return executeScript(job, analysisScript);
    }

    @Jpf.Action
    protected Forward showUploadRuns() throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        ViewURLHelper forward = PipelineService.get().getViewUrlHelper(getViewURLHelper(), FlowPipelineProvider.NAME, "upload", null);
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


}
