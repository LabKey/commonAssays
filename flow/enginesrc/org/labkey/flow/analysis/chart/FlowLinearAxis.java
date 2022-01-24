package org.labkey.flow.analysis.chart;

import org.jfree.chart.axis.NumberAxis;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Sets up tick format to use compact labels: "200K", "1.5M"
 */
public class FlowLinearAxis extends NumberAxis
{
    public FlowLinearAxis(String label)
    {
        super(label);

        var format = NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);
        format.setMaximumFractionDigits(2);
        setNumberFormatOverride(format);
    }
}
