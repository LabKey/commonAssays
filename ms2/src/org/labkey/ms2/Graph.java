/*
 * Copyright (c) 2004-2016 Fred Hutchinson Cancer Research Center
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

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.StringTokenizer;

public abstract class Graph
{
    private static Logger _log = Logger.getLogger(Graph.class);

    private int _width = 600;
    private int _height = 400;
    private double _xStart = Double.MIN_VALUE;
    private double _xEnd = Double.MAX_VALUE;
    private double _xMargin = 20;
    private double _yMargin = 35;  // UNDONE: _topMargin, _bottomMargin (top needs to be bigger in spectrum graph for y15+++... SpectrumGraph subclass overrides normal graph margins?)
    private double _bigTickIncrement = 100;
    private double _smallTickIncrement = 10;
    private double _xMin;
    private double _xMax;
    private double _yMin;
    private double _yMax;
    private String imageType = "png";

    private static FontMetrics _pixelFM = null;

    protected int _plotCount = 0;
    protected float[] _x = null;
    protected float[] _y = null;
    protected double _graphWidth = 0;
    protected double _graphHeight = 0;
    protected double _xRatio = 1;
    protected double _yRatio = 1;
    protected double _textHeight = 0;
    protected double _textHeightPixels = 0;
    protected Color _foregroundColor = Color.black;
    protected Color _backgroundColor = Color.white;

    private String _noDataErrorMessage = "No data to plot";


    public Graph()
    {
        setData(new float[0], new float[0]);
    }

    public Graph(float[] x, float[] y)
    {
        setData(x, y);
    }


    public Graph(float[] x, float[] y, int width, int height)
    {
        setData(x, y);
        setSize(width, height);
    }


    public Graph(float[] x, float[] y, int width, int height, double xStart, double xEnd)
    {
        setXRange(xStart, xEnd);
        setData(x, y);
        setSize(width, height);
    }


    public void setNoDataErrorMessage(String noDataErrorMessage)
    {
        if (noDataErrorMessage != null)
        {
            _noDataErrorMessage = noDataErrorMessage;
        }
    }

    public void setXStart(double x)
    {
        _xStart = x;
    }


    public double getXStart()
    {
        // Don't let the user scroll off the end of the graph
        double scaledMin = _xMin < Double.MAX_VALUE ? _bigTickIncrement * Math.floor(_xMin / _bigTickIncrement) : 0;  

        if (Double.MIN_VALUE != _xStart && _xStart >= scaledMin)
            return _xStart;

        return scaledMin;
    }


    public double getXEnd()
    {
        // Don't let the user scroll off the end of the graph
        double scaledMax = _xMax > Double.MIN_VALUE ? _bigTickIncrement * Math.ceil(_xMax / _bigTickIncrement) : 1000;

        if (Double.MAX_VALUE != _xEnd && _xEnd <= scaledMax)
            return _xEnd;

        return scaledMax;
    }


    public void setXEnd(double x)
    {
        _xEnd = x;
    }


    public void setXRange(double xStart, double xEnd)
    {
        setXStart(xStart);
        setXEnd(xEnd);
    }


    public void setSize(int width, int height)
    {
        _width = width;
        _height = height;
    }


    public int getWidth()
    {
        return _width;
    }


    public int getHeight()
    {
        return _height;
    }


    public void setData(float[] x, float[] y)
    {
        _x = x;
        _y = y;

        _plotCount = Math.min(_x.length, _y.length);

        _xMin = Double.MAX_VALUE;
        _xMax = Double.MIN_VALUE;
        _yMin = Double.MAX_VALUE;
        _yMax = Double.MIN_VALUE;

        for (int i = 0; i < _plotCount; i++)
        {
            if (_x[i] < _xStart || _x[i] > _xEnd)
                continue;

            if (_x[i] < _xMin) _xMin = _x[i];
            if (_x[i] > _xMax) _xMax = _x[i];
            if (_y[i] < _yMin) _yMin = _y[i];
            if (_y[i] > _yMax) _yMax = _y[i];
        }
    }


    public void render(HttpServletResponse response)
            throws IOException
    {
        BufferedImage bi = new BufferedImage(_width, _height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, _width, _height);
        g.setColor(_foregroundColor);
        OutputStream outputStream = response.getOutputStream();
        response.setContentType("image/png");

        if (0 == _plotCount)
        {
            StringTokenizer st = new StringTokenizer(_noDataErrorMessage, "\n", false);
            int lineCount = st.countTokens();
            int lineHeight = g.getFontMetrics().getHeight();
            int lineIndex = 1;
            while (st.hasMoreTokens())
            {
                String line = st.nextToken();
                g.drawString(line, (_width - g.getFontMetrics().stringWidth(line)) / 2, (_height - lineHeight) / 2 - (lineCount - lineIndex++) * lineHeight);
            }
            ImageIO.write(bi, imageType, outputStream);
            outputStream.flush();
            return;
        }

        // stringWidth and getStringBounds don't really work with scaled Graphics2D, so save away
        // a pixel-based fontMetrics so we can do accurate text positioning using manual scaling
        if (null == _pixelFM)
            _pixelFM = g.getFontMetrics();

        _textHeightPixels = g.getFontMetrics().getHeight();
        double graphWidthPixels = _width - 2 * _xMargin;
        double graphHeightPixels = _height - 2 * _yMargin;

        double xStart = getXStart();
        double xEnd = getXEnd();
        _graphWidth = xEnd - xStart;
        _graphHeight = _yMax;

        // Set origin to lower left corner of graph
        g.translate(_xMargin, _height - _yMargin);

        // Scale in X direction to range of X, in Y direction based on maxY
        _xRatio = graphWidthPixels / _graphWidth;
        _yRatio = graphHeightPixels / _graphHeight;
        g.scale(_xRatio, -_yRatio);

        // Compute the (scaled) textHeight manually since _g.getFontMetrics().getHeight() returns 0 most
        // of the time (because it returns integer, which doesn't work well for this scaling?)
        _textHeight = _textHeightPixels / _yRatio;

        // Now reset the X origin so xStart is at the lower left corner of the graph
        g.translate(-xStart, 0);

        // We want one-pixel-wide lines, regardless of scaling
        // So, take the reciprocal of the larger pixelToScale ratio
        g.setStroke(new BasicStroke((float) (1 / Math.max(_xRatio, _yRatio))));

        // Draw X axis
        g.draw(new Line2D.Double(xStart, 0, xEnd, 0));

        double bigTickHeight = _graphHeight / 50;
        double smallTickHeight = bigTickHeight / 2;

        double bigTickCount = (int) Math.ceil(_graphWidth / _bigTickIncrement) + 1;
        double bigTickWidth = _graphWidth / (bigTickCount - 1);

        double smallTickCount = (bigTickCount - 1) * _smallTickIncrement + 1;
        double smallTickWidth = _graphWidth / (smallTickCount - 1);

        for (double x = xStart; x <= xEnd; x += smallTickWidth)
            g.draw(new Line2D.Double(x, 0, x, -smallTickHeight));

        for (double x = xStart; x <= xEnd; x += bigTickWidth)
            g.draw(new Line2D.Double(x, 0, x, -bigTickHeight));

        // Label the X axis.
        // I don't want the font size to scale with the graph scale, especially since
        // we have totally different scale factors in the x and y directions.  So,
        // apply a transform to the font that reverses the effect of the graph scale.
        AffineTransform at = g.getFont().getTransform();
        at.scale(1 / _xRatio, -1 / _yRatio);
        g.setFont(g.getFont().deriveFont(at));

        // Calculate the width of the largest label.  Use this to determine the label increment
        // to ensure that all the labels will fit, including at least a 20% space between then.
        double maxLabelWidth = g.getFontMetrics().stringWidth(String.valueOf(Math.round(xEnd)));
        double labelIncrement = bigTickWidth * Math.ceil(maxLabelWidth * 1.1 / bigTickWidth);

        float labelYPosition = - (float) (_textHeight + smallTickHeight);

        // maxLabelWidth (and therefore labelIncrement) end up zero for very zoomed in graphs
        // In that case, skip the labels
        if (0 != labelIncrement)
        {
            for (double x = xStart; x <= xEnd; x += labelIncrement)
            {
                String labelString = String.valueOf(Math.round(x));
                g.drawString(labelString, (float) x - g.getFontMetrics().stringWidth(labelString) / 2, labelYPosition);
            }
        }

        initializeDataPoints(g);
        renderDataPoints(g, xStart, xEnd);

        ImageIO.write(bi, imageType, outputStream);
        outputStream.flush();
    }


    protected void renderDataPoints(Graphics2D g, double xStart, double xEnd)
    {
        for (int i = 0; i < _plotCount; i++)
            if (xStart <= _x[i] && xEnd >= _x[i])
                renderDataPoint(g, _x[i], _y[i]);
    }


    protected int pixelWidth(String text)
    {
        return _pixelFM.stringWidth(text);
    }


    protected abstract void initializeDataPoints(Graphics2D g);

    protected abstract void renderDataPoint(Graphics2D g, double x, double y);
}
