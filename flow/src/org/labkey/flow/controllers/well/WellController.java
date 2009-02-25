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

package org.labkey.flow.controllers.well;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URIUtil;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.api.view.*;
import org.labkey.api.data.*;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.web.FCSViewer;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.SpringFlowController;
import org.labkey.flow.data.*;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.ObjectType;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.flow.query.AttributeCache;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.sql.SQLException;
import java.sql.ResultSet;

public class WellController extends SpringFlowController<WellController.Action>
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
        keywords,
    }

    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(WellController.class);

    public WellController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ACL.PERM_NONE)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return HttpView.redirect(urlFor(FlowController.Action.begin));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    public FlowWell getWell() throws Exception
    {
        return FlowWell.fromURL(getActionURL(), getRequest());
    }

    public Page getPage(String name) throws Exception
    {
        Page ret = (Page) getFlowPage(name);
        FlowWell well = getWell();
        if (well == null)
            HttpView.throwNotFound("well not found");
        ret.setWell(well);
        return ret;
    }

    public static ActionURL getShowWellURL()
    {
        return new ActionURL(ShowWellAction.class, ContainerManager.getRoot());
    }

    @RequiresPermission(ACL.PERM_READ)
    public class ShowWellAction extends SimpleViewAction
    {
        FlowWell well;

        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            Page page = getPage("showWell.jsp");
            well = page.getWell();
            return new JspView(page);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String label = well != null ? null : "Well not found";
            return appendFlowNavTrail(root, well, label, Action.showWell);
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
    public class EditWellAction extends FormViewAction<EditWellForm>
    {
        FlowWell well;

        public void validateCommand(EditWellForm form, Errors errors)
        {
            try
            {
                well = getWell();
                form.setWell(well);
            }
            catch (Exception e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                return;
            }

            if (StringUtils.isEmpty(form.ff_name))
            {
                errors.reject(ERROR_MSG, "Name cannot be blank");
            }
            if (form.ff_keywordName != null)
            {
                Set<String> keywords = new HashSet<String>();
                for (int i = 0; i < form.ff_keywordName.length; i ++)
                {
                    String name = form.ff_keywordName[i];
                    String value = form.ff_keywordValue[i];
                    if (StringUtils.isEmpty(name))
                    {
                        if (!StringUtils.isEmpty(value))
                        {
                            errors.reject(ERROR_MSG, "Missing name for value '" + value + "'");
                        }
                    }
                    else if (!keywords.add(name))
                    {
                        errors.reject(ERROR_MSG, "There is already a keyword '" + name + "'");
                        break;
                    }
                }
            }
        }

        public ModelAndView getView(EditWellForm form, boolean reshow, BindException errors) throws Exception
        {
            if (well == null)
            {
                well = getWell();
                form.setWell(well);
            }
            return FormPage.getView(WellController.class, form, errors, "editWell.jsp");
        }

        public boolean handlePost(EditWellForm form, BindException errors) throws Exception
        {
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
            FlowManager.get().flowObjectModified();
            return true;
        }

        public ActionURL getSuccessURL(EditWellForm form)
        {
            return form.getWell().urlFor(Action.showWell);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String label = well != null ? "Edit " + well.getLabel() : "Well not found";
            return appendFlowNavTrail(root, well, label, Action.editWell);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ChooseGraphAction extends SimpleViewAction<ChooseGraphForm>
    {
        FlowWell well;

        public ModelAndView getView(ChooseGraphForm form, BindException errors) throws Exception
        {
            well = form.getWell();
            FormPage page = FormPage.get(WellController.class, form, "chooseGraph.jsp");
            return new JspView(page);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(root, well, "Choose Graph", Action.chooseGraph);
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class ShowGraphAction extends SimpleViewAction
    {
        FlowWell well;

        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            well = getWell();
            if (well == null)
            {
                int objectId = getIntParam(FlowParam.objectId);
                if (objectId == 0)
                    return null;
                FlowObject obj = FlowDataObject.fromAttrObjectId(objectId);
                if (!(obj instanceof FlowWell))
                    return null;
                well = (FlowWell) obj;
                well.checkContainer(getActionURL());
            }
            String graph = getParam(FlowParam.graph);
            byte[] bytes = well.getGraphBytes(new GraphSpec(graph));
            if (bytes != null)
            {
                streamBytes(getViewContext().getResponse(),
                        bytes, "image/png", System.currentTimeMillis() + DateUtils.MILLIS_PER_HOUR);
            }
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    static void streamBytes(HttpServletResponse response, byte[] bytes, String contentType, long expires) throws IOException
    {
        response.setDateHeader("Expires", expires);
        response.setContentType(contentType);
        response.getOutputStream().write(bytes);
    }

    @RequiresPermission(ACL.PERM_READ)
    public class GenerateGraphAction extends SimpleViewAction<ChooseGraphForm>
    {
        public ModelAndView getView(ChooseGraphForm form, BindException errors) throws Exception
        {
            GraphSpec graph = new GraphSpec(getRequest().getParameter("graph"));
            FCSAnalyzer.GraphResult res = FlowAnalyzer.generateGraph(form.getWell(), form.getScript(), FlowProtocolStep.fromActionSequence(form.getActionSequence()), form.getCompensationMatrix(), graph);
            if (res.exception != null)
            {
                _log.error("Error generating graph", res.exception);
            }
            streamBytes(getViewContext().getResponse(), res.bytes, "image/png", 0);
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    abstract class FCSAction extends SimpleViewAction<Object>
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            FlowWell well = getWell();
            if (null == well)
                HttpView.throwNotFound();

            try
            {
                return internalGetView(well);
            }
            catch (FileNotFoundException fnfe)
            {
                errors.reject(ERROR_MSG, "FCS File not found at this location: " + well.getFCSURI());
                return new SimpleErrorView(errors);
            }
        }

        protected abstract ModelAndView internalGetView(FlowWell well) throws Exception;
    }
    
    
    @RequiresPermission(ACL.PERM_READ)
    public class KeywordsAction extends FCSAction
    {
        protected ModelAndView internalGetView(FlowWell well) throws Exception
        {
            // convert to use the same Ext control as ShowWellAction
            getViewContext().getResponse().setContentType("text/plain");
            FCSViewer viewer = new FCSViewer(FlowAnalyzer.getFCSUri(well));
            viewer.writeKeywords(getViewContext().getResponse().getWriter());
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    // this is really for dev use as far as I can tell
    @RequiresPermission(ACL.PERM_READ)
    public class ShowFCSAction extends FCSAction
    {
        public ModelAndView internalGetView(FlowWell well) throws Exception
        {
            String mode = getActionURL().getParameter("mode");

            if (mode.equals("raw"))
            {
                String strEventCount = getActionURL().getParameter("eventCount");
                int maxEventCount = Integer.MAX_VALUE;
                if (strEventCount != null)
                {
                    maxEventCount = Integer.valueOf(strEventCount);
                }
                byte[] bytes = FCSAnalyzer.get().getFCSBytes(well.getFCSURI(), maxEventCount);
                PageFlowUtil.streamFileBytes(getViewContext().getResponse(), URIUtil.getFilename(well.getFCSURI()), bytes, true);
                return null;
            }

            getViewContext().getResponse().setContentType("text/plain");
            FCSViewer viewer = new FCSViewer(FlowAnalyzer.getFCSUri(well));
            if ("compensated".equals(mode))
            {
                FlowCompensationMatrix comp = well.getRun().getCompensationMatrix();
                // viewer.applyCompensationMatrix(URIUtil.resolve(base, compFiles[0].getPath()));
            }
            if ("keywords".equals(mode))
            {
                viewer.writeKeywords(getViewContext().getResponse().getWriter());
            }
            else
            {
                viewer.writeValues(getViewContext().getResponse().getWriter());
            }
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }


    @RequiresPermission(ACL.PERM_ADMIN)
    public class BulkUpdateKeywordsAction extends FormViewAction<UpdateKeywordsForm>
    {
        Integer keywordid = null;
        int updated = -1;

        public void validateCommand(UpdateKeywordsForm form, Errors errors)
        {
            if (null == form.keyword)
                errors.rejectValue("keyword", ERROR_REQUIRED);
            if (null == form.from)
                form.from = new String[0];
            if (null == form.to)
                form.to = new String[0];
            if (form.from.length != form.to.length)
            {
                errors.reject("from length and to length do not match");
            }
            else
            {
                for (int i=0 ; i<form.from.length ; i++)
                {
                    form.from[i] = StringUtils.trimToNull(form.from[i]);
                    form.to[i] = StringUtils.trimToNull(form.to[i]);
                    if ((null == form.from[i]) != (null == form.to[i]))
                        errors.reject(ERROR_MSG, "Empty value not allowed");
                }
            }

            SQLFragment sql = new SQLFragment("SELECT rowid FROM flow.attribute WHERE name=?", form.keyword);
            try
            {
                keywordid = Table.executeSingleton(DbSchema.get("flow"), sql.getSQL(), sql.getParams().toArray(), Integer.class);
                if (null == keywordid)
                    errors.rejectValue("keyword", ERROR_MSG, "keyword not found: " + form.keyword);
            }
            catch (SQLException x)
            {
                throw new RuntimeSQLException(x);
            }
        }

        public ModelAndView getView(UpdateKeywordsForm form, boolean reshow, BindException errors) throws Exception
        {
            return new JspView<UpdateKeywordsForm>(WellController.class, "bulkUpdate.jsp", form, errors);
            //return new GroovyView<UpdateKeywordsForm>("/org/labkey/flow/controllers/well/bulkUpdate.jsp", form);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }

        public boolean handlePost(UpdateKeywordsForm form, BindException errors) throws Exception
        {
            SQLFragment update = new SQLFragment();
            update.append("UPDATE flow.keyword SET value = CASE\n");
            for (int i=0 ; i<form.from.length ; i++)
            {
                if (form.from[i] != null && form.to[i] != null)
                {
                    update.append("  WHEN value=? THEN ?\n");
                    update.add(form.from[i]);
                    update.add(form.to[i]);
                }
            }
            update.append("  ELSE value END\n");
            update.append("WHERE objectid IN (SELECT O.rowid from flow.object O where O.container=? and O.typeid=?) AND keywordid=?");
            update.add(getViewContext().getContainer());
            update.add(ObjectType.fcsKeywords.getTypeId());
            update.add(keywordid);
            update.append("  AND value IN (");
            String param = "?";
            for (int i=0 ; i<form.from.length ; i++)
            {
                if (null != form.from[i])
                {
                    update.append(param);
                    update.add(form.from[i]);
                    param = ",?";
                }
            }
            update.append(")");

            updated = Table.execute(DbSchema.get("flow"), update);

            form.message = "" + updated + " values updated";
            // CONSIDER handle nulls (requires INSERT and DELETE)
            return true;
        }

        public ActionURL getSuccessURL(UpdateKeywordsForm form)
        {
            return null;
        }

        public ModelAndView getSuccessView(UpdateKeywordsForm form)
        {
            return new MessageView(form.message, "bulkUpdateKeywords.view");
        }
    }

    public static class MessageView extends HtmlView
    {
        MessageView(String message, String href)
        {
            super(null);
            StringBuilder sb = new StringBuilder();
            sb.append("<span style='color:green;'>");
            sb.append(PageFlowUtil.filter(message));
            sb.append("</span><br>");
            sb.append(PageFlowUtil.generateButton("OK",href));
            setHtml(sb.toString());
        }
    }

    public static class UpdateKeywordsForm
    {
        String returnURL = "begin.view";
        public String keyword = null;
        public String[] from = new String[0];
        public String[] to = new String[0];
        public String message;

        public void setKeyword(String keyword)
        {
            this.keyword = keyword;
        }

        public String getKeyword()
        {
            return keyword;
        }

        public void setFrom(String[] from)
        {
            this.from = from;
        }

        public void setTo(String[] to)
        {
            this.to = to;
        }

        public void setReturnURL(String url)
        {
            this.returnURL = url;
        }

        public TreeSet<String> getKeywords(ViewContext context)
        {
            Map<String,Integer> map = AttributeCache.KEYWORDS.getAttrValues(context.getContainer(), null);
            return new TreeSet<String>(map.keySet());
        }

        public TreeSet<String> getValues(ViewContext context, String keyword)
        {
            ResultSet rs = null;
            try
            {
                rs = Table.executeQuery(DbSchema.get("flow"),
                        "SELECT DISTINCT value FROM flow.keyword WHERE keywordid = (SELECT rowid FROM flow.attribute WHERE name=?)", new Object[]{keyword});
                TreeSet<String> set = new TreeSet<String>();
                while (rs.next())
                    set.add(rs.getString(1));
                return set;
            }
            catch (SQLException x)
            {
                throw new RuntimeSQLException(x);
            }
            finally
            {
                ResultSetUtil.close(rs);
            }
        }
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
