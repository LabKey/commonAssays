/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.ui.RectangleEdge;
import org.jfree.data.Range;
import org.labkey.flow.analysis.util.LogAxisFunction;
import org.labkey.flow.analysis.util.RangeFunction;
import org.labkey.flow.analysis.util.SimpleLogAxisFunction;

import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;


public class FlowLogarithmicAxis extends LogarithmicAxis
{
    static public final int LOG_LIN_SWITCH = 50;
    static LogAxisFunction loglinFN = new LogAxisFunction(LOG_LIN_SWITCH);
    static SimpleLogAxisFunction simpleFN = new SimpleLogAxisFunction();
    final RangeFunction fn;

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


    public FlowLogarithmicAxis(String label, boolean simpleLog)
    {
        super(label);
        setAllowNegativesFlag(true);
        setNumberFormatOverride(new TickFormat());
        fn = simpleLog ? simpleFN : loglinFN;
    }

    public boolean isSimpleLog()
    {
        return fn == simpleFN;
    }

    /**
     * Turn on the smallLogFlag if the axis is a simple log and the lower bound is >= zero and < 10.
     * The superclass will only turn on the smallLogFlag if negatives aren't allowed and the lower bound is > 0 and < 10.
     */
    @Override
    protected void setupSmallLogFlag()
    {
        if (fn == simpleFN)
        {
            double lowerVal = getRange().getLowerBound();
            this.smallLogFlag
                    = (lowerVal < 10.0 && lowerVal >= 0.0);
        }
        else
        {
            super.setupSmallLogFlag();
        }
    }

    /**
     * LogarithmicAxis tries to force the formatting of the tick to "0.00" style numbers
     * when displaying small log values in 0-1 range.  We don't care about the small log tick marks.
     */
    @Override
    protected String makeTickLabel(double val, boolean forceFmtFlag)
    {
        return makeTickLabel(val);
    }

    @Override
    protected String makeTickLabel(double val)
    {
        if (val == 0 || val == SMALL_LOG_VALUE)
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
        if (fn == loglinFN && power < 2)
            return "";
        if (tester == 1)
            return (neg ? "-" : "") + "10^" + power;
        else
            return "";
    }

    // To place the tick marks in the 0-1 range, we subtract one and use our log fn.
    @Override
    protected double switchedLog10(double val)
    {
        return fn == loglinFN ?
                super.switchedLog10(val) :
                (this.smallLogFlag) ? s_adjustedLog10(val-1) : s_adjustedLog10(val);
    }

    public double s_adjustedLog10(double val)
    {
        return fn.compute(val);
    }

    public double s_adjustedPow10(double val)
    {
        return fn.invert(val);
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
