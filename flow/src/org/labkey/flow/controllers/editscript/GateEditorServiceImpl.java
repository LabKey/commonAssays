/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.ACL;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.flow.gateeditor.client.model.*;
import org.labkey.flow.gateeditor.client.model.GWTGraphOptions;
import org.labkey.flow.gateeditor.client.model.GWTGraphInfo;
import org.labkey.flow.gateeditor.client.GateEditorService;
import org.labkey.flow.gateeditor.client.GWTGraphException;
import org.labkey.flow.data.*;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.PlotInfo;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.model.*;
import org.labkey.flow.analysis.chart.FlowLogarithmicAxis;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.FlowPreference;
import org.fhcrc.cpas.flow.script.xml.*;
import org.jfree.chart.axis.ValueAxis;
import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.List;
import java.sql.SQLException;
import java.awt.*;


public class GateEditorServiceImpl extends BaseRemoteService implements GateEditorService
{
    static private final Logger _log = Logger.getLogger(GateEditorServiceImpl.class);
    protected HttpServletRequest _request;
    public GateEditorServiceImpl(ViewContext context)
    {
        super(context);
        _request = context.getRequest();
    }

    public GWTScript getScript(int id)
    {
        FlowScript script = FlowScript.fromScriptId(id);
        if (script == null)
        {
            return null;
        }
        try
        {
            return makeGWTScript(script);
        }
        catch (Exception e)
        {
            _log.error("Error", e);
            throw UnexpectedException.wrap(e);
        }
    }

    protected GWTScript makeGWTScript(FlowScript flowScript) throws Exception
    {
        ScriptDocument scriptDoc = flowScript.getAnalysisScriptDocument();
        ScriptDef script = scriptDoc.getScript();
        GWTScript ret = new GWTScript();
        ret.setSettings(makeGWTSettings(script.getSettings()));
        ret.setCompensationCalculation(makeCompensationCalculation(script.getCompensationCalculation()));
        ret.setAnalysis(makeAnalysis(script.getAnalysis()));
        ret.setScriptId(flowScript.getScriptId());
        ret.setName(flowScript.getName());
        return ret;
    }

    protected GWTSettings makeGWTSettings(SettingsDef settings)
    {
        if (settings == null)
            return null;
        GWTSettings ret = new GWTSettings();
        List<GWTParameterInfo> params = new ArrayList();
        for (ParameterDef param : settings.getParameterArray())
        {
            GWTParameterInfo gwtParam = new GWTParameterInfo();
            gwtParam.setName(param.getName());
            if (param.isSetMinValue())
            {
                gwtParam.setMinValue(param.getMinValue());
            }
        }
        ret.setParameters(params.toArray(new GWTParameterInfo[0]));
        return ret;
    }

    protected GWTCompensationCalculation makeCompensationCalculation(CompensationCalculationDef calc)
    {
        if (calc == null)
            return null;
        GWTCompensationCalculation ret = new GWTCompensationCalculation();
        populatePopulationSet(null, ret, calc.getPopulationArray());
        return ret;
    }

    protected GWTAnalysis makeAnalysis(AnalysisDef analysis)
    {
        if (analysis == null)
            return null;
        GWTAnalysis ret = new GWTAnalysis();
        populatePopulationSet(null, ret, analysis.getPopulationArray());
        return ret;
    }

    protected void populatePopulationSet(SubsetSpec subset, GWTPopulationSet popset, PopulationDef[] populations)
    {
        GWTPopulation[] gwtPopulations = new GWTPopulation[populations.length];
        for (int i = 0; i < populations.length; i ++)
        {
            gwtPopulations[i] = makePopulation(subset, populations[i]);
        }
        popset.setPopulations(gwtPopulations);
    }

