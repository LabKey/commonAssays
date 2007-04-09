package org.labkey.flow.controllers.run;

import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.log4j.Logger;
import org.labkey.flow.data.*;
import org.labkey.flow.view.FlowQueryView;
import org.labkey.flow.util.PFUtil;
import org.labkey.api.view.*;
import org.labkey.api.security.ACL;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.script.FlowAnalyzer;

import javax.servlet.http.HttpServletResponse;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.Map;
import java.util.TreeMap;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URI;

@Jpf.Controller
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
    }

    @Jpf.Action
    protected Forward begin() throws Exception
    {
        return new ViewForward(urlFor(FlowController.Action.begin));
    }

    protected Page getPage(String name) throws Exception
    {
        Page ret = (Page) getFlowPage(name);
        ret.setRun(FlowRun.fromURL(getViewURLHelper()));
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
        private FlowWell[] _wells;
        private FlowCompensationMatrix _comp;

        public void setRun(FlowRun run) throws Exception
        {
            _run = run;
            _wells = _run.getWells();
            _comp = _run.getCompensationMatrix();
        }

        public FlowRun getRun()
        {
            return _run;
        }

        public FlowWell[] getWells()
        {
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
        ViewURLHelper forward = PFUtil.urlFor(Action.showRun, getContainer());
        forward.addParameter(FlowParam.runId.toString(), getViewURLHelper().getParameter("Runs.RunId~eq"));
        return new ViewForward(forward);
    }

    @Jpf.Action
    protected Forward download() throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        HttpServletResponse response = getResponse();

        FlowRun run = getRun();
        Map<String, File> files = new TreeMap();
        FlowWell[] wells = run.getWells();
        if (wells.length == 0)
        {
            response.getWriter().write("Error: no wells in run");
            return null;
        }
        for (FlowWell well : run.getWells())
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
            InputStream is = new FileInputStream(file);
            int cb;
            while((cb = is.read(buffer)) > 0)
            {
                stream.write(buffer, 0, cb);
            }
        }
        stream.close();
        return null;
    }
}
