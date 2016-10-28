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

import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpProtocolAction;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.controllers.FlowParam;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class FlowProtocolStep implements Serializable
{
    static public FlowProtocolStep keywords = new FlowProtocolStep("Keywords", "Read a directory containing FCS files", ExpProtocol.ApplicationType.ProtocolApplication, 10);
    static public FlowProtocolStep calculateCompensation = new FlowProtocolStep("Compensation", "Calculate the compensation matrix", ExpProtocol.ApplicationType.ProtocolApplication, 20);
    static public FlowProtocolStep defineGates = new FlowProtocolStep("DefineGates", "Define Gates", ExpProtocol.ApplicationType.ProtocolApplication, 25);
    static public FlowProtocolStep analysis = new FlowProtocolStep("Analysis", "Calculate statistics and generate graphs for Flow Data", ExpProtocol.ApplicationType.ProtocolApplication, 30);
    static public FlowProtocolStep markRunOutputs = new FlowProtocolStep("MarkRunOutputData", "", ExpProtocol.ApplicationType.ExperimentRunOutput, 1000, keywords, calculateCompensation, analysis);

    final String lsidName;
    final String description;
    final ExpProtocol.ApplicationType applicationType;
    final int actionSequence;
    final FlowProtocolStep[] predecessors;

    public FlowProtocolStep(String lsidName, String protocolDescription, ExpProtocol.ApplicationType applicationType, int actionSequence, FlowProtocolStep ... predecessors)
    {
        this.lsidName = lsidName;
        this.description = protocolDescription;
        this.applicationType = applicationType;
        this.actionSequence = actionSequence;
        this.predecessors = predecessors;
    }

    public ExpProtocolAction addAction(User user, ExpProtocol parentProtocol)
    {
        return parentProtocol.addStep(user, ensureForContainer(user, parentProtocol.getContainer()).getExpObject(), getDefaultActionSequence());
    }

    public String getLSID(Container container)
    {
        return FlowObject.generateLSID(container, FlowProtocol.getProtocolLSIDPrefix(), lsidName);
    }

    public String getLSID()
    {
        throw new UnsupportedOperationException();
    }

    public String getOwnerObjectLSID()
    {
        throw new UnsupportedOperationException();
    }

    public String getName()
    {
        return lsidName;
    }

    public int getDefaultActionSequence()
    {
        return actionSequence;
    }

    public void addParams(ActionURL url)
    {
        url.addParameter(FlowParam.actionSequence.toString(), Integer.toString(getDefaultActionSequence()));
    }

    static public FlowProtocolStep fromActionSequence(Integer actionSequence)
    {
        if (actionSequence == null)
            return null;
        if (actionSequence == keywords.getDefaultActionSequence())
            return keywords;
        if (actionSequence == calculateCompensation.getDefaultActionSequence())
            return calculateCompensation;
        if (actionSequence == analysis.getDefaultActionSequence())
            return analysis;
        return null;
    }

    static public FlowProtocolStep fromRequest(HttpServletRequest request)
    {
        String strActionSequence = request.getParameter("actionSequence");
        if (strActionSequence == null || strActionSequence.length() == 0)
            return null;
        try
        {
            return fromActionSequence(Integer.valueOf(strActionSequence));
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }

    public FlowProtocol ensureForContainer(User user, Container container)
    {
        FlowProtocol ret = getForContainer(container);
        if (ret != null)
            return ret;
        ExpProtocol protocol = ExperimentService.get().createExpProtocol(container, applicationType, getName());
        protocol.setDescription(description);
        protocol.save(user);
        return new FlowProtocol(protocol);
    }

    public FlowProtocol getForContainer(Container container)
    {
        return FlowProtocol.getForContainer(container, getName());
    }

    public void addParams(Map<FlowParam, Object> map)
    {
        map.put(FlowParam.actionSequence, getDefaultActionSequence());
    }

    public String getContainerId()
    {
        throw new UnsupportedOperationException();
    }

    public String getLabel()
    {
        return getName();
    }

    public FlowObject getParent()
    {
        return null;
    }

    public ActionURL urlShow()
    {
        throw new UnsupportedOperationException();
    }

    public ExpProtocolAction getAction(ExpProtocol protocol)
    {
        for (ExpProtocolAction step : protocol.getSteps())
        {
            if (step.getActionSequence() == getDefaultActionSequence())
            {
                return step;
            }
        }
        return null;
    }

    static public void initProtocol(User user, FlowProtocol flowProtocol) throws Exception
    {
        ExpProtocol protocol = flowProtocol.getExpObject();
        Map<Integer, ExpProtocolAction> existingSteps = new HashMap();
        for (ExpProtocolAction existingStep : protocol.getSteps())
        {
            existingSteps.put(existingStep.getActionSequence(), existingStep);
        }


        ExpProtocolAction stepRun = existingSteps.get(0);
        if (stepRun == null)
        {
            stepRun = protocol.addStep(user, protocol, 0);
            stepRun.addSuccessor(user, stepRun);
        }
        ExpProtocolAction stepMarkRunOutputs = existingSteps.get(markRunOutputs.getDefaultActionSequence());
        if (stepMarkRunOutputs == null)
        {
            stepMarkRunOutputs = markRunOutputs.addAction(user, protocol);
        }
        for (FlowProtocolStep step : new FlowProtocolStep[] { keywords, calculateCompensation, defineGates, analysis })
        {
            if (existingSteps.containsKey(step.getDefaultActionSequence()))
                continue;
            ExpProtocolAction action = step.addAction(user, protocol);
            stepRun.addSuccessor(user, action);
            action.addSuccessor(user, stepMarkRunOutputs);
        }
    }

    static public FlowProtocolStep fromLSID(Container container, String lsid)
    {
        if (lsid.equals(keywords.getLSID(container)))
            return keywords;
        if (lsid.equals(calculateCompensation.getLSID(container)))
            return calculateCompensation;
        if (lsid.equals(analysis.getLSID(container)))
            return analysis;
        return null;
    }
}
