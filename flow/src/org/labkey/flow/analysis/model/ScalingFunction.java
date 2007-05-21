/*
 * Copyright (C) 2005 LabKey LLC. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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


}
