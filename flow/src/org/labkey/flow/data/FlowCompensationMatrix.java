/*
 * Copyright (c) 2005-2017 LabKey Corporation
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

import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.FlowSettings;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.compensation.CompensationController;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.AttributeSetHelper;
import org.labkey.flow.persist.FlowDataHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class FlowCompensationMatrix extends FlowDataObject implements Serializable
{
    public FlowCompensationMatrix(ExpData data)
    {
        super(data);
    }

    static public FlowCompensationMatrix fromCompId(int id)
    {
        FlowDataObject flowobj = fromRowId(id);
        if (flowobj instanceof FlowCompensationMatrix)
            return (FlowCompensationMatrix)flowobj;
        return null;
    }

    static public FlowCompensationMatrix fromURL(ActionURL url, HttpServletRequest request, Container actionContainer, User user)
    {
        FlowCompensationMatrix ret = fromCompId(getIntParam(url, request, FlowParam.compId));
        if (ret != null)
            ret.checkContainer(actionContainer, user, url);
        return ret;
    }

    static public FlowCompensationMatrix create(User user, Container container, String name, AttributeSet attrs) throws Exception
    {
        ExperimentService svc = ExperimentService.get();

        FlowCompensationMatrix flowComp;
        try (DbScope.Transaction transaction = svc.ensureTransaction())
        {
            ExpData data;
            if (name == null)
            {
                data = svc.createData(container, FlowDataType.CompensationMatrix);
            }
            else
            {
                data = svc.createData(container, FlowDataType.CompensationMatrix, name);
            }
            data.setDataFileURI(new File(FlowSettings.getWorkingDirectory(), "compensation." + FlowDataHandler.EXT_DATA).toURI());
            data.save(user);
            AttributeSetHelper.doSave(attrs, user, data);
            flowComp = (FlowCompensationMatrix) FlowDataObject.fromData(data);
            transaction.commit();
            return flowComp;
        }
    }

    public CompensationMatrix getCompensationMatrix()
    {
        return getCompensationMatrix(getName(), getAttributeSet());
    }

    static public CompensationMatrix getCompensationMatrix(String name, AttributeSet attrs)
    {
        TreeSet<String> channelNames = new TreeSet();
        Map<String, Double> values = new HashMap();
        for (Map.Entry<StatisticSpec, Double> entry : attrs.getStatistics().entrySet())
        {
            StatisticSpec spec = entry.getKey();
            if (spec.getStatistic() != StatisticSpec.STAT.Spill)
                continue;
            String strParameter = spec.getParameter();
            int ichColon = strParameter.indexOf(":");

            String strChannel = strParameter.substring(0, ichColon);
            channelNames.add(strChannel);
            values.put(spec.getParameter(), entry.getValue());
        }
        if (channelNames.size() == 0)
            return null;
        CompensationMatrix ret = new CompensationMatrix(name);
        String[] arrChannelNames = channelNames.toArray(new String[channelNames.size()]);

        for (int iChannel = 0; iChannel < arrChannelNames.length; iChannel ++)
        {
            Map<String, Double> channelValues = new TreeMap();
            for (int iChannelValue = 0; iChannelValue < arrChannelNames.length; iChannelValue ++)
            {
                String key = arrChannelNames[iChannel] + ":" + arrChannelNames[iChannelValue];
                channelValues.put(arrChannelNames[iChannelValue], values.get(key));
            }
            ret.setChannel(arrChannelNames[iChannel], channelValues);
        }
        return ret;
    }

    public ActionURL urlShow()
    {
        return urlFor(CompensationController.ShowCompensationAction.class);
    }

    public ActionURL urlDownload()
    {
        return urlFor(CompensationController.DownloadAction.class);
    }

    public void addParams(Map<FlowParam, Object> map)
    {
        map.put(FlowParam.compId, getCompId());
    }


    public String getLabel()
    {
        return getLabel(false);
    }

    public String getLabel(boolean includeExperiment)
    {
        FlowRun run = getRun();
        if (run != null)
        {
            String strLabel = getName();
            if (includeExperiment)
            {
                FlowExperiment experiment = run.getExperiment();
                if (experiment != null)
                {
                    return strLabel + " (" + experiment.getLabel() + ")";
                }
            }
            return strLabel;
        }
        return getName();
    }
    public int getCompId()
    {
        return getRowId();
    }

    static public List<FlowCompensationMatrix> getCompensationMatrices(Container container)
    {
        return (List) FlowDataObject.fromDataType(container, FlowDataType.CompensationMatrix);
    }
    static public List<FlowCompensationMatrix> getUploadedCompensationMatrices(Container container)
    {
        List<FlowCompensationMatrix> all = getCompensationMatrices(container);
        List<FlowCompensationMatrix> ret = new ArrayList();
        for (FlowCompensationMatrix comp : all)
        {
            if (comp.getRun() == null)
                ret.add(comp);
        }
        return ret;
    }
}
