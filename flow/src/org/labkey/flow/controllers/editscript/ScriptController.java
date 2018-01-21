/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.fhcrc.cpas.flow.script.xml.AnalysisDef;
import org.fhcrc.cpas.flow.script.xml.FilterDef;
import org.fhcrc.cpas.flow.script.xml.FiltersDef;
import org.fhcrc.cpas.flow.script.xml.GraphDef;
import org.fhcrc.cpas.flow.script.xml.OpDef;
import org.fhcrc.cpas.flow.script.xml.ParameterDef;
import org.fhcrc.cpas.flow.script.xml.ScriptDef;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.fhcrc.cpas.flow.script.xml.SettingsDef;
import org.fhcrc.cpas.flow.script.xml.StatisticDef;
import org.fhcrc.cpas.flow.script.xml.SubsetDef;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.flow.ScriptParser;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.CompensationCalculation;
import org.labkey.flow.analysis.model.FlowException;
import org.labkey.flow.analysis.model.Population;
import org.labkey.flow.analysis.model.PopulationName;
import org.labkey.flow.analysis.model.PopulationSet;
import org.labkey.flow.analysis.model.ScriptComponent;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.analysis.model.WorkspaceCompensation;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.ScriptAnalyzer;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowRunWorkspace;
import org.labkey.flow.data.FlowScript;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * User: kevink
 * Date: Nov 25, 2008 5:27:35 PM
 */
public class ScriptController extends BaseFlowController
{
    private static Logger _log = Logger.getLogger(ScriptController.class);

    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(ScriptController.class);

    public ScriptController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction<EditScriptForm>
    {
        public ModelAndView getView(EditScriptForm form, BindException errors) throws Exception
        {
            FlowScript script = form.getFlowScript();
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

    @RequiresPermission(ReadPermission.class)
    public class DownloadAction extends SimpleViewAction<EditScriptForm>
    {
        public ModelAndView getView(EditScriptForm form, BindException errors) throws Exception
        {
            FlowScript script = form.getFlowScript();
            if (script == null)
                throw new NotFoundException("Analysis script not found");

            String filename = script.getName();
            if (!filename.endsWith(".xml"))
                filename = filename + ".xml";

            byte[] bytes = script.getAnalysisScript().getBytes(StringUtilsLabKey.DEFAULT_CHARSET);
            PageFlowUtil.streamFileBytes(getViewContext().getResponse(), filename, bytes, true);
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class EditScriptAction extends FlowAction<EditScriptForm>
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
                        ActionURL forward = form.urlFor(ScriptController.EditScriptAction.class);
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
            return new JspView<>(page, form, errors);
        }

        public String getPageTitle()
        {
            return "Source Editor";
        }

        ScriptParser.Error validateScript(FlowScript script)
        {
            ScriptParser parser = new ScriptParser();
            parser.parse(script.getAnalysisScript());
            if (parser.getErrors() != null)
                return parser.getErrors()[0];
            return null;
        }
    }

    @RequiresPermission(UpdatePermission.class)
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

            // TODO: Ensure this works and figure out a new strategy for setting focus element
            JspView<NewProtocolForm> page = FormPage.getView(ScriptController.class, form, errors, "newProtocol.jsp");
//            HomeTemplate template = new HomeTemplate(getViewContext(), getContainer(), page);
//            template.getModelBean().setFocusId("ff_name");
            return page;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("New Analysis Script", new ActionURL(NewProtocolAction.class, getContainer()));
            return root;
        }

        protected ActionURL createScript(NewProtocolForm form, BindException errors) throws SQLException
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

    protected boolean isScriptNameUnique(Container c, String name)
    {
        String lsid = FlowScript.lsidForName(c, name);
        return ExperimentService.get().getExpData(lsid) == null;
    }

    public Page getPage(String name, EditScriptForm form)
    {
        Page ret = (Page)getFlowPage(name);
        ret.setForm(form);
        return ret;
    }

    @RequiresPermission(UpdatePermission.class)
    public class EditAnalysisAction extends FlowAction<AnalysisForm>
    {
        public ModelAndView getView(AnalysisForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                ActionURL forward = updateAnalysis(form, errors);
                if (forward != null)
                    return HttpView.redirect(forward);
            }

            return new JspView<>(getPage("editAnalysis.jsp", form), form, errors);
        }

        public String getPageTitle()
        {
            return "Choose statistics and graphs";
        }

        protected ActionURL updateAnalysis(AnalysisForm form, BindException errors) throws Exception
        {
            try
            {
                Set<StatisticSpec> stats = new LinkedHashSet<>();
                StringTokenizer stStats = new StringTokenizer(StringUtils.trimToEmpty(form.statistics), "\n");
                while (stStats.hasMoreElements())
                {
                    String strStat = StringUtils.trimToNull(stStats.nextToken());
                    if (strStat != null)
                    {
                        stats.add(new StatisticSpec(strStat));
                    }
                }
                Set<GraphSpec> graphs = new LinkedHashSet<>();
                StringTokenizer stGraphs = new StringTokenizer(StringUtils.trimToEmpty(form.graphs), "\n");
                while (stGraphs.hasMoreElements())
                {
                    String strGraph = StringUtils.trimToNull(stGraphs.nextToken());
                    if (strGraph != null)
                    {
                        graphs.add(new GraphSpec(strGraph));
                    }
                }
                Set<SubsetSpec> subsets = new LinkedHashSet<>();
                StringTokenizer stSubsets = new StringTokenizer(StringUtils.trimToEmpty(form.subsets), "\n");
                while (stSubsets.hasMoreElements())
                {
                    String strSubset = StringUtils.trimToNull(stSubsets.nextToken());
                    if (strSubset != null)
                    {
                        subsets.add(SubsetSpec.fromEscapedString(strSubset));
                    }
                }
                ScriptDocument doc = form.getFlowScript().getAnalysisScriptDocument();
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
                if (!safeSetAnalysisScript(form.getFlowScript(), doc.toString(), errors))
                    return null;
                return form.urlShow();
            }
            catch (FlowException e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                ExceptionUtil.logExceptionToMothership(form.getRequest(), e);
                return null;
            }
        }
    }

