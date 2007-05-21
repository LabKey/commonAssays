package org.labkey.flow.analysis.chart;

import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ColorBar;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.Range;
import org.labkey.flow.analysis.model.*;

import java.awt.*;

/**
 */
public class PlotFactory
{
    public static int MAX_DENSITY_BUCKETS = Integer.getInteger("flow.maxchannels", 512).intValue();
    public static int MAX_HISTOGRAM_BUCKETS = Integer.getInteger("flow.maxchannels", 512).intValue();
    public static final Color COLOR_GATE = Color.RED;

    static public double adjustedLog10(double value)
    {
        return FlowLogarithmicAxis.s_adjustedLog10(value);
    }

    static public double adjustedPow10(double value)
    {
        return FlowLogarithmicAxis.s_adjustedPow10(value);
    }

    /**
     * Return a set of buckets usable for binning a dataset.
     * @param field The field in question
     * @param fLogarithmic whether the buckets should be logarithmically spaced
     * @param bucketCount The maximum number of buckets
     * @return
     */
    static public double[] getPossibleValues(DataFrame.Field field, boolean fLogarithmic, int bucketCount)
    {
        double maxValue = field.getMaxValue();
        double minValue = field.getMinValue();
        int cBuckets = (int) Math.min(maxValue - minValue, bucketCount);
        double[] ret = new double[cBuckets];

        int i = 0;

        for (; i < cBuckets; i ++)
        {
            if (fLogarithmic)
            {
                double x = (adjustedLog10(maxValue) - adjustedLog10(minValue)) * i / cBuckets + adjustedLog10(minValue);
                ret[i] = adjustedPow10(x);
            }
            else
            {
                ret[i] = minValue + (i * (maxValue - minValue)) / cBuckets;
            }
        }
        return ret;
    }

    static double[] getPossibleValues(Subset subset, DataFrame.Field field, int maxCount)
    {
        return getPossibleValues(field, displayLogarithmic(subset, field), maxCount);
    }

    static protected boolean displayLogarithmic(Subset subset, DataFrame.Field field)
    {
        String strDisplay = subset.getFCSHeader().getKeyword("P" + (field.getOrigIndex() + 1) + "DISPLAY");
        if (strDisplay != null)
        {
            if ("LOG".equals(strDisplay))
                return true;
            if ("LIN".equals(strDisplay))
                return false;
        }
        ScalingFunction scale = field.getScalingFunction();
        if (scale == null || !scale.isLogarithmic())
            return false;
        return true;
    }

    static protected ValueAxis getValueAxis(Subset subset, String name, DataFrame.Field field)
    {
        if (!displayLogarithmic(subset, field))
            return new NumberAxis(name);
        return new FlowLogarithmicAxis(name);
    }

    static public DataFrame.Field getField(DataFrame data, String name)
    {
        DataFrame.Field ret = data.getField(CompensationMatrix.DITHERED_PREFIX + name);
        if (ret != null)
            return ret;
        ret = data.getField(name);
        if (ret == null)
            throw new IllegalArgumentException("No such field '" + name + "'");
        return ret;
    }

    static public String getLabel(FCSHeader fcs, int index, boolean compensated)
    {
        String name = fcs.getKeyword("$P" + (index + 1) + "N");
        String label = name;
        String stain = fcs.getKeyword("$P" + (index + 1) + "S");

        String prefix = "";
        String suffix = "";
        if (compensated)
        {
            prefix = "comp-";
            suffix = "";
            label = prefix + label + suffix;
        }
        if (stain != null)
        {
            int ichHyphen = name.indexOf("-");
            if (ichHyphen < 0)
                ichHyphen = name.length();
            if (stain.startsWith(name.substring(0, ichHyphen)))
            {
                label = prefix + stain + suffix;
            }
            else
                label += " " + stain;
        }
        return label;
    }

    static public String getLabel(Subset subset, String fieldName)
    {
        DataFrame.Field field = subset.getDataFrame().getField(fieldName);
        boolean compensated = field.getOrigIndex() != field.getIndex();
        return getLabel(subset.getFCSHeader(), field.getOrigIndex(), compensated);
    }

    static public DensityPlot createContourPlot(Subset subset, String domainAxis, String rangeAxis)
    {
        DataFrame data = subset.getDataFrame();
        DataFrame.Field fieldDomain = getField(data, domainAxis);
        DataFrame.Field fieldRange = getField(data, rangeAxis);
        double[] xValues = getPossibleValues(subset, fieldDomain, MAX_DENSITY_BUCKETS);
        double[] yValues = getPossibleValues(subset, fieldRange, MAX_DENSITY_BUCKETS);
        DensityDataset cds = new DensityDataset(
                DatasetFactory.createXYDataset(subset.getDataFrame(), fieldDomain.getIndex(), fieldRange.getIndex()),
                xValues,
                yValues);
        ColorBar bar = new DensityColorBar("");
        bar.setColorPalette(new DensityColorPalette());
        DensityPlot plot = new DensityPlot(cds,
                getValueAxis(subset, getLabel(subset, domainAxis), fieldDomain),
                getValueAxis(subset, getLabel(subset, rangeAxis), fieldRange),
                bar);
        plot.setDomainCrosshairLockedOnData(false);
        plot.setRangeCrosshairLockedOnData(false);

        // When calculating the Z value range, exclude values on the axes
        Range rangeZValues = cds.getZValueRange(new Range(xValues[1], xValues[xValues.length - 2]),
                new Range(yValues[1], yValues[yValues.length - 2]));
        plot.getColorBar().setMaximumValue(rangeZValues.getUpperBound());
        return plot;
    }
    
    static public XYPlot createScatterPlot(Subset subset, String domainAxis, String rangeAxis)
    {
        DataFrame data = subset.getDataFrame();
        DataFrame.Field fieldDomain = getField(data, domainAxis);
        DataFrame.Field fieldRange = getField(data, rangeAxis);
        XYDataset dataset = DatasetFactory.createXYDataset(data, fieldDomain.getIndex(), fieldRange.getIndex());
        return new XYPlot(dataset, getValueAxis(subset, domainAxis, fieldDomain), getValueAxis(subset, rangeAxis, fieldRange), new XYDotRenderer());
    }

    static public HistPlot createHistogramPlot(Subset subset, String axis)
    {
        DataFrame data = subset.getDataFrame();
        DataFrame.Field field = getField(data, axis);
        double[] bins = getPossibleValues(subset, field, MAX_HISTOGRAM_BUCKETS);
        HistDataset dataset = new HistDataset(bins, data.getColumn(axis));

        NumberAxis xAxis;

        if (displayLogarithmic(subset, field))
        {
            xAxis = new FlowLogarithmicAxis(getLabel(subset, axis));
        }
        else
        {
            xAxis = new NumberAxis(getLabel(subset, axis));
        }

        ValueAxis yAxis = new NumberAxis("Count");
        double yMax = 0;
        for (int i = 1; i < dataset.getItemCount(0) - 1; i ++)
        {
            yMax = Math.max(dataset.getY(0, i), yMax);
        }
        yAxis.setRange(0, yMax * 1.1);
        HistPlot plot = new HistPlot(dataset, xAxis, yAxis);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        return plot;
    }
}
