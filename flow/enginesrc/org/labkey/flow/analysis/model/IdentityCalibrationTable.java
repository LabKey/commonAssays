/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

package org.labkey.flow.analysis.model;

public class IdentityCalibrationTable implements CalibrationTable
{
    double range;
    public IdentityCalibrationTable(double range)
    {
        this.range = range;
    }


    @Override
    public double fromIndex(double index)
    {
        return index;
    }

    @Override
    public double getRange()
    {
        return range;
    }

    @Override
    public double indexOf(double value)
    {
        return value;
    }

    @Override
    public boolean isLinear()
    {
        return true;
    }
}
