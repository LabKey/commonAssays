/*
 * Copyright (c) 2005-2014 LabKey Corporation
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

package org.labkey.flow.analysis.chart;

import org.jfree.chart.axis.ColorBar;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.xy.XYDataset;
import org.labkey.api.arrays.DoubleArray;
import org.labkey.api.data.statistics.MathStat;
import org.labkey.api.view.Stats;
import org.labkey.flow.analysis.model.*;
import org.labkey.flow.analysis.util.RangeFunction;
import org.labkey.flow.analysis.web.StatisticSpec;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class PlotFactory
{
    public static int MAX_DENSITY_BUCKETS = Integer.getInteger("flow.maxchannels", 512).intValue();
    public static int MAX_HISTOGRAM_BUCKETS = Integer.getInteger("flow.maxchannels", 512).intValue();
    public static final Color COLOR_GATE = Color.RED;


    /**
     * Return a set of buckets usable for binning a dataset.
     * @param minValue min value
     * @param maxValue max value
     * @param fLogarithmic whether the buckets should be logarithmically spaced
     * @param bucketCount The maximum number of buckets
     * @return
     */
    static public double[] getPossibleValues(double minValue, double maxValue, boolean fLogarithmic, boolean simpleLog, int bucketCount)
    {
        // Allow for ranges smaller than the default bucket count.
        // The Time parameter may be scaled by gain 0.01 reducing the range to 0-40.
        int cBuckets = (int) Math.min(maxValue - minValue, bucketCount);
        double[] ret = new double[cBuckets];

        int i = 0;

        RangeFunction fn = !fLogarithmic ? null : simpleLog ? FlowLogarithmicAxis.simpleFN : FlowLogarithmicAxis.loglinFN;
        
        for (; i < cBuckets; i ++)
        {
            if (fLogarithmic)
            {
                double x = (fn.compute(maxValue) - fn.compute(minValue)) * i / cBuckets + fn.compute(minValue);
                ret[i] = fn.invert(x);
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
        double max = field.getMaxValue();
        double min = field.getMinValue();

        if (field.isTimeChannel())
        {
            Subset root = subset;
            while (root.getParent() != null)
                root = root.getParent();

            // UNDONE: Share this rootStatsMap
            Map<String, MathStat> rootStatsMap = new HashMap<>();
            min = StatisticSpec.calculate(root, new StatisticSpec(null, StatisticSpec.STAT.Min, field.getName()), rootStatsMap);
            min = field.getScalingFunction().translate(min);

            max = StatisticSpec.calculate(root, new StatisticSpec(null, StatisticSpec.STAT.Max, field.getName()), rootStatsMap);
            max = field.getScalingFunction().translate(max);
        }
        boolean logarithmic = displayLogarithmic(subset, field);
        boolean simpleLog = logarithmic && field.isSimpleLogAxis();
        return getPossibleValues(min, max, logarithmic, simpleLog, maxCount);
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


    static private ValueAxis getValueAxis(Subset subset, String name, DataFrame.Field field)
    {
        if (!displayLogarithmic(subset, field))
            return new NumberAxis(name);
        return new FlowLogarithmicAxis(name, field.isSimpleLogAxis());
    }


    static private DataFrame.Field getField(DataFrame data, String name) throws FlowException
    {
        DataFrame.Field ret = data.getField(CompensationMatrix.DITHERED_PREFIX + name);
        if (ret != null)
            return ret;
        ret = data.getField(name);
        if (ret == null)
            throw new FlowException("Channel '" + name + "' required for graph");
        return ret;
    }

    static public String getLabel(FCSHeader fcs, int index, boolean compensated)
    {
        String name = fcs.getParameterName(index);
        String label = name;
        String stain = fcs.getParameterDescription(index);

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

    static private String getLabel(Subset subset, String fieldName) throws FlowException
    {
        DataFrame.Field field = subset.getDataFrame().getField(fieldName);
        if (field == null)
            throw new FlowException("Channel '" + fieldName + "' required for graph");
        boolean compensated =
                (field.getOrigIndex() != field.getIndex()) ||
                CompensationMatrix.isParamCompensated(fieldName);
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

        // Ignore the top 2% of values when determining the color of an x,y coordinate.
        double maxValue = computePercentile(cds, xValues, yValues, 0.98);
        plot.getColorBar().setMaximumValue(maxValue);
        return plot;
    }

    private static double computePercentile(DensityDataset cds, double[] xValues, double[] yValues, double percentile)
    {
        // When calculating the range, exclude z-values on the axes.
        DoubleArray da = new DoubleArray();
        for (int x = 1; x < xValues.length-1; x++)
        {
            for (int y = 1; y < yValues.length-1; y++)
            {
                int index = (x * yValues.length) + y;
                double d = cds.getZValue(0, index);
                if (d > 0)
                    da.add(d);
            }
        }
        if (da.size() == 0)
            return 0;

        // Find the n-th percentile of the z-values
        double[] z = da.toArray(null);
        Arrays.sort(z);
        int scaled = (int)Math.ceil(z.length * percentile);
        int index = Math.max(Math.min(scaled, z.length - 1), 0);
        return z[index];
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
            xAxis = new FlowLogarithmicAxis(getLabel(subset, axis), field.isSimpleLogAxis());
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
