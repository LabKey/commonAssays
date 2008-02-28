package org.labkey.flow.controllers.run;

import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.log4j.Logger;
import org.labkey.flow.data.*;
import org.labkey.flow.view.FlowQueryView;
import org.labkey.api.view.*;
import org.labkey.api.view.template.HomeTemplate;
import org.labkey.api.security.ACL;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.flow.script.MoveRunFromWorkspaceJob;
import org.labkey.flow.analysis.model.FCS;

import javax.servlet.http.HttpServletResponse;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.Map;
import java.util.TreeMap;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.net.URI;

@Jpf.Controller(messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")})
public class RunController extends BaseFlowController<RunController.Action>
{
    static private final Logger _log = Logger.getLogger(RunController.class);
    public enum Action
    {
        begin,
        showRun,
        showCompensation,
        export,
        details,
        download,
        moveToWorkspace,
        moveToAnalysis
    }

    @Jpf.Action
    protected Forward begin() throws Exception
    {
        return new ViewForward(urlFor(FlowController.Action.begin));
    }

    protected Page getPage(String name) throws Exception
    {
        FlowRun run = FlowRun.fromURL(getActionURL());
        if (null == run)
            HttpView.throwNotFound();
        Page ret = (Page) getFlowPage(name);
        ret.setRun(run);
        return ret;
    }

    @Jpf.Action
    protected Forward showRun(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);


        Page page = getPage("showRun.jsp");
        page.__form = form;
        JspView view = new JspView(page);

        return includeView(new HomeTemplate(getViewContext(), view, getNavTrailConfig(form._run, null, Action.showRun)));
    }

    @Jpf.Action
    protected Forward showCompensation(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        NavTrailConfig ntc = getNavTrailConfig(form.getRun(),
                "Compensation for '" + form.getRun().getName() + "'", Action.showCompensation);
        return includeView(new HomeTemplate(getViewContext(),
                FormPage.getView(RunController.class, form, "showCompensation.jsp"), ntc));
    }

    @Jpf.Action
    protected Forward export(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        FlowQueryView view = new FlowQueryView(form);
        return view.exportToExcel(getResponse());
    }


    abstract static public class Page extends FlowPage<RunController>
    {
        public ViewForm __form;
        private FlowRun _run;
        private FlowWell[] _wells = null;
        private FlowCompensationMatrix _comp;

        public void setRun(FlowRun run)
        {
            _run = run;
            _comp = _run.getCompensationMatrix();
        }

        public FlowRun getRun()
        {
            return _run;
        }

        public FlowWell[] getWells()
        {
            if (null == _wells)
                _wells = _run.getWells();
            return _wells;
        }

        public FlowCompensationMatrix getCompensationMatrix()
        {
            return _comp;
        }
    }

    @Jpf.Action
    protected Forward details() throws Exception
    {
        ActionURL forward = PageFlowUtil.urlFor(Action.showRun, getContainer());
        forward.addParameter(FlowParam.runId.toString(), getActionURL().getParameter("Runs.RunId~eq"));
        return new ViewForward(forward);
    }

    @Jpf.Action
    protected Forward download() throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        HttpServletResponse response = getResponse();

        FlowRun run = getRun();
        String strEventCount = getRequest().getParameter("eventCount");
        Map<String, File> files = new TreeMap<String, File>();
        FlowWell[] wells = run.getWells(true);
        if (wells.length == 0)
        {
            response.getWriter().write("Error: no wells in run");
            return null;
        }
        for (FlowWell well : wells)
        {
            URI uri = FlowAnalyzer.getFCSUri(well);
            File file = new File(uri);
            files.put(file.getName(), file);
        }
        response.reset();
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + run.getName() + ".zip");
        ZipOutputStream stream = new ZipOutputStream(response.getOutputStream());
        byte[] buffer = new byte[524288];
        for (File file : files.values())
        {
            ZipEntry entry = new ZipEntry(file.getName());
            stream.putNextEntry(entry);
            InputStream is;
            if (strEventCount == null)
            {
                is = new FileInputStream(file);
            }
            else
            {
                is = new ByteArrayInputStream(new FCS(file).getFCSBytes(file, Integer.valueOf(strEventCount)));
            }
            int cb;
            while((cb = is.read(buffer)) > 0)
            {
                stream.write(buffer, 0, cb);
            }
        }
        stream.close();
        return null;
    }

    @Jpf.Action
    protected Forward moveToWorkspace(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        if (isPost())
        {
            Forward forward = doMoveToWorkspace(form);
            if (forward != null)
                return forward;
        }
        NavTrailConfig ntc = getNavTrailConfig(form.getRun(),
                "Move '" + form.getRun().getName() + "' to the workspace", Action.moveToWorkspace);
        return includeView(new HomeTemplate(getViewContext(),
                FormPage.getView(RunController.class, form, "moveToWorkspace.jsp"), ntc));
    }

    protected Forward doMoveToWorkspace(RunForm form)
    {
        try
        {
            FlowRun run = form.getRun();
            if (run.getStep() != FlowProtocolStep.analysis)
            {
                addError("This run cannot be moved to the workspace because it is not the analysis step.");
                return null;
            }
            FlowExperiment workspace = FlowExperiment.ensureWorkspace(getUser(), getContainer());
            FlowRun[] existing = workspace.findRun(new File(run.getPath()), FlowProtocolStep.analysis);
            if (existing.length != 0)
            {
                addError("This run cannot be moved to the workspace because the workspace already contains a run from this directory.");
                return null;
            }
            run.moveToWorkspace(getUser());
            return new ViewForward(run.urlFor(ScriptController.Action.gateEditor));
        }
        catch (Exception e)
        {
            addError("An exception occurred: " + e);
            _log.error("Error", e);
            return null;
        }
    }

    @Jpf.Action
    protected Forward moveToAnalysis(MoveToAnalysisForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        if (isPost())
        {
            Forward forward = doMoveToAnalysis(form);
            if (forward != null)
            {
                return forward;
            }
        }
        NavTrailConfig ntc = getNavTrailConfig(form.getRun(),
                "Move '" + form.getRun().getName() + "' to an analysis", Action.moveToWorkspace);
        return includeView(new HomeTemplate(getViewContext(),
                FormPage.getView(RunController.class, form, "moveToAnalysis.jsp"), ntc));

    }

    protected Forward doMoveToAnalysis(MoveToAnalysisForm form) throws Exception
    {
        FlowRun run = form.getRun();
        if (!run.isInWorkspace())
        {
            addError("This run is not in the workspace");
            return null;
        }

        FlowExperiment experiment = FlowExperiment.fromExperimentId(form.getExperimentId());
        if (experiment.findRun(new File(run.getPath()), FlowProtocolStep.analysis).length != 0)
        {
            addError("This run cannot be moved to this analysis because there is already a run there.");
            return null;
        }
        MoveRunFromWorkspaceJob job = new MoveRunFromWorkspaceJob(getViewBackgroundInfo(), experiment, run);
        return executeScript(job, run.getScript());
    }
}
