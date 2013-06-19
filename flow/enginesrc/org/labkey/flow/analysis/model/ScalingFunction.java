/*
 * Copyright (c) 2005-2013 LabKey Corporation
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

import org.labkey.flow.analysis.data.NumberArray;
import org.labkey.flow.analysis.data.FloatArray;
import org.labkey.flow.analysis.data.ConstrainedNumberArray;
import org.labkey.flow.analysis.data.DoubleArray;

import static java.lang.Math.max;

/**
 * User: mbellew
 * Date: May 2, 2005
 * Time: 3:08:10 PM
 */
public abstract class ScalingFunction implements Cloneable
{
	double _minValue;
    double _range;

	/**
	 * @param decade	0 for linear, otherwise maxOutput = 10^decade
	 * @param scale		linear scaling factor
	 * @param range		max input value
	 * @param minValue	min output value
	 * @return
	 */
	private static ScalingFunction makeFunction(double decade, double scale, double range, double minValue)
	{
		ScalingFunction fn;
		if (decade > 0)
            //if (fcsVersion == 2.0 || bits < 32)
                fn = new LogarithmicFunction(decade, scale, range);
            //else
            //    fn = new LogicleFunction(4.5, 1.0, -10.0, 0);
		else if (scale == 0 || scale == 1)
			fn = new IdentityFunction();
		else
			fn = new LinearFunction(scale);
		fn._range = range;
        fn._minValue = fn.translate(minValue);
		return fn;
	}

	
	static ScalingFunction makeFunction(double decade, double scale, double range)
	{
		return makeFunction(decade, scale, range, 0);
	}


	static ScalingFunction makeFunction(ScalingFunction that, double minValue)
	{
        try
        {
            ScalingFunction fn = (ScalingFunction) that.clone();
            fn._minValue = fn.translate(minValue);
            return fn;
        }
        catch (CloneNotSupportedException e)
        {
            throw new RuntimeException(e);
        }
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
		return max(_minValue, value);
    }

    public abstract boolean isLogarithmic();

    public abstract double dither(double value);

    public NumberArray dither(NumberArray from)
    {
        int len = from.size();
        float[] out = new float[len];
        if (from instanceof FloatArray)
        {
            float[] in = ((FloatArray)from).getArray();
            for (int i=0 ; i<len ; i++)
                out[i] =  (float)(dither(in[i]));
        }
        else if (from instanceof DoubleArray)
        {
            double[] in = ((DoubleArray)from).getArray();
            for (int i=0 ; i<len ; i++)
                out[i] =  (float)(dither(in[i]));
        }
        else
        {
            for (int i=0 ; i<len ; i++)
                out[i] = (float)dither(from.getDouble(i));
        }
        return new FloatArray(out);
    }

	public abstract double translate(double value);

	public NumberArray translate(NumberArray from)
	{
		int len = from.size();
		float[] out = new float[len];
		if (from instanceof FloatArray)
		{
			float[] in = ((FloatArray)from).getArray();
			for (int i=0 ; i<len ; i++)
				out[i] = (float)translate(in[i]);
		}
		else if (from instanceof DoubleArray)
		{
			double[] in = ((DoubleArray)from).getArray();
			for (int i=0 ; i<len ; i++)
				out[i] = (float)translate(in[i]);
		}
		else
		{
			for (int i=0 ; i<len ; i++)
				out[i] = (float)translate(from.getDouble(i));
		}
		return new FloatArray(out);
	}


	protected ScalingFunction()
	{
	}


    static abstract class SimpleFunction extends ScalingFunction
    {
        public boolean isLogarithmic()
        {
            return false;
        }

        public double dither(double value)
        {
            return value;
        }

        @Override
        public NumberArray dither(NumberArray values)
        {
            return values;
        }
    }

	static class IdentityFunction extends SimpleFunction
	{
		IdentityFunction()
		{
		}

		public final double translate(double value)
		{
			return value;
		}

		public NumberArray translate(NumberArray from)
		{
			return new ConstrainedNumberArray(from, getMinValue(), getMaxValue());
		}
	}


	static class LinearFunction extends SimpleFunction
	{
        double _scale;

		LinearFunction(double scale)
		{
			_scale = scale == 0 ? 1 : scale;
		}
		
		public final double translate(double value)
		{
			return _scale * value;
		}
	}


	static class LogarithmicFunction extends ScalingFunction
	{
        double _decade;
        double _scale;
        double _exp;

		protected LogarithmicFunction(double decade, double scale, double range)
		{
			_decade = decade;
			_scale = scale == 0 ? 1 : scale;
			_range = range;
			if (_decade != 0)
				_exp = Math.log(Math.pow(10, _decade)) / _range;
		}

        public boolean isLogarithmic()
        {
            return true;
        }

		public final double translate(double value)
		{
			return _scale * Math.exp(value * _exp);
		}

		public final double dither(double value)
		{
			return value * Math.exp((Math.random() - .5) * _exp);
		}
	}

}
