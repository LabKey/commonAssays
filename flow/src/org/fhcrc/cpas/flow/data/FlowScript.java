package org.fhcrc.cpas.flow.data;

import org.fhcrc.cpas.security.User;

import org.fhcrc.cpas.exp.*;
import org.fhcrc.cpas.exp.api.ExpData;
import org.fhcrc.cpas.exp.api.ExperimentService;
import org.fhcrc.cpas.exp.xml.SimpleTypeNames;
import org.fhcrc.cpas.data.*;
import org.fhcrc.cpas.flow.script.xml.*;
import org.fhcrc.cpas.flow.script.FlowAnalyzer;
import org.fhcrc.cpas.flow.persist.FlowManager;
import org.fhcrc.cpas.flow.persist.FlowDataHandler;
import org.fhcrc.cpas.view.ViewURLHelper;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.*;
import java.net.URI;
import java.io.File;

import Flow.ExecuteScript.AnalysisScriptController;
import Flow.FlowParam;
import com.labkey.flow.model.PopulationSet;

public class FlowScript extends FlowDataObject
{
    private static final Logger _log = Logger.getLogger(FlowScript.class);
    public static final String STAT_COLUMN_PREFIX = "statistic.";
    public static final String KEYWORD_COLUMN_PREFIX = "keyword.";
    public static final String DEFAULT_UPLOAD_PROTOCOL_NAME = "Default Upload Settings";


    static public FlowScript fromScriptId(int id)
    {
        if (id == 0)
            return null;
        return new FlowScript(ExperimentService.get().getData(id));
    }

    static public FlowScript fromLSID(String lsid)
    {
        return new FlowScript(ExperimentService.get().getData(lsid));
    }

    static public FlowScript fromURL(ViewURLHelper url) throws ServletException
    {
        return fromURL(url, null);
    }


    static public FlowScript fromURL(ViewURLHelper url, HttpServletRequest request) throws ServletException
    {
        FlowScript ret = FlowScript.fromScriptId(getIntParam(url, request, FlowParam.scriptId));
        if (ret == null)
            return null;
        ret.checkContainer(url);
        return ret;
    }

    static public FlowScript[] getScripts(Container container)
    {
        ExpData[] datas = ExperimentService.get().getDatas(container, FlowDataType.Script);
        FlowScript[] ret = new FlowScript[datas.length];
        for (int i = 0; i < datas.length; i ++)
        {
            ret[i] = new FlowScript(datas[i]);
        }
        return ret;
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

    public String getAnalysisScript() throws SQLException
    {
        return FlowManager.get().getScript(getData());
    }

    public ScriptDocument getAnalysisScriptDocument() throws Exception
    {
        return ScriptDocument.Factory.parse(getAnalysisScript());
    }

    public void setAnalysisScript(User user, String script) throws SQLException
    {
        FlowManager.get().setScript(user, getData(), script);
    }

    public int getScriptId()
    {
        return getExpObject().getRowId();
    }

    static public String lsidForName(Container container, String name)
    {
        return generateLSID(container, "Protocol", name);
    }

    static public FlowScript create(User user, Container container, String name, String analysisScript) throws Exception
    {
        ExpData data = ExperimentService.get().createData(container, FlowDataType.Script, name);
        data.setDataFileURI(new File("script." + FlowDataHandler.EXT_SCRIPT).toURI());
        data.save(user);
        FlowScript ret = new FlowScript(data);
        ret.setAnalysisScript(user, analysisScript);
        return ret;
    }

    public void addParams(Map<FlowParam, Object> map)
    {
        map.put(FlowParam.scriptId, getScriptId());
    }

    public ViewURLHelper urlShow()
    {
        return urlFor(AnalysisScriptController.Action.begin);
    }

    public String getLabel()
    {
        return getName();
    }

    public FlowObject getParent()
    {
        return null;
    }

    public RunDef getRunElement()
    {
        try
        {
            ScriptDocument doc = getAnalysisScriptDocument();
            if (doc == null)
                return null;
            if (doc.getScript() == null)
                return null;
            return doc.getScript().getRun();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public String[] getDeclaredKeywords()
    {
        RunDef run = getRunElement();
        if (run == null)
            return new String[0];
        WellDef well = run.getWell();
        if (well == null)
            return new String[0];
        List<String> ret = new ArrayList();
        for (KeywordDef keyword : well.getKeywordArray())
        {
            ret.add(keyword.getName());
        }
        return ret.toArray(new String[0]);
    }

    public List<String> getSubsets() throws Exception
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
        if (step == FlowProtocolStep.keywords)
        {
            return script.getRun() != null;
        }
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

    public PopulationSet getCompensationCalcOrAnalysis(FlowProtocolStep step) throws Exception
    {
        if (step == FlowProtocolStep.calculateCompensation)
        {
            return FlowAnalyzer.makeCompensationCalculation(getAnalysisScriptDocument().getScript().getCompensationCalculation());
        }
        return FlowAnalyzer.makeAnalysis(getAnalysisScriptDocument().getScript().getAnalysis());
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

    public ViewURLHelper urlFor(Enum action, FlowProtocolStep step)
    {
        ViewURLHelper ret = super.urlFor(action);
        step.addParams(ret);
        return ret;
    }

    public int getRunCount()
    {
        return getExpObject().getTargetRuns().length;
    }
}
