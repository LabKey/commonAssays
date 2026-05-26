/*
 * Copyright (c) 2021-2026 LabKey Corporation
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
package org.labkey.flow.analysis.util;

// NOTE: Unlike the other range functions, this adjusts for the min/max range
public class LinearRangeFunction extends AbstractRangeFunction
{
    private final double _gain;

    public LinearRangeFunction(double min, double max, double gain)
    {
        super(min, max);
        this._gain = gain;
    }

    @Override
    public boolean isLogarithmic()
    {
        return false;
    }

    @Override
    public double compute(double range)
    {
        if (range == 0d)
            return 0d;

        return ((range * getWidth()) + _min) / _gain;
    }

    @Override
    public double invert(double domain)
    {
        if (domain == 0d)
            return 0d;

        return ((domain * _gain) - _min) / getWidth();
    }
}
