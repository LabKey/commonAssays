package org.labkey.flow.analysis.chart;

import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ColorBar;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.contour.ContourDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.Range;
import org.labkey.flow.analysis.model.*;


/**
 */
public class PlotFactory
{
    public static int MAX_BUCKETS = Integer.getInteger("flow.maxchannels", 512).intValue();
    public static boolean SHOW_TOOLTIPS = !Boolean.getBoolean("flow.notooltips");

    static public double[] getPossibleValues(DataFrame.Field field, boolean fLogarithmic)
    {
        int range = field.getRange();
        int cBuckets = Math.min(field.getRange(), MAX_BUCKETS);
        double[] ret = new double[cBuckets];
        ScalingFunction scalingFunction = field.getScalingFunction();
        boolean fLogRawValues = fLogarithmic && (scalingFunction == null || !scalingFunction.isLogarithmic());

        for (int i = 0; i < cBuckets; i ++)
        {
            if (fLogRawValues)
            {
                ret[i] = Math.pow(range, ((double) i) / cBuckets);
            }
            else
            {
                ret[i] = (i * range) / cBuckets;
            }

            if (scalingFunction != null)
            {
                ret[i] = scalingFunction.translate(ret[i]);
            }
        }
        return ret;
    }

    static double[] getPossibleValues(Subset subset, DataFrame.Field field)
    {
        return getPossibleValues(field, displayLogarthmic(subset, field));
    }

    static protected boolean displayLogarthmic(Subset subset, DataFrame.Field field)
    {
        String strDisplay = subset.getFCS().getKeyword("P" + (field.getOrigIndex() + 1) + "DISPLAY");
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

    private static class FlowLogarithmicAxis extends LogarithmicAxis
    {
        public FlowLogarithmicAxis(String label)
        {
            super(label);
        }

        protected String makeTickLabel(double val)
        {
            double tester = val;
            int power = 0;
            while (tester >= 10)
            {
                tester = tester / 10;
                power++;
            }
            if (tester == 1)
                return "10^" + power;
            else
                return "";
        }
    }

    static protected ValueAxis getValueAxis(Subset subset, String name, DataFrame.Field field)
    {
        if (!displayLogarthmic(subset, field))
            return new NumberAxis(name);
        LogarithmicAxis ret = new FlowLogarithmicAxis(name);
        ret.setAllowNegativesFlag(true);
        ret.setLog10TickLabelsFlag(true);
        return ret;
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
        return getLabel(subset.getFCS(), field.getOrigIndex(), compensated);
    }

    static public DensityPlot createContourPlot(Subset subset, String domainAxis, String rangeAxis)
    {
        DataFrame data = subset.getDataFrame();
        DataFrame.Field fieldDomain = getField(data, domainAxis);
        DataFrame.Field fieldRange = getField(data, rangeAxis);
        double[] xValues = getPossibleValues(subset, fieldDomain);
        double[] yValues = getPossibleValues(subset, fieldRange);
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
}
