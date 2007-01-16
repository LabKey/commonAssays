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

import org.labkey.flow.analysis.data.NumberArray;
import org.labkey.flow.analysis.data.FloatArray;
import org.labkey.flow.analysis.data.IntArray;
import org.labkey.flow.analysis.data.SubsetNumberArray;
import org.labkey.flow.analysis.web.StatisticSpec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.BitSet;
import java.util.Arrays;

import Jama.Matrix;
import org.labkey.api.view.Stats;


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
    protected HashMap fieldsMap = new HashMap();
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

    private void init(Field[] fields, NumberArray[] data)
    {
        assert fields.length == data.length;
        this.fields = (Field[]) fields.clone();
        for (int i = 0; i < fields.length; i++)
            fieldsMap.put(fields[i].getName(), fields[i]);
        this.data = data;
    }


    public DataFrame Translate(ScalingFunction[] translate)
    {
        Field[] fields = new Field[getColCount()];
        NumberArray[] out = new NumberArray[data.length];

        for (int p = 0; p < getColCount(); p++)
        {
            ScalingFunction fn = translate[p];
            NumberArray from = data[p];

            if (null == fn)
            {
                fields[p] = getField(p);
                out[p] = from;
            }
            else
            {
                fields[p] = new Field(p, getField(p), fn);
                int len = from.size();
                float[] to = new float[len];
                out[p] = new FloatArray(to);
                for (int e = 0; e < len; e++)
                    to[e] = (float) fn.translate(from.getDouble(e));
            }
        }
        return new DataFrame(fields, out);
    }

    public DataFrame Multiply(Matrix mul)
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
                out.data[i].set(r, b.get(i, 0));
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
            if (function == null || !function.isLogarithmic())
            {
                cols[c] = getColumn(c);
                continue;
            }
            cols[c] = new FloatArray(new float[getRowCount()]);
            for (int r = 0; r < cRow; r++)
            {
                double value = getColumn(c).getDouble(r);
                double newVal = function.dither(value);
                cols[c].set(r, newVal);
            }
        }
        DataFrame out = new DataFrame(fields, cols);
        return out;
    }

    /**
     * Round the data off to the nearest value that maps to a scaling of an
     * integer value.  This is what FlowJo does with data after applying
     * a compensation matrix.
     */
    public DataFrame coerceToBuckets()
    {
        int cRow = getRowCount();
        int cCol = getColCount();

        NumberArray[] cols = new NumberArray[fields.length];

        for (int c = 0; c < cCol; c ++)
        {
            Field field = getField(c);
            ScalingFunction function = field.getScalingFunction();
            if (function == null || !function.isLogarithmic())
            {
                cols[c] = getColumn(c);
                continue;
            }
            cols[c] = new FloatArray(new float[getRowCount()]);
            for (int r = 0; r < cRow; r++)
            {
                double value = getColumn(c).getDouble(r);
                double rawVal = function.untranslate(value);
                rawVal = Math.min(rawVal, field._range);
                rawVal = Math.max(rawVal, 0);
                double minVal = Math.floor(rawVal);
                double maxVal = Math.ceil(rawVal);

                double scaledMin = function.translate(minVal);
                double scaledMax = function.translate(maxVal);
                double newVal;
                if (value - scaledMin < scaledMax - value)
                    newVal = scaledMin;
                else
                    newVal = scaledMax;

                cols[c].set(r, newVal);
            }
        }
        DataFrame out = new DataFrame(fields, cols);
        return out;
    }

    public DataFrame Filter(BitSet bs)
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
        DataFrame out = new DataFrame(fields, cols);
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
        return (Field) fieldsMap.get(s);
    }

    public NumberArray getColumn(String s)
    {
        Field field = getField(s);
        if (field == null)
        {
            throw new IllegalArgumentException("No such field '" + s + "'");
        }
        return data[field.getIndex()];
    }

    public double[] getDoubleArray(String s)
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

    public void SaveTSV(File fSave) throws IOException
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

        public int getRange()
        {
            return _range;
        }
    }


}
