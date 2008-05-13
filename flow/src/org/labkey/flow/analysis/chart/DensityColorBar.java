/*
 * Copyright (c) 2005-2008 LabKey Corporation
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

import org.jfree.chart.axis.ColorBar;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.plot.Plot;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Subclass which draws as small of a colorbar as possible.
 */
public class DensityColorBar extends ColorBar
    {
    public DensityColorBar(String label)
        {
        super(label);
        }

    public AxisSpace reserveSpace(Graphics2D g2, Plot plot, Rectangle2D plotArea, Rectangle2D dataArea, RectangleEdge edge, AxisSpace space)
        {
        return new AxisSpace();
        }

        public double draw(Graphics2D g2, double cursor, Rectangle2D plotArea, Rectangle2D dataArea, Rectangle2D reservedArea, RectangleEdge edge)
        {
            // no-op
            return cursor;
        }
    }
