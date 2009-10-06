/*
 * Copyright (c) 2005-2009 LabKey Corporation
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

import org.labkey.flow.analysis.data.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.BitSet;

import Jama.Matrix;


/**
 * Created by IntelliJ IDEA.
 * User: mbellew
 * Date: May 2, 2005
 * Time: 3:30:51 PM
 * <p/>
 * DataFrams is much like an R data frame (except all data is type float.
 * I created this to factor the functionality in class FCS
 */
public class DataFrame
{
    protected NumberArray[] data;
    protected Field[] fields;
    protected HashMap<String, Field> fieldsMap = new HashMap<String,Field>();
    protected String version = "";


    public DataFrame(Field[] fields, int rows)
    {
        this(fields, new float[fields.length][rows]);
    }


    public DataFrame(Field[] fields, float[][] data)
    {
        NumberArray[] dataArray = new NumberArray[data.length];
        for (int i = 0; i < dataArray.length; i ++)
        {
            dataArray[i] = new FloatArray(data[i]);
        }
        init(fields, dataArray);
    }

    public DataFrame(Field[] fields, NumberArray[] data)
    {
        init(fields, data);
    }


    public static String canonicalFieldName(String name)
    {
        name = name.toLowerCase().replace(" ", "").replace("-","");
        // UNDONE: these aliases should be configured in admin pages
        name = name.replace("petexasred","petr");
        name = name.replace("petxrd", "petr");
        name = name.replace("cy5.5","cy55");
        return name;
    }


    private void init(Field[] fields, NumberArray[] data)
    {
        assert fields.length == data.length;
        this.fields = fields.clone();
        for (Field field : fields)
        {
            String name = field.getName();
            fieldsMap.put(name, field);
            name = canonicalFieldName(name);
            if (!fieldsMap.containsKey(name))
                fieldsMap.put(name, field);
        }
        this.data = data;
    }


    public DataFrame translate(ScriptSettings settings)
    {
        Field[] fields = new Field[getColCount()];
        NumberArray[] out = new NumberArray[data.length];

        for (int p = 0; p < getColCount(); p++)
        {
            Field field = getField(p);
            ScalingFunction fn = field.getScalingFunction();
            ScriptSettings.ParameterInfo paramInfo = null;
            if (settings != null)
            {
                paramInfo = settings.getParameterInfo(field.getName(), false);
            }
            if (paramInfo != null && paramInfo.getMinValue() != null)
            {
                fn = ScalingFunction.makeFunction(fn, paramInfo.getMinValue().intValue());
            }
            NumberArray from = data[p];

            if (null == fn)
            {
                fields[p] = getField(p);
                out[p] = from;
            }
            else
            {
                fields[p] = new Field(p, getField(p), fn);
				out[p] = fn.translate(from);
            }
        }
        return new DataFrame(fields, out);
    }


    public DataFrame multiply(Matrix mul)
    {
        int rows = getRowCount();
        int cols = getColCount();
        DataFrame out = new DataFrame(fields, new float[cols][rows]);

        Matrix a = new Matrix(cols, 1);
        for (int r = 0; r < rows; r++)
        {
            for (int i = 0; i < cols; i ++)
            {
                a.set(i, 0, data[i].getDouble(r));
            }
            Matrix b = mul.times(a);
            for (int i = 0; i < cols; i ++)
            {
                ScalingFunction fn = fields[i].getScalingFunction();
                out.data[i].set(r, fn.constrain(b.get(i, 0)));
            }
        }
        return out;
    }


    /**
     * For data which comes from a scaling of a discrete set of integer values,
     * randomly change the data within each range.  This is done so that, after
     * applying a compensation matrix, artifacts do not appear in the graph.
     */
    public DataFrame dither()
    {
        int cRow = getRowCount();
        int cCol = getColCount();

        NumberArray[] cols = new NumberArray[getColCount()];

        for (int c = 0; c < cCol; c ++)
        {
            Field field = getField(c);
            ScalingFunction function = field.getScalingFunction();
            if (function == null)
            {
                cols[c] = getColumn(c);
            }
			else
			{
				cols[c] = function.dither(getColumn(c));
			}
        }
        DataFrame out;
		out = new DataFrame(fields, cols);
        return out;
    }


