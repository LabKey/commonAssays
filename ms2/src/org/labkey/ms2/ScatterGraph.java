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

import java.awt.geom.Line2D;
import java.awt.*;

public class ScatterGraph extends Graph
{
    private double xPrev;
    private double yPrev;

    public ScatterGraph(float[] x, float[] y)
    {
        super(x, y);
    }


    public ScatterGraph(float[] x, float[] y, int width, int height)
    {
        super(x, y, width, height);
    }


    protected void initializeDataPoints(Graphics2D g)
    {
        xPrev = Double.NaN;
        yPrev = Double.NaN;
    }


    // Draw point as XY scatter graph
    protected void renderDataPoint(Graphics2D g, double x, double y)
    {
        if (!Double.isNaN(xPrev))
            g.draw(new Line2D.Double(xPrev, yPrev, x, y));

        xPrev = x;
        yPrev = y;
    }
}
