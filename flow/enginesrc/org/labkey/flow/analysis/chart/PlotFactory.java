/*
 * Copyright (c) 2011-2019 LabKey Corporation
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
import org.labkey.api.arrays.DoubleArray;
import org.labkey.api.data.statistics.MathStat;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.DataFrame;
import org.labkey.flow.analysis.model.FCSHeader;
import org.labkey.flow.analysis.model.FlowException;
import org.labkey.flow.analysis.model.ScalingFunction;
import org.labkey.flow.analysis.model.Subset;
import org.labkey.flow.analysis.util.LinearRangeFunction;
import org.labkey.flow.analysis.util.LogRangeFunction;
import org.labkey.flow.analysis.util.LogicleRangeFunction;
import org.labkey.flow.analysis.util.RangeFunction;
import org.labkey.flow.analysis.util.SimpleLogRangeFunction;
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
     * @param bucketCount The maximum number of buckets
     * @param fn function used to space the buckets
     * @return
     */
    static public double[] getPossibleValues(double minValue, double maxValue, int bucketCount, RangeFunction fn)
    {
        // Allow for ranges smaller than the default bucket count.
        // The Time parameter may be scaled by gain 0.01 reducing the range to 0-40.
        int cBuckets = (int) Math.min(maxValue - minValue, bucketCount);
        double[] ret = new double[cBuckets];

        final double min = fn.compute(minValue);
        final double max = fn.compute(maxValue);
        final double width = max - min;

        for (int i = 0; i < cBuckets; i ++)
        {
            double x = ((width * i) / cBuckets) + min;
            ret[i] = fn.invert(x);
        }
        return ret;
    }


    static double[] getPossibleValues(Subset subset, DataFrame.Field field, int maxCount)
    {
        RangeFunction fn = getRangeFunction(subset, field);
        double min = fn.getMin();
        double max = fn.getMin();
        return getPossibleValues(min, max, maxCount, fn);
    }

    protected static RangeFunction getRangeFunction(Subset subset, DataFrame.Field field)
    {
        // TODO: get transform parameters and range from FlowJo's Sample's <Transformations> element
        double min = field.getMinValue();
        double max = field.getMaxValue();

        if (displayLogarithmic(field))
        {
            if (field.isSimpleLogAxis())
            {
                return new SimpleLogRangeFunction(min, max);
            }
            else
            {
                // TODO: need a better check for loglin vs. logicle
                var version = subset.getFCSHeader().getVersionNumber();
                if (version != null && 2 == version.getMajor())
                {
                    return new LogRangeFunction(LogRangeFunction.LOG_LIN_SWITCH, min, max);
                }

                // TODO: support other: old simple Log, old LinLog, BiEx, ArcSinH, Hyperlog
                // TODO: use FCSHeader.LogarithmicParameterDisplay display hint to set up Logicle

                double minValue = 0;

                // Issue 45300: the customer wants to see negative values in plot.
                // However, we don't have a good answer on how to decide the min limit of the plot.
                // max/1000 seems reasonable based on the limited example data we have.
                return new LogicleRangeFunction(max/-1000.0, max);
            }
        }
        else
        {
            double gain = subset.getFCSHeader().getParameterGain(field.getOrigIndex());
            var pd = field.getParameterDisplay();
            if (pd instanceof FCSHeader.LinearParameterDisplay)
            {
                min = ((FCSHeader.LinearParameterDisplay)pd).getLowerBound();
                max = ((FCSHeader.LinearParameterDisplay)pd).getUpperBound();
            }

            if (field.isTimeChannel())
            {
                Subset root = subset;
                while (root.getParent() != null)
                    root = root.getParent();

                // UNDONE: Share this rootStatsMap
                Map<String, MathStat> rootStatsMap = new HashMap<>();
                min = StatisticSpec.calculate(root, new StatisticSpec(null, StatisticSpec.STAT.Min, field.getName()), rootStatsMap);
                max = StatisticSpec.calculate(root, new StatisticSpec(null, StatisticSpec.STAT.Max, field.getName()), rootStatsMap);

                gain = 1.0d;
            }

            return new LinearRangeFunction(min, max, gain);
        }
    }

    static protected boolean displayLogarithmic(DataFrame.Field field)
    {
        // FCS3.1 "$PnD" parameter display
        var pd = field.getParameterDisplay();
        if (pd instanceof FCSHeader.LogrithmicParameterDisplay)
            return true;

        // FCS2.0 and FCS3.0
        ScalingFunction scale = field.getScalingFunction();
        return scale != null && scale.isLogarithmic();
    }

    static private ValueAxis getValueAxis(String name, RangeFunction fn)
    {
        return fn.isLogarithmic() ?
                new FlowLogarithmicAxis(name, fn) :
                new FlowLinearAxis(name);
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

        RangeFunction fnDomain = getRangeFunction(subset, fieldDomain);
        RangeFunction fnRange = getRangeFunction(subset, fieldRange);

        double[] xValues = getPossibleValues(fnDomain.getMin(), fnDomain.getMax(), MAX_DENSITY_BUCKETS, fnDomain);
        double[] yValues = getPossibleValues(fnRange.getMin(), fnRange.getMax(), MAX_DENSITY_BUCKETS, fnRange);

        DensityDataset cds = new DensityDataset(
                DatasetFactory.createXYDataset(subset.getDataFrame(), fieldDomain.getIndex(), fieldRange.getIndex()),
                xValues,
                yValues);
        ColorBar bar = new DensityColorBar("");
        bar.setColorPalette(new DensityColorPalette());
        DensityPlot plot = new DensityPlot(cds,
                getValueAxis(getLabel(subset, domainAxis), fnDomain),
                getValueAxis(getLabel(subset, rangeAxis), fnRange),
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
    
    static public HistPlot createHistogramPlot(Subset subset, String axis)
    {
        DataFrame data = subset.getDataFrame();
        DataFrame.Field field = getField(data, axis);
        RangeFunction fn = getRangeFunction(subset, field);
        double[] bins = getPossibleValues(fn.getMin(), fn.getMax(), MAX_HISTOGRAM_BUCKETS, fn);
        HistDataset dataset = new HistDataset(bins, data.getColumn(axis));

        ValueAxis xAxis = getValueAxis(getLabel(subset, axis), fn);

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
