package org.labkey.flow.gateeditor.client.model;


public class GWTEllipseGate extends GWTGate
{
    private String xAxis;
    private String yAxis;

    private double centerX;
    private double centerY;

    private double majorX;      // unit vector long major axis
    private double majorY;      // unit vector long major axis
    private double majorL;      // major axis length
    private double minorL;      // minor axis length
   

    public GWTEllipseGate()
    {
    }


    public GWTEllipseGate(GWTEllipseGate copy)
    {
        this.xAxis = copy.xAxis;
        this.yAxis = copy.yAxis;

        this.centerX = copy.centerX;
        this.centerY = copy.centerY;

        this.majorX = copy.majorX;
        this.majorY = copy.majorY;
        this.majorL = copy.majorL;

        this.minorL = copy.minorL;

        assert this.hashCode() == copy.hashCode();
        assert this.equals(copy);
    }

    /**
     * Note server side we store foci[] and distance
     * which works well for gating.  For editing we will use
     *      center
     *      angle       represented as unit vector
     *      size        length of each axis
     */
    public GWTEllipseGate(String xAxis, String yAxis, double distance, double x0, double y0, double x1, double y1)
    {
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        centerX = (x0+x1)/2;
        centerY = (y0+y1)/2;

        // angle of rotation
        double dx = x1-centerX;
        double dy = y1-centerY;
        setAxis(dx, dy);

        // size
        double r = distance/2;
        majorL = distance/2;
        minorL = Math.sqrt(r*r - (dx*dx+dy*dy));
    }


    void setAxis(double dx, double dy)
    {
        double f = Math.sqrt(dx*dx+dy*dy);
        majorX = f==0 ? 1 : dx/f;
        majorY = f==0 ? 0 : dy/f;
    }


    public void setXAxis(String xAxis)
    {
        this.xAxis = xAxis;
    }


    public void setYAxis(String yAxis)
    {
        this.yAxis = yAxis;
    }


    public String getXAxis()
    {
        return xAxis;
    }


    public String getYAxis()
    {
        return yAxis;
    }


    public double getMajorAxisLength()
    {
        return majorL;
    }


    public double getMinorAxisLength()
    {
        return minorL;
    }


    public GWTPoint getCenter()
    {
        return new GWTPoint(centerX, centerY);
    }


    public boolean canSave()
    {
        return true;
    }


    public GWTGate close()
    {
        GWTEllipseGate copy = new GWTEllipseGate(this);
        copy.normalize();
        return copy;
    }


    //
    // editing methods
    //
    public void move(double dx, double dy)
    {
        centerX += dx;
        centerY += dy;
    }

    

    public void rotateRad(double a)
    {
        double sin_a = Math.sin(a);
        double cos_a = Math.cos(a);
        double x = cos_a*majorX - sin_a*majorY;
        double y = sin_a*majorX + cos_a*majorY;
        setAxis(x,y);
    }


    /** majorEdge and minorEdge indicate which axes are affected by the move */
    public void moveEdge(int majorEdge, int minorEdge, double dx, double dy)
    {
        assert (majorEdge == -1 || majorEdge == 1 || majorEdge==0) && (minorEdge == -1 || minorEdge == 1 || minorEdge == 0);

        double minorX = -1 * majorY;
        double minorY = 1 * majorX;
        
        // translate x,y movement in to axis relative movement
        double majord = majorEdge * (dx * majorX + dy * majorY);
        double minord = minorEdge * (dx * minorX + dy * minorY);

        centerX += (majord * majorX + minord * minorX) / 2;
        centerY += (majord * minorY + minord * minorY) / 2;
        majorL += majord / 2;
        minorL += minord / 2; 
    }


    public void moveCorner(double dx, double dy)
    {
        moveEdge(1, 1, dx, dy);
    }


    public boolean equals(Object other)
    {
        if (!(other instanceof GWTEllipseGate))
            return false;
        GWTEllipseGate that = (GWTEllipseGate) other;
        return this.getYAxis().equals(that.getYAxis()) &&
                this.centerX == that.centerX && this.centerY == that.centerY &&
                this.majorX == that.majorX && this.majorX == that.majorX && this.majorL == that.majorL && 
                this.minorL == that.minorL;
    }


