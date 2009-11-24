/*
 * Copyright (c) 2006-2009 LabKey Corporation
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

package org.labkey.flow.controllers.well;

import org.labkey.flow.data.FlowWell;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.data.FlowCompensationMatrix;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.api.view.ViewForm;
import org.labkey.api.study.Well;

public class ChooseGraphForm extends ViewForm
{
    private int _wellId;
    private int _compId;
    private int _scriptId;
    private int _actionSequence;
    private String _xaxis;
    private String _yaxis;
    private String _subset;


    public void setWellId(int wellId)
    {
        _wellId = wellId;
    }
    public void setCompId(int compId)
    {
        _compId = compId;
    }
    public void setScriptId(int scriptId)
    {
        _scriptId = scriptId;
    }

    public int getWellId()
    {
        return _wellId;
    }

    public int getCompId()
    {
        if (_compId != 0)
            return _compId;
        FlowWell well = getWell();
        if (well == null)
            return 0;
        FlowCompensationMatrix matrix = well.getCompensationMatrix();
        if (matrix != null)
            return matrix.getRowId();
        return 0;
    }

    public int getScriptId()
    {
        if (_scriptId != 0)
            return _scriptId;
        FlowScript script = getWell().getScript();
        if (script != null)
            return script.getRowId();
        return 0;
    }

    private boolean isActionSequence(int i)
    {
        return i == FlowProtocolStep.calculateCompensation.getDefaultActionSequence() ||
                i == FlowProtocolStep.analysis.getDefaultActionSequence();
    }

    public int getActionSequence()
    {
        int ret = _actionSequence;
        if (!isActionSequence(ret))
        {
            ret = getWell().getProtocolApplication().getActionSequence();
        }
        if (!isActionSequence(ret))
        {
            ret = FlowProtocolStep.analysis.getDefaultActionSequence();
        }
        return ret;
    }

    public void setActionSequence(int step)
    {
        _actionSequence = step;
    }

    public String getXaxis()
    {
        return _xaxis;
    }

    public void setXaxis(String xaxis)
    {
        _xaxis = xaxis;
    }
    public void setYaxis(String yaxis)
    {
        _yaxis = yaxis;
    }
    public String getYaxis()
    {
        return _yaxis;
    }
    public void setSubset(String subset)
    {
        _subset = subset;
    }
    public String getSubset()
    {
        return _subset;
    }

    public FlowWell getWell()
    {
        return FlowWell.fromWellId(getWellId());
    }

    public FlowScript getScript()
    {
        if (_scriptId != 0)
        {
            return FlowScript.fromScriptId(_scriptId);
        }
        return getWell().getScript();
    }

    public FlowCompensationMatrix getCompensationMatrix()
    {
        if (_compId != 0)
        {
            return FlowCompensationMatrix.fromCompId(_compId);
        }
        return getWell().getCompensationMatrix();
    }
}
