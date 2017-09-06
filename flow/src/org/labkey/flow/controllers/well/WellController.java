/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.ReturnUrlForm;
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Selector.ForEachBlock;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryAction;
import org.labkey.api.security.ContextualRoles;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.SecurityPolicy;
import org.labkey.api.security.SecurityPolicyManager;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.HeartBeat;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewContext;
import org.labkey.flow.analysis.model.FCSHeader;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.web.FCSViewer;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.data.FlowCompensationMatrix;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowWell;
import org.labkey.flow.persist.AttributeCache;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.ObjectType;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.flow.util.KeywordUtil;
import org.labkey.flow.view.GraphColumn;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class WellController extends BaseFlowController
{
    private static final Logger _log = Logger.getLogger(WellController.class);

    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(WellController.class);

    public WellController()
    {
        super();
        setActionResolver(_actionResolver);
    }

    @RequiresNoPermission
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return HttpView.redirect(new ActionURL(FlowController.BeginAction.class, getContainer()));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    public FlowWell getWell()
    {
        return FlowWell.fromURL(getActionURL(), getRequest(), getContainer(), getUser());
    }

    protected List<FlowWell> getWells(boolean isBulkEdit)
    {
        List<FlowWell> ret = new ArrayList<>();

        if (isBulkEdit)
        {
            String[] wellIds = getRequest().getParameterValues("ff_fileRowId");
            if (wellIds != null && wellIds.length > 0)
            {
                for (String wellId : wellIds)
                {
                    FlowWell flowWell = FlowWell.fromWellId(Integer.parseInt(wellId));
                    flowWell.checkContainer(getContainer(), getUser(), getActionURL());
                    ret.add(flowWell);
                }
                return ret;
            }
        }

        FlowWell well = FlowWell.fromURL(getActionURL(), getRequest(), getContainer(), getUser());
        if(well != null)
        {
            ret.add(well);
        }

        return ret;
    }

    protected String[] getKeywordIntersection(List<FlowWell> wells, boolean filterHiddenKeywords)
    {
        Set<String> intersection = new HashSet<>(wells.get(0).getKeywords().keySet());
        for (FlowWell well : wells)
        {
            Set<String> c = well.getKeywords().keySet();
            intersection.retainAll(c);
        }

        List<String> sortList = new ArrayList<>(intersection);

        if (filterHiddenKeywords)
        {
            sortList = (List<String>) KeywordUtil.filterHidden(sortList);
        }

        Collections.sort(sortList);
        return sortList.toArray(new String[0]);
    }

    public Page getPage(String name)
    {
        Page ret = (Page) getFlowPage(name);
        FlowWell well = getWell();
        if (well == null)
            throw new NotFoundException("well not found");

        ret.setWell(well);
        return ret;
    }

    public static ActionURL getShowWellURL()
    {
        return new ActionURL(ShowWellAction.class, ContainerManager.getRoot());
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowWellAction extends SimpleViewAction
    {
        FlowWell well;

        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            Page page = getPage("showWell.jsp");
            well = page.getWell();
            JspView v = new JspView(page);
            v.setClientDependencies(page.getClientDependencies());
            return v;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String label = well != null ? null : "Well not found";
            return appendFlowNavTrail(getPageConfig(), root, well, label);
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class EditWellAction extends FormViewAction<EditWellForm>
    {
        List<FlowWell> wells;
        boolean isBulkEdit;
        boolean isUpdate;
        String returnURL;
        @Override
        public void validateCommand(EditWellForm form, Errors errors)
        {
            wells = getWells(form.ff_isBulkEdit);
            form.setWells(wells, form.ff_isBulkEdit);

            if (form.ff_keywordName != null)
            {
                Set<String> keywords = new HashSet<>();
                for (int i = 0; i < form.ff_keywordName.length; i ++)
                {
                    String name = form.ff_keywordName[i];
                    String value = form.ff_keywordValue[i];
                    form.ff_keywordError[i] = null;
                    if (StringUtils.isEmpty(name))
                    {
                        if (!StringUtils.isEmpty(value))
                        {
                            String missingNameMessage = "Missing name for value '" + value + "'";
                            errors.reject(ERROR_MSG, missingNameMessage);
                            form.ff_keywordError[i] = missingNameMessage;
                        }
                    }
                    else if (!keywords.add(name))
                    {
                        String duplicateNameMessage = "There is already a keyword '" + name + "'";
                        errors.reject(ERROR_MSG, duplicateNameMessage);
                        form.ff_keywordError[i] = duplicateNameMessage;
                        for (int j = 0; j < form.ff_keywordName.length; j ++){
                            if (j != i && name.equals(form.ff_keywordName[j]))
                            {
                                form.ff_keywordError[j] = "Duplicate keyword";
                            }
                        }

                        break;
                    }
                }
            }
        }

        @Override
        public ModelAndView getView(EditWellForm form, boolean reshow, BindException errors) throws Exception
        {
            String returnUrl = getRequest().getParameter("editWellReturnUrl");
            form.editWellReturnUrl = returnUrl;
            if (returnUrl != null)
            {
                form.editWellReturnUrl = returnUrl.toString();
            }
            isUpdate = Boolean.parseBoolean(getRequest().getParameter("isUpdate"));
            if(!isUpdate)
            {
                if (wells == null)
                {
                    wells = getWells(form.ff_isBulkEdit);
                }
                if (wells == null || wells.size() == 0)
                {
                    Set<String> selected = DataRegionSelection.getSelected(form.getViewContext(), null, true, false);
                    wells = new ArrayList<>();

                    for (String wellId : selected)
                    {
                        wells.add(FlowWell.fromWellId(Integer.parseInt(wellId)));
                    }
                    DataRegionSelection.clearAll(form.getViewContext());
                }
                form.setWells(wells, form.ff_isBulkEdit);
                if (form.ff_isBulkEdit && !isUpdate)
                {
                    form.ff_keywordName = getKeywordIntersection(wells, true);
                }
            }
            return FormPage.getView(WellController.class, form, errors, "editWell.jsp");
        }

        @Override
        public boolean handlePost(EditWellForm form, BindException errors) throws Exception
        {
            isBulkEdit = form.ff_isBulkEdit;
            form.editWellReturnUrl = getRequest().getParameter("editWellReturnUrl");
            returnURL = form.editWellReturnUrl;
            isUpdate = Boolean.parseBoolean(getRequest().getParameter("isUpdate"));

            if (!isUpdate)
            {
                return false;
            }

            wells = getWells(form.ff_isBulkEdit);

            for (FlowWell well : wells)
            {
                if (!form.ff_isBulkEdit)
                {
                    well.setName(getUser(), form.ff_name);
                    well.getExpObject().setComment(getUser(), form.ff_comment);
                }
                if (form.ff_keywordName != null)
                {
                    for (int i = 0; i < form.ff_keywordName.length; i++)
                    {
                        String name = form.ff_keywordName[i];
                        if (StringUtils.isEmpty(name))
                            continue;

                        boolean isEmptyValueOnBulkEdit = form.ff_isBulkEdit && form.ff_keywordValue[i] == null;
                        if (!isEmptyValueOnBulkEdit)
                        {
                            well.setKeyword(name, form.ff_keywordValue[i]);
                        }
                    }
                }
            }
            FlowManager.get().flowObjectModified();
            return true;
        }

        public ActionURL getSuccessURL(EditWellForm form)
        {
            if (form.ff_isBulkEdit)
            {
                return new ActionURL(form.editWellReturnUrl);
            }
            return form.getWells().get(0).urlFor(ShowWellAction.class);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            if (isBulkEdit)
            {
                ActionURL urlFcsFiles = FlowTableType.FCSFiles.urlFor(getUser(), getContainer(), QueryAction.executeQuery);
                root.addChild(new NavTree("FSC Files",urlFcsFiles));
                root.addChild(new NavTree("Edit Keywords"));
                return root;
            }
            String label = wells != null && !wells.isEmpty() ? "Edit " + wells.get(0).getLabel() : "Well not found";
            return appendFlowNavTrail(getPageConfig(), root, wells.get(0), label);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ChooseGraphAction extends SimpleViewAction<ChooseGraphForm>
    {
        FlowWell well;

        public ModelAndView getView(ChooseGraphForm form, BindException errors) throws Exception
        {
            well = form.getWell();
            if (null == well)
            {
                throw new NotFoundException();
            }

            URI fileURI = well.getFCSURI();
            if (fileURI == null)
                return new HtmlView("<span class='labkey-error'>There is no file on disk for this well.</span>");

            PipeRoot r = PipelineService.get().findPipelineRoot(well.getContainer());
            if (r == null)
                return new HtmlView("<span class='labkey-error'>Pipeline not configured</span>");

            // UNDONE: PipeRoot should have wrapper for this
            //NOTE: we are specifically not inheriting policies from the parent container
            //as the old permissions-checking code did not do this. We need to consider
            //whether the pipeline root's parent really is the container, or if we should
            //be checking a different (more specific) permission.
            SecurityPolicy policy = SecurityPolicyManager.getPolicy(r, false);
            if (!policy.hasPermission(getUser(), ReadPermission.class))
                return new HtmlView("<span class='labkey-error'>You don't have permission to the FCS file.</span>");

            boolean canRead = false;
            URI rel = URIUtil.relativize(r.getUri(), fileURI);
            if (rel != null)
            {
                File f = r.resolvePath(rel.getPath());
                canRead = f != null && f.canRead();
            }
            if (!canRead)
                return new HtmlView("<span class='labkey-error'>The original FCS file is no longer available or is not readable: " + PageFlowUtil.filter(rel.getPath()) + "</span>");

            FormPage page = FormPage.get(WellController.class, form, "chooseGraph.jsp");
            return new JspView(page);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, well, "Choose Graph");
        }
    }

    @RequiresPermission(ReadPermission.class)
    @ContextualRoles(GraphContextualRoles.class)
    public class ShowGraphAction extends SimpleViewAction
    {
        FlowWell well;

        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            Pair<Integer, String> objectId_graph = parseObjectIdGraph();

            well = getWell();
            if (well == null)
            {
                int objectId = getIntParam(FlowParam.objectId);
                if (objectId == 0 && objectId_graph != null)
                    objectId = objectId_graph.first;
                if (objectId == 0)
                    return null;
                FlowDataObject obj = FlowDataObject.fromAttrObjectId(objectId);
                if (!(obj instanceof FlowWell))
                    return null;
                well = (FlowWell) obj;
                well.checkContainer(getContainer(), getUser(), getActionURL());
            }

            String graph = getParam(FlowParam.graph);
            if (graph == null && objectId_graph != null)
                graph = objectId_graph.second;
            if (graph == null)
                throw new NotFoundException("Graph spec required");

            byte[] bytes = null;
            try
            {
                GraphSpec spec = new GraphSpec(graph);
                bytes = well.getGraphBytes(spec);
            }
            catch (Exception ex)
            {
                _log.error("Error retrieving graph", ex);
                ExceptionUtil.logExceptionToMothership(getRequest(), ex);
            }

            if (bytes != null)
            {
                streamBytes(getViewContext().getResponse(),
                        bytes, "image/png", HeartBeat.currentTimeMillis() + DateUtils.MILLIS_PER_HOUR);
            }
            return null;
        }

        @Nullable
        private Pair<Integer, String> parseObjectIdGraph()
        {
            String param = getParam(FlowParam.objectId_graph);
            if (param == null)
                return null;

            return GraphColumn.parseObjectIdGraph(param);
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
        response.reset();
        response.getOutputStream().write(bytes);
    }

    @RequiresPermission(ReadPermission.class)
    public class GenerateGraphAction extends SimpleViewAction<ChooseGraphForm>
    {
        public ModelAndView getView(ChooseGraphForm form, BindException errors) throws IOException
        {
            FlowWell well = form.getWell();
            if (well == null)
                throw new NotFoundException("Well not found");

            String graph = getParam(FlowParam.graph);
            if (graph == null)
                throw new NotFoundException("Graph spec required");

            GraphSpec graphSpec = new GraphSpec(graph);
            FCSAnalyzer.GraphResult res = null;
            try
            {
                res = FlowAnalyzer.generateGraph(form.getWell(), form.getScript(), FlowProtocolStep.fromActionSequence(form.getActionSequence()), form.getCompensationMatrix(), graphSpec);
            }
            catch (IOException ioe)
            {
                _log.error("Error retrieving graph", ioe);
                return null;
            }
            catch (Exception ex)
            {
                _log.error("Error retrieving graph", ex);
                ExceptionUtil.logExceptionToMothership(getRequest(), ex);
                return null;
            }

            if (res == null || res.exception != null)
            {
                _log.error("Error generating graph", res.exception);
            }
            else
            {
                streamBytes(getViewContext().getResponse(), res.bytes, "image/png", 0);
            }
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
                throw new NotFoundException("Well not found");

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


    @RequiresPermission(ReadPermission.class)
    public class DownloadAction extends FCSAction
    {
        @Override
        protected ModelAndView internalGetView(FlowWell well) throws Exception
        {
            URI fileURI = well.getFCSURI();
            if (fileURI == null)
                throw new NotFoundException("file not found");

            File file = new File(fileURI);
            if (!file.exists())
                throw new NotFoundException("file not found");

            FileInputStream fis = new FileInputStream(file);

            Map<String, String> headers = Collections.singletonMap("Content-Type", FCSHeader.CONTENT_TYPE);
            PageFlowUtil.streamFile(getViewContext().getResponse(), headers, file.getName(), fis, true);
            return null;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }
    
    @RequiresPermission(ReadPermission.class)
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
    @RequiresPermission(ReadPermission.class)
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
                    try
                    {
                        maxEventCount = Integer.valueOf(strEventCount);
                    }
                    catch (NumberFormatException ex) { }
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


    @RequiresPermission(AdminPermission.class)
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

            SQLFragment sql = new SQLFragment("SELECT RowId FROM flow.KeywordAttr WHERE Container = ? AND Name = ?", getContainer(), form.keyword);
            keywordid = new SqlSelector(FlowManager.get().getSchema(), sql).getObject(Integer.class);
            if (null == keywordid)
                errors.rejectValue("keyword", ERROR_MSG, "keyword not found: " + form.keyword);
        }

        public ModelAndView getView(UpdateKeywordsForm form, boolean reshow, BindException errors) throws Exception
        {
            return new JspView<>(WellController.class, "bulkUpdate.jsp", form, errors);
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
            update.add(getContainer());
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

            updated = new SqlExecutor(FlowManager.get().getSchema()).execute(update);

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
            return new MessageView(form.message, new ActionURL(WellController.BulkUpdateKeywordsAction.class, getContainer()));
        }
    }

    public static class MessageView extends HtmlView
    {
        MessageView(String message, ActionURL url)
        {
            super(null);
            StringBuilder sb = new StringBuilder();
            sb.append("<span style='color:green;'>");
            sb.append(PageFlowUtil.filter(message));
            sb.append("</span><br>");
            sb.append(PageFlowUtil.button("OK").href(url));
            setHtml(sb.toString());
        }
    }

    public static class UpdateKeywordsForm extends ReturnUrlForm
    {
        public String keyword = null;
        public String[] from = new String[0];
        public String[] to = new String[0];
        public String message;

        UpdateKeywordsForm()
        {
            setReturnUrl("flow-well-begin.view");
        }

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

        public TreeSet<String> getKeywords(ViewContext context)
        {
            Collection<AttributeCache.KeywordEntry> entries = AttributeCache.KEYWORDS.byContainer(context.getContainer());
            TreeSet<String> keywords = new TreeSet<>();
            for (AttributeCache.KeywordEntry entry : entries)
                keywords.add(entry.getAttribute());
            return keywords;
        }

        public TreeSet<String> getValues(ViewContext context, String keyword)
        {
            final TreeSet<String> set = new TreeSet<>();

            new SqlSelector(FlowManager.get().getSchema(),
                    "SELECT DISTINCT value FROM flow.keyword WHERE keywordid = (SELECT rowid FROM flow.KeywordAttr WHERE container=? AND name=?)", context.getContainer(), keyword).forEach(new ForEachBlock<String>()
            {
                @Override
                public void exec(String value) throws SQLException
                {
                    if (value != null)
                        set.add(value);
                }
            }, String.class);

            return set;
        }
    }

    
    static abstract public class Page extends FlowPage
    {
        private FlowRun _run;
        private FlowWell _well;
        Map<String, String> _keywords;
        Map<StatisticSpec, Double> _statistics;
        GraphSpec[] _graphs;

        public void setWell(FlowWell well)
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
