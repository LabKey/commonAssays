/*
 * Copyright (c) 2005-2008 LabKey Corporation
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

import java.io.Serializable;
import java.util.List;


public class Polygon implements Serializable
{
    public int len;
    public double[] X;
    public double[] Y;
    public double xmin = Double.MAX_VALUE;
    public double ymin = Double.MAX_VALUE;
    public double xmax = -Double.MAX_VALUE;
    public double ymax = -Double.MAX_VALUE;

    public Polygon()
    {
        X = new double[4];
        Y = new double[4];
        len = 0;
    }

    public Polygon(double[] X, double[] Y)
    {
        assert X.length == Y.length;
        this.len = X.length;
        this.X = X.clone();
        this.Y = Y.clone();
        computeBoundingRectangle();
    }

    static private double[] toDoubleArray(List<Double> lst)
    {
        double[] ret = new double[lst.size()];
        for (int i = 0; i < ret.length; i ++)
        {
            ret[i] = lst.get(i).doubleValue();
        }
        return ret;
    }

    public Polygon(List<Double> lstX, List<Double> lstY)
    {
        this(toDoubleArray(lstX), toDoubleArray(lstY));
    }

    void computeBoundingRectangle()
    {
        for (int i = 0; i < len; i++)
        {
            xmin = Math.min(xmin, X[i]);
            xmax = Math.max(xmax, X[i]);
            ymin = Math.min(ymin, Y[i]);
            ymax = Math.max(ymax, Y[i]);
        }
    }

    void updateBoundingRectangle(double x, double y)
    {
        xmin = Math.min(xmin, x);
        xmax = Math.max(xmax, x);
        ymin = Math.min(ymin, y);
        ymax = Math.max(ymax, y);
    }


    public void addPoint(double x, double y)
    {
        if (len == X.length)
        {
            double[] oldX = X;
            X = new double[len * 2];
            System.arraycopy(oldX, 0, X, 0, len);

            double[] oldY = Y;
            Y = new double[len * 2];
            System.arraycopy(oldY, 0, Y, 0, len);
        }
        X[len] = x;
        Y[len] = y;
        len++;
        updateBoundingRectangle(x, y);
    }


    public boolean contains(double x, double y)
    {
        if (x < xmin || x > xmax || y < ymin || y > ymax)
            return false;

        int i, j;
        boolean contains = false;
        for (i = 0, j = len - 1; i < len; j = i++)
        {
            if ((Y[i] <= y && y < Y[j] || Y[j] <= y && y < Y[i]) &&
                    (x < (X[j] - X[i]) * (y - Y[i]) / (Y[j] - Y[i]) + X[i]))
                contains = !contains;
        }
        return contains;
    }


    public int hashCode()
    {
        long ret = 0;
        for (int i = 0; i < len; i ++)
        {
            ret += Double.doubleToLongBits(X[i]);
            ret *= 31;
            ret += Double.doubleToLongBits(Y[i]);
            ret *= 31;
        }
        return (int)(ret % 0x7FFFFFFF);
    }


    public boolean equals(Object other)
    {
        if (other.getClass() != this.getClass())
            return false;
        Polygon poly = (Polygon) other;
        if (len != poly.len)
            return false;
        for (int i = 0; i < poly.len; i ++)
        {
            if (X[i] != poly.X[i])
                return false;
            if (Y[i] != poly.Y[i])
                return false;
        }
        return true;
    }


    Polygon translate(double[][] m)
    {
        assert m.length == 3;
        assert m[0].length == 3 && m[1].length == 3 && m[2].length==3;
        // since this is 2D transform, third column should be identity
        assert m[0][2] == 0 && m[1][2] == 0 && m[2][2] == 1;

        double[] x = new double[this.len];
        double[] y = new double[this.len];
        for (int i=0 ; i<this.len ; i++)
        {
            double _x = X[i];
            double _y = Y[i];
            x[i] = _x * m[0][0] + _y * m[1][0] + m[2][0];
            y[i] = _x * m[0][1] + _y * m[1][1] + m[2][1];
        }
        return new Polygon(x,y);
    }


    Polygon invert()
    {
        //noinspection SuspiciousNameCombination
        return new Polygon(Y, X);
    }


    public static void main(String[] args)
    {
        Polygon diamond = new Polygon(new double[]{0, -1, 0, 1}, new double[]{1, 0, -1, 0});
        System.out.println(diamond.contains(0.0, 0.0));
        System.out.println(diamond.contains(0.4, 0.4));
        System.out.println(diamond.contains(0.499, 0.499));
        System.out.println(diamond.contains(0.5, 0.5));
        System.out.println(diamond.contains(0.501, 0.501));
        System.out.println(diamond.contains(0.6, 0.6));
        System.out.println(diamond.contains(0.5, 0.5));
        System.out.println(diamond.contains(0.5, -0.5));
        System.out.println(diamond.contains(-0.5, 0.5));
        System.out.println(diamond.contains(-0.5, -0.5));
    }
}
