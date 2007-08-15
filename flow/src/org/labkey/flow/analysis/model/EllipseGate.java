package org.labkey.flow.analysis.model;

import org.labkey.flow.analysis.data.NumberArray;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.log4j.Logger;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.io.Serializable;

public class EllipseGate extends Gate
{
    static final private Logger _log = Logger.getLogger(EllipseGate.class);
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

    public BitSet apply(DataFrame data)
    {
        BitSet ret = new BitSet(data.getRowCount());
        NumberArray xValues = data.getColumn(xAxis);
        NumberArray yValues = data.getColumn(yAxis);
        for (int i = 0; i < data.getRowCount(); i ++)
        {
            double dCompare = 0;
            for (int f = 0; f < 2; f ++)
            {
                double dx = xValues.getDouble(i) - foci[f].x;
                double dy = yValues.getDouble(i) - foci[f].y;
                dCompare += Math.sqrt(dx * dx + dy * dy);
            }
            if (dCompare <= distance)
            {
                ret.set(i, true);
            }
        }
        return ret;
    }


    public boolean requiresCompensationMatrix()
    {
        return CompensationMatrix.isParamCompensated(xAxis) || CompensationMatrix.isParamCompensated(yAxis);
    }

    public void getPolygons(List<Polygon> list, String xAxis, String yAxis)
    {
    }

    static private double length(Point[] points)
    {
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
        Point[] minorAxis;
        double majorAxisLength;
        double minorAxisLength;
        if (d1 >= d2)
        {
            majorAxis = axis1;
            minorAxis = axis2;
            majorAxisLength = d1;
            minorAxisLength = d2;
        }
        else
        {
            majorAxis = axis2;
            minorAxis = axis1;
            majorAxisLength = d2;
            minorAxisLength = d1;
        }
        Point center = new Point((majorAxis[0].x + majorAxis[1].x)/2, (majorAxis[0].y + majorAxis[1].y)/2);
        Point center2 = new Point((minorAxis[0].x + minorAxis[1].x) / 2, (minorAxis[0].y + minorAxis[1].y) / 2);

        double dotProduct = (majorAxis[1].x - majorAxis[0].x) * (minorAxis[1].x - minorAxis[0].x) +
                (majorAxis[1].y - majorAxis[0].y) * (minorAxis[1].y - minorAxis[0].y);

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

        double dSquared = majorAxisLength * majorAxisLength;
        for (Point vertex : vertices)
        {
            double dCompare = distance(vertex, ret.getFoci());
            double difference = dCompare - dSquared;
        }
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
}
