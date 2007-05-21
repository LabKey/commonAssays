package org.labkey.flow.analysis.chart;

import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.Tick;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;
import org.jfree.data.Range;

import java.util.List;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;

public class FlowLogarithmicAxis extends LogarithmicAxis
{
    static public final int LOG_LIN_SWITCH = 50;

    private class TickFormat extends NumberFormat
    {
        public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos)
        {
            return toAppendTo.append(makeTickLabel(number));
        }

        public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos)
        {
            return format((double) number, toAppendTo, pos);
        }

        public Number parse(String source, ParsePosition parsePosition)
        {
            throw new UnsupportedOperationException();
        }

    }

    public FlowLogarithmicAxis(String label)
    {
        super(label);
        setAllowNegativesFlag(true);
        setNumberFormatOverride(new TickFormat());
    }

    protected String makeTickLabel(double val)
    {
        if (val == 0)
            return "0";
        boolean neg = false;
        if (val < 0)
        {
            val = -val;
            neg = true;
        }
        double tester = val;
        int power = 0;
        while (tester >= 10)
        {
            tester = tester / 10;
            power++;
        }
        if (power < 2)
            return "";
        if (tester == 1)
            return (neg ? "-" : "") + "10^" + power;
        else
            return "";
    }


    static public double s_adjustedLog10(double val)
    {
        boolean negFlag = (val < 0.0);
        if (negFlag) {
            val = -val;          // if negative then set flag and make positive
        }
        double ret;
        if (val < LOG_LIN_SWITCH) {                // if < 10 then
            ret = val / LOG_LIN_SWITCH / Math.log(10);
        }
        else
        {
            ret = Math.log10(val) + (1 - Math.log(LOG_LIN_SWITCH)) / Math.log(10);
        }
        //return value; negate if original value was negative:
        return negFlag ? -ret
                : ret;
    }

    static public double s_adjustedPow10(double val)
    {
        boolean neg = false;
        if (val < 0)
        {
            neg = true;
            val = - val;
        }
        double ret;
        if (val < 1 / Math.log(10))
        {
            ret = val * LOG_LIN_SWITCH * Math.log(10);
        }
        else
        {
            ret = Math.pow(10, val - (1 - Math.log(LOG_LIN_SWITCH)) / Math.log(10));
        }

        return neg ? -ret : ret;
    }

    /**
     * Converts a data value to a coordinate in Java2D space, assuming that
     * the axis runs along one edge of the specified plotArea.
     * Note that it is possible for the coordinate to fall outside the
     * plotArea.
     *
     * @param value  the data value.
     * @param plotArea  the area for plotting the data.
     * @param edge  the axis location.
     *
     * @return The Java2D coordinate.
     */
    public double valueToJava2D(double value, Rectangle2D plotArea,
                                RectangleEdge edge) {

        Range range = getRange();
        double axisMin = s_adjustedLog10(range.getLowerBound());
        double axisMax = s_adjustedLog10(range.getUpperBound());

        double min = 0.0;
        double max = 0.0;
        if (RectangleEdge.isTopOrBottom(edge)) {
            min = plotArea.getMinX();
            max = plotArea.getMaxX();
        }
        else if (RectangleEdge.isLeftOrRight(edge)) {
            min = plotArea.getMaxY();
            max = plotArea.getMinY();
        }

        value = s_adjustedLog10(value);

        if (isInverted()) {
            return max
                - (((value - axisMin) / (axisMax - axisMin)) * (max - min));
        }
        else {
            return min
                + (((value - axisMin) / (axisMax - axisMin)) * (max - min));
        }

    }

    /**
     * Converts a coordinate in Java2D space to the corresponding data
     * value, assuming that the axis runs along one edge of the specified
     * plotArea.
     *
     * @param java2DValue  the coordinate in Java2D space.
     * @param plotArea  the area in which the data is plotted.
     * @param edge  the axis location.
     *
     * @return The data value.
     */
    public double java2DToValue(double java2DValue, Rectangle2D plotArea,
                                RectangleEdge edge) {

        Range range = getRange();
        double axisMin = s_adjustedLog10(range.getLowerBound());
        double axisMax = s_adjustedLog10(range.getUpperBound());

        double plotMin = 0.0;
        double plotMax = 0.0;
        if (RectangleEdge.isTopOrBottom(edge)) {
            plotMin = plotArea.getX();
            plotMax = plotArea.getMaxX();
        }
        else if (RectangleEdge.isLeftOrRight(edge)) {
            plotMin = plotArea.getMaxY();
            plotMax = plotArea.getMinY();
        }

        if (isInverted()) {
            return s_adjustedPow10(
                axisMax - ((java2DValue - plotMin) / (plotMax - plotMin))
                * (axisMax - axisMin)
            );
        }
        else {
            return s_adjustedPow10(
                axisMin + ((java2DValue - plotMin) / (plotMax - plotMin))
                * (axisMax - axisMin)
            );
        }
    }


}