    protected GWTPopulation makePopulation(SubsetSpec parent, PopulationDef population)
    {
        GWTPopulation ret = new GWTPopulation();
        SubsetSpec current = new SubsetSpec(parent, population.getName());
        ret.setName(population.getName());
        ret.setFullName(current.toString());
        ret.setGate(makeGate(population.getGate()));
        populatePopulationSet(current, ret, population.getPopulationArray());
        return ret;
    }

    protected GWTGate makeGate(GateDef gate)
    {
        if (gate == null)
            return null;
        if (gate.getInterval() != null)
        {
            IntervalDef interval = gate.getInterval();
            return new GWTIntervalGate(interval.getAxis(), interval.getMin(), interval.getMax());
        }
        if (gate.getPolygon() != null)
        {
            PolygonDef polygon = gate.getPolygon();
            PointDef[] points = polygon.getPointArray();
            double[] arrX = new double[points.length];
            double[] arrY = new double[points.length];
            for (int i = 0; i < points.length; i ++)
            {
                arrX[i] = points[i].getX();
                arrY[i] = points[i].getY();
            }
            return new GWTPolygonGate(polygon.getXAxis(), arrX, polygon.getYAxis(), arrY);
        }
        if (gate.getEllipse() != null)
        {
            EllipseDef def = gate.getEllipse();
            PointDef f0 = def.getFocusArray(0);
            PointDef f1 = def.getFocusArray(1);
            GWTEllipseGate e = new GWTEllipseGate(def.getXAxis(), def.getYAxis(), def.getDistance(), f0.getX(), f0.getY(), f1.getX(), f1.getY());
            return e;
        }
        return null;
    }


