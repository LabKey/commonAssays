/*
 * Copyright (C) 2005 LabKey LLC. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.labkey.flow.analysis.model;

/**
 * Created by IntelliJ IDEA.
 * User: mbellew
 * Date: May 2, 2005
 * Time: 3:08:10 PM
 */
public interface ScalingFunction
	{
	double untranslate(double x);
    double translate(double x);
    boolean isLogarithmic();
    double dither(double x);

    static ScalingFunction IDENTITY = new ScalingFunction()
        {
        public double untranslate(double x)
            {
            return x;
            }

        public double translate(double x)
            {
            return x;
            }

        public boolean isLogarithmic()
            {
            return false;
            }
        public double dither(double x)
            {
            return x;
            }
        };
	}
