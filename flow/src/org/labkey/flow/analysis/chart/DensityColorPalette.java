package org.labkey.flow.analysis.chart;

import org.jfree.chart.plot.RainbowPalette;

/**
 * Subclass which maps the value "0" to white.
 */
public class DensityColorPalette extends RainbowPalette
    {
    public DensityColorPalette()
        {
        super();
        initialize();
        }

    /*public void initialize()
        {
        super.initialize();
        // Palette indexes 0 and 1 are unused (apparently reserved for "white" and "black").
        // Therefore, the number "0" maps to palette index 2.
        // We want that color to be "white".
        this.r[2] = 255;
        this.g[2] = 255;
        this.b[2] = 255;
        }*/
    }
