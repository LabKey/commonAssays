/*
 * Copyright (C) 2004 Fred Hutchinson Cancer Research Center. All rights reserved.
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
        this.X = (double[]) X.clone();
        this.Y = (double[]) Y.clone();
        computeBoundingRectangle();
    }

    static private double[] toDoubleArray(List<Double> lst)
    {
        double[] ret = new double[lst.size()];
        for (int i = 0; i < ret.length; i ++)
        {
            ret[i] = lst.get(i);
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

    public int hashCode()
    {
        int ret = 0;
        for (int i = 0; i < len; i ++)
        {
            ret ^= Double.valueOf(X[i]).hashCode();
            ret ^= Double.valueOf(Y[i]).hashCode();
        }
        return ret;
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
}
