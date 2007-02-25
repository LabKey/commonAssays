package org.labkey.flow.analysis.chart;

import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberTick;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

import java.util.List;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;

class FlowLogarithmicAxis extends LogarithmicAxis
{
    public FlowLogarithmicAxis(String label)
    {
        super(label);
        setAllowNegativesFlag(true);
        setLog10TickLabelsFlag(true);
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
