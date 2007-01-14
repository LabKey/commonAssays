package org.labkey.ms2.protein.tools;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.plot.Plot;
import org.jfree.data.general.AbstractDataset;
import org.jfree.ui.HorizontalAlignment;

import java.io.OutputStream;

/**
 * User: tholzman
 * Date: Oct 28, 2005
 * Time: 3:18:10 PM
 */
public abstract class JChartHelper
{
    public JFreeChart getChart()
    {
        return chart;
    }

    public void setChart(JFreeChart chart)
    {
        this.chart = chart;
    }

    protected JFreeChart chart;

    public Plot getPlot()
    {
        return plot;
    }

    public void setPlot(Plot plot)
    {
        this.plot = plot;
    }

    protected Plot plot;

    public String getChartTitle()
    {
        return chartTitle;
    }

    public void setChartTitle(String chartTitle)
    {
        this.chartTitle = chartTitle;
        TextTitle tt = new TextTitle(this.chartTitle, TextTitle.DEFAULT_FONT);
        tt.setHorizontalAlignment(HorizontalAlignment.CENTER);
        this.getChart().setTitle(tt);
    }

    protected String chartTitle = "";

    public AbstractDataset getDataset()
    {
        return dataset;
    }

    public void setDataset(AbstractDataset dataset)
    {
        this.dataset = dataset;
    }

    protected AbstractDataset dataset;

    public ChartRenderingInfo getChartRenderingInfo()
    {
        return chartRenderingInfo;
    }

    public void setChartRenderingInfo(ChartRenderingInfo chartRenderingInfo)
    {
        this.chartRenderingInfo = chartRenderingInfo;
    }

    protected ChartRenderingInfo chartRenderingInfo =
            new ChartRenderingInfo(new StandardEntityCollection());

    public int getWidth()
    {
        return width;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }

    protected int width = 800;

    public int getHeight()
    {
        return height;
    }

    public void setHeight(int height)
    {
        this.height = height;
    }

    protected int height = 800;

    public void renderAsPNG(OutputStream out) throws Exception
    {
        ChartUtilities.writeChartAsPNG(
                out, getChart(), getWidth(), getHeight(), getChartRenderingInfo());
    }
}
