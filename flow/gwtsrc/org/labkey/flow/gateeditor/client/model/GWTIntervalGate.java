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

public class GWTIntervalGate extends GWTGate
{
    private double minValue;
    private double maxValue;
    private String axis;

    public GWTIntervalGate()
    {
        super("interval");
    }

    public GWTIntervalGate(String axis, double minValue, double maxValue)
    {
        this();
        this.axis = axis;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public void setMinValue(double minValue)
    {
        this.minValue = minValue;
    }

    public void setMaxValue(double maxValue)
    {
        this.maxValue = maxValue;
    }

    public void setAxis(String axis)
    {
        this.axis = axis;
    }

    public double getMinValue()
    {
        return minValue;
    }

    public double getMaxValue()
    {
        return maxValue;
    }

    public String getAxis()
    {
        return axis;
    }

    public boolean canSave()
    {
        return true;
    }

//    public GWTGate close()
//    {
//        return new GWTIntervalGate(getAxis(), getMinValue(), getMaxValue());
//    }

    public boolean equals(Object other)
    {
        if (!(other instanceof GWTIntervalGate))
            return false;
        GWTIntervalGate that = (GWTIntervalGate) other;
        return this.getAxis().equals(that.getAxis()) &&
                this.getMinValue() == that.getMinValue() &&
                this.getMaxValue() == that.getMaxValue();
    }

    public int hashCode()
    {
        int result = new Double(minValue).hashCode();
        result = 31 * result + new Double(maxValue).hashCode();
        result = 31 * result + axis.hashCode();
        return result;
    }
}
