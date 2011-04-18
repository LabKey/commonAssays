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
