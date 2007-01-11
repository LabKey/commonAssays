package org.fhcrc.cpas.flow.data;

import org.fhcrc.cpas.exp.api.*;
import org.fhcrc.cpas.exp.xml.SimpleTypeNames;
import org.fhcrc.cpas.security.User;
import org.fhcrc.cpas.view.ViewURLHelper;
import org.fhcrc.cpas.pipeline.PipelineService;
import org.fhcrc.cpas.flow.persist.AttributeSet;
import org.fhcrc.cpas.flow.persist.FlowManager;
import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;

import java.net.URI;
import java.sql.SQLException;
import java.util.*;

import Flow.Well.WellController;
import Flow.EditScript.ScriptController;
import Flow.FlowParam;
import com.labkey.flow.web.StatisticSpec;
import com.labkey.flow.web.GraphSpec;

import javax.servlet.http.HttpServletRequest;

public class FlowWell extends FlowDataObject
{
    static private final Logger _log = Logger.getLogger(FlowWell.class);

    static public FlowWell fromWellId(int id)
    {
        return (FlowWell) fromRowId(id);
    }

    public FlowLog getLog(LogType type) throws SQLException
    {
        return getRun().getLog(type);
    }

    static public FlowWell fromURL(ViewURLHelper url) throws Exception
    {
        return fromURL(url, null);
    }

    static public FlowWell fromURL(ViewURLHelper url, HttpServletRequest request) throws Exception
    {
        int wellId = getIntParam(url, request, FlowParam.wellId);
        if (wellId == 0)
            return null;
        FlowWell ret = fromWellId(wellId);
        ret.checkContainer(url);
        return ret;

    }

    public FlowWell(ExpData data)
    {
        super(data);
    }

    public FlowWell getFCSFile()
    {
        if (getDataType() == FlowDataType.FCSFile)
            return null;
        ExpData[] inputs = getProtocolApplication().getInputDatas();
        for (ExpData input : inputs)
        {
            if (input.getDataType() == FlowDataType.FCSFile)
                return (FlowWell) FlowDataObject.fromData(input);
        }
        return null;
    }

    public URI getFCSURI() throws Exception
    {
        return getAttributeSet().getURI();
    }

    public String getKeyword(String keyword) throws SQLException
    {
        return FlowManager.get().getKeyword(getData(), keyword);
    }

    public void setKeyword(User user, String keyword, String value) throws SQLException
    {
        FlowManager.get().setKeyword(user, getData(), keyword, value);
    }

    public Map<String, String> getKeywords() throws SQLException
    {
        return getAttributeSet().getKeywords();
    }

    public Map<StatisticSpec, Double> getStatistics() throws SQLException
    {
        return getAttributeSet().getStatistics();
    }

    public GraphSpec[] getGraphs() throws SQLException
    {
        return getAttributeSet().getGraphNames().toArray(new GraphSpec[0]);
    }

    public byte[] getGraphBytes(GraphSpec graph) throws SQLException
    {
        return FlowManager.get().getGraphBytes(getData(), graph);
    }

    public String getLabel()
    {
        String prefix;
        DataType type = getDataType();
        if (type == FlowDataType.FCSFile)
        {
            prefix = "FCS File";
        }
        else if (type == FlowDataType.FCSAnalysis)
        {
            prefix = "FCSAnalysis";
        }
        else if (type == FlowDataType.CompensationControl)
        {
            prefix = "Compensation Control";
        }
        else
        {
            prefix = "Unknown object";
        }
        return prefix + " '" + getName() + "'";
    }

    public int getWellId()
    {
        return getRowId();
    }

    public void addParams(Map<FlowParam,Object> map)
    {
        map.put(FlowParam.wellId, getWellId());
    }

    public ViewURLHelper urlShow()
    {
        return addParams(pfURL(WellController.Action.showWell));
    }

    public ViewURLHelper urlEditAnalysisScript() throws Exception
    {
        FlowScript analysisScript = getScript();
        ViewURLHelper ret = analysisScript.urlFor(ScriptController.Action.begin);
        addParams(ret);
        return ret;
    }

    public FlowScript getScript()
    {
        ExpProtocolApplication app = getProtocolApplication();
        if (app == null)
        {
            return null;
        }
        for (ExpData input : app.getInputDatas())
        {
            if (input.getDataType() == FlowDataType.Script)
            {
                return new FlowScript(input);
            }
        }
        return null;
    }

    public FlowCompensationMatrix getCompensationMatrix()
    {
        ExpProtocolApplication app = getProtocolApplication();
        if (app == null)
            return null;
        for (ExpDataInput input : app.getDataInputs())
        {
            if (input.getData().getDataType() == FlowDataType.CompensationMatrix)
            {
                return new FlowCompensationMatrix(input.getData());
            }
        }
        return null;
    }

    public void setName(User user, String name) throws Exception
    {
        if (ObjectUtils.equals(name, getName()))
            return;
        ExpData data = getData();
        data.setName(name);
        data.save(user);
    }

    public String getComment() throws SQLException
    {
        return getExpObject().getComment();
    }

    public List<FlowWell> getFCSAnalyses() throws SQLException
    {
        ExpProtocolApplication[] apps = getExpObject().getTargetApplications();
        List<FlowWell> ret = new ArrayList();
        for (ExpProtocolApplication app : apps)
        {
            addDataOfType(app.getDataOutputs(), FlowDataType.FCSAnalysis, ret);
        }
        return ret;
    }

    public ExpMaterial getSample(ExpSampleSet set)
    {
        for (ExpMaterial material : getSamples())
        {
            if (set.equals(material.getSampleSet()))
                return material;
        }
        return null;
    }

    public ExpMaterial[] getSamples()
    {
        ExpProtocolApplication app = getExpObject().getSourceApplication();
        if (app == null)
            return null;

        return app.getInputMaterials();
    }
}