    public int hashCode()
    {
        double result = 0;
        result += centerX;
        result *= 31;
        result += centerY;
        result *= 31;
        result += majorX;
        result *= 31;
        result += majorY;
        result *= 31;
        result *= majorL;
        return (int)((long)result % 0x7FFFFFFF);
    }


    public double[] getDefinition()
    {
        // to avoid side effects, use a copy.  This keeps the users idea of the orientation consistent.
        GWTEllipseGate copy = new GWTEllipseGate(this);
        copy.normalize();
        return copy._getDefinition();
    }


    private double[] _getDefinition()
    {
        double distance = majorL * 2;
        double f = Math.sqrt(majorL*majorL-minorL*minorL);
        return new double[] {distance, centerX + f*majorX, centerY+f*majorY, centerX - f*majorX, centerY - f*majorY};
    }


    public void normalize()
    {
        setAxis(majorX, majorY);

        majorL = Math.abs(majorL);
        minorL = Math.abs(minorL);
        if (majorL < minorL)
        {
            double t = majorL; majorL = minorL; minorL = t;
            setAxis(-1 * majorY, majorX);
        }
    }


    //
    // Testing
    //

    // this is just for testing since we use the server to render
    double[] getPoints(int count)
    {
        double slice = 2*Math.PI/count;
        double[] arr = new double[2*count];
        int r = 0;

        for (int i=0 ; i<count ; i++)
        {
            double a = i*slice;
            double m = majorL * Math.cos(a);
            double n = minorL * Math.sin(a);

            arr[r++] = centerX + m * majorX - n * majorY;
            arr[r++] = centerY + m * majorY + n * majorX;
        }
        return arr;
    }


/*
    public static void main(String[] args)
    {
        GWTEllipseGate g = new GWTEllipseGate("x", "y", 3, 2, 1, 4, 3);
        double[] pts = g.getPoints(12);
        double[] ptsDef = g.getDefinition();

        g.rotateRad(.5);
        double[] rot = g.getPoints(12);
        double[] rotDef = g.getDefinition();

        g.move(2, -1);
        double[] mov = g.getPoints(12);
        double[] movDef = g.getDefinition();

        g.moveEdge(-1, 0, 0, .3);
        double[] siz = g.getPoints(12);
        double[] sizDef = g.getDefinition();

        g.moveEdge(0, 1, -3.0, 0);
        double[] wid = g.getPoints(12);
        double[] widDef = g.getDefinition();

        System.out.print(ptsDef[1] + "\t" + ptsDef[2] + "\t");
        System.out.print(rotDef[1] + "\t" + rotDef[2] + "\t");
        System.out.print(movDef[1] + "\t" + movDef[2] + "\t");
        System.out.print(sizDef[1] + "\t" + sizDef[2] + "\t");
        System.out.print(widDef[1] + "\t" + widDef[2] + "\t");
        System.out.println();
        for (int j=3 ; j<16 ; j++)
        {
            int i=j%12;
            System.out.print(pts[i*2] + "\t" + pts[i*2+1] + "\t");
            System.out.print(rot[i*2] + "\t" + rot[i*2+1] + "\t");
            System.out.print(mov[i*2] + "\t" + mov[i*2+1] + "\t");
            System.out.print(siz[i*2] + "\t" + siz[i*2+1] + "\t");
            System.out.print(wid[i*2] + "\t" + wid[i*2+1]);
            System.out.println();
        }
        System.out.print(ptsDef[3] + "\t" + ptsDef[4] + "\t");
        System.out.print(rotDef[3] + "\t" + rotDef[4] + "\t");
        System.out.print(movDef[3] + "\t" + movDef[4] + "\t");
        System.out.print(sizDef[3] + "\t" + sizDef[4] + "\t");
        System.out.print(widDef[3] + "\t" + widDef[4] + "\t");
        System.out.println();
    }
*/
}