    public GWTRun[] getRuns()
    {
        try
        {
            FlowRun[] runs = FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords);
            List<GWTRun> ret = new ArrayList();
            for (int i = 0; i < runs.length; i ++)
            {
                if (runs[i].hasRealWells())
                    ret.add(makeRun(runs[i]));
            }
            return ret.toArray(new GWTRun[ret.size()]);
        }
        catch (SQLException e)
        {
            _log.error("Error", e);
            throw UnexpectedException.wrap(e);
        }
    }


    public GWTRun makeRun(FlowRun run)
    {
        GWTRun ret = new GWTRun();
        ret.setRunId(run.getRunId());
        ret.setName(run.getName());
        return ret;
    }


    public GWTWell makeWell(FlowWell well)
    {
        try
        {
            GWTWell ret = new GWTWell();
            ret.setWellId(well.getWellId());
            ret.setName(well.getName());
            ret.setLabel(well.getName());
            return ret;
        }
        catch (Exception e)
        {
            _log.error("Error", e);
            throw UnexpectedException.wrap(e);
        }
    }


    GWTWell makeRunModeWell(FlowWell well, FlowScript runScript) throws Exception
    {
        GWTWell ret = makeWell(well);
        ret.setLabel(well.getName());
        FlowScript wellScript = well.getScript();
        if (wellScript.getScriptId() != runScript.getScriptId())
        {
            ret.setScript(makeGWTScript(wellScript));
        }
        FlowCompensationMatrix matrix = well.getCompensationMatrix();
        if (matrix != null)
        {
            ret.setCompensationMatrix(makeCompensationMatrix(matrix, false));
        }
        return ret;
    }

    public GWTWell[] getWells(FlowRun run, FlowScript script, GWTEditingMode editingMode)
    {
        try
        {
            if (editingMode.equals(GWTEditingMode.run))
            {
                FlowWell[] wells = run.getWells(true);
                List<GWTWell> ret = new ArrayList();
                for (FlowWell well : wells)
                {
                    ret.add(makeRunModeWell(well, script));
                }
                return ret.toArray(new GWTWell[0]);
            }

            ScriptComponent scriptComponent;
            ScriptDocument doc = script.getAnalysisScriptDocument();
            ScriptDef scriptDef = doc.getScript();
            FlowProtocolStep step;
            if (editingMode.equals(GWTEditingMode.compensation))
            {
                scriptComponent = FlowAnalyzer.makeCompensationCalculation(scriptDef.getSettings(), scriptDef.getCompensationCalculation());
                step = FlowProtocolStep.calculateCompensation;
            }
            else
            {
                scriptComponent = FlowAnalyzer.makeAnalysis(scriptDef.getSettings(), scriptDef.getAnalysis());
                step = FlowProtocolStep.analysis;
            }
            Map<Integer, String> wellOptions = run.getWells(FlowProtocol.getForContainer(getContainer()), scriptComponent, step);
            List<GWTWell> ret = new ArrayList();
            for (Map.Entry<Integer, String> entry : wellOptions.entrySet())
            {
                FlowWell flowWell = FlowWell.fromWellId(entry.getKey());
                GWTWell gwtWell = makeWell(flowWell);
                gwtWell.setLabel(entry.getValue());
                ret.add(gwtWell);
            }
            return ret.toArray(new GWTWell[0]);
        }
        catch (Exception e)
        {
            _log.error("Error", e);
            throw UnexpectedException.wrap(e);
        }
    }

    public GWTCompensationMatrix makeCompensationMatrix(FlowCompensationMatrix matrix, boolean includeExperimentInLabel)
    {
        try
        {
            GWTCompensationMatrix ret = new GWTCompensationMatrix();
            ret.setCompId(matrix.getCompId());
            ret.setName(matrix.getName());
            ret.setLabel(matrix.getLabel(includeExperimentInLabel));
            ret.setParameterNames(matrix.getCompensationMatrix().getChannelNames());
            return ret;
        }
        catch (SQLException e)
        {
            _log.error("Error", e);
            throw UnexpectedException.wrap(e);
        }
    }

    public GWTWorkspace getWorkspace(GWTWorkspaceOptions workspaceOptions)
    {
        try
        {
            GWTWorkspace ret = new GWTWorkspace();
            ret.setEditingMode(workspaceOptions.editingMode);
            FlowRun run = FlowRun.fromRunId(workspaceOptions.runId);
            if (run != null)
            {
                FlowScript script;
                if (workspaceOptions.editingMode.isRunMode())
                {
                    script = run.getScript();
                    if (null == script)
                        HttpView.throwNotFound();
                    if (!canUpdate(run))
                    {
                        ret.setReadOnly(true);
                    }
                }
                else
                {
                    script = FlowScript.fromScriptId(workspaceOptions.scriptId);
                    if (null == script)
                        HttpView.throwNotFound();
                    if (!canUpdate(script))
                    {
                        ret.setReadOnly(true);
                    }
                }
                GWTScript gwtScript = makeGWTScript(script);
                if (!workspaceOptions.editingMode.isRunMode() && !workspaceOptions.editingMode.isCompensation())
                {
                    if (gwtScript.getAnalysis() == null)
                    {
                        gwtScript.setAnalysis(new GWTAnalysis());
                    }
                }
                ret.setScript(gwtScript);
                ret.setRun(makeRun(run));
                ret.setWells(getWells(run, script, workspaceOptions.editingMode));
                if (workspaceOptions.editingMode.isCompensation())
                {
                    ret.setSubsetReleventWellMap(getSubsetReleventWellMap((CompensationCalculation) script.getCompensationCalcOrAnalysis(FlowProtocolStep.calculateCompensation), ret.getWells()));
                }
                Map<String, String> parameters = EditScriptForm.getParameterNames(run, new String[0]);
                ret.setParameterNames(parameters.keySet().toArray(new String[0]));
                ret.setParameterLabels(parameters.values().toArray(new String[0]));
                FlowPreference.editScriptRunId.setValue(_context.getRequest(), Integer.toString(run.getRunId()));
            }
            return ret;
        }
        catch (Exception e)
        {
            _log.error("Error", e);
            throw UnexpectedException.wrap(e);
        }
    }

    public GWTCompensationMatrix[] getCompensationMatrices()
    {
        List<FlowCompensationMatrix> comps = FlowCompensationMatrix.getCompensationMatrices(getContainer());
        GWTCompensationMatrix[] ret = new GWTCompensationMatrix[comps.size()];
        boolean sameExperiment = FlowCompensationMatrix.sameExperiment(comps);
        for (int i = 0; i < comps.size(); i ++)
        {
            ret[i] = makeCompensationMatrix(comps.get(i), !sameExperiment);
        }
        return ret;
    }

    public int getRunCompensationMatrix(int runId)
    {
        FlowRun run = FlowRun.fromRunId(runId);
        if (run != null)
        {
            FlowCompensationMatrix comp = run.getCompensationMatrix();
            if (comp != null)
                return comp.getRowId();
        }
        return 0;
    }

    public GWTGraphInfo getGraphInfo(GWTGraphOptions graphOptions) throws GWTGraphException
    {
        try
        {
            GraphCache cache = GraphCache.get(_request);
            if (graphOptions.compensationMatrix != null)
            {
                FlowPreference.editScriptCompId.setValue(_request, Integer.toString(graphOptions.compensationMatrix.getCompId()));
            }
            GWTGraphInfo ret = cache.getGraphInfo(graphOptions);
            if (ret != null)
                return ret;
            SubsetSpec subsetSpec = SubsetSpec.fromString(graphOptions.subset);
            SubsetSpec parentSubsetSpec = subsetSpec == null ? null : subsetSpec.getParent();
            GraphSpec graphSpec;
            if (StringUtils.isEmpty(graphOptions.yAxis))
            {
                graphSpec = new GraphSpec(parentSubsetSpec, graphOptions.xAxis);
            }
            else
            {
                graphSpec = new GraphSpec(parentSubsetSpec, graphOptions.xAxis, graphOptions.yAxis);
            }
            FlowScript script = FlowScript.fromScriptId(graphOptions.script.getScriptId());
            ScriptDef scriptDef = script.getAnalysisScriptDocument().getScript();
            FlowProtocolStep step = FlowProtocolStep.fromActionSequence(graphOptions.editingMode.getActionSequence());
            ScriptComponent group;
            if (step == FlowProtocolStep.calculateCompensation)
            {
                group = FlowAnalyzer.makeCompensationCalculation(scriptDef.getSettings(), scriptDef.getCompensationCalculation());
            }
            else
            {
                group = FlowAnalyzer.makeAnalysis(scriptDef.getSettings(), scriptDef.getAnalysis());
            }
            FlowWell well = FlowWell.fromWellId(graphOptions.well.getWellId());
            FlowCompensationMatrix comp = null;
            if (graphOptions.compensationMatrix != null)
            {
                comp = FlowCompensationMatrix.fromCompId(graphOptions.compensationMatrix.getCompId());
            }
            PlotInfo plotInfo = FCSAnalyzer.get().generateDesignGraph(FlowAnalyzer.getFCSUri(well), comp == null ? null : comp.getCompensationMatrix(), group, graphSpec, graphOptions.width, graphOptions.height, false);
            GWTGraphInfo graphInfo = makeGraphInfo(plotInfo);
            ActionURL url = script.urlFor(ScriptController.Action.graphImage);
            well.addParams(url);
            if (comp != null)
            {
                comp.addParams(url);
            }
            url.addParameter("xaxis", graphOptions.xAxis);
            url.addParameter("yaxis", graphOptions.yAxis);
            step.addParams(url);
            url.addParameter("editingMode", graphOptions.editingMode.toString());
            url.addParameter("width", Integer.toString(graphOptions.width));
            url.addParameter("height", Integer.toString(graphOptions.height));
            url.addParameter("subset", graphOptions.subset);
            cache.setGraphInfo(graphOptions, graphInfo, plotInfo);
            graphInfo.graphURL = url.toString();
            graphInfo.graphOptions = graphOptions;
            return graphInfo;
        }
        catch (FlowException e)
        {
            throw new GWTGraphException(e.getMessage(), graphOptions, e);
        }
        catch (Exception e)
        {
            String msg = "Internal Error getting graph info: " + e.getMessage();
            if (graphOptions.well != null && graphOptions.well.getName() != null)
                msg += "\n  FCS file: " + graphOptions.well.getName();
            if (graphOptions.compensationMatrix != null && graphOptions.compensationMatrix.getName() != null)
                msg += "\n  Comp Matrix: " + graphOptions.compensationMatrix.getName();
            if (graphOptions.subset != null)
                msg += "\n  Subset: " + graphOptions.subset;
            _log.error(msg, e);
            throw new GWTGraphException(e.getMessage(), graphOptions, e);
        }
    }


    public GWTScript save(GWTScript script)
    {
        FlowScript flowScript = FlowScript.fromScriptId(script.getScriptId());
        if (!canUpdate(flowScript))
            return script;
        try
        {
            if (flowScript.getRunCount() == 0)
            {
                ScriptDocument doc = flowScript.getAnalysisScriptDocument();
                ScriptDef scriptDef = doc.getScript();
                if (script.getCompensationCalculation() != null)
                {
                    CompensationCalculationDef calcDef = scriptDef.getCompensationCalculation();
                    calcDef.setPopulationArray(updatePopulationDefs(calcDef.getPopulationArray(), script.getCompensationCalculation()));
                }
                if (script.getAnalysis() != null)
                {
                    AnalysisDef analysisDef = scriptDef.getAnalysis();
                    if (analysisDef == null)
                    {
                        analysisDef = scriptDef.addNewAnalysis();
                    }
                    analysisDef.setPopulationArray(updatePopulationDefs(analysisDef.getPopulationArray(), script.getAnalysis()));
                }
                flowScript.setAnalysisScript(getUser(), doc.toString());
            }
            return makeGWTScript(flowScript);
        }
        catch (Exception e)
        {
            _log.error("Error", e);
            throw UnexpectedException.wrap(e);
        }
    }

    private PopulationDef[] updatePopulationDefs(PopulationDef[] populationArray, GWTPopulationSet popset)
    {
        Map<String, PopulationDef> popdefMap = new LinkedHashMap();
        for (int i = 0; i < populationArray.length; i ++)
        {
            PopulationDef popdef = populationArray[i];
            GWTPopulation population = popset.getPopulation(popdef.getName());
            if (population == null)
            {
                continue;
            }
            updatePopulationDef(popdef, population);
            popdefMap.put(popdef.getName(), popdef);
        }
        for (GWTPopulation population : popset.getPopulations())
        {
            PopulationDef popdef = popdefMap.get(population.getName());
            if (popdef != null)
                continue;
            popdef = PopulationDef.Factory.newInstance();
            updatePopulationDef(popdef, population);
            popdefMap.put(popdef.getName(), popdef);
        }
        return popdefMap.values().toArray(new PopulationDef[0]);
    }

    private void updatePopulationDef(PopulationDef populationDef, GWTPopulation population)
    {
        populationDef.setName(population.getName());
        setGate(populationDef, population.getGate());
        populationDef.setPopulationArray(updatePopulationDefs(populationDef.getPopulationArray(), population));
    }

    private void setGate(PopulationDef population, GWTGate gate)
    {
        GateDef gateDef = GateDef.Factory.newInstance();
        if (gate instanceof GWTIntervalGate)
        {
            GWTIntervalGate interval = (GWTIntervalGate) gate;
            IntervalDef intervalDef = gateDef.addNewInterval();
            intervalDef.setAxis(interval.getAxis());
            intervalDef.setMin(interval.getMinValue());
            intervalDef.setMax(interval.getMaxValue());
        }
        else if (gate instanceof GWTPolygonGate)
        {
            GWTPolygonGate polygon = (GWTPolygonGate) gate;
            PolygonDef polygonDef = gateDef.addNewPolygon();
            polygonDef.setXAxis(polygon.getXAxis());
            polygonDef.setYAxis(polygon.getYAxis());
            for (int i = 0; i < polygon.length(); i++)
            {
                PointDef pointDef = polygonDef.addNewPoint();
                pointDef.setX(polygon.getArrX()[i]);
                pointDef.setY(polygon.getArrY()[i]);
            }
        }
        else
        {
            return;
        }
        population.setGate(gateDef);
    }

    private GWTGraphInfo makeGraphInfo(PlotInfo plotInfo)
    {
        GWTGraphInfo ret = new GWTGraphInfo();
        ret.rangeX = makeRange(plotInfo.getDomainAxis());
        ret.rangeY = makeRange(plotInfo.getRangeAxis());
        ret.rcChart = makeRectangle(plotInfo.getChartArea());
        ret.rcData = makeRectangle(plotInfo.getDataArea());
        return ret;
    }

    private GWTRange makeRange(ValueAxis axis)
    {
        GWTRange ret = new GWTRange(FlowLogarithmicAxis.LOG_LIN_SWITCH);
        ret.min = axis.getLowerBound();
        ret.max = axis.getUpperBound();
        ret.log = axis instanceof FlowLogarithmicAxis;
        return ret;
    }

    private GWTRectangle makeRectangle(Rectangle rectangle)
    {
        GWTRectangle ret = new GWTRectangle();
        ret.x = rectangle.x;
        ret.y = rectangle.y;
        ret.width = rectangle.width;
        ret.height = rectangle.height;
        return ret;
    }

    private ScriptDocument makeScriptDocument(FlowWell well, GWTScript gwtScript) throws Exception
    {
        FlowScript baseScript = well.getRun().getScript();
        if (baseScript == null)
        {
            baseScript = well.getScript();
        }
        ScriptDocument doc = baseScript.getAnalysisScriptDocument();
        doc.getScript().setCompensationCalculation(null);
        AnalysisDef analysisDef = doc.getScript().getAnalysis();
        analysisDef.setPopulationArray(updatePopulationDefs(analysisDef.getPopulationArray(), gwtScript.getAnalysis()));
        return doc;
    }

    private boolean canUpdate(FlowRun run)
    {
        if (!run.getContainer().hasPermission(getUser(), UpdatePermission.class))
            return false;
        if (!run.isInWorkspace())
            return false;
        return true;
    }

    private boolean canUpdate(FlowScript script)
    {
        if (script.getRun() != null)
            return false;
        if (script.getRunCount() != 0)
            return false;
        return true;
    }

    public GWTWell save(GWTWell well, GWTScript script)
    {
        boolean transaction = false;
        FlowWell flowWell = FlowWell.fromWellId(well.getWellId());
        if (!canUpdate(flowWell.getRun()))
        {
            return well;
        }
        try
        {
            if (!ExperimentService.get().isTransactionActive())
            {
                ExperimentService.get().beginTransaction();
                transaction = true;
            }
            FlowScript scriptOld = flowWell.getScript();
            if (scriptOld != null)
            {
                if (script != null && scriptOld.getTargetApplicationCount() == 1)
                {
                    ScriptDocument doc = makeScriptDocument(flowWell, script);
                    scriptOld.setAnalysisScript(getUser(), doc.toString());
                    well.setScript(makeGWTScript(scriptOld));
                    script = null;
                }
                else
                {

                    flowWell.getData().getSourceApplication().removeDataInput(getUser(), scriptOld.getData());
                    if (scriptOld.getRun() != null && scriptOld.getTargetApplicationCount() == 0)
                    {
                        // TODO: delete protocol application for this script.
                        scriptOld.getData().delete(getUser());
                    }
                }
            }

            if (script != null)
            {
                ScriptDocument doc = makeScriptDocument(flowWell, script);
                flowWell = FlowScript.createScriptForWell(getUser(), flowWell, script.getName() + "_modified", doc, flowWell.getRun().getScript().getData(), InputRole.AnalysisScript);
            }
            FlowManager.get().deleteAttributes(flowWell.getExpObject());
            if (transaction)
            {
                ExperimentService.get().commitTransaction();
                transaction = false;
            }
            return makeRunModeWell(flowWell, flowWell.getRun().getScript());
        }
        catch (Exception e)
        {
            _log.error("Error", e);
            throw UnexpectedException.wrap(e);
        }
        finally
        {
            if (transaction)
            {
                ExperimentService.get().rollbackTransaction();
            }
        }
    }

    private boolean isRelevent(SubsetSpec subset, CompensationCalculation.ChannelSubset channelSubset)
    {
        SubsetSpec subsetCompare = channelSubset.getSubset();
        if (subsetCompare == null)
        {
            return false;
        }
        return subsetCompare.toString().startsWith(subset.toString());
    }

    private List<CompensationCalculation.ChannelSubset> getReleventSubsets(SubsetSpec subset, CompensationCalculation calc)
    {
        List<CompensationCalculation.ChannelSubset> matches = new ArrayList();
        for (CompensationCalculation.ChannelInfo channel : calc.getChannels())
        {
            CompensationCalculation.ChannelSubset positiveSubset = channel.getPositive();
            CompensationCalculation.ChannelSubset negativeSubset = channel.getNegative();
            if (isRelevent(subset, positiveSubset))
            {
                matches.add(positiveSubset);
            }
            if (isRelevent(subset, negativeSubset))
            {
                matches.add(negativeSubset);
            }
        }
        return matches;
    }

    public int findReleventWell(SubsetSpec subset, CompensationCalculation calc, FCSKeywordData[] headers)
    {
        List<FCSKeywordData> lstHeaders = Arrays.asList(headers);
        List<CompensationCalculation.ChannelSubset> matches = getReleventSubsets(subset, calc);
        if (matches.size() != 1)
            return -1;
        try
        {
            FCSKeywordData match = FCSAnalyzer.get().findHeader(lstHeaders, matches.get(0).getCriteria());
            if (match != null)
            {
                return lstHeaders.indexOf(match);
            }
            return -1;
        }
        catch (FlowException e)
        {
            return -1;
        }
    }


    private void fillSubsetReleventWellMap(Map<String, GWTWell> map, CompensationCalculation comp, FCSKeywordData[] data, GWTWell[] wells, Population population, SubsetSpec parent)
    {
        SubsetSpec subset = new SubsetSpec(parent, population.getName());
        int index = findReleventWell(subset, comp, data);
        if (index >= 0)
        {
            map.put(subset.toString(), wells[index]);
        }
        for (int i = 0; i < population.getPopulations().size(); i ++)
        {
            fillSubsetReleventWellMap(map, comp, data, wells, population.getPopulations().get(i), subset);
        }
    }

    public Map<String, GWTWell> getSubsetReleventWellMap(CompensationCalculation comp, GWTWell[] wells)
    {
        try
        {
            FCSKeywordData[] data = new FCSKeywordData[wells.length];
            for (int i = 0; i < wells.length; i ++)
            {
                data[i] = FCSAnalyzer.get().readAllKeywords(FlowAnalyzer.getFCSRef(FlowWell.fromWellId(wells[i].getWellId())));
            }
            Map<String, GWTWell> ret = new HashMap();
            for (int i = 0; i < comp.getPopulations().size(); i ++)
            {
                fillSubsetReleventWellMap(ret, comp, data, wells, comp.getPopulations().get(i), null);
            }
            return ret;
        }
        catch (Exception e)
        {
            return Collections.EMPTY_MAP;
        }
    }
}
