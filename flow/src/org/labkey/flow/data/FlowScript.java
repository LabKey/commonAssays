/*
 * Copyright (c) 2005-2008 LabKey Corporation
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

import org.labkey.api.security.User;

import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.data.*;
import org.fhcrc.cpas.flow.script.xml.*;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.FlowDataHandler;
import org.labkey.api.view.ActionURL;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.*;
import java.io.File;

import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.analysis.model.PopulationSet;
import org.labkey.flow.analysis.model.ScriptComponent;
import org.labkey.flow.analysis.web.SubsetSpec;

public class FlowScript extends FlowDataObject
{
    private static final Logger _log = Logger.getLogger(FlowScript.class);
    public static final String STAT_COLUMN_PREFIX = "statistic.";
    public static final String KEYWORD_COLUMN_PREFIX = "keyword.";
    public static final String DEFAULT_UPLOAD_PROTOCOL_NAME = "Default Upload Settings";
    public static final String PRIVATE_SCRIPT_SUFFIX = "_modified";

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

    static public FlowScript fromURL(ActionURL url) throws ServletException
    {
        return fromURL(url, null);
    }

    static public FlowScript fromName(Container container, String name)
    {
        return FlowScript.fromLSID(FlowObject.generateLSID(container, FlowDataType.Script.getNamespacePrefix(), name));
    }


    static public FlowScript fromURL(ActionURL url, HttpServletRequest request) throws ServletException
    {
        FlowScript ret = FlowScript.fromScriptId(getIntParam(url, request, FlowParam.scriptId));
        if (ret == null)
            return null;
        ret.checkContainer(url);
        return ret;
    }

    static public FlowScript[] getScripts(Container container)
    {
        ExpData[] datas = ExperimentService.get().getExpDatas(container, FlowDataType.Script);
        List<FlowScript> ret = new ArrayList();
        for (int i = 0; i < datas.length; i ++)
        {
            FlowScript script = new FlowScript(datas[i]);
            if (script.isPrivate())
                continue;
            ret.add(script);
        }
        return ret.toArray(new FlowScript[0]);
    }

    static public FlowScript[] getUploadRunProtocols(Container container) throws SQLException
    {
        FlowScript[] all = getScripts(container);
        List<FlowScript> ret = new ArrayList();
        for (FlowScript prot : all)
        {
            if (prot.hasStep(FlowProtocolStep.keywords))
                ret.add(prot);
        }
        return ret.toArray(new FlowScript[0]);
    }

    static public FlowScript[] getAnalysisScripts(Container container) throws SQLException
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
            try
            {
                strScript = FlowManager.get().getScript(getData());
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
        return strScript;
    }

    public ScriptDocument getAnalysisScriptDocument() throws Exception
    {
        return ScriptDocument.Factory.parse(getAnalysisScript());
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

    static public String lsidForName(Container container, String name)
    {
        return generateLSID(container, FlowDataType.Script, name);
    }

    static private void initScript(ExpData data)
    {
        data.setDataFileURI(new File("script." + FlowDataHandler.EXT_SCRIPT).toURI());
    }

    static public FlowScript create(User user, Container container, String name, String analysisScript) throws Exception
    {
        ExpData data = ExperimentService.get().createData(container, FlowDataType.Script, name);
        initScript(data);
        data.save(user);
        FlowScript ret = new FlowScript(data);
        ret.setAnalysisScript(user, analysisScript);
        return ret;
    }

    static public FlowWell createScriptForWell(User user, FlowWell well, String name, ScriptDocument analysisScript, ExpData input, InputRole inputRole) throws Exception
    {
        Container container = well.getContainer();
        FlowRun run = well.getRun();
        ExpData data = ExperimentService.get().createData(container, FlowDataType.Script);
        data.setName(name);
        initScript(data);
        data.save(user);
        ExpProtocolApplication app = run.getExperimentRun().addProtocolApplication(user, FlowProtocolStep.defineGates.getAction(run.getExperimentRun().getProtocol()), FlowProtocolStep.defineGates.applicationType);
        if (input != null)
        {
            app.addDataInput(user, input, inputRole.toString(), null);
        }
        data.setSourceApplication(app);
        data.save(user);
        FlowScript ret = new FlowScript(data);
        ret.setAnalysisScript(user, analysisScript.toString());
        well.getData().getSourceApplication().addDataInput(user, data, InputRole.AnalysisScript.toString(), InputRole.AnalysisScript.getPropertyDescriptor(container));
        return well;
    }

    public void addParams(Map<FlowParam, Object> map)
    {
        map.put(FlowParam.scriptId, getScriptId());
    }

    public boolean isPrivate()
    {
        return getRun() != null;
    }

    public ActionURL urlShow()
    {
        return urlFor(AnalysisScriptController.Action.begin);
    }

    public String getLabel()
    {
        return "Script '" + getName() + "'";
    }

    public FlowObject getParent()
    {
        return null;
    }

    public Collection<SubsetSpec> getSubsets() throws Exception
    {
        return FlowAnalyzer.getSubsets(this);
    }

    public String[] getCompensationChannels()
    {
        try
        {
            ArrayList<String> ret = new ArrayList();
            CompensationCalculationDef calc = getAnalysisScriptDocument().getScript().getCompensationCalculation();
            if (calc == null)
                return null;
            for (ChannelDef channel : calc.getChannelArray())
            {
                ret.add(channel.getName());
            }
            return ret.toArray(new String[0]);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public boolean hasStep(FlowProtocolStep step)
    {
        ScriptDocument doc = null;
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
            return script.getCompensationCalculation() != null;
        }
        if (step == FlowProtocolStep.analysis)
        {
            return script.getAnalysis() != null;
        }
        return false;
    }

    static public List<FlowScript> getProtocolsWithStep(Container container, FlowProtocolStep step) throws SQLException
    {
        FlowScript[] protocols = getScripts(container);
        List<FlowScript> ret = new ArrayList();
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
            return FlowAnalyzer.makeCompensationCalculation(script.getSettings(), script.getCompensationCalculation());
        }
        return FlowAnalyzer.makeAnalysis(script.getSettings(), script.getAnalysis());
    }

    public String getProtocolType()
    {
        StringBuilder ret = new StringBuilder();
        String strAnd = "";
        if (hasStep(FlowProtocolStep.keywords))
        {
            strAnd = " and ";
            ret.append("Upload");
        }
        if (hasStep(FlowProtocolStep.calculateCompensation))
        {
            ret.append(strAnd);
            strAnd = " and ";
            ret.append("Compensation");
        }
        if (hasStep(FlowProtocolStep.analysis))
        {
            ret.append(strAnd);
            ret.append("Analysis");
        }
        if (ret.length() == 0)
            return "None";
        return ret.toString();
    }

    public ActionURL urlFor(Enum action, FlowProtocolStep step)
    {
        ActionURL ret = super.urlFor(action);
        step.addParams(ret);
        return ret;
    }

    public int getRunCount()
    {
        return getExpObject().getTargetRuns().length;
    }
    public int getTargetApplicationCount()
    {
        return getExpObject().getTargetApplications().length;
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
