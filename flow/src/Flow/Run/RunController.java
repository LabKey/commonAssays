package Flow.Run;

import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.beehive.netui.pageflow.Forward;
import org.fhcrc.cpas.flow.data.*;
import org.fhcrc.cpas.flow.view.FlowQueryView;
import org.fhcrc.cpas.flow.util.PFUtil;
import org.fhcrc.cpas.view.*;
import org.fhcrc.cpas.security.ACL;
import org.fhcrc.cpas.jsp.FormPage;
import Flow.BaseFlowController;
import Flow.FlowController;
import Flow.FlowParam;

@Jpf.Controller
public class RunController extends BaseFlowController<RunController.Action>
{
    public enum Action
    {
        begin,
        showRun,
        showCompensation,
        export,
        details,
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
}
