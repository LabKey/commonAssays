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
package org.labkey.ms2.protein.fasta;

import org.labkey.ms2.Hydrophobicity3;

/**
 * User: migra
 * Date: Jun 23, 2004
 * Time: 10:35:50 PM
 *
 */
public class Peptide
{
    FastaProtein protein;
    int start;
    int length;
    double[] _massTab = null;
    double _mass; //According to the _massTab
    private int _hashCode = 0;

    public int hashCode()
    {
        int off = start;
        byte[] bytes = protein.getBytes();
        if (0 == _hashCode)
        {
            int h = 0;
            for (int i = 0; i < length; i++)
                h = 31*h + bytes[off++];

            _hashCode = h;
        }

        return _hashCode;
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof Peptide))
            return false;

        Peptide p = (Peptide) o;
        if(p.length != this.length)
            return false;

        byte[] bytes1 = protein.getBytes();
        byte[] bytes2 = p.protein.getBytes();

        for (int i = start, j = p.start; i < start + length; i++, j++)
            if (bytes1[i] != bytes2[j])
                return false;

        return true;
    }

    public Peptide(FastaProtein protein, int start, int length)
    {
        this.start = start;
        this.length = length;
        this.protein = protein;
    }

    public double getPi()
    {
        return computePI(protein.getBytes(), start, length);
    }

    /**
     * PI Calculation
     */

    private static final double  EPSI   = 0.0001;  /* desired precision */


    /* the 7 amino acid which matter */
    private static final int R = 'R' - 'A',
                             H = 'H' - 'A',
                             K = 'K' - 'A',
                             D = 'D' - 'A',
                             E = 'E' - 'A',
                             C = 'C' - 'A',
                             Y = 'Y' - 'A';

    /*
     *  table of pk values :
     *  Note: the current algorithm does not use the last two columns. Each
     *  row corresponds to an amino acid starting with Ala. J, O and U are
     *  non-existent, but here only in order to have the complete alphabet.
     *
     *          Ct    Nt   Sm     Sc     Sn
     */

    private static final double[][] pk = new double[][]
    {
            /* A */    {3.55, 7.59, 0.0  , 0.0  , 0.0   },
            /* B */    {3.55, 7.50, 0.0  , 0.0  , 0.0   },
            /* C */    {3.55, 7.50, 9.00 , 9.00 , 9.00  },
            /* D */    {4.55, 7.50, 4.05 , 4.05 , 4.05  },
            /* E */    {4.75, 7.70, 4.45 , 4.45 , 4.45  },
            /* F */    {3.55, 7.50, 0.0  , 0.0  , 0.0   },
            /* G */    {3.55, 7.50, 0.0  , 0.0  , 0.0   },
            /* H */    {3.55, 7.50, 5.98 , 5.98 , 5.98  },
            /* I */    {3.55, 7.50, 0.0  , 0.0  , 0.0   },
            /* J */    {0.00, 0.00, 0.0  , 0.0  , 0.0   },
            /* K */    {3.55, 7.50, 10.00, 10.00, 10.00 },
            /* L */    {3.55, 7.50, 0.0  , 0.0  , 0.0   },
            /* M */    {3.55, 7.00, 0.0  , 0.0  , 0.0   },
            /* N */    {3.55, 7.50, 0.0  , 0.0  , 0.0   },
            /* O */    {0.00, 0.00, 0.0  , 0.0  , 0.0   },
            /* P */    {3.55, 8.36, 0.0  , 0.0  , 0.0   },
            /* Q */    {3.55, 7.50, 0.0  , 0.0  , 0.0   },
            /* R */    {3.55, 7.50, 12.0 , 12.0 , 12.0  },
            /* S */    {3.55, 6.93, 0.0  , 0.0  , 0.0   },
            /* T */    {3.55, 6.82, 0.0  , 0.0  , 0.0   },
            /* U */    {0.00, 0.00, 0.0  , 0.0  , 0.0   },
            /* V */    {3.55, 7.44, 0.0  , 0.0  , 0.0   },
            /* W */    {3.55, 7.50, 0.0  , 0.0  , 0.0   },
            /* X */    {3.55, 7.50, 0.0  , 0.0  , 0.0   },
            /* Y */    {3.55, 7.50, 10.00, 10.00, 10.00 },
            /* Z */    {3.55, 7.50, 0.0  , 0.0  , 0.0   },
    };

    private static double exp10(double value)
    {
        return Math.pow(10.0,value);
    }

    private static final double PH_MIN = 0;        /* minimum pH value */
    private static final double PH_MAX = 14;       /* maximum pH value */
    private static final double MAX_LOOP = 2000;    /* maximum number of iterations */

    private static double computePI(byte[] seq, int start, int seq_length)
    {
        int[]             comp = new int[26];    /* Amino acid composition of the protein */
        int    nterm_res,   /* N-terminal residue */
                cterm_res;   /* C-terminal residue */
        int i;
        int charge_increment = 0;
        double charge,
                ph_mid = 0,
                ph_min,
                ph_max;
        double          cter,
                nter;
        double          carg,
                clys,
                chis,
                casp,
                cglu,
                ctyr,
                ccys;


        for (i = 0; i < seq_length; i++)        /* compute the amino acid composition */
        {
            comp[seq[i + start] - 'A']++;
        }

        nterm_res = seq[start] - 'A';               /* Look up N-terminal residue */
        cterm_res = seq[start + seq_length-1] - 'A';    /* Look up C-terminal residue */

        ph_min = PH_MIN;
        ph_max = PH_MAX;

        for (i = 0, charge = 1.0; i< MAX_LOOP && (ph_max - ph_min)>EPSI; i++)
        {
            ph_mid = ph_min + (ph_max - ph_min) / 2.0;

            cter = exp10(-pk[cterm_res][0]) / (exp10(-pk[cterm_res][0]) + exp10(-ph_mid));
            nter = exp10(-ph_mid) / (exp10(-pk[nterm_res][1]) + exp10(-ph_mid));

            carg = comp[R] * exp10(-ph_mid) / (exp10(-pk[R][2]) + exp10(-ph_mid));
            chis = comp[H] * exp10(-ph_mid) / (exp10(-pk[H][2]) + exp10(-ph_mid));
            clys = comp[K] * exp10(-ph_mid) / (exp10(-pk[K][2]) + exp10(-ph_mid));

            casp = comp[D] * exp10(-pk[D][2]) / (exp10(-pk[D][2]) + exp10(-ph_mid));
            cglu = comp[E] * exp10(-pk[E][2]) / (exp10(-pk[E][2]) + exp10(-ph_mid));

            ccys = comp[C] * exp10(-pk[C][2]) / (exp10(-pk[C][2]) + exp10(-ph_mid));
            ctyr = comp[Y] * exp10(-pk[Y][2]) / (exp10(-pk[Y][2]) + exp10(-ph_mid));

            charge = carg + clys + chis + nter + charge_increment
                    - (casp + cglu + ctyr + ccys + cter);

            if (charge > 0.0)
            {
                ph_min = ph_mid;
            }
            else
            {
                ph_max = ph_mid;
            }
        }

        return ph_mid;
    }

    public double getHydrophobicity()
    {
        return getHydrophobicity(protein.getBytes(), start, length);
    }

    public double getHydrophobicity3()
    {
        return Hydrophobicity3.TSUM3(new String(protein.getBytes(),start,length));
    }

    static final double[] rc = new double[]
    {
        /* A */  0.8, /* B */  0.0, /* C */ -0.8, /* D */ -0.5, /* E */  0.0, /* F */ 10.5,
        /* G */ -0.9, /* H */ -1.3, /* I */  8.4, /* J */  0.0, /* K */ -1.9, /* L */  9.6,
        /* M */  5.8, /* N */ -1.2, /* O */  0.0, /* P */  0.2, /* Q */ -0.9, /* R */ -1.3,
        /* S */ -0.8, /* T */  0.4, /* U */  0.0, /* V */  5.0, /* W */ 11.0, /* X */  0.0,
        /* Y */  4.0, /* Z */  0.0
    };


    static final double[] rcnt = new double[]
    {
        /* A */ -1.5, /* B */  0.0, /* C */  4.0, /* D */  9.0, /* E */  7.0, /* F */ -7.0,
        /* G */  5.0, /* H */  4.0, /* I */ -8.0, /* J */  0.0, /* K */  4.6, /* L */ -9.0,
        /* M */ -5.5, /* N */  5.0, /* O */  0.0, /* P */  4.0, /* Q */  1.0, /* R */  8.0,
        /* S */  5.0, /* T */  5.0, /* U */  0.0, /* V */ -5.5, /* W */ -4.0, /* X */  0.0,
        /* Y */ -3.0, /* Z */  0.0
    };


    static final double[] nt = {.42, .22, .05};


    /**
     * Implementation of Version 1.0 hydrophobicity algorithm by Oleg V. Krokhin, et al.
     * See http://hs2.proteome.ca/SSRCalc/SSRCalc.html for more details
     */
    public static double getHydrophobicity(byte[] bytes, int start, int n)
    {
        double kl = 1;

        if (n < 10)
            kl = 1 - 0.027 * (10 - n);
        else
            if (n > 20)
//                kl = 1 - 0.014 * (n - 20);      // As published in paper
                kl = 1 / (1 + 0.015 * (n -20));   // Revision from paper's author

        double h = 0;

        for (int i=0; i<n; i++)
        {
            char c = (char) bytes[i + start];
            h += rc[c - 'A'];
            if (i < 3)
                h += nt[i] * rcnt[c - 'A'];
        }

        h *= kl;

        if (h < 38)
            return h;
        else
            return h - .3 * (h - 38);
    }


    // Call version 3.0 hydrophobicity algorithm by Krokhin, et al
    public static double getHydrophobicity3(String peptide)
    {
        return Hydrophobicity3.TSUM3(peptide);
    }

    /**
     * Returns the mass according to the default mass table.
     * Default mass table is the FIRST one used to compute a mass on this peptide.
     * If mass has not been computed, mass table is set to monoisotopic, unmodified table.
     * @return mass using default mass table
     */
    public double getMass()
    {
        if (_mass != 0)
            return _mass;

        if (null != _massTab)
            return getMass(_massTab);

        throw new UnsupportedOperationException("GetMass without mass table");
    }

    public double getMass(double[] massTab)
    {
        if (_massTab == massTab && _mass != 0)
            return _mass;

        if (null == _massTab)
        {
            _massTab = massTab;
            _mass = PeptideHelpers.computeMass(protein.getBytes(), start, length, massTab);
            return _mass;
        }
        else
            return PeptideHelpers.computeMass(protein.getBytes(), start, length, massTab);
    }

    public double getAverageMass()
    {
       return getMass(PeptideHelpers.AMINO_ACID_AVERAGE_MASSES);
    }

    public double getMonoisotopicMass()
    {
        return getMass(PeptideHelpers.AMINO_ACID_MONOISOTOPIC_MASSES);
    }

    public char[] getChars()
    {
        byte[] bytes = protein.getBytes();
        char[] chars = new char[length];
        for(int i = 0; i < length; i++)
            chars[i] = (char) bytes[start + i];

        return chars;
    }

    public String toString()
    {
        return new String(protein.getBytes(), start, length);
    }

    public FastaProtein getProtein()
    {
        return protein;
    }

    public void setProtein(FastaProtein protein)
    {
        this.protein = protein;
    }

    public int getStart()
    {
        return start;
    }

    public void setStart(int start)
    {
        this.start = start;
    }

    public int getLength()
    {
        return length;
    }

    public void setLength(int length)
    {
        this.length = length;
    }
}
