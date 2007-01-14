package org.labkey.ms2.protein.tools;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import java.util.*;
import java.text.AttributedString;

public class GOPieSectionLabelGenerator implements PieSectionLabelGenerator
{
    public AttributedString generateAttributedSectionLabel(PieDataset pieDataset, Comparable comparable)
    {
        return new AttributedString(generateSectionLabel(pieDataset, comparable));
    }

    public String generateSectionLabel(PieDataset p, Comparable key)
    {
        String retVal = key.toString();
        if (retVal.startsWith("GO:")) retVal = retVal.substring(11);
        int n = p.getValue(key).intValue();
        retVal += "[" + n + "]";
        return retVal;
    }
}

