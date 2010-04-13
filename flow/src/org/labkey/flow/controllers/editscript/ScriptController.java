/*
 * Copyright (c) 2006-2010 LabKey Corporation
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

package org.labkey.flow.controllers.editscript;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.fhcrc.cpas.flow.script.xml.*;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.*;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.api.view.template.HomeTemplate;
import org.labkey.flow.ScriptParser;
import org.labkey.flow.analysis.model.*;
import org.labkey.flow.analysis.model.Polygon;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.PlotInfo;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.data.*;
import org.labkey.flow.gateeditor.client.model.GWTGraphOptions;
import org.labkey.flow.script.FlowAnalyzer;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.w3c.dom.Element;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * User: kevink
 * Date: Nov 25, 2008 5:27:35 PM
 */
public class ScriptController extends BaseFlowController<ScriptController.Action>
{
    private static Logger _log = Logger.getLogger(ScriptController.class);

    public enum Action
    {
        begin,
        editScript,
        newProtocol,
        editSettings,
        editProperties,
        editCompensationCalculation,
        showCompensationCalulation,
        uploadCompensationCalculation,
        chooseCompensationRun,
        editAnalysis,
        editGateTree,
        uploadAnalysis,
        graphImage,
        copy,
        delete,
        gateEditor,
    }

    static SpringActionController.DefaultActionResolver _actionResolver =
            new SpringActionController.DefaultActionResolver(ScriptController.class);

