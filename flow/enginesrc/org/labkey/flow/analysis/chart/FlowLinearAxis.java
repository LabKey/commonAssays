package org.labkey.flow.analysis.chart;

import org.jfree.chart.axis.NumberAxis;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Sets up tick format to use compact labels: "200K", "1.5M"
 */
public class FlowLinearAxis extends NumberAxis
{
    private static final NumberFormat TICK_FORMAT;

    static {
        TICK_FORMAT = NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);
        TICK_FORMAT.setMaximumFractionDigits(2);
    }

    public FlowLinearAxis(String label)
    {
        super(label);
        setNumberFormatOverride(TICK_FORMAT);
    }
}
