/*
 * Copyright (c) 2007-2013 LabKey Corporation
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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.List;
import java.io.Serializable;

public class EllipseGate extends RegionGate
{
    String xAxis;
    String yAxis;
    static public class Point implements Serializable
    {
        final public double x;
        final public double y;
        public Point(double x, double y)
        {
            this.x = x;
            this.y = y;
        }
    }
    Point[] foci;
    double distance;

    public EllipseGate(String xAxis, String yAxis, double distance, Point focus1, Point focus2)
    {
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.distance = distance;
        this.foci = new Point[] { focus1, focus2 };
    }

    public double getDistance()
    {
        return distance;
    }

    public Point[] getFoci()
    {
        return foci;
    }

    public String getXAxis()
    {
        return xAxis;
    }

    public String getYAxis()
    {
        return yAxis;
    }

    public List<String> getAxes()
    {
        return Arrays.asList(yAxis, yAxis);
    }


    public BitSet apply(PopulationSet populations, DataFrame data)
    {
        // quick compute bounding rect (could even draw it in closer with a bit more math)
        double r = distance/2;
        double xMax = Math.max(foci[0].x,foci[1].x) + r;
        double xMin = Math.min(foci[0].x,foci[1].x) - r;
        double yMax = Math.max(foci[0].y,foci[1].y) + r;
        double yMin = Math.min(foci[0].y,foci[1].y) - r;

        BitSet ret = new BitSet(data.getRowCount());
        NumberArray xValues = data.getColumn(xAxis);
        NumberArray yValues = data.getColumn(yAxis);
        for (int i = 0; i < data.getRowCount(); i ++)
        {
            double x = xValues.getDouble(i);
            double y = yValues.getDouble(i);
            if (x < xMin || x > xMax || y < yMin || y > yMax)
                continue;
            double dCompare = 0;
            for (int f = 0; f < 2; f ++)
            {
                double dx = x - foci[f].x;
                double dy = y - foci[f].y;
                dCompare += Math.sqrt(dx * dx + dy * dy);
            }
            if (dCompare > distance)
                continue;
            ret.set(i, true);
        }
        return ret;
    }



    public boolean requiresCompensationMatrix()
    {
        return CompensationMatrix.isParamCompensated(xAxis) || CompensationMatrix.isParamCompensated(yAxis);
    }


    static Polygon unitCircle;
    static
    {
        double[] x = new double[60];
        double[] y = new double[60];
        double d = (2*Math.PI)/x.length;
        for (int i=0 ; i<x.length ; i++)
        {
            x[i] = Math.cos(i*d);
            y[i] = Math.sin(i*d);
        }
        unitCircle = new Polygon(x,y);
    }


    public void getPolygons(List<Polygon> list, String xAxis, String yAxis)
    {
        if (!(xAxis.equals(getXAxis()) && yAxis.equals(getYAxis()) ||
           xAxis.equals(getYAxis()) && yAxis.equals(getXAxis())))
                return;

        // Compute ellipse points as transformation of unit circle
        // stretch, translate, rotate
        double[] center = new double[] {(foci[0].x+foci[1].x)/2, (foci[0].y+foci[1].y)/2, 1};
        double c = length(foci)/2;
        double a = Math.max(distance/2,c);      // width
        double b = Math.sqrt((a*a)-(c*c));      // height
        double v[] = new double[] { c==0?1:(foci[0].x-center[0])/c, c==0?0:(foci[0].y-center[1])/c};
        double[] xrotate = new double[] {a*v[0],a*v[1],0};
        double[] yrotate = new double[] {-1*b*v[1], b*v[0], 0};
        double[][] m = new double[][] {xrotate, yrotate, center};

        Polygon ellipse = unitCircle.translate(m);
        if (xAxis.equals(getXAxis()))
            list.add(ellipse);
        else
            list.add(ellipse.invert());
    }

    static private double length(Point[] points)
    {
        assert points.length == 2;
        double dx = points[0].x - points[1].x;
        double dy = points[0].y - points[1].y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    static private double distance(Point point, Point[] foci)
    {
        double ret = 0;
        for (Point focus : foci)
        {
            double dx = point.x - focus.x;
            double dy = point.y - focus.y;
            ret += Math.sqrt(dx * dx + dy * dy);
        }
        return ret;
    }

    static public EllipseGate fromVertices(String xAxis, String yAxis, Point[] vertices)
    {
        Point[] axis1 = new Point[] { vertices[0], vertices[1] };
        Point[] axis2 = new Point[] { vertices[2], vertices[3] };
        double d1 = length(axis1);
        double d2 = length(axis2);
        Point[] majorAxis;
        double majorAxisLength;
        double minorAxisLength;
        if (d1 >= d2)
        {
            majorAxis = axis1;
            majorAxisLength = d1;
            minorAxisLength = d2;
        }
        else
        {
            majorAxis = axis2;
            majorAxisLength = d2;
            minorAxisLength = d1;
        }
        Point center = new Point((majorAxis[0].x + majorAxis[1].x)/2, (majorAxis[0].y + majorAxis[1].y)/2);

        double focalAxisLength = Math.sqrt(majorAxisLength * majorAxisLength - minorAxisLength * minorAxisLength);
        double focalRatio = focalAxisLength / majorAxisLength;
        Point[] foci = new Point[2];
        for (int i = 0; i < 2; i ++)
        {
            foci[i] = new Point(
                    majorAxis[i].x * focalRatio + center.x * (1-focalRatio),
                    majorAxis[i].y * focalRatio + center.y * (1-focalRatio)
            );
        }
        EllipseGate ret = new EllipseGate(xAxis, yAxis, majorAxisLength, foci[0], foci[1]);
        return ret;
    }

    static private Point toPoint(Element elPoint)
    {
        return new Point(Double.parseDouble(elPoint.getAttribute("x")), Double.parseDouble(elPoint.getAttribute("y")));
    }

    static public EllipseGate readEllipse(Element el)
    {
        NodeList nlFoci = el.getElementsByTagName("focus");

        Point focus0 = toPoint((Element) nlFoci.item(0));
        Point focus1 = toPoint((Element) nlFoci.item(1));

        return new EllipseGate(el.getAttribute("xAxis"),
                el.getAttribute("yAxis"),
                Double.parseDouble(el.getAttribute("distance")),
                focus0,
                focus1);
    }


    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EllipseGate that = (EllipseGate) o;

        if (distance != that.distance) return false;
        if (!Arrays.equals(foci, that.foci)) return false;
        if (!xAxis.equals(that.xAxis)) return false;
        if (!yAxis.equals(that.yAxis)) return false;

        return true;
    }

    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + xAxis.hashCode();
        result = 31 * result + yAxis.hashCode();
        result = 31 * result + Arrays.hashCode(foci);
        result = 31 * result + Double.valueOf(distance).hashCode();
        return result;
    }


    public static void main(String[] args)
    {
        EllipseGate g = new EllipseGate("x", "y", 14, new Point(10,10), new Point(20,15));
        Polygon p;

        System.out.println("10\t10");
        System.out.println("20\t15");
        List<Polygon> l = new ArrayList<>();

        l.clear();
        g.getPolygons(l, "x", "y");
        p = l.get(0);
        for (int i=0 ; i<p.len ;i++)
        {
            System.out.println(p.X[i] + "\t" + p.Y[i]);
        }

        System.out.println();
        System.out.println();

        l.clear();
        g.getPolygons(l, "y", "x");
        p = l.get(0);
        for (int i=0 ; i<p.len ;i++)
        {
            System.out.println(p.X[i] + "\t" + p.Y[i]);
        }
    }
}