    abstract static public class EditPage extends Page
    {
        public ScriptParser.Error scriptParseError;
    }

    abstract static public class Page<F extends EditScriptForm> extends BaseFlowController.FlowPage<ScriptController>
    {
        public F form;

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
            return form.getFlowScript();
        }

        public String formAction(Class<? extends Controller> actionClass)
        {
            return form.urlFor(actionClass).toString();
        }
    }

    static abstract public class UploadAnalysisPage extends Page<UploadAnalysisForm>
    {
        public PopulationName[] getGroupAnalysisNames()
        {
            if (form._workspaceObject == null)
                return new PopulationName[0];
            List<PopulationName> ret = new ArrayList<>();
            for (Analysis analysis : form._workspaceObject.getGroupAnalyses().values())
            {
                if (analysis.getPopulations().size() > 0)
                {
                    ret.add(analysis.getName());
                }
            }
            return ret.toArray(new PopulationName[ret.size()]);
        }

        public Map<String, String> getSampleAnalysisNames()
        {
            if (form._workspaceObject == null)
                return Collections.emptyMap();

            Map<String, String> ret = new LinkedHashMap<>();

            for (Workspace.SampleInfo sample : form._workspaceObject.getSamplesComplete())
            {
                Analysis analysis = form._workspaceObject.getSampleAnalysis(sample);
                if (analysis.getPopulations().size() > 0)
                {
                    ret.put(sample.getSampleId(), sample.getLabel());
                }
            }
            return ret;
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class UploadAnalysisAction extends FlowAction<UploadAnalysisForm>
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
            return new JspView<>(page, form, errors);
        }

        public String getPageTitle()
        {
            return "Upload FlowJo Analysis";
        }

        protected ActionURL doUploadAnalysis(UploadAnalysisForm form, MultipartFile file, BindException errors) throws Exception
        {
            Workspace newWorkspace = handleWorkspaceUpload(file, errors);
            if (newWorkspace != null)
            {
                form._workspaceObject = newWorkspace;
            }
            if (form._workspaceObject == null)
            {
                errors.reject(ERROR_MSG, "No workspace was uploaded.");
                return null;
            }
            Workspace workspace = form._workspaceObject;
            PopulationName groupName = PopulationName.fromString(form.groupName);
            String sampleId= form.sampleId;

            FlowScript analysisScript = getScript();
            ScriptDocument doc = analysisScript.getAnalysisScriptDocument();
            AnalysisDef analysisElement = doc.getScript().getAnalysis();
            if (analysisElement == null)
            {
                analysisElement = doc.getScript().addNewAnalysis();
            }

            try
            {
                ScriptAnalyzer.makeAnalysisDef(doc.getScript(), workspace, groupName, sampleId, form.ff_statisticSet);
            }
            catch (Exception e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                return null;
            }
            if (!safeSetAnalysisScript(analysisScript, doc.toString(), errors))
                return null;
            return analysisScript.urlShow();
        }
    }

    protected Workspace handleWorkspaceUpload(MultipartFile file, BindException errors)
    {
        if (file != null && !file.isEmpty())
        {
            try
            {
                return Workspace.readWorkspace(file.getName(), null, file.getInputStream());
            }
            catch (Exception e)
            {
                errors.reject(ERROR_MSG, "Exception parsing workspace: " + e);
                return null;
            }
        }
        return null;
    }

    @RequiresPermission(UpdatePermission.class)
    public class UploadCompensationCalculationAction extends FlowAction<EditCompensationCalculationForm>
    {
        public ModelAndView getView(EditCompensationCalculationForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                throw new UnsupportedOperationException("should call post on EditCompensationCalculationAction");
            }
            return new JspView<>(getPage("uploadCompensationCalculation.jsp", form), form, errors);
        }

        public String getPageTitle()
        {
            return "Upload FlowJo Workspace compensation";
        }

    }

    @RequiresPermission(UpdatePermission.class)
    public class ChooseCompensationRunAction extends FlowAction<EditCompensationCalculationForm>
    {
        public ModelAndView getView(EditCompensationCalculationForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                throw new UnsupportedOperationException("should call post on EditCompensationCalculationAction");
            }
            return new JspView<>(getPage("chooseCompensationRun.jsp", form), form, errors);
        }

        @Override
        protected String getPageTitle()
        {
            return "Choose run for compensation";
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

    @RequiresPermission(UpdatePermission.class)
    public class EditCompensationCalculationAction extends FlowAction<EditCompensationCalculationForm>
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
            return new JspView<>(page, form, errors);
        }

        public String getPageTitle()
        {
            return "Compensation Calculation Editor";
        }

        protected Workspace handleCompWorkspaceUpload(EditCompensationCalculationForm form, BindException errors)
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
                    return new FlowRunWorkspace(form.getFlowScript(), form.step, run);
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

        protected ActionURL doEditCompensationCalculation(EditCompensationCalculationForm form, BindException errors)
        {
            Workspace workspace = handleCompWorkspaceUpload(form, errors);
            if (workspace != null)
            {
                form.initSettings();
                form.setWorkspace(workspace);
                return null;
            }
            if (form.workspace == null)
                return null;
            workspace = form.workspace;
            Map<String, Workspace.CompensationChannelData> dataMap = new HashMap<>();
            for (int i = 0; i < form.parameters.length; i ++)
            {
                String parameter = form.parameters[i];
                Workspace.CompensationChannelData cd = new Workspace.CompensationChannelData();
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
            List<String> errorslist = new ArrayList<>();
            PopulationName groupName = null;
            if (StringUtils.isNotEmpty(form.selectGroupName))
                groupName = PopulationName.fromString(form.selectGroupName);
            WorkspaceCompensation calculator = new WorkspaceCompensation(workspace);
            CompensationCalculation calc = calculator.makeCompensationCalculation(dataMap, groupName, errorslist);
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

            ScriptAnalyzer.makeCompensationCalculationDef(doc, calc);
            if (!safeSetAnalysisScript(form.getFlowScript(), doc.toString(), errors))
                return null;
            return form.urlFor(EditCompensationCalculationAction.class);
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

    @RequiresPermission(UpdatePermission.class)
    public class EditGateTreeAction extends FlowAction<EditGateTreeForm>
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
                    ScriptDocument doc = form.getFlowScript().getAnalysisScriptDocument();
                    fSuccess = saveAnalysisOrComp(form.getFlowScript(), doc, newAnalysis, errors);
                }
                if (fSuccess)
                {
                    return HttpView.redirect(form.urlFor(EditGateTreeAction.class));
                }
            }

            return new JspView<>(getPage("editGateTree.jsp", form), form, errors);
        }

        public String getPageTitle()
        {
            return "Population Names Editor";
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
            PopulationName name = PopulationName.fromString(newName);
            newPop.setName(name);
            newPop.getGates().addAll(pop.getGates());
            if (newParent.getPopulation(name) != null)
            {
                errors.reject(ERROR_MSG, "There are two populations called '" + new SubsetSpec(subsetParent, name) + "'");
                fSuccess = false;
            }

            newParent.addPopulation(newPop);

            for (Population child : pop.getPopulations())
            {
                fSuccess = renamePopulations(child, newPop, subset, newNames, errors) && fSuccess;
            }
            return fSuccess;
        }

        protected boolean saveAnalysisOrComp(FlowScript analysisScript, ScriptDocument doc, ScriptComponent popset, BindException errors)
        {
            if (popset instanceof CompensationCalculation)
            {
                ScriptAnalyzer.makeCompensationCalculationDef(doc, (CompensationCalculation) popset);
            }
            else
            {
                ScriptAnalyzer.makeAnalysisDef(doc.getScript(), (Analysis) popset, null);
            }
            return safeSetAnalysisScript(analysisScript, doc.toString(), errors);
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class CopyAction extends FlowAction<CopyProtocolForm>
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

            scriptName = form.getFlowScript().getName();
            return new JspView<>(getPage("copy.jsp", form), form, errors);
        }

        public String getPageTitle()
        {
            return "Make a copy of '" + scriptName + "'";
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
            ScriptDef src = form.getFlowScript().getAnalysisScriptDocument().getScript();
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

    @RequiresPermission(UpdatePermission.class)
    public class EditPropertiesAction extends FlowAction<EditPropertiesForm>
    {
        public ModelAndView getView(EditPropertiesForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                ExpData protocol = form.getFlowScript().getExpObject();
                protocol.setComment(getUser(), form.ff_description);
                return HttpView.redirect(form.urlFor(BeginAction.class));
            }
            return new JspView<>(getPage("editProperties.jsp", form), form, errors);
        }

        public String getPageTitle()
        {
            return "Edit Properties";
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class EditSettingsAction extends FlowAction<EditSettingsForm>
    {
        public ModelAndView getView(EditSettingsForm form, BindException errors) throws Exception
        {
            if (isPost() && form.canEdit())
            {
                ScriptDocument doc = form.analysisDocument;
                if (updateSettingsMinValues(form, doc, errors) &&
                    updateSettingsFilter(form, doc) &&
                    safeSetAnalysisScript(form.getFlowScript(), doc.toString(), errors))
                {
                    return HttpView.redirect(form.urlFor(BeginAction.class));
                }
            }

            return new JspView<>(getPage("editSettings.jsp", form), form, errors);
        }

        public String getPageTitle()
        {
            return "Edit Settings";
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

    protected boolean updateSettingsFilter(EditSettingsForm form, ScriptDocument doc)
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

    @RequiresPermission(DeletePermission.class)
    public class DeleteAction extends FlowAction<EditScriptForm>
    {
        public ModelAndView getView(EditScriptForm form, BindException errors) throws Exception
        {
            if (isPost())
            {
                ExpData protocol = form.getFlowScript().getExpObject();
                protocol.delete(getUser());
                return HttpView.redirect(new ActionURL(FlowController.BeginAction.class, getContainer()));
            }
            return new JspView<>(getPage("delete.jsp", form), form, errors);
        }

        public String getPageTitle()
        {
            return "Confirm Delete";
        }
    }
}

