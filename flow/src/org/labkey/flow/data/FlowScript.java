/*
 * Copyright (c) 2005-2016 LabKey Corporation
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

package org.labkey.flow.data;

import org.fhcrc.cpas.flow.script.xml.ChannelDef;
import org.fhcrc.cpas.flow.script.xml.CompensationCalculationDef;
import org.fhcrc.cpas.flow.script.xml.ScriptDef;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.analysis.model.PopulationSet;
import org.labkey.flow.analysis.model.ScriptComponent;
import org.labkey.flow.analysis.web.ScriptAnalyzer;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.persist.FlowDataHandler;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.InputRole;
import org.labkey.flow.script.FlowAnalyzer;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FlowScript extends FlowDataObject
{
    private String strScript;

    static public FlowScript fromScriptId(int id)
    {
        if (id == 0)
            return null;
        return new FlowScript(ExperimentService.get().getExpData(id));
    }

    static public FlowScript fromLSID(String lsid)
    {
        ExpData data = ExperimentService.get().getExpData(lsid);
        if (data == null)
            return null;
        return new FlowScript(data);
    }

    static public FlowScript fromURL(ActionURL url, Container actionContainer, User user)
    {
        return fromURL(url, null, actionContainer, user);
    }

    static public FlowScript fromName(Container container, String name)
    {
        return FlowScript.fromLSID(FlowObject.generateLSID(container, FlowDataType.Script.getNamespacePrefix(), name));
    }


    static public FlowScript fromURL(ActionURL url, HttpServletRequest request, Container actionContainer, User user)
    {
        FlowScript ret = FlowScript.fromScriptId(getIntParam(url, request, FlowParam.scriptId));
        if (ret == null || ret.getExpObject() == null)
            return null;
        ret.checkContainer(actionContainer, user, url);
        return ret;
    }

    static public FlowScript[] getScripts(Container container)
    {
        List<? extends ExpData> datas = ExperimentService.get().getExpDatas(container, FlowDataType.Script, null);
        List<FlowScript> ret = new ArrayList<>();
        for (ExpData data : datas)
        {
            FlowScript script = new FlowScript(data);
            if (script.isPrivate())
                continue;
            ret.add(script);
        }
        return ret.toArray(new FlowScript[ret.size()]);
    }

    static public FlowScript[] getAnalysisScripts(Container container)
    {
        return getScripts(container);
    }

    public FlowScript(ExpData data)
    {
        super(data);
    }

    public String getAnalysisScript()
    {
        if (strScript == null)
        {
            strScript = FlowManager.get().getScript(getData());
        }
        return strScript;
    }

    private ScriptDocument _document = null;

    public ScriptDocument getAnalysisScriptDocument() throws Exception
    {
        if (_document == null)
            _document = ScriptDocument.Factory.parse(getAnalysisScript());
        return _document;
    }

    public void setAnalysisScript(User user, String script) throws SQLException
    {
        FlowManager.get().setScript(user, getData(), script);
        strScript = script;
    }

    public int getScriptId()
    {
        return getExpObject().getRowId();
    }

    public String getComment()
    {
        return getExpObject().getComment();
    }

    static public String lsidForName(Container container, String name)
    {
        return generateLSID(container, FlowDataType.Script, name);
    }

    static private void initScript(ExpData data)
    {
        data.setDataFileURI(new File("script." + FlowDataHandler.EXT_SCRIPT).toURI());
    }

    static public FlowScript create(User user, Container container, String name, String analysisScript) throws SQLException
    {
        ExpData data = ExperimentService.get().createData(container, FlowDataType.Script, name);
        initScript(data);
        data.save(user);
        FlowScript ret = new FlowScript(data);
        ret.setAnalysisScript(user, analysisScript);
        return ret;
    }

    static public FlowWell createScriptForWell(User user, FlowWell well, String name, ScriptDocument analysisScript, ExpData input, InputRole inputRole) throws SQLException
    {
        Container container = well.getContainer();
        FlowRun run = well.getRun();
        ExpData data = ExperimentService.get().createData(container, FlowDataType.Script);
        data.setName(name);
        initScript(data);
        data.save(user);
        ExpProtocolApplication app = run.getExperimentRun().addProtocolApplication(user, FlowProtocolStep.defineGates.getAction(run.getExperimentRun().getProtocol()), FlowProtocolStep.defineGates.applicationType, FlowProtocolStep.defineGates.getName());
        if (input != null)
        {
            app.addDataInput(user, input, inputRole.toString());
        }
        data.setSourceApplication(app);
        data.save(user);
        FlowScript ret = new FlowScript(data);
        ret.setAnalysisScript(user, analysisScript.toString());
        well.getData().getSourceApplication().addDataInput(user, data, InputRole.AnalysisScript.toString());
        return well;
    }

    public void addParams(Map<FlowParam, Object> map)
    {
        map.put(FlowParam.scriptId, getScriptId());
    }

    public boolean isPrivate()
    {
        return getData().getRunId() != null;
    }

    public ActionURL urlShow()
    {
        return urlFor(AnalysisScriptController.BeginAction.class);
    }

    public ActionURL urlDownload()
    {
        return urlFor(ScriptController.DownloadAction.class);
    }

    public String getLabel()
    {
        return "Script '" + getName() + "'";
    }

    public FlowObject getParent()
    {
        return null;
    }

    public Collection<SubsetSpec> getSubsets()
    {
        return FlowAnalyzer.getSubsets(this);
    }

    public String[] getCompensationChannels()
    {
        try
        {
            ArrayList<String> ret = new ArrayList<>();
            CompensationCalculationDef calc = getAnalysisScriptDocument().getScript().getCompensationCalculation();
            if (calc == null)
                return null;
            for (ChannelDef channel : calc.getChannelArray())
            {
                ret.add(channel.getName());
            }
            return ret.toArray(new String[ret.size()]);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public boolean hasStep(FlowProtocolStep step)
    {
        ScriptDocument doc;
        try
        {
            doc = getAnalysisScriptDocument();
        }
        catch (Exception e)
        {
            return false;
        }
        ScriptDef script = doc.getScript();
        if (script == null)
            return false;
        if (step == FlowProtocolStep.calculateCompensation)
        {
            return script.isSetCompensationCalculation();
        }
        if (step == FlowProtocolStep.analysis)
        {
            return script.isSetAnalysis();
        }
        return false;
    }

    static public List<FlowScript> getProtocolsWithStep(Container container, FlowProtocolStep step)
    {
        FlowScript[] protocols = getScripts(container);
        List<FlowScript> ret = new ArrayList<>();
        for (FlowScript analysisScript : protocols)
        {
            if (analysisScript.hasStep(step))
            {
                ret.add(analysisScript);
            }
        }
        return ret;
    }

    public ScriptComponent getCompensationCalcOrAnalysis(FlowProtocolStep step) throws Exception
    {
        ScriptDef script = getAnalysisScriptDocument().getScript();
        if (step == FlowProtocolStep.calculateCompensation)
        {
            return ScriptAnalyzer.makeCompensationCalculation(script.getSettings(), script.getCompensationCalculation());
        }
        return ScriptAnalyzer.makeAnalysis(script.getSettings(), script.getAnalysis());
    }

    public ActionURL urlFor(Class<? extends Controller> actionClass, FlowProtocolStep step)
    {
        ActionURL ret = super.urlFor(actionClass);
        step.addParams(ret);
        return ret;
    }

    public int getRunCount()
    {
        return getExpObject().getTargetRuns().size();
    }

    public ActionURL getRunsUrl()
    {
        return getRunsUrl(null);
    }

    public ActionURL getRunsUrl(ActionURL runsUrl)
    {
        if (runsUrl == null)
            runsUrl = new ActionURL(RunController.ShowRunsAction.class, getContainer());
        if (runsUrl.isReadOnly())
            runsUrl = runsUrl.clone();
        FieldKey key = FieldKey.fromParts("AnalysisScript", "RowId");
        runsUrl.addFilter("query", key, CompareType.EQUAL, this.getScriptId());
        return runsUrl;
    }

    public int getTargetApplicationCount()
    {
        return getExpObject().getTargetApplications().size();
    }

    public boolean requiresCompensationMatrix(FlowProtocolStep step)
    {
        try
        {
            PopulationSet populationSet = getCompensationCalcOrAnalysis(step);
            return populationSet.requiresCompensationMatrix();
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