    public DataFrame filter(BitSet bs)
    {
        int cCol = getColCount();
        NumberArray[] cols = new NumberArray[cCol];
        boolean fAlreadySubset = getColumn(0) instanceof SubsetNumberArray;
        IntArray subset;
        if (fAlreadySubset)
        {
            subset = new IntArray(((SubsetNumberArray) getColumn(0)).getSubset(), bs);
        }
        else
        {
            subset = new IntArray(bs);
        }
        for (int i = 0; i < cCol; i++)
        {
            if (fAlreadySubset)
            {
                cols[i] = new SubsetNumberArray(((SubsetNumberArray) getColumn(i)).getArray(), subset);
            }
            else
            {
                cols[i] = new SubsetNumberArray(getColumn(i), subset);
            }
        }
        DataFrame out;
		out = new DataFrame(fields, cols);
        return out;
    }


    public int getRowCount()
    {
        return data[0].size();
    }


    public int getColCount()
    {
        return data.length;
    }


    public Field getField(int i)
    {
        return fields[i];
    }


    public Field getField(String s)
    {
        Field f = fieldsMap.get(s);
        if (f != null)
            return f;
            s = canonicalFieldName(s);
        return fieldsMap.get(s);
    }


    public NumberArray getColumn(String s) throws FlowException
    {
        Field field = getField(s);
        if (field == null)
        {
            throw new FlowException("No such channel '" + s + "' in FCS file");
        }
        return data[field.getIndex()];
    }

    public double[] getDoubleArray(String s) throws FlowException
    {
        NumberArray narray = getColumn(s);
        double[] ret = new double[narray.size()];
        for (int i = 0; i < ret.length; i ++)
        {
            ret[i] = narray.getDouble(i);
        }
        return ret;
    }

    public NumberArray getColumn(int index)
    {
        return data[index];
    }

    public void set(int row, int col, float f)
    {
        data[col].set(row, f);
    }

    public void saveTSV(File fSave) throws IOException
    {
        java.io.PrintStream out = new PrintStream(new FileOutputStream(fSave));
        int rows = getRowCount();
        int cols = getColCount();
        String tab = "";
        for (int p = 0; p < cols; p++)
        {
            out.print(tab);
            out.print(getField(p).getName());
            tab = "\t";
        }
        out.println();

        for (int e = 0; e < rows; e++)
        {
            tab = "";
            for (int p = 0; p < cols; p++)
            {
                out.print(tab);
                out.print(data[p].getDouble(e));
                tab = "\t";
            }
            out.println();
        }
    }

    public void dump()
    {
        for (int irow = 0; irow < getRowCount(); irow ++)
        {
            String line = "";
            for (int icol = 0; icol < getColCount(); icol ++)
            {
                line += getColumn(icol).getDouble(irow) + "\t";
            }
            System.out.println(line);
        }
    }


    public static class Field
    {
        private String _name;
        private String _description;
        private int _index;
        private int _origIndex;

        private int _range;
        private ScalingFunction _scalingFunction;

        public Field(int index, String name, int range)
        {
            _index = index;
            _origIndex = index;
            _name = name;
            _range = range;
        }

        public Field(int index, Field other, ScalingFunction scalingFunction)
        {
            _index = index;
            _origIndex = other._origIndex;
            _name = other.getName();
            _range = other._range;
            _scalingFunction = scalingFunction;
            _description = other.getDescription();
        }

        public Field(int index, Field other, String newName)
        {
            _index = index;
            _origIndex = other._origIndex;
            _name = newName;
            _range = other._range;
            _scalingFunction = other._scalingFunction;
            _description = other._description;
        }

        public String getName()
        {
            return _name;
        }

        public String getDescription()
        {
            return _description;
        }

        public void setDescription(String description)
        {
            _description = description;
        }

        public int getIndex()
        {
            return _index;
        }

        public int getOrigIndex()
        {
            return _origIndex;
        }

        public ScalingFunction getScalingFunction()
        {
            return _scalingFunction;
        }

        public void setScalingFunction(ScalingFunction function)
        {
            _scalingFunction = function;
        }

        public double getMinValue()
        {
            return _scalingFunction.getMinValue();
        }

        public double getMaxValue()
        {
            return _scalingFunction.getMaxValue();
        }

		@Override
		public String toString()
		{
			return _name + "[" + _index + "]";
		}
	}


}
