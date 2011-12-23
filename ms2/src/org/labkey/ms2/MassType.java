/*
 * Copyright (c) 2011 LabKey Corporation
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

/**
 * Different methods for calculating peptide and protein weighs, based on the different ways to handle
 * isotopic mass differences.
 * User: jeckels
 * Date: Dec 19, 2011
 */
public enum MassType
{
    /** Use the average mass across all isotopes */
    Average(1.00794, 15.9994, new double[]
        {
            /* A */  71.07880, /* B */ 114.59622, /* C */ 103.13880, /* D */ 115.08860, /* E */ 129.11548,
            /* F */ 147.17656, /* G */  57.05192, /* H */ 137.14108, /* I */ 113.15944, /* J */   0.00000,
            /* K */ 128.17408, /* L */ 113.15944, /* M */ 131.19256, /* N */ 114.10384, /* O */ 114.14720,
            /* P */  97.11668, /* Q */ 128.13072, /* R */ 156.18748, /* S */  87.07820, /* T */ 101.10508,
            /* U */   0.00000, /* V */  99.13256, /* W */ 186.21320, /* X */ 113.15944, /* Y */ 163.17596,
            /* Z */   0.00000
        }),
    /** Use the mass of the most common isotope */
    Monoisotopic(1.007825035, 15.99491463, new double[]
        {
            /* A */  71.03711, /* B */ 114.53494, /* C */ 103.00919, /* D */ 115.02694, /* E */ 129.04259,
            /* F */ 147.06841, /* G */  57.02146, /* H */ 137.05891, /* I */ 113.08406, /* J */   0.00000,
            /* K */ 128.09496, /* L */ 113.08406, /* M */ 131.04049, /* N */ 114.04293, /* O */ 114.14720,
            /* P */  97.05276, /* Q */ 128.05858, /* R */ 156.10111, /* S */  87.03203, /* T */ 101.04768,
            /* U */   0.00000, /* V */  99.06841, /* W */ 186.07931, /* X */ 113.08406, /* Y */ 163.06333,
            /* Z */   0.00000
        });

    private final double _hydrogenMass;
    private final double _oxygenMass;
    private final double[] _aaMasses;

    MassType(double hydrogenMass, double oxygenMass, double[] aaMasses)
    {
        _hydrogenMass = hydrogenMass;
        _oxygenMass = oxygenMass;
        _aaMasses = aaMasses;
    }

    public double getHydrogenMass()
    {
        return _hydrogenMass;
    }

    public double getOxygenMass()
    {
        return _oxygenMass;
    }

    /**
     * 26 element array. 'A' is at index 0, 'Z' at index 25.
     * B denotes N or D. The average of mass of N and D, which are approx 1 dalton apart
     * X denotes L or I - the two have identical masses
     *  UNDONE: Add U == Selenocysteine??  Add Z == glutamic acid or glutamine?
     */
    public double[] getAaMasses()
    {
        return _aaMasses;
    }
}
