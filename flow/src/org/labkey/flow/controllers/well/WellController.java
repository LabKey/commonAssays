package org.labkey.flow.controllers.well;

import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.struts.action.ActionError;
import org.apache.log4j.Logger;
import org.labkey.flow.data.*;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.view.*;
import org.labkey.api.view.template.HomeTemplate;
import org.labkey.api.security.ACL;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URIUtil;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;

import java.util.*;
import java.io.FileNotFoundException;

import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.web.FCSViewer;
import org.labkey.flow.analysis.web.StatisticSpec;

@Jpf.Controller(messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")})
public class WellController extends BaseFlowController
{
    static private final Logger _log = Logger.getLogger(WellController.class);
    public enum Action
    {
        begin,
        showWell,
        editWell,
        chooseGraph,
        showGraph,
        generateGraph,
        showFCS,
    }

    @Jpf.Action
    protected Forward begin() throws Exception
    {
        return new ViewForward(urlFor(FlowController.Action.begin));
    }

    public FlowWell getWell() throws Exception
    {
        return FlowWell.fromURL(getViewURLHelper(), getRequest());
    }

    public Page getPage(String name) throws Exception
    {
        Page ret = (Page) getFlowPage(name);
        ret.setWell(getWell());
        return ret;
    }

    @Jpf.Action
    protected Forward showWell() throws Exception
    {
        requiresPermission(ACL.PERM_READ);


        Page page = getPage("showWell.jsp");
        NavTrailConfig ntc = getNavTrailConfig(page.getWell(), null, Action.showWell);

        return includeView(new HomeTemplate(getViewContext(), new JspView(page), ntc));
    }

    @Jpf.Action
    protected Forward editWell(EditWellForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);

        form.setWell(getWell());
        if (isPost())
        {
            Forward forward = updateWell(form);
            if (forward != null)
                return forward;
        }

        NavTrailConfig ntc = getNavTrailConfig(form.getWell(), "Edit " + form.getWell().getLabel(), Action.editWell);
        return includeView(new HomeTemplate(getViewContext(), FormPage.getView(WellController.class, form, "editWell.jsp"), ntc));
    }

    protected boolean addError(String error)
    {
        PageFlowUtil.getActionErrors(getRequest(), true).add("main", new ActionError("Error", error));
        return true;
    }

    protected Forward updateWell(EditWellForm form) throws Exception
    {
        FlowWell well = form.getWell();
        boolean anyErrors = false;
        if (StringUtils.isEmpty(form.ff_name))
        {
            anyErrors = addError("Name cannot be blank");
        }
        if (form.ff_keywordName != null)
        {
            Set<String> keywords = new HashSet();
            for (int i = 0; i < form.ff_keywordName.length; i ++)
            {
                String name = form.ff_keywordName[i];
                String value = form.ff_keywordValue[i];
                if (StringUtils.isEmpty(name))
                {
                    if (!StringUtils.isEmpty(value))
                    {
                        anyErrors = addError("Missing name for value '" + value + "'");
                    }
                }
                else if (!keywords.add(name))
                {
                    anyErrors = addError("There is already a keyword '" + name + "'");
                    break;
                }
            }
        }
        if (anyErrors)
            return null;
        well.setName(getUser(), form.ff_name);
        well.getExpObject().setComment(getUser(), form.ff_comment);
        if (form.ff_keywordName != null)
        {
            for (int i = 0; i < form.ff_keywordName.length; i ++)
            {
                String name = form.ff_keywordName[i];
                if (StringUtils.isEmpty(name))
                    continue;
                well.setKeyword(getUser(), name, form.ff_keywordValue[i]);
            }
        }
        return new ViewForward(well.urlFor(Action.showWell));
    }

    @Jpf.Action
    protected Forward chooseGraph(ChooseGraphForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        FormPage page = FormPage.get(WellController.class, form, "chooseGraph.jsp");
        NavTrailConfig ntc = getNavTrailConfig(form.getWell(), "Choose Graph", Action.chooseGraph);
        return includeView(new HomeTemplate(getViewContext(), new JspView(page), ntc));
    }

    @Jpf.Action
    protected Forward showGraph() throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        FlowWell well = getWell();
        if (well == null)
        {
            int objectId = getIntParam(FlowParam.objectId);
            if (objectId == 0)
                return null;
            FlowObject obj = FlowDataObject.fromAttrObjectId(objectId);
            if (!(obj instanceof FlowWell))
                return null;
            well = (FlowWell) obj;
            well.checkContainer(getViewURLHelper());
        }
        String graph = getParam(FlowParam.graph);
        byte[] bytes = well.getGraphBytes(new GraphSpec(graph));
        if (bytes != null)
        {
            streamBytes(bytes, "image/png", System.currentTimeMillis() + DateUtils.MILLIS_PER_HOUR);
        }
        return null;
    }

    @Jpf.Action
    protected Forward generateGraph(ChooseGraphForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        GraphSpec graph = new GraphSpec(getRequest().getParameter("graph"));
        FCSAnalyzer.GraphResult res = FlowAnalyzer.generateGraph(form.getWell(), form.getScript(), FlowProtocolStep.fromActionSequence(form.getActionSequence()), form.getCompensationMatrix(), graph);
        if (res.exception != null)
        {
            _log.error("Error generating graph", res.exception);
        }
        return streamBytes(res.bytes, "image/png", 0);
    }

    @Jpf.Action
    protected Forward showFCS() throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        String mode = getViewURLHelper().getParameter("mode");
        FlowWell well = getWell();

        try
        {
            if (mode.equals("raw"))
            {
                String strEventCount = getViewURLHelper().getParameter("eventCount");
                int maxEventCount = Integer.MAX_VALUE;
                if (strEventCount != null)
                {
                    maxEventCount = Integer.valueOf(strEventCount);
                }
                byte[] bytes = FCSAnalyzer.get().getFCSBytes(well.getFCSURI(), maxEventCount);
                PageFlowUtil.streamFileBytes(getResponse(), URIUtil.getFilename(well.getFCSURI()), bytes, true);
                return null;
            }

            getResponse().setContentType("text/plain");
            FCSViewer viewer = new FCSViewer(FlowAnalyzer.getFCSUri(well));
            if ("compensated".equals(mode))
            {
                FlowCompensationMatrix comp = well.getRun().getCompensationMatrix();
                // viewer.applyCompensationMatrix(URIUtil.resolve(base, compFiles[0].getPath()));
            }
            if ("keywords".equals(mode))
            {
                viewer.writeKeywords(getResponse().getWriter());
            }
            else
            {
                viewer.writeValues(getResponse().getWriter());
            }
        }
        catch (FileNotFoundException fnfe)
        {
            return renderInTemplate(new HtmlView("The specified FCS file could not be found."), well, "File Not Found", Action.showFCS);
        }
        return null;
    }

    static abstract public class Page extends FlowPage
    {
        private FlowRun _run;
        private FlowWell _well;
        Map<String, String> _keywords;
        Map<StatisticSpec, Double> _statistics;
        GraphSpec[] _graphs;

        public void setWell(FlowWell well) throws Exception
        {
            _run = well.getRun();
            _well = well;
            _keywords = _well.getKeywords();
            _statistics = _well.getStatistics();
            _graphs = _well.getGraphs();
        }

        public FlowRun getRun()
        {
            return _run;
        }

        public Map<String, String> getKeywords()
        {
            return _keywords;
        }

        public Map<StatisticSpec, Double> getStatistics()
        {
            return _statistics;
        }

        public FlowWell getWell()
        {
            return _well;
        }

        public GraphSpec[] getGraphs()
        {
            return _graphs;
        }
    }
}