    public ScriptController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class BeginAction extends SimpleViewAction<EditScriptForm>
    {
        public ModelAndView getView(EditScriptForm form, BindException errors) throws Exception
        {
            FlowScript script = form.analysisScript;
            if (script == null)
            {
                return HttpView.redirect(new ActionURL(FlowController.BeginAction.class, getContainer()));
            }
            return HttpView.redirect(script.urlShow());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class EditScriptAction extends SimpleViewAction<EditScriptForm>
    {
        public ModelAndView getView(EditScriptForm form, BindException errors) throws Exception
        {
            ScriptParser.Error error = null;
            FlowScript script = getScript();
            if (isPost())
            {
                if (safeSetAnalysisScript(script, getRequest().getParameter("script"), errors))
                {
                    error = validateScript(script);
                    if (error == null)
                    {
                        ActionURL forward = form.urlFor(ScriptController.Action.editScript);
                        return HttpView.redirect(forward);
                    }
                }
            }
            else if (getRequest().getParameter("checkSyntax") != null)
            {
                error = validateScript(script);
            }

            EditPage page = (EditPage)getPage("editScript.jsp", form);
            page.scriptParseError = error;
            return new JspView<EditScriptForm>(page, form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Source Editor", new ActionURL(EditScriptAction.class, getContainer()));
            return root;
        }

        ScriptParser.Error validateScript(FlowScript script) throws SQLException
        {
            ScriptParser parser = new ScriptParser();
            parser.parse(script.getAnalysisScript());
            if (parser.getErrors() != null)
                return parser.getErrors()[0];
            return null;
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class NewProtocolAction extends SimpleViewAction<NewProtocolForm>
    {
        public ModelAndView getView(NewProtocolForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                ActionURL forward = createScript(form, errors);
                if (forward != null)
                    return HttpView.redirect(forward);
            }

            JspView<NewProtocolForm> page = FormPage.getView(ScriptController.class, form, errors, "newProtocol.jsp");
            HomeTemplate template = new HomeTemplate(getViewContext(), getContainer(), page);
            template.getModelBean().setFocusId("ff_name");
            return page;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("New Analysis Script", new ActionURL(NewProtocolAction.class, getContainer()));
            return root;
        }

        protected ActionURL createScript(NewProtocolForm form, BindException errors) throws Exception
        {
            if (form.ff_name == null || form.ff_name.length() == 0)
            {
                errors.reject(ERROR_MSG, "The name cannot be blank.");
                return null;
            }
            if (!isScriptNameUnique(getContainer(), form.ff_name))
            {
                errors.reject(ERROR_MSG, "The name '" + form.ff_name + "' is already in use.  Please choose a unique name.");
                return null;
            }

            ScriptDocument doc = ScriptDocument.Factory.newInstance();
            doc.addNewScript();

            FlowScript script = FlowScript.create(getUser(), getContainer(), form.ff_name, doc.toString());


            ActionURL forward = script.urlShow();
            putParam(forward, FlowParam.scriptId, script.getScriptId());
            return forward;
        }

    }

    protected boolean isScriptNameUnique(Container c, String name) throws Exception
    {
        String lsid = FlowScript.lsidForName(c, name);
        return ExperimentService.get().getExpData(lsid) == null;
    }

    public Page getPage(String name, EditScriptForm form) throws Exception
    {
        Page ret = (Page)getFlowPage(name);
        ret.setForm(form);
        return ret;
    }

    static public ScriptDocument ensureScript(FlowScript analysisScript) throws ServletException
    {
        try
        {
            return analysisScript.getAnalysisScriptDocument();
        }
        catch (Exception e)
        {
            ActionURL redirect = analysisScript.urlFor(Action.editScript);
            redirect.addParameter("checkSyntax", "1");
            HttpView.throwRedirect(redirect.toString());
            return null;
        }
    }

    static void setAttribute(XmlObject obj, String attribute, String value)
    {
        Element el = (Element) obj.getDomNode();
        value = StringUtils.trimToNull(value);
        if (value == null)
        {
            el.removeAttribute(attribute);
        }
        else
        {
            el.setAttribute(attribute, value);
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class EditAnalysisAction extends SimpleViewAction<AnalysisForm>
    {
        public ModelAndView getView(AnalysisForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                ActionURL forward = updateAnalysis(form, errors);
                if (forward != null)
                    return HttpView.redirect(forward);
            }

            return new JspView<AnalysisForm>(getPage("editAnalysis.jsp", form), form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Choose statistics and graphs", new ActionURL(EditAnalysisAction.class, getContainer()));
            return root;
        }

        protected ActionURL updateAnalysis(AnalysisForm form, BindException errors) throws Exception
        {
            try
            {
                Set<StatisticSpec> stats = new LinkedHashSet();
                StringTokenizer stStats = new StringTokenizer(StringUtils.trimToEmpty(form.statistics), "\n");
                while (stStats.hasMoreElements())
                {
                    String strStat = StringUtils.trimToNull(stStats.nextToken());
                    if (strStat != null)
                    {
                        stats.add(new StatisticSpec(strStat));
                    }
                }
                Set<GraphSpec> graphs = new LinkedHashSet();
                StringTokenizer stGraphs = new StringTokenizer(StringUtils.trimToEmpty(form.graphs), "\n");
                while (stGraphs.hasMoreElements())
                {
                    String strGraph = StringUtils.trimToNull(stGraphs.nextToken());
                    if (strGraph != null)
                    {
                        graphs.add(new GraphSpec(strGraph));
                    }
                }
                Set<SubsetSpec> subsets = new LinkedHashSet();
                StringTokenizer stSubsets = new StringTokenizer(StringUtils.trimToEmpty(form.subsets), "\n");
                while (stSubsets.hasMoreElements())
                {
                    String strSubset = StringUtils.trimToNull(stSubsets.nextToken());
                    if (strSubset != null)
                    {
                        subsets.add(SubsetSpec.fromString(strSubset));
                    }
                }
                ScriptDocument doc = form.analysisScript.getAnalysisScriptDocument();
                ScriptDef script = doc.getScript();
                AnalysisDef analysis = script.getAnalysis();
                if (analysis == null)
                {
                    analysis = script.addNewAnalysis();
                }
                while (analysis.getStatisticArray().length > 0)
                {
                    analysis.removeStatistic(0);
                }
                while (analysis.getGraphArray().length > 0)
                {
                    analysis.removeGraph(0);
                }
                while (analysis.getSubsetArray().length > 0)
                {
                    analysis.removeSubset(0);
                }
                for (StatisticSpec stat : stats)
                {
                    StatisticDef statDef = analysis.addNewStatistic();
                    statDef.setName(stat.getStatistic().toString());
                    if (stat.getSubset() != null)
                    {
                        statDef.setSubset(stat.getSubset().toString());
                    }
                    if (stat.getParameter() != null)
                        statDef.setParameter(stat.getParameter());
                }
                for (GraphSpec graph : graphs)
                {
                    addGraph(analysis, graph);
                }
                for (SubsetSpec subset : subsets)
                {
                    SubsetDef subsetDef = analysis.addNewSubset();
                    subsetDef.setSubset(subset.toString());
                }
                if (!safeSetAnalysisScript(form.analysisScript, doc.toString(), errors))
                    return null;
                return form.analysisScript.urlShow();
            }
            catch (FlowException e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                ExceptionUtil.logExceptionToMothership(form.getRequest(), e);
                return null;
            }
        }
    }

    public ActionURL urlFor(Enum action)
    {
        // Methods in ScriptController should use form.urlFor().
        throw new UnsupportedOperationException();
    }

    abstract static public class EditPage extends Page
    {
        public ScriptParser.Error scriptParseError;
    }

    abstract static public class Page<F extends EditScriptForm> extends BaseFlowController.FlowPage<ScriptController>
    {
        public F form;

        public String pageHeader(Action action)
        {
            return "";
        }

        public void setForm(F form)
        {
            this.form = form;
        }

        public F getForm()
        {
            return form;
        }

        public FlowScript getScript()
        {
            return form.analysisScript;
        }

        public FlowProtocolStep getStep()
        {
            return form.step;
        }

        public PopulationSet getAnalysis() throws Exception
        {
            return this.form.getAnalysis();
        }

        public String formAction(Action action)
        {
            return urlFor(action).toString();
        }

        public ActionURL urlFor(Action action)
        {
            return form.urlFor(action);
        }
    }

    static abstract public class UploadAnalysisPage extends Page<UploadAnalysisForm>
    {
        public String[] getGroupAnalysisNames()
        {
            if (form.workspaceObject == null)
                return new String[0];
            List<String> ret = new ArrayList();
            for (Analysis analysis : form.workspaceObject.getGroupAnalyses().values())
            {
                if (analysis.getPopulations().size() > 0)
                {
                    ret.add(analysis.getName());
                }
            }
            return ret.toArray(new String[0]);
        }

        public Map<String, String> getSampleAnalysisNames()
        {
            if (form.workspaceObject == null)
                return Collections.EMPTY_MAP;
            Map<String, String> ret = new LinkedHashMap();
            for (FlowJoWorkspace.SampleInfo sample : form.workspaceObject.getSamples())
            {
                Analysis analysis = form.workspaceObject.getSampleAnalysis(sample);
                if (analysis.getPopulations().size() > 0)
                {
                    ret.put(sample.getSampleId(), sample.getLabel());
                }
            }
            return ret;
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class UploadAnalysisAction extends SimpleViewAction<UploadAnalysisForm>
    {
        public ModelAndView getView(UploadAnalysisForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                Map<String, MultipartFile> files = getFileMap();
                MultipartFile file = files.get("workspaceFile");
                ActionURL forward = doUploadAnalysis(form, file, errors);
                if (forward != null)
                    return HttpView.redirect(forward);
            }

            UploadAnalysisPage page = (UploadAnalysisPage) getPage("uploadAnalysis.jsp", form);
            page.form = form;
            return new JspView<UploadAnalysisForm>(page, form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Upload FlowJo Analysis", new ActionURL(UploadAnalysisAction.class, getContainer()));
            return root;
        }

        protected ActionURL doUploadAnalysis(UploadAnalysisForm form, MultipartFile file, BindException errors) throws Exception
        {
            FlowJoWorkspace newWorkspace = handleWorkspaceUpload(file, errors);
            if (newWorkspace != null)
            {
                form.workspaceObject = newWorkspace;
            }
            if (form.workspaceObject == null)
            {
                errors.reject(ERROR_MSG, "No workspace was uploaded.");
                return null;
            }
            FlowJoWorkspace workspace = form.workspaceObject;
            String groupName = form.groupName;
            String sampleId= form.sampleId;
            Analysis analysis = null;
            if (groupName != null)
            {
                analysis = workspace.getGroupAnalyses().get(groupName);
                if (analysis == null)
                {
                    errors.reject(ERROR_MSG, "No group analysis with the name '" + groupName + "' could be found.");
                    return null;
                }
            }
            else if (sampleId != null)
            {
                FlowJoWorkspace.SampleInfo sample = workspace.getSample(sampleId);
                if (sample == null)
                {
                    errors.reject(ERROR_MSG, "Cannot find sample '" + sampleId);
                    return null;
                }
                analysis = workspace.getSampleAnalysis(sample);
                if (analysis == null)
                {
                    errors.reject(ERROR_MSG, "Cannot find analysis for sample " + sampleId);
                    return null;
                }
            }
            else
            {
                for (Analysis analysisTry : workspace.getGroupAnalyses().values())
                {
                    if (analysisTry.getPopulations().size() > 0)
                    {
                        if (analysis != null)
                        {
                            errors.reject(ERROR_MSG, "There is more than one group analysis in this workspace.  Please specify which one to use.");
                            return null;
                        }
                        analysis = analysisTry;
                    }
                }
                if (analysis == null)
                {
                    for (FlowJoWorkspace.SampleInfo sample : workspace.getSamples())
                    {
                        Analysis analysisTry = workspace.getSampleAnalysis(sample);
                        if (analysisTry == null)
                            continue;
                        if (analysisTry.getPopulations().size() > 0)
                        {
                            if (analysis != null)
                            {
                                errors.reject(ERROR_MSG, "There is more than one sample analysis in this workspace.  Please specify which one to use.");
                                return null;
                            }
                            analysis = analysisTry;
                        }
                    }
                }
            }
            if (analysis == null)
            {
                errors.reject(ERROR_MSG, "No analyses were found in this workspace.");
                return null;
            }
            FlowScript analysisScript = getScript();
            ScriptDocument doc = analysisScript.getAnalysisScriptDocument();
            AnalysisDef analysisElement = doc.getScript().getAnalysis();
            if (analysisElement == null)
            {
                analysisElement = doc.getScript().addNewAnalysis();
            }

            FlowAnalyzer.makeAnalysisDef(doc.getScript(), analysis, form.ff_statisticSet);
            if (!safeSetAnalysisScript(analysisScript, doc.toString(), errors))
                return null;
            return analysisScript.urlShow();
        }
    }

    protected FlowJoWorkspace handleWorkspaceUpload(MultipartFile file, BindException errors)
    {
        if (file != null && !file.isEmpty())
        {
            try
            {
                return FlowJoWorkspace.readWorkspace(file.getInputStream());
            }
            catch (Exception e)
            {
                errors.reject(ERROR_MSG, "Exception parsing workspace: " + e);
                return null;
            }
        }
        return null;
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class UploadCompensationCalculationAction extends SimpleViewAction<EditCompensationCalculationForm>
    {
        public ModelAndView getView(EditCompensationCalculationForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                throw new UnsupportedOperationException("should call post on EditCompensationCalculationAction");
            }
            return new JspView<EditCompensationCalculationForm>(getPage("uploadCompensationCalculation.jsp", form), form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Upload FlowJo Workspace compensation", new ActionURL(UploadCompensationCalculationAction.class, getContainer()));
            return root;
        }

    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class ChooseCompensationRunAction extends SimpleViewAction<EditCompensationCalculationForm>
    {
        public ModelAndView getView(EditCompensationCalculationForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                throw new UnsupportedOperationException("should call post on EditCompensationCalculationAction");
            }
            return new JspView<EditCompensationCalculationForm>(getPage("chooseCompensationRun.jsp", form), form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Choose run for compensation", new ActionURL(ChooseCompensationRunAction.class, getContainer()));
            return root;
        }
    }

    public GraphDef addGraph(AnalysisDef analysis, GraphSpec graph)
    {
        GraphDef graphDef = analysis.addNewGraph();
        if (graph.getSubset() != null)
        {
            graphDef.setSubset(graph.getSubset().toString());
        }
        graphDef.setXAxis(graph.getParameters()[0]);
        if (graph.getParameters().length > 1)
            graphDef.setYAxis(graph.getParameters()[1]);
        return graphDef;
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class EditCompensationCalculationAction extends SimpleViewAction<EditCompensationCalculationForm>
    {
        public ModelAndView getView(EditCompensationCalculationForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                ActionURL forward = doEditCompensationCalculation(form, errors);
                if (forward != null)
                    return HttpView.redirect(forward);
            }

            String pageName = form.workspace == null ? "showCompensationCalculation.jsp" : "editCompensationCalculation.jsp";
            CompensationCalculationPage page = (CompensationCalculationPage) getPage(pageName, form);
            return new JspView<EditCompensationCalculationForm>(page, form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Compensation Calculation Editor", new ActionURL(EditCompensationCalculationAction.class, getContainer()));
            return root;
        }

        protected FlowJoWorkspace handleCompWorkspaceUpload(EditCompensationCalculationForm form, BindException errors)
        {
            if (form.selectedRunId != 0)
            {
                FlowRun run = FlowRun.fromRunId(form.selectedRunId);
                if (run == null)
                {
                    errors.reject(ERROR_MSG, "Run '" + form.selectedRunId + "' does not exist");
                    return null;
                }

                try
                {
                    return new FlowRunWorkspace(form.analysisScript, form.step, run);
                }
                catch (IOException e)
                {
                    errors.reject(ERROR_MSG, "Error reading FCS files in run (" + run.getName() + "):\n" + e.getMessage());
                    return null;
                }
                catch (Exception e)
                {
                    errors.reject(ERROR_MSG, "Exception reading run:\n" + e);
                    ExceptionUtil.logExceptionToMothership(form.getRequest(), e);
                    return null;
                }
            }
            MultipartFile file = getFileMap().get("workspaceFile");
            return handleWorkspaceUpload(file, errors);
        }

        protected ActionURL doEditCompensationCalculation(EditCompensationCalculationForm form, BindException errors) throws Exception
        {
            FlowJoWorkspace workspace = handleCompWorkspaceUpload(form, errors);
            if (workspace != null)
            {
                form.initSettings();
                form.setWorkspace(workspace);
                return null;
            }
            if (form.workspace == null)
                return null;
            workspace = form.workspace;
            Map<String, FlowJoWorkspace.CompensationChannelData> dataMap = new HashMap();
            for (int i = 0; i < form.parameters.length; i ++)
            {
                String parameter = form.parameters[i];
                FlowJoWorkspace.CompensationChannelData cd = new FlowJoWorkspace.CompensationChannelData();
                cd.positiveKeywordName = StringUtils.trimToNull(form.positiveKeywordName[i]);
                cd.negativeKeywordName = StringUtils.trimToNull(form.negativeKeywordName[i]);
                if (cd.positiveKeywordName == null)
                {
                    continue;
                }
                cd.positiveKeywordValue = StringUtils.trimToNull(form.positiveKeywordValue[i]);
                cd.positiveSubset = StringUtils.trimToNull(form.positiveSubset[i]);
                cd.negativeKeywordValue = StringUtils.trimToNull(form.negativeKeywordValue[i]);
                cd.negativeSubset = StringUtils.trimToNull(form.negativeSubset[i]);
                dataMap.put(parameter, cd);
            }
            List<String> errorslist = new ArrayList();
            CompensationCalculation calc = workspace.makeCompensationCalculation(dataMap, form.selectGroupName, errorslist);
            if (errorslist.size() > 0)
            {
                for (String error : errorslist)
                {
                    errors.reject(ERROR_MSG, error);
                }
                return null;
            }

            ScriptDocument doc = form.analysisDocument;
            if (!updateSettingsFilter(form, doc))
                return null;

            FlowAnalyzer.makeCompensationCalculationDef(doc, calc);
            if (!safeSetAnalysisScript(form.analysisScript, doc.toString(), errors))
                return null;
            return form.urlFor(Action.editCompensationCalculation);
        }
    }

    protected boolean safeSetAnalysisScript(FlowScript script, String str, BindException errors)
    {
        int runCount = script.getRunCount();
        if (runCount != 0)
        {
            errors.reject(ERROR_MSG, "This analysis script cannot be edited because it has been used in the analysis of " + runCount + " runs.");
            return false;
        }
        try
        {
            script.setAnalysisScript(getUser(), str);
        }
        catch (SQLException e)
        {
            errors.reject(ERROR_MSG, "An exception occurred: " + e);
            _log.error("Error", e);
            return false;
        }
        return true;
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class GraphImageAction extends SimpleViewAction<GraphForm>
    {
        public ModelAndView getView(GraphForm form, BindException errors) throws Exception
        {
            GateEditorServiceImpl service = new GateEditorServiceImpl(getViewContext());
            GWTGraphOptions options = form.getGraphOptions();
            PlotInfo info = service.getPlotInfo(options);
            if (null == info)
                return null;
            BufferedImage image = info.getImage();
            Gate gate = gateFromPoints(form.xAxis, form.yAxis, form.ptX, form.ptY, form.open);
            image = addSelection(image, info, gate, !form.open, true, form.xAxis, form.yAxis);
            SubsetSpec subset = SubsetSpec.fromString(form.subset);
            SubsetSpec parent = subset.getParent();
            PopulationSet popset;
            if (parent != null)
            {
                popset = form.getPopulations().get(parent);
            }
            else
            {
                popset = form.getAnalysis();
            }
            if (popset != null && !StringUtils.isEmpty(form.yAxis))
            {
                for (Population childPop : popset.getPopulations())
                {
                    if (childPop.getGates().size() != 1)
                        continue;
                    if (!(childPop.getGates().get(0) instanceof RegionGate))
                        continue;
                    RegionGate regionGate = (RegionGate) childPop.getGates().get(0);

                    // PolygonGates are editable, skip if it is being edited
                    if (childPop.getName().equals(subset.getSubset()) && gate instanceof PolygonGate)
                        continue;

                    if (StringUtils.equals(regionGate.getXAxis(), form.xAxis) && (regionGate.getYAxis()==null||StringUtils.equals(regionGate.getYAxis(), form.yAxis)))
                    {
                        // nothing to do
                    }
                    else if (StringUtils.equals(regionGate.getXAxis(), form.yAxis) && (regionGate.getYAxis()==null||StringUtils.equals(regionGate.getYAxis(), form.xAxis)))
                    {
                        if (regionGate instanceof PolygonGate)
                        {
                            // XXX: negate
                            Polygon newPoly = new Polygon(((PolygonGate)regionGate).getPolygon().Y, ((PolygonGate)regionGate).getPolygon().X);
                            regionGate = new PolygonGate(regionGate.getYAxis(), regionGate.getXAxis(), newPoly);
                        }
                    }
                    else
                    {
                        // doesn't match
                        continue;
                    }
                    image = addSelection(image, info, regionGate, true, false, form.xAxis, form.yAxis);
                }
            }
            streamImage(image);
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }

        protected void streamImage(BufferedImage image) throws Exception
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            PageFlowUtil.streamFileBytes(getViewContext().getResponse(), "graph.png", baos.toByteArray(), false);
        }

        protected Gate gateFromPoints(String xAxis, String yAxis, double[] arrX, double[] arrY, boolean unfinishedGate)
        {
            if (arrX == null || arrX.length < 2)
            {
                return null;
            }
            if (StringUtils.isEmpty(yAxis) || arrX.length == 2 && !unfinishedGate)
            {
                double min = Math.min(arrX[0], arrX[1]);
                double max = Math.max(arrX[0], arrX[1]);
                return new IntervalGate(xAxis, min, max);
            }
            // UNDONE: negate the gate
            PolygonGate poly = new PolygonGate(xAxis, yAxis, new Polygon(arrX, arrY));
            return poly;
        }

        protected BufferedImage addSelection(BufferedImage imageIn, PlotInfo info, Gate gate, boolean closePoly, boolean primaryGate, String xAxis, String yAxis)
        {
            if (gate == null)
                return imageIn;
            BufferedImage image = new BufferedImage(imageIn.getWidth(), imageIn.getHeight(), imageIn.getType());
            imageIn.copyData(image.getRaster());
            Graphics2D g = image.createGraphics();
            g.setColor(primaryGate ? Color.BLACK : Color.GRAY);
            if (gate instanceof RegionGate)
            {
                RegionGate regionGate = (RegionGate) gate;
                org.labkey.flow.analysis.model.Polygon polygon;

                if (regionGate instanceof PolygonGate)
                {
                    polygon = ((PolygonGate)regionGate).getPolygon();
                }
                else
                {
                    java.util.List<org.labkey.flow.analysis.model.Polygon> list = new ArrayList<org.labkey.flow.analysis.model.Polygon>();
                    gate.getPolygons(list, xAxis, yAxis);
                    if (list.size() != 1)
                        return imageIn;
                    polygon = list.get(0);
                }
                Point[] pts = new Point[polygon.X.length];
                for (int i = 0; i < pts.length; i ++)
                {
                    pts[i] = info.toScreenCoordinates(new Point2D.Double(polygon.X[i], polygon.Y[i]));
                }

                for (int i = 0; i < polygon.X.length - 1; i ++)
                {
                    g.drawLine(pts[i].x, pts[i].y, pts[i + 1].x, pts[i + 1].y);
                }
                if (closePoly)
                {
                    g.drawLine(pts[pts.length - 1].x, pts[pts.length- 1].y, pts[0].x, pts[0].y);
                }
            }
            if (primaryGate)
            {
                String strFreq = info.getFrequency(gate);
                g.drawString(strFreq, 0, image.getHeight() - g.getFontMetrics().getDescent());
            }
            g.dispose();

            return image;
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class EditGateTreeAction extends SimpleViewAction<EditGateTreeForm>
    {
        public ModelAndView getView(EditGateTreeForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                Map<SubsetSpec, String> newNames = new HashMap();
                for (int i = 0; i < form.populationNames.length; i ++)
                {
                    newNames.put(form.subsets[i], form.populationNames[i]);
                }
                ScriptComponent oldAnalysis = form.getAnalysis();
                ScriptComponent newAnalysis = form.getAnalysis();
                // form.getAnalysis should create a new copy each time it's called.
                assert oldAnalysis != newAnalysis;
                newAnalysis.getPopulations().clear();
                boolean fSuccess = true;
                for (Population pop : oldAnalysis.getPopulations())
                {
                    fSuccess = renamePopulations(pop, newAnalysis, null, newNames, errors);
                }
                if (fSuccess)
                {
                    ScriptDocument doc = form.analysisScript.getAnalysisScriptDocument();
                    fSuccess = saveAnalysisOrComp(form.analysisScript, doc, newAnalysis, errors);
                }
                if (fSuccess)
                {
                    return HttpView.redirect(form.urlFor(Action.editGateTree));
                }
            }

            return new JspView<EditGateTreeForm>(getPage("editGateTree.jsp", form), form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Population Names Editor", new ActionURL(EditGateTreeAction.class, getContainer()));
            return root;
        }

        String checkValidPopulationName(String name)
        {
            if (StringUtils.indexOfAny(name, "/<'") >= 0)
            {
                return "Population names cannot contain the characters / ' \" or <";
            }
            return null;
        }

        private boolean renamePopulations(Population pop, PopulationSet newParent, SubsetSpec subsetParent, Map<SubsetSpec, String> newNames, BindException errors)
        {
            SubsetSpec subset = new SubsetSpec(subsetParent, pop.getName());
            String newName = newNames.get(subset);
            boolean fSuccess = true;
            if (StringUtils.isEmpty(newName))
            {
                // deleted
                return fSuccess;
            }
            Population newPop = new Population();
            String nameError = checkValidPopulationName(newName);
            if (nameError != null)
            {
                errors.reject(ERROR_MSG, nameError);
                fSuccess = false;
            }
            newPop.setName(newName);
            newPop.getGates().addAll(pop.getGates());
            if (newParent.getPopulation(newName) != null)
            {
                errors.reject(ERROR_MSG, "There are two populations called '" + new SubsetSpec(subsetParent, newName) + "'");
                fSuccess = false;
            }

            newParent.addPopulation(newPop);

            for (Population child : pop.getPopulations())
            {
                fSuccess = renamePopulations(child, newPop, subset, newNames, errors) && fSuccess;
            }
            return fSuccess;
        }

        protected boolean saveAnalysisOrComp(FlowScript analysisScript, ScriptDocument doc, ScriptComponent popset, BindException errors) throws SQLException
        {
            if (popset instanceof CompensationCalculation)
            {
                FlowAnalyzer.makeCompensationCalculationDef(doc, (CompensationCalculation) popset);
            }
            else
            {
                FlowAnalyzer.makeAnalysisDef(doc.getScript(), (Analysis) popset, null);
            }
            return safeSetAnalysisScript(analysisScript, doc.toString(), errors);
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class CopyAction extends SimpleViewAction<CopyProtocolForm>
    {
        String scriptName;

        public ModelAndView getView(CopyProtocolForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                ActionURL forward = doCopy(form, errors);
                if (forward != null)
                    return HttpView.redirect(forward);
            }

            scriptName = form.analysisScript.getName();
            return new JspView<CopyProtocolForm>(getPage("copy.jsp", form), form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Make a copy of '" + scriptName + "'", new ActionURL(CopyAction.class, getContainer()));
            return root;
        }

        protected void addChild(XmlObject parent, XmlObject child)
        {
            if (child == null)
                return;
            parent.getDomNode().appendChild(parent.getDomNode().getOwnerDocument().importNode(child.getDomNode(), true));
        }

        protected ActionURL doCopy(CopyProtocolForm form, BindException errors) throws Exception
        {
            if (StringUtils.isEmpty(form.name))
            {
                errors.reject(ERROR_MSG, "The name cannot be blank.");
                return null;
            }
            if (!isScriptNameUnique(getContainer(), form.name))
            {
                errors.reject(ERROR_MSG, "There is already a protocol named '" + form.name + "'");
                return null;
            }
            ScriptDef src = form.analysisScript.getAnalysisScriptDocument().getScript();
            ScriptDocument doc = ScriptDocument.Factory.newInstance();
            ScriptDef script = doc.addNewScript();
            addChild(script, src.getSettings());
            if (form.copyCompensationCalculation)
            {
                addChild(script, src.getCompensationCalculation());
            }
            if (form.copyAnalysis)
            {
                addChild(script, src.getAnalysis());
            }
            FlowScript newAnalysisScript = FlowScript.create(getUser(), getContainer(), form.name, doc.toString());
            return newAnalysisScript.urlShow();
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class EditPropertiesAction extends SimpleViewAction<EditPropertiesForm>
    {
        public ModelAndView getView(EditPropertiesForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                ExpData protocol = form.analysisScript.getExpObject();
                protocol.setComment(getUser(), form.ff_description);
                return HttpView.redirect(form.urlFor(Action.begin));
            }
            return new JspView<EditPropertiesForm>(getPage("editProperties.jsp", form), form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Edit Properties", new ActionURL(EditPropertiesAction.class, getContainer()));
            return root;
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class EditSettingsAction extends SimpleViewAction<EditSettingsForm>
    {
        public ModelAndView getView(EditSettingsForm form, BindException errors) throws Exception
        {
            if (isPost() && form.canEdit())
            {
                ScriptDocument doc = form.analysisDocument;
                if (updateSettingsMinValues(form, doc, errors) &&
                    updateSettingsFilter(form, doc) &&
                    safeSetAnalysisScript(form.analysisScript, doc.toString(), errors))
                {
                    return HttpView.redirect(form.urlFor(Action.begin));
                }
            }

            return new JspView<EditSettingsForm>(getPage("editSettings.jsp", form), form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }

        protected boolean updateSettingsMinValues(EditSettingsForm form, ScriptDocument doc, BindException errors)
        {
            boolean success = true;
            SettingsDef settingsDef = doc.getScript().getSettings();
            if (settingsDef == null)
                settingsDef = doc.getScript().addNewSettings();

            while (settingsDef.sizeOfParameterArray() > 0)
                settingsDef.removeParameter(0);

            for (int i = 0; i < form.ff_parameter.length; i++)
            {
                String value = form.ff_minValue[i];
                if (value != null)
                {
                    double val;
                    try
                    {
                        val = Double.valueOf(value);
                    }
                    catch (Exception e)
                    {
                        errors.reject(ERROR_MSG, "Error converting '" + value + "' to a number.");
                        success = false;
                        continue;
                    }
                    String name = form.ff_parameter[i];
                    ParameterDef param = settingsDef.addNewParameter();
                    param.setName(name);
                    param.setMinValue(val);
                }
            }

            return success;
        }

    }

    protected boolean updateSettingsFilter(EditSettingsForm form, ScriptDocument doc) throws Exception
    {
        boolean success = true;
        SettingsDef settingsDef = doc.getScript().getSettings();
        if (settingsDef == null)
            settingsDef = doc.getScript().addNewSettings();

        FiltersDef filtersDef = null;
        XmlCursor cur = null;
        try
        {
            filtersDef = settingsDef.getFilters();
            if (filtersDef != null)
            {
                cur = filtersDef.newCursor();
                cur.removeXmlContents();
            }
        }
        finally
        {
            if (cur != null) cur.dispose();
        }

        for (int i = 0; i < form.ff_filter_field.length; i++)
        {
            FieldKey field = form.ff_filter_field[i];
            String op = form.ff_filter_op[i];
            String value = form.ff_filter_value[i];
            if (field != null && op != null)
            {
                if (filtersDef == null)
                    filtersDef = settingsDef.addNewFilters();
                FilterDef filter = filtersDef.addNewFilter();
                filter.setField(field.toString());
                filter.setOp(OpDef.Enum.forString(op));
                if (value != null)
                    filter.setValue(value);
            }
        }

        return success;
    }

    @RequiresPermissionClass(DeletePermission.class)
    public class DeleteAction extends SimpleViewAction<EditScriptForm>
    {
        public ModelAndView getView(EditScriptForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                ExpData protocol = form.analysisScript.getExpObject();
                protocol.delete(getUser());
                return HttpView.redirect(PageFlowUtil.urlFor(FlowController.Action.begin, getContainer()));
            }
            return new JspView<EditScriptForm>(getPage("delete.jsp", form), form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Confirm Delete", new ActionURL(DeleteAction.class, getContainer()));
            return root;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class GateEditorAction extends SimpleViewAction<GateEditorForm>
    {
        FlowObject object;

        public ModelAndView getView(GateEditorForm form, BindException errors) throws Exception
        {
            object = form.getFlowObject();
            Map<String, String> props = new HashMap();
            int scriptId = form.getScriptId();
            if (scriptId != 0)
            {
                props.put("scriptId", Integer.toString(scriptId));
            }
            int runId = form.getRunId();
            if (runId == 0)
            {
                FlowRun run = FlowRun.findMostRecent(getContainer(), FlowProtocolStep.keywords);
                if (run != null)
                    runId = run.getRunId();
            }
            if (runId != 0)
            {
                props.put("runId", Integer.toString(runId));
            }
            props.put("editingMode", form.getEditingMode().toString());
            if (form.getSubset() != null)
            {
                props.put("subset", form.getSubset());
            }
            return new GWTView("org.labkey.flow.gateeditor.GateEditor", props);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(root, object, "Gate Editor", Action.gateEditor);
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class GateEditorService extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            GateEditorServiceImpl service = new GateEditorServiceImpl(getViewContext());
            service.doPost(getRequest(), getViewContext().getResponse());
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }
}

