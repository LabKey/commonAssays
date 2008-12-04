/*
 * Copyright (c) 2008 LabKey Corporation
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

import org.labkey.flow.gateeditor.client.model.GWTEditingMode;
import org.labkey.flow.gateeditor.client.model.GWTGraphOptions;
import org.labkey.flow.data.FlowCompensationMatrix;
import org.labkey.flow.data.FlowWell;
import org.apache.commons.lang.StringUtils;

/**
 * User: kevink
* Date: Nov 27, 2008 11:29:00 AM
*/
public class GraphForm extends EditScriptForm
{
    public GWTEditingMode editingMode;
    public int height = 300;
    public int width = 300;
    public boolean open;
    public String subset;
    public String xAxis;
    public String yAxis;
    public double[] ptX;
    public double[] ptY;

    public void setEditingMode(String editingMode)
    {
        this.editingMode = GWTEditingMode.valueOf(editingMode);
    }

    public void setWidth(int width)
    {
        this.width = width;
    }
    public void setHeight(int height)
    {
        this.height = height;
    }
    public void setOpen(boolean open)
    {
        this.open = open;
    }
    public GWTGraphOptions getGraphOptions() throws Exception
    {
        GWTGraphOptions ret = new GWTGraphOptions();
        GateEditorServiceImpl service = new GateEditorServiceImpl(getViewContext());
        ret.script = service.makeGWTScript(analysisScript);
        ret.compensation = editingMode.isCompensation();
        ret.editingMode = editingMode;
        FlowCompensationMatrix m = getCompensationMatrix();
        if (null != m)
            ret.compensationMatrix = service.makeCompensationMatrix(m, false);
        ret.well = service.makeWell(getWell());
        ret.width = width;
        ret.height = height;
        ret.xAxis = xAxis;
        if (!StringUtils.isEmpty(yAxis))
            ret.yAxis = yAxis;
        ret.subset = subset;
        return ret;
    }

    public void setSubset(String subset)
    {
        this.subset = subset;
    }

    public void setXaxis(String axis)
    {
        this.xAxis = axis;
    }

    public void setYaxis(String axis)
    {
        this.yAxis = axis;
    }

    public void setPtX(double[] ptX)
    {
        this.ptX = ptX;
    }

    public void setPtY(double[] ptY)
    {
        this.ptY = ptY;
    }

    public FlowWell getWell() throws Exception
    {
        return FlowWell.fromURL(getContext().getActionURL(), getRequest());
    }
}
