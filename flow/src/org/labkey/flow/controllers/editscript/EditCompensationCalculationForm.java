/*
 * Copyright (c) 2006-2012 LabKey Corporation
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

import org.fhcrc.cpas.flow.script.xml.ChannelDef;
import org.fhcrc.cpas.flow.script.xml.ChannelSubsetDef;
import org.fhcrc.cpas.flow.script.xml.CompensationCalculationDef;
import org.fhcrc.cpas.flow.script.xml.CriteriaDef;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.data.FlowProtocolStep;

import java.util.ArrayList;
import java.util.List;

public class EditCompensationCalculationForm extends EditSettingsForm
{
    public int selectedRunId;
    public Workspace workspace;
    public String selectGroupName;
    public String selectAutoCompScript;
    public String[] parameters = new String[0];
    public String[] positiveKeywordName;
    public String[] positiveKeywordValue;
    public String[] positiveSubset;
    public String[] negativeKeywordName;
    public String[] negativeKeywordValue;
    public String[] negativeSubset;

    public void setSelectedRunId(int runId)
    {
        this.selectedRunId = runId;
    }

    public String getSelectGroupName()
    {
        return selectGroupName;
    }

    public void setSelectGroupName(String selectGroupName)
    {
        this.selectGroupName = selectGroupName;
    }

    public String getSelectAutoCompScript()
    {
        return selectAutoCompScript;
    }

    public void setSelectAutoCompScript(String selectAutoCompScript)
    {
        this.selectAutoCompScript = selectAutoCompScript;
    }

    public String[] getPositiveKeywordName()
    {
        return positiveKeywordName;
    }
    public void setPositiveKeywordName(String[] value)
    {
        throw new UnsupportedOperationException();
    }
    public String[] getPositiveKeywordValue()
    {
        return positiveKeywordValue;
    }
    public void setPositiveKeywordValue(String[] value)
    {
        throw new UnsupportedOperationException();
    }
    public String[] getPositiveSubset()
    {
        return positiveSubset;
    }
    public void setPositiveSubset(String[] value)
    {
        throw new UnsupportedOperationException();
    }
    public String[] getNegativeKeywordName()
    {
        return negativeKeywordName;
    }
    public void setNegativeKeywordName(String[] value)
    {
        throw new UnsupportedOperationException();
    }
    public String[] getNegativeKeywordValue()
    {
        return negativeKeywordValue;
    }
    public void setNegativeKeywordValue(String[] value)
    {
        throw new UnsupportedOperationException();
    }
    public String[] getNegativeSubset()
    {
        return negativeSubset;
    }
    public void setNegativeSubset(String[] value)
    {
        throw new UnsupportedOperationException();
    }

    protected String trimRootSubset(String subset)
    {
        if (subset == null)
            return null;
        int ichSlash = subset.indexOf("/");
        if (ichSlash < 0)
            return subset;
        return subset.substring(ichSlash + 1);
    }

    /**
     * Called when we know what the workspace is.  At this point, we fill in what the current values
     * of the compensation stuff are.
     */
    protected void setWorkspace(Workspace workspace)
    {
        this.workspace = workspace;
        List<String> lstParameters = new ArrayList();
        CompensationCalculationDef calc = analysisDocument.getScript().getCompensationCalculation();
        if (workspace == null)
        {
            if (calc != null)
            {
                for (ChannelDef channel : calc.getChannelArray())
                {
                    lstParameters.add(channel.getName());
                }
            }
        }
        else
        {
            lstParameters.addAll(this.workspace.getParameterNames());
        }
        this.parameters = lstParameters.toArray(new String[0]);
        int parameterCount = this.parameters.length;
        this.positiveKeywordName = new String[parameterCount];
        this.positiveKeywordValue = new String[parameterCount];
        this.positiveSubset = new String[parameterCount];
        this.negativeKeywordName = new String[parameterCount];
        this.negativeKeywordValue = new String[parameterCount];
        this.negativeSubset = new String[parameterCount];
        if (calc == null)
            return;
        for (ChannelDef channel : calc.getChannelArray())
        {
            int index = lstParameters.indexOf(channel.getName());
            if (index < 0)
                continue;
            ChannelSubsetDef positive = channel.getPositive();
            if (positive != null)
            {
                this.positiveSubset[index] = positive.getSubset();
                CriteriaDef criteria = positive.getCriteria();
                if (criteria != null)
                {
                    this.positiveKeywordName[index] = criteria.getKeyword();
                    this.positiveKeywordValue[index] = criteria.getPattern();
                }
            }
            ChannelSubsetDef negative = channel.getNegative();
            if (negative != null)
            {
                this.negativeSubset[index] = negative.getSubset();
                CriteriaDef criteria = negative.getCriteria();
                if (criteria != null)
                {
                    this.negativeKeywordName[index] = criteria.getKeyword();
                    this.negativeKeywordValue[index] = criteria.getPattern();
                }
            }
        }


    }

    @Override
    public void reset()
    {
        super.reset();
        this.step = FlowProtocolStep.calculateCompensation;

        try
        {
            setWorkspace((Workspace) PageFlowUtil.decodeObject(getRequest().getParameter("workspaceObject")));
        }
        catch (Exception e)
        {
            throw UnexpectedException.wrap(e);
        }
    }
}
