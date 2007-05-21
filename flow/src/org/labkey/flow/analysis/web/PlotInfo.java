package org.labkey.flow.analysis.web;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.ui.RectangleEdge;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.io.ByteArrayOutputStream;
import java.util.BitSet;
import java.text.DecimalFormat;

import org.labkey.flow.analysis.model.DataFrame;
import org.labkey.flow.analysis.model.FCS;
import org.labkey.flow.analysis.model.Subset;
import org.labkey.flow.analysis.model.Gate;
import org.labkey.flow.analysis.chart.FlowLogarithmicAxis;
import org.labkey.flow.analysis.chart.DensityPlot;

public class PlotInfo
{
    private ChartRenderingInfo _info;
    BufferedImage _image;
    ValueAxis _domainAxis;
    ValueAxis _rangeAxis;
    Range _rangeX;
    Range _rangeY;
    Subset _subset;
    public PlotInfo(Subset subset, BufferedImage image, ChartRenderingInfo info, ValueAxis domainAxis, ValueAxis rangeAxis)
    {
        _subset = subset;
        _image = image;
        _info = info;
        _domainAxis = domainAxis;
        _rangeAxis = rangeAxis;
        _rangeX = rangeFromAxis(domainAxis);
        _rangeY = rangeFromAxis(rangeAxis);

    }

    private Range rangeFromAxis(ValueAxis axis)
    {
        Range ret = new Range();
        ret.min = axis.getRange().getLowerBound();
        ret.max = axis.getRange().getUpperBound();
        ret.log = axis instanceof LogarithmicAxis;
        return ret;
    }

    public Rectangle getChartArea()
    {
        return _info.getChartArea().getBounds();
    }
    public Rectangle getDataArea()
    {
        Rectangle2D plotRect = _info.getPlotInfo().getDataArea();
        return plotRect.getBounds();
    }
    public BufferedImage getImage()
    {
        return _image;
    }

    public byte[] getBytes() throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(getImage(), "png", baos);
        return baos.toByteArray();
    }
    public Point toScreenCoordinates(Point2D logicalPoint)
    {
        Point ret = new Point((int) Math.round(_domainAxis.valueToJava2D(logicalPoint.getX(), _info.getPlotInfo().getDataArea(), RectangleEdge.BOTTOM)),
            (int) Math.round(_rangeAxis.valueToJava2D(logicalPoint.getY(), _info.getPlotInfo().getDataArea(), RectangleEdge.LEFT))
        );
        Rectangle2D rcBounds = _info.getPlotInfo().getDataArea();
        ret.x = Math.min((int) Math.ceil(rcBounds.getMaxX()) + 1, ret.x);
        ret.x = Math.max((int) Math.floor(rcBounds.getMinX()) - 1, ret.x);
        ret.y = Math.min((int) Math.ceil(rcBounds.getMaxY()) + 1, ret.y);
        ret.y = Math.max((int) Math.floor(rcBounds.getMinY()) - 1, ret.y);
        return ret;
    }

    static public class Range
    {
        public double min;
        public double max;
        public boolean log;
    }

    public Range getRangeX()
    {
        return _rangeX;
    }
    public Range getRangeY()
    {
        return _rangeY;
    }

    public String getFrequency(Gate gate)
    {
        Subset newSubset = _subset.apply(gate);
        int rowCount = newSubset.getDataFrame().getRowCount();
        double fraction = ((double) rowCount) / ((double) _subset.getDataFrame().getRowCount());
        DecimalFormat fmt = new DecimalFormat("0.#");
        String ret = fmt.format(fraction * 100) + "% (" + rowCount + " events)";
        return ret;
    }
}
