/*
 * Copyright (c) 2005-2010 LabKey Corporation
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

import org.labkey.api.exp.api.*;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.persist.FlowManager;
import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;

import java.net.URI;
import java.sql.SQLException;
import java.util.*;

import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.GraphSpec;

import javax.servlet.http.HttpServletRequest;

public class FlowWell extends FlowDataObject
{
    static private final Logger _log = Logger.getLogger(FlowWell.class);

    static public FlowWell fromWellId(int id)
    {
        FlowObject flowobj = fromRowId(id);
        if (flowobj instanceof FlowWell)
            return (FlowWell)flowobj;
        return null;
    }

    public FlowLog getLog(LogType type) throws SQLException
    {
        return getRun().getLog(type);
    }

    static public FlowWell fromURL(ActionURL url) throws Exception
    {
        return fromURL(url, null);
    }

    static public FlowWell fromURL(ActionURL url, HttpServletRequest request) throws Exception
    {
        int wellId = getIntParam(url, request, FlowParam.wellId);
        if (wellId == 0)
            return null;
        FlowWell ret = fromWellId(wellId);
        if (null == ret)
            return null;
        ret.checkContainer(url);
        return ret;

    }

    public FlowWell(ExpData data)
    {
        super(data);
    }

    public FlowFCSFile getFCSFile()
    {
        if (getDataType() == FlowDataType.FCSFile)
            return (FlowFCSFile) this;
        for (ExpData input : getProtocolApplication().getInputDatas())
        {
            if (input.getDataType() == FlowDataType.FCSFile)
                return (FlowFCSFile) FlowDataObject.fromData(input);
        }
        return null;
    }

    public URI getFCSURI()
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

    public Map<String, String> getKeywords()
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

    public ActionURL urlShow()
    {
        return urlFor(WellController.Action.showWell);
    }

    public ActionURL urlEditAnalysisScript() throws Exception
    {
        FlowScript analysisScript = getScript();
        ActionURL ret = analysisScript.urlFor(ScriptController.Action.begin);
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
        for (ExpDataRunInput input : app.getDataInputs())
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
            addDataOfType(app.getOutputDatas(), FlowDataType.FCSAnalysis, ret);
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

    public List<ExpMaterial> getSamples()
    {
        ExpProtocolApplication app = getExpObject().getSourceApplication();
        if (app == null)
            return null;

        return app.getInputMaterials();
    }
}
