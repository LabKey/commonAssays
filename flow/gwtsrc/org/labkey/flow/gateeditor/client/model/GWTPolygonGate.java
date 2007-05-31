package org.labkey.flow.gateeditor.client.model;

import java.util.Arrays;

public class GWTPolygonGate extends GWTGate implements Cloneable
{
    String xAxis;
    String yAxis;
    double[] arrX;
    double[] arrY;

    public GWTPolygonGate()
    {
        
    }

    public GWTPolygonGate(String xAxis, double[] arrX, String yAxis, double[] arrY)
    {
        this.xAxis = xAxis;
        this.arrX = arrX;
        this.yAxis = yAxis;
        this.arrY = arrY;
    }

    public void setXAxis(String xAxis)
    {
        this.xAxis = xAxis;
    }

    public void setYAxis(String yAxis)
    {
        this.yAxis = yAxis;
    }

    public void setArrX(double[] arrX)
    {
        this.arrX = arrX;
    }

    public void setArrY(double[] arrY)
    {
        this.arrY = arrY;
    }

    public String getXAxis()
    {
        return xAxis;
    }

    public String getYAxis()
    {
        return yAxis;
    }

    public double[] getArrX()
    {
        return arrX;
    }

    public double[] getArrY()
    {
        return arrY;
    }

    public boolean contains(double x, double y)
    {
        int i, j;
        boolean contains = false;
        int len = arrX.length;
        for (i = 0, j = len - 1; i < len; j = i++)
        {
            if ((arrY[i] <= y && y < arrY[j] || arrY[j] <= y && y < arrY[i]) &&
                    (x < (arrX[j] - arrX[i]) * (y - arrY[i]) / (arrY[j] - arrY[i]) + arrX[i]))
                contains = !contains;
        }
        return contains;
    }

    public boolean contains(GWTPoint point)
    {
        return contains(point.x, point.y);
    }

    public GWTRectangle getBoundingRectangle()
    {
        GWTRectangle ret = new GWTRectangle();
        if (arrX.length == 0)
            return ret;
        double left = arrX[0];
        double top = arrY[0];
        double right = arrX[0];
        double bottom = arrY[0];
        for (int i = 1; i < arrX.length; i++)
        {
            left = arrX[i] < left ? arrX[i] : left;
            top = arrY[i] < top ? arrY[i] : top;
            right = arrX[i] < right ? right : arrX[i];
            bottom = arrY[i] < bottom ? bottom : arrY[i];
        }
        ret.x = left;
        ret.y = top;
        ret.width = right - left;
        ret.height = bottom - top;
        return ret;
    }

    public int length()
    {
        return arrX.length;
    }

    public GWTGate close()
    {
        if (length() < 2)
            return this;
        if (length() == 2)
        {
            return new GWTIntervalGate(this.xAxis, Math.min(arrX[0], arrX[1]), Math.max(arrX[0], arrX[1]));
        }
        else
        {
            return new GWTPolygonGate(this.xAxis, this.arrX, this.yAxis, this.arrY);
        }
    }

    public GWTGate addPoint(GWTPoint point)
    {
        GWTPolygonGate ret = new GWTPolygonGate();
        ret.arrX = new double[length() + 1];
        ret.arrY = new double[length() + 1];
        for (int i = 0; i < length(); i ++)
        {
            ret.arrX[i] = arrX[i];
            ret.arrY[i] = arrY[i];
        }
        ret.arrX[length()] = point.x;
        ret.arrY[length()] = point.y;
        ret.open = true;
        return ret;
    }

    public boolean canSave()
    {
        return length() >= 2;
    }


    private boolean arrayEquals(double[] array1, double[] array2)
    {
        if (array1.length != array2.length)
            return false;
        for (int i = 0; i < array1.length; i ++)
        {
            if (array1[i] != array2[i])
                return false;
        }
        return true;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof GWTPolygonGate))
            return false;

        GWTPolygonGate that = (GWTPolygonGate) o;

        if (!arrayEquals(arrX, that.arrX))
            return false;
        if (!arrayEquals(arrY, that.arrY))
            return false;
        if (!xAxis.equals(that.xAxis)) return false;
        if (!yAxis.equals(that.yAxis)) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = xAxis.hashCode();
        result = 31 * result + yAxis.hashCode();
        return result;
    }
}
