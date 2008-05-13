/*
 * Copyright (c) 2007 LabKey Corporation
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

package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.labkey.flow.gateeditor.client.model.GWTScript;
import org.labkey.flow.gateeditor.client.model.GWTCompensationMatrix;
import org.labkey.flow.gateeditor.client.model.GWTWell;

public class GWTGraphOptions implements IsSerializable
{
    public GWTScript script;
    public boolean compensation;
    public GWTEditingMode editingMode;
    public GWTCompensationMatrix compensationMatrix;
    public GWTWell well;
    public int width;
    public int height;
    public String xAxis;
    public String yAxis;
    public String subset;


    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof GWTGraphOptions)) return false;

        GWTGraphOptions that = (GWTGraphOptions) o;

        if (!script.equals(that.script)) return false;
        if (!editingMode.equals(that.editingMode)) return false;
        if (compensationMatrix != null ? !compensationMatrix.equals(that.compensationMatrix) : that.compensationMatrix != null) return false;
        if (subset != null ? !subset.equals(that.subset) : that.subset != null) return false;
        if (!well.equals(that.well)) return false;
        if (width != that.width) return false;
        if (height != that.height) return false;
        if (!xAxis.equals(that.xAxis)) return false;
        if (yAxis != null ? !yAxis.equals(that.yAxis) : that.yAxis != null) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = script.hashCode();
        result = 31 * result + (compensationMatrix != null ? compensationMatrix.hashCode() : 0);
        result = 31 * result + well.hashCode();
        result = 31 * result + xAxis.hashCode();
        result = 31 * result + (yAxis != null ? yAxis.hashCode() : 0);
        result = 31 * result + (subset != null ? subset.hashCode() : 0);
        return result;
    }

    public boolean isHistogram()
    {
        return yAxis == null;
    }
}
