/*
 * Copyright (c) 2004-2018 Fred Hutchinson Cancer Research Center
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
package org.labkey.api.protein.fasta;

public class PeptideHelpers
{
    public static double[] AMINO_ACID_AVERAGE_MASSES = getMasses(false);
    public static double[] AMINO_ACID_MONOISOTOPIC_MASSES = getMasses(true);
    //Index of H-ION in acid tables
    public static final int H_ION_INDEX = 0;
    public static final double ELECTRON_MASS = 5.485e-4;

    public static double computeMass(byte[] bytes, int start, int length, double[] massTab)
    {
        double pepMass = massTab['h'] + massTab['o'] + massTab['h'];
        for (int a = start; a < start + length; a++ )
            pepMass += massTab[bytes[a]];

        return pepMass;
    }

    /**
     * Taken from AminoAcidMasses.h on sourceforge.net. Returns 128 character array.
     * Lower case indexes h, o, c, n, p, s are masses for corresponding elements
     * Upper case indexes are Amino Acid Masses.
     *
     * @param monoisotopic If true, return monoisotopic masses, otherwise average masses
     * @return the masses
     */
    private static double[] getMasses(boolean monoisotopic)
    {
       double[] aaMasses = new double[128];
       if (!monoisotopic)
       {
          aaMasses['h']=  1.00794;  /* hydrogen */
          aaMasses['o']= 15.9994;   /* oxygen */
          aaMasses['c']= 12.0107;   /* carbon */
          aaMasses['n']= 14.00674;  /* nitrogen */
          aaMasses['p']= 30.973761; /* phosporus */
          aaMasses['s']= 32.066;    /* sulphur */

          aaMasses['G']= 57.05192;
          aaMasses['A']= 71.07880;
          aaMasses['S']= 87.07820;
          aaMasses['P']= 97.11668;
          aaMasses['V']= 99.13256;
          aaMasses['T']=101.10508;
          aaMasses['C']=103.13880; /* 103.1448, 103.14080 */
          aaMasses['L']=113.15944;
          aaMasses['I']=113.15944;
          aaMasses['X']=113.15944;
          aaMasses['N']=114.10384;
          aaMasses['O']=114.14720;
          aaMasses['B']=114.59622;
          aaMasses['D']=115.08860;
          aaMasses['Q']=128.13072;
          aaMasses['K']=128.17408;
          aaMasses['Z']=128.62310;
          aaMasses['E']=129.11548;
          aaMasses['M']=131.19256; /* 131.19456 131.1986 */
          aaMasses['H']=137.14108;
          aaMasses['F']=147.17656;
          aaMasses['R']=156.18748;
          aaMasses['Y']=163.17596;
          aaMasses['W']=186.21320;
       }
       else /* monoisotopic masses */
       {
          aaMasses['h']=  1.0078250;
          aaMasses['o']= 15.9949146;
          aaMasses['c']= 12.0000000;
          aaMasses['n']= 14.0030740;
          aaMasses['p']= 30.9737633;
          aaMasses['s']= 31.9720718;

          aaMasses['G']= 57.0214636;
          aaMasses['A']= 71.0371136;
          aaMasses['S']= 87.0320282;
          aaMasses['P']= 97.0527636;
          aaMasses['V']= 99.0684136;
          aaMasses['T']=101.0476782;
          aaMasses['C']=103.0091854;
          aaMasses['L']=113.0840636;
          aaMasses['I']=113.0840636;
          aaMasses['X']=113.0840636;
          aaMasses['N']=114.0429272;
          aaMasses['O']=114.0793126;
          aaMasses['B']=114.5349350;
          aaMasses['D']=115.0269428;
          aaMasses['Q']=128.0585772;
          aaMasses['K']=128.0949626;
          aaMasses['Z']=128.5505850;
          aaMasses['E']=129.0425928;
          aaMasses['M']=131.0404854;
          aaMasses['H']=137.0589116;
          aaMasses['F']=147.0684136;
          aaMasses['R']=156.1011106;
          aaMasses['Y']=163.0633282;
          aaMasses['W']=186.0793126;
       }

        aaMasses[H_ION_INDEX] = aaMasses['h'] - ELECTRON_MASS;
        return aaMasses;
    } /*ASSIGN_MASS*/
}
