/*
 * Copyright (c) 2007-2019 LabKey Corporation
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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.ReturnUrlForm;
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.jsp.JspBase;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryAction;
import org.labkey.api.security.ContextualRoles;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermission;
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

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.labkey.api.util.DOM.Attribute;
import static org.labkey.api.util.DOM.BR;
import static org.labkey.api.util.DOM.SPAN;
import static org.labkey.api.util.DOM.at;

public class WellController extends BaseFlowController
{
    private static final Logger _log = LogManager.getLogger(WellController.class);

    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(WellController.class);

    public WellController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresNoPermission
    public class BeginAction extends SimpleViewAction
    {
        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            return HttpView.redirect(new ActionURL(FlowController.BeginAction.class, getContainer()));
        }

        @Override
        public void addNavTrail(NavTree root)
        {
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

    public Page getPage(String jspPath)
    {
        Page ret = (Page) getFlowPage(jspPath);
        FlowWell well = getWell();
        if (well == null)
            throw new NotFoundException("well not found");

        ret.setWell(well);
        return ret;
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowWellAction extends SimpleViewAction
    {
        private FlowWell _well;

        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            Page page = getPage("/org/labkey/flow/controllers/well/showWell.jsp");
            _well = page.getWell();
            JspView v = new JspView(page);
            v.setClientDependencies(page.getClientDependencies());
            return v;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            String label = _well != null ? null : "Well not found";
            addFlowNavTrail(getPageConfig(), root, _well, label);
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class EditWellAction extends FormViewAction<EditWellForm>
    {
        private List<FlowWell> _wells;
        private boolean _isBulkEdit;
        private boolean _isUpdate;

        @Override
        public void validateCommand(EditWellForm form, Errors errors)
        {
            _wells = getWells(form.ff_isBulkEdit);
            form.setWells(_wells, form.ff_isBulkEdit);

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
        public ModelAndView getView(EditWellForm form, boolean reshow, BindException errors)
        {
            String returnUrl = getRequest().getParameter("editWellReturnUrl");
            form.editWellReturnUrl = returnUrl;

            if (returnUrl != null)
            {
                form.editWellReturnUrl = returnUrl;
            }

            _isUpdate = Boolean.parseBoolean(getRequest().getParameter("isUpdate"));

            if (!_isUpdate)
            {
                if (_wells == null)
                {
                    _wells = getWells(form.ff_isBulkEdit);
                }
                if (_wells == null || _wells.size() == 0)
                {
                    Set<String> selected = DataRegionSelection.getSelected(form.getViewContext(), null, false);
                    _wells = new ArrayList<>();

                    for (String wellId : selected)
                    {
                        _wells.add(FlowWell.fromWellId(Integer.parseInt(wellId)));
                    }
                    DataRegionSelection.clearAll(form.getViewContext());
                }
                form.setWells(_wells, form.ff_isBulkEdit);
                if (form.ff_isBulkEdit && !_isUpdate)
                {
                    form.ff_keywordName = getKeywordIntersection(_wells, true);
                }
            }

            if (form.getWells().isEmpty())
                throw new NotFoundException();

            return FormPage.getView("/org/labkey/flow/controllers/well/editWell.jsp", form, errors);
        }

        @Override
        public boolean handlePost(EditWellForm form, BindException errors) throws Exception
        {
            _isBulkEdit = form.ff_isBulkEdit;
            form.editWellReturnUrl = getRequest().getParameter("editWellReturnUrl");
            _isUpdate = Boolean.parseBoolean(getRequest().getParameter("isUpdate"));

            if (!_isUpdate)
            {
                return false;
            }

            _wells = getWells(form.ff_isBulkEdit);

            for (FlowWell well : _wells)
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
                            well.setKeyword(name, form.ff_keywordValue[i], getUser());
                        }
                    }
                }
            }
            FlowManager.get().flowObjectModified();
            return true;
        }

        @Override
        public ActionURL getSuccessURL(EditWellForm form)
        {
            if (form.ff_isBulkEdit)
            {
                return new ActionURL(form.editWellReturnUrl);
            }
            return form.getWells().get(0).urlFor(ShowWellAction.class);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            if (_isBulkEdit)
            {
                ActionURL urlFcsFiles = FlowTableType.FCSFiles.urlFor(getUser(), getContainer(), QueryAction.executeQuery);
                root.addChild(new NavTree("FSC Files",urlFcsFiles));
                root.addChild(new NavTree("Edit Keywords"));
                return;
            }
            FlowWell well = null;
            if (_wells != null && !_wells.isEmpty())
                well = _wells.get(0);
            String label =  null != well ? "Edit " + well.getLabel() : "Well not found";
            addFlowNavTrail(getPageConfig(), root, well, label);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ChooseGraphAction extends SimpleViewAction<ChooseGraphForm>
    {
        private FlowWell _well;

        @Override
        public ModelAndView getView(ChooseGraphForm form, BindException errors)
        {
            _well = form.getWell();
            if (null == _well)
            {
                throw new NotFoundException();
            }

            URI fileURI = _well.getFCSURI();
            if (fileURI == null)
                return HtmlView.err("There is no file on disk for this well.");

            PipeRoot r = PipelineService.get().findPipelineRoot(_well.getContainer());
            if (r == null)
                return HtmlView.err("Pipeline not configured");

            // NOTE: see 30001, we are not checking the pipeline permissions which control access via webdav
            // I don't think it necessary or what is intended by those permissions

            boolean canRead = false;
            URI rel = URIUtil.relativize(r.getUri(), fileURI);
            if (rel != null)
            {
                File f = r.resolvePath(rel.getPath());
                canRead = f != null && f.canRead();
            }
            if (!canRead)
                return HtmlView.err("The original FCS file is no longer available or is not readable" + (rel == null ? "." : ": " + rel.getPath()));

            return new JspView<>("/org/labkey/flow/controllers/well/chooseGraph.jsp", form);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addFlowNavTrail(getPageConfig(), root, _well, "Choose Graph");
        }
    }

    @RequiresPermission(ReadPermission.class)
    @ContextualRoles(GraphContextualRoles.class)
    public class ShowGraphAction extends SimpleViewAction
    {
        @Override
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            Pair<Integer, String> objectId_graph = parseObjectIdGraph();

            FlowWell well = getWell();
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

        @Override
        public void addNavTrail(NavTree root)
        {
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
        @Override
        public ModelAndView getView(ChooseGraphForm form, BindException errors) throws IOException
        {
            FlowWell well = form.getWell();
            if (well == null)
                throw new NotFoundException("Well not found");

            String graph = getParam(FlowParam.graph);
            if (graph == null)
                throw new NotFoundException("Graph spec required");

            GraphSpec graphSpec = new GraphSpec(graph);
            FCSAnalyzer.GraphResult res;
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

        @Override
        public void addNavTrail(NavTree root)
        {
        }
    }

    abstract class FCSAction extends SimpleViewAction<Object>
    {
        @Override
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
        public void addNavTrail(NavTree root)
        {
        }
    }
    
    @RequiresPermission(ReadPermission.class)
    public class KeywordsAction extends FCSAction
    {
        @Override
        protected ModelAndView internalGetView(FlowWell well) throws Exception
        {
            // convert to use the same Ext control as ShowWellAction
            getViewContext().getResponse().setContentType("text/plain");
            FCSViewer viewer = new FCSViewer(FlowAnalyzer.getFCSUri(well));
            viewer.writeKeywords(getViewContext().getResponse().getWriter());
            return null;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
        }
    }


    // this is really for dev use as far as I can tell
    @RequiresPermission(ReadPermission.class)
    public class ShowFCSAction extends FCSAction
    {
        @Override
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

        @Override
        public void addNavTrail(NavTree root)
        {
        }
    }


    @RequiresPermission(AdminPermission.class)
    public class BulkUpdateKeywordsAction extends FormViewAction<UpdateKeywordsForm>
    {
        private Integer _keywordid = null;

        @Override
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
            _keywordid = new SqlSelector(FlowManager.get().getSchema(), sql).getObject(Integer.class);
            if (null == _keywordid)
                errors.rejectValue("keyword", ERROR_MSG, "keyword not found: " + form.keyword);
        }

        @Override
        public ModelAndView getView(UpdateKeywordsForm form, boolean reshow, BindException errors)
        {
            return new JspView<>("/org/labkey/flow/controllers/well/bulkUpdate.jsp", form, errors);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
        }

        @Override
        public boolean handlePost(UpdateKeywordsForm form, BindException errors)
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
            update.add(_keywordid);
            update.append(" AND value IN (");
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

            int updated = new SqlExecutor(FlowManager.get().getSchema()).execute(update);

            form.message = "" + updated + " values updated";
            // CONSIDER handle nulls (requires INSERT and DELETE)
            return true;
        }

        @Override
        public ActionURL getSuccessURL(UpdateKeywordsForm form)
        {
            return null;
        }

        @Override
        public ModelAndView getSuccessView(UpdateKeywordsForm form)
        {
            return new MessageView(form.message, new ActionURL(WellController.BulkUpdateKeywordsAction.class, getContainer()));
        }
    }

    public static class MessageView extends HtmlView
    {
        MessageView(String message, ActionURL url)
        {
            super(SPAN(at(Attribute.style, "color:green;"), message, BR(), PageFlowUtil.button("OK").href(url)));
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
                "SELECT DISTINCT value FROM flow.keyword WHERE keywordid = (SELECT rowid FROM flow.KeywordAttr WHERE container=? AND name=?)", context.getContainer(), keyword).forEach(String.class, value -> {
                    if (value != null)
                        set.add(value);
                });

            return set;
        }
    }

    
    static abstract public class Page extends JspBase
    {
        private FlowRun _run;
        private FlowWell _well;
        private Map<String, String> _keywords;
        private Map<StatisticSpec, Double> _statistics;
        private GraphSpec[] _graphs;

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


    @RequiresPermission(AdminPermission.class)
    public class SetFileDateAction extends MutatingApiAction<Object>
    {
        @Override
        public Object execute(Object o, BindException errors)
        {
            if (getContainer().isRoot())
            {
                FlowManager.get().setFileDateForAllFCSFiles(getUser());
            }
            else
            {
                FlowManager.get().setFileDateForAllFCSFiles(getContainer(), getUser());
            }

            return null;
        }
    }
}
