/*
 * Copyright (c) 2005-2011 LabKey Corporation
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

import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Logger;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpDataRunInput;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.persist.FlowManager;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FlowWell extends FlowDataObject
{
    static private final Logger _log = Logger.getLogger(FlowWell.class);

    static public FlowWell fromWellId(int id)
    {
        FlowDataObject flowobj = fromRowId(id);
        if (flowobj instanceof FlowWell)
            return (FlowWell)flowobj;
        return null;
    }

    static public List<? extends FlowWell> fromWellIds(int... ids)
    {
        List<FlowWell> wells = new ArrayList<FlowWell>(ids.length);
        List<FlowDataObject> flowobjs = fromRowIds(ids);
        for (FlowDataObject flowobj : flowobjs)
            if (flowobj instanceof FlowWell)
                wells.add((FlowWell)flowobj);
        return wells;
    }

    static public List<FlowWell> fromWellIds(Collection<Integer> ids)
    {
        List<FlowWell> wells = new ArrayList<FlowWell>(ids.size());
        List<FlowDataObject> flowobjs = fromRowIds(ids);
        for (FlowDataObject flowobj : flowobjs)
            if (flowobj instanceof FlowWell)
                wells.add((FlowWell)flowobj);
        return wells;
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

    /**
     * Get the FlowFCSFile DataInput of this FCSAnalysis (or fake FCSFile.)
     * @return
     */
    public FlowFCSFile getFCSFileInput()
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

    /**
     * If this FlowFCSFile was created by an external analysis import (FlowJo workspace keywords, R normalization keywords),
     * try to get the original FCSFile well if possible.  The original FCSFile may be null if it doesn't exist or
     * wasn't associated with the external analysis during import.
     *
     * @return Original FlowFCSFile well if possible.
     * @see org.labkey.flow.data.FlowFCSFile#isOriginalFCSFile()
     */
    public FlowFCSFile getOriginalFCSFile()
    {
        if (getDataType() != FlowDataType.FCSFile)
            return null;

        // UNDONE: Use the pre-calculated column flow.object.fcsid instead
        for (ExpData input : getProtocolApplication().getInputDatas())
        {
            if (input.getDataType() == FlowDataType.FCSFile)
                return (FlowFCSFile) FlowDataObject.fromData(input);
        }

        // CONSIDER: Also find FlowFCSFile well with same URI as this well.
        return null;
    }

    /**
     * Get all FlowFCSFile wells that are DataOutputs of this 'fake' FCSFile well.
     * @return
     */
    public List<FlowFCSFile> getFCSFileOutputs()
    {
        ExpProtocolApplication[] apps = getExpObject().getTargetApplications();
        List<FlowFCSFile> ret = new ArrayList<FlowFCSFile>();
        for (ExpProtocolApplication app : apps)
        {
            addDataOfType(app.getOutputDatas(), FlowDataType.FCSFile, ret);
        }
        return ret;
    }

    /**
     * Get all FlowFCSAnalysis wells that are DataOutputs of this FCSFile well.
     * @return
     */
    public List<FlowFCSAnalysis> getFCSAnalysisOutputs() throws SQLException
    {
        ExpProtocolApplication[] apps = getExpObject().getTargetApplications();
        List<FlowFCSAnalysis> ret = new ArrayList<FlowFCSAnalysis>();
        for (ExpProtocolApplication app : apps)
        {
            addDataOfType(app.getOutputDatas(), FlowDataType.FCSAnalysis, ret);
        }
        return ret;
    }

    public URI getFCSURI()
    {
        return getAttributeSet().getURI();
    }

    public String getKeyword(String keyword)
    {
        return FlowManager.get().getKeyword(getData(), keyword);
    }

    public void setKeyword(String keyword, String value) throws SQLException
    {
        FlowManager.get().setKeyword(getContainer(), getData(), keyword, value);
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
        return urlFor(WellController.ShowWellAction.class);
    }

    public ActionURL urlEditAnalysisScript() throws Exception
    {
        FlowScript analysisScript = getScript();
        ActionURL ret = analysisScript.urlFor(ScriptController.BeginAction.class);
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
