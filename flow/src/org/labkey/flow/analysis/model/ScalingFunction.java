/*
 * Copyright (c) 2005-2007 LabKey Corporation
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

/**
 * Created by IntelliJ IDEA.
 * User: mbellew
 * Date: May 2, 2005
 * Time: 3:08:10 PM
 */
public class ScalingFunction
{
	double _minValue;
    double _range;
    double _decade;
    double _scale;
    double _exp;

    public ScalingFunction(double decade, double scale, double range)
    {
        _decade = decade;
        _scale = scale;
        _range = range;
        if (_decade != 0)
        {
            if (_scale == 0)
                _scale = 1;
            _exp = Math.log(Math.pow(10, _decade)) / _range;
        }

    }

    public ScalingFunction(ScalingFunction that, double minValue)
    {
        this(that._decade, that._scale, that._range);
        this._minValue = minValue;
    }

    public double getMinValue()
    {
        return _minValue;
    }
    public double getMaxValue()
    {
        return translate(_range);
    }
    public double constrain(double value)
    {
        if (value < _minValue)
            return _minValue;
        return value;
    }

    public double dither(double value)
    {
        if (_decade == 0)
            return -1;
        double rand = Math.random() - .5;
        return value * Math.exp(rand * _exp) * _scale;
    }

    public boolean isLogarithmic()
    {
        return _decade != 0;
    }
    public double translate(double value)
    {
        if (_decade == 0)
        {
            if (_scale == 0)
                return value;
            return value * _scale;
        }
        else
        {
            return _scale * Math.exp(value * _exp);
        }
    }

    public boolean isIdentity()
    {
        return _decade == 0 && _scale == 0;
    }

}
