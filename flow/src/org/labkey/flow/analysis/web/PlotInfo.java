package org.labkey.flow.analysis.web;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.ValueAxis;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;

import org.labkey.flow.analysis.model.DataFrame;
import org.labkey.flow.analysis.model.FCS;
import org.labkey.flow.analysis.chart.DensityPlot;

public class PlotInfo
{
    private ChartRenderingInfo _info;
    BufferedImage _image;
    Range _rangeX;
    Range _rangeY;
    public PlotInfo(BufferedImage image, ChartRenderingInfo info, DensityPlot plot)
    {
        _image = image;
        _info = info;
        _rangeX = rangeFromAxis(plot.getDomainAxis());
        _rangeY = rangeFromAxis(plot.getRangeAxis());

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
}
