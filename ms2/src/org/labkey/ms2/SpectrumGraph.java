/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2;

import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;

public class SpectrumGraph extends Graph
{
    private static Logger _log = Logger.getLogger(SpectrumGraph.class);

    private double _tolerance = 1.00000;
    private int _ionCount = 1;
    private java.util.List<String>[] _massMatches;
    private Color _bFragColor = Color.red;
    private Color _yFragColor = Color.blue;

    public SpectrumGraph(MS2Peptide peptide, int width, int height, double tolerance, double xStart, double xEnd)
    {
        super(peptide.getSpectrumMZ(), peptide.getSpectrumIntensity(), width, height, xStart, xEnd);
        _ionCount = peptide.getIonCount();
        _massMatches = peptide.getMassMatches();
        _tolerance = tolerance;
        setNoDataErrorMessage(peptide.getSpectrumErrorMessage());
    }


    protected void renderDataPoints(Graphics2D g, double xStart, double xEnd)
    {
        g.setColor(_foregroundColor);

        // Draw all the non-matches first so they don't draw over the matches
        for (int i = 0; i < _plotCount; i++)
            if (xStart <= _x[i] && xEnd >= _x[i])
                if (null == _massMatches[i])
                    renderDataPoint(g, _x[i], _y[i]);

        for (int i = 0; i < _plotCount; i++)
            if (xStart <= _x[i] && xEnd >= _x[i])
                if (null != _massMatches[i])
                    renderDataPoint(g, i);
    }


    // UNDONE: Check for proper number of '+' for charge 3+, 4+
    protected void renderDataPoint(Graphics2D g, int index)
    {
        double x = _x[index];
        double y = _y[index];

        float yLocation = (float) (y + pixelWidth(" ") / _yRatio);

        for (String fragment : _massMatches[index])
        {
            if (fragment.startsWith("b"))
                g.setColor(_bFragColor);
            else
                g.setColor(_yFragColor);

            // UNDONE: Get + to always line up with intensity bar.  Use something like (_textHeight * _yRatio / _xRatio* .29166655) instead?
            g.drawString(" " + fragment, (float) (x + _graphWidth * .00833333), yLocation);
            yLocation += pixelWidth(" " + fragment) / _yRatio;
        }

        renderDataPoint(g, x, y);
    }


    protected void renderDataPoint(Graphics2D g, double x, double y)
    {
        g.draw(new Line2D.Double(x, 0, x, y));
    }


    public void setTolerance(double tolerance)
    {
        _tolerance = tolerance;
    }


    public double getTolerance(double tolerance)
    {
        return _tolerance;
    }


    public void setIonCount(int ionCount)
    {
        _ionCount = ionCount;
    }


    public double getIonCount(double ionCount)
    {
        return _ionCount;
    }


    // Rotate font 90° to label the fragments
    public void initializeDataPoints(Graphics2D g)
    {
        AffineTransform at = g.getFont().getTransform();
        at.rotate(Math.toRadians(-90));
        g.setFont(g.getFont().deriveFont(at));
    }
}
