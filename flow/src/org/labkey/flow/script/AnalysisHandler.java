/*
 * Copyright (c) 2005-2013 LabKey Corporation
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

package org.labkey.flow.script;

import org.fhcrc.cpas.exp.xml.DataBaseType;
import org.fhcrc.cpas.exp.xml.ExperimentRunType;
import org.fhcrc.cpas.exp.xml.ProtocolApplicationBaseType;
import org.fhcrc.cpas.flow.script.xml.AnalysisDef;
import org.fhcrc.cpas.flow.script.xml.ScriptDef;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.fhcrc.cpas.flow.script.xml.SettingsDef;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.util.URIUtil;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.ScriptSettings;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.web.FCSRef;
import org.labkey.flow.analysis.web.ScriptAnalyzer;
import org.labkey.flow.data.*;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.AttributeSetHelper;
import org.labkey.flow.persist.FlowDataHandler;
import org.labkey.flow.persist.InputRole;
import org.labkey.flow.persist.ObjectType;

import java.io.File;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;

public class AnalysisHandler extends BaseHandler
{
    AnalysisDef _analysis;
    Analysis _groupAnalysis;
    ScriptSettings _settings;
    int _wellIndex;
    boolean _getScriptFromWells;

    public AnalysisHandler(ScriptJob job, SettingsDef settings, AnalysisDef analysis)
    {
        super(job, FlowProtocolStep.analysis);
        _analysis = analysis;
        _settings = ScriptSettings.fromSettingsDef(settings);
        _groupAnalysis = ScriptAnalyzer.makeAnalysis(settings, _analysis);
    }

    synchronized public DataBaseType addWell(ExperimentRunType runElement, FlowFCSFile src, FlowCompensationMatrix flowComp, String scriptLSID) throws SQLException
    {
        ProtocolApplicationBaseType app = addProtocolApplication(runElement, scriptLSID);
        DataBaseType ret = duplicateWell(app, src, FlowDataType.FCSAnalysis);
        ret.setName(_job.getProtocol().getFCSAnalysisName(src));
        if (flowComp != null)
        {
            _job.addInput(app, flowComp, InputRole.CompensationMatrix);
        }
        _job.addRunOutput(ret.getAbout(), null);
        return ret;
    }

    public void processRun(FlowRun run, ExperimentRunType runElement, File workingDirectory) throws Exception
    {
        FlowCompensationMatrix flowComp;

        if (_getScriptFromWells)
        {
            flowComp = run.getCompensationMatrix();
        }
        else
        {
            flowComp = _job.findCompensationMatrix(run);
        }
        CompensationMatrix comp = null;
        if (flowComp != null)
        {
            comp = flowComp.getCompensationMatrix();
        }
        if (comp == null && _groupAnalysis.requiresCompensationMatrix())
        {
            _job.warn("No compensation matrix found and a compensation matrix is required for analysis.  You will encounter errors during analysis unless the FCS files have been machine compensated.");
        }

        try
        {
            FlowWell[] wells;
            if (_getScriptFromWells)
            {
                wells = run.getWells();
            }
            else
            {
                wells = run.getFCSFilesToBeAnalyzed(_job.getProtocol(), _settings);
            }
            if (wells.length == 0)
            {
                FlowWell[] allWells = run.getWells();
                if (allWells.length == 0)
                {
                    _job.addStatus("This run contains no FCS files");
                }
                else
                {
                    _job.addStatus("This run contains FCS files but they are all excluded by either the Protocol's or Analysis Script's FCS Filter");
                }
                return;
            }
            _wellIndex = 0;
            Runnable[] tasks = new Runnable[wells.length];
            for (int iWell = 0; iWell < wells.length; iWell ++)
            {
                String scriptLSID;
                FlowWell srcWell = wells[iWell];
                Analysis wellAnalysis;
                FlowCompensationMatrix wellFlowComp;
                CompensationMatrix wellComp;
                if (_getScriptFromWells)
                {
                    FlowScript script = wells[iWell].getScript();
                    if (script.getScriptId() != _job._runAnalysisScript.getScriptId())
                    {
                        File file = _job.decideFileName(workingDirectory, URIUtil.getFilename(srcWell.getFCSURI()), FlowDataHandler.EXT_SCRIPT);
                        ScriptDocument doc = script.getAnalysisScriptDocument();
                        doc.save(file);

                        ProtocolApplicationBaseType app = addProtocolApplication(runElement, null);
                        scriptLSID = ExperimentService.get().generateGuidLSID(getContainer(), FlowDataType.Script);
                        DataBaseType dbtScript = app.getOutputDataObjects().addNewData();
                        dbtScript.setAbout(scriptLSID);
                        dbtScript.setDataFileUrl(file.toURI().toString());
                        dbtScript.setName(script.getName());

                        ScriptDef scriptDef = doc.getScript();
                        wellAnalysis = ScriptAnalyzer.makeAnalysis(scriptDef.getSettings(), scriptDef.getAnalysis());
                    }
                    else
                    {
                        scriptLSID = script.getLSID();
                        wellAnalysis = _groupAnalysis;
                    }
                    wellFlowComp = srcWell.getCompensationMatrix();
                    wellComp = wellFlowComp == null ? null : wellFlowComp.getCompensationMatrix();
                }
                else
                {
                    scriptLSID = _job._runAnalysisScript.getLSID();
                    wellAnalysis = _groupAnalysis;
                    wellFlowComp = flowComp;
                    wellComp = comp;
                }
                Runnable task = new AnalyzeTask(workingDirectory, run, runElement, srcWell, wells.length, scriptLSID, wellAnalysis, wellFlowComp, wellComp);
                tasks[iWell] = task;
            }
            FlowThreadPool.runTaskSet(new FlowTaskSet(tasks));
        }
        catch (SQLException e)
        {
            _job.addStatus("An exception occurred: " + e);
        }
    }

    synchronized int getNextWellIndex()
    {
        return ++_wellIndex;
    }

    private class AnalyzeTask implements Runnable
    {
        File _workingDirectory;
        FlowRun _run;
        FlowWell _well;
        int _wellCount;
        CompensationMatrix _comp;
        FlowCompensationMatrix _flowComp;
        ExperimentRunType _runElement;
        Analysis _groupAnalysis;
        String _scriptLSID;

        AnalyzeTask(File workingDirectory, FlowRun run, ExperimentRunType runElement, FlowWell well, int wellCount, String scriptLSID, Analysis groupAnalysis, FlowCompensationMatrix flowComp, CompensationMatrix comp)
        {
            _workingDirectory = workingDirectory;
            _run = run;
            _runElement = runElement;
            _well = well;
            _wellCount = wellCount;
            _flowComp = flowComp;
            _comp = comp;
            _groupAnalysis = groupAnalysis;
            _scriptLSID = scriptLSID;
        }

        private AttributeSet tryCopyAttributes()
        {
            if (!_getScriptFromWells)
                return null;
            AttributeSet attrSet = AttributeSetHelper.fromData(_well.getData(), true);
            if (attrSet.getStatistics().isEmpty() && attrSet.getGraphNames().isEmpty())
            {
                return null;
            }
            return attrSet;
        }

        public void run()
        {
            try
            {
                int iWell = getNextWellIndex();
                if (_job.checkInterrupted())
                    return;
                FCSRef ref = FlowAnalyzer.getFCSRef(_well);
                URI uri = ref.getURI();
                String description = "well " + iWell + "/" + _wellCount + ":" + _run.getName() + ":" + _well.getName();

                AttributeSet attrs = tryCopyAttributes();
                DataBaseType dbt = addWell(_runElement, _well.getFCSFileInput(), _flowComp, _scriptLSID);
                if (attrs != null)
                {
                    _job.addStatus("Copying " + description);
                }
                else
                {
                    attrs = new AttributeSet(ObjectType.fcsAnalysis, uri);
                    _job.addStatus("Starting " + description);
                    List<FCSAnalyzer.StatResult> stats = FCSAnalyzer.get().calculateStatistics(uri, _comp, _groupAnalysis);
                    addResults(dbt, attrs, stats);
                    List<FCSAnalyzer.GraphResult> graphResults = FCSAnalyzer.get().generateGraphs(_well.getFCSURI(), _comp, _groupAnalysis, _groupAnalysis.getGraphs());
                    addResults(dbt, attrs, graphResults);
                }
                attrs.save(_job.decideFileName(_workingDirectory, URIUtil.getFilename(uri), FlowDataHandler.EXT_DATA), dbt);
                _job.addStatus("Completed " + description);
            }
            catch (Throwable t)
            {
                _job.error("Error", t);
            }

        }
    }

    public String getRunName(FlowRun srcRun)
    {
        return srcRun.getName() + " analysis";
    }
}
