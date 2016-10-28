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

import org.junit.Test;
import org.labkey.api.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class MS2Modification
{
    // UNDONE: Change Strings to chars... JDBC seems to have a problem with chars right now
    protected int run = 0;
    protected String aminoAcid = "";
    protected float massDiff = 0;
    protected float mass = 0;
    protected boolean variable = false;
    protected String symbol = null;


    public MS2Modification()
    {
    }


    public String toString()
    {
        return run + " " + aminoAcid + " " + massDiff + " " + variable + " " + symbol;
    }

    public int getRun()
    {
        return run;
    }


    public void setRun(int run)
    {
        this.run = run;
    }


    public String getAminoAcid()
    {
        return aminoAcid;
    }


    public void setAminoAcid(String aminoAcid)
    {
        if (aminoAcid.length() != 1 || aminoAcid.compareTo("A") < 0 || "Z".compareTo(aminoAcid) < 0)
            throw new RuntimeException("Invalid amino acid specified: \"" + aminoAcid + "\"");

        this.aminoAcid = aminoAcid;
    }


    public float getMassDiff()
    {
        return massDiff;
    }


    public void setMassDiff(float massDiff)
    {
        this.massDiff = massDiff;
    }


    public float getMass()
    {
        return mass;
    }


    public void setMass(float mass)
    {
        this.mass = mass;
    }


    public boolean getVariable()
    {
        return variable;
    }


    public void setVariable(boolean variable)
    {
        this.variable = variable;
    }


    public String getSymbol()
    {
        return symbol;
    }


    public void setSymbol(String symbol)
    {
        this.symbol = symbol;
    }


    private static final String VALID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static class MS2ModificationTest
    {
        @Test
        public void test()
        {
            List<Pair<String, Boolean>> testAminoAcids = new ArrayList<>(300);

            // Add all characters 0 - 255, marking each as valid or not
            for (int i = 0; i < 256; i++)
            {
                String aa = String.valueOf((char)i);
                testAminoAcids.add(new Pair<>(aa, VALID_CHARS.contains(aa)));
            }

            // Add a few more bogus amino acids
            testAminoAcids.add(new Pair<>("", false));
            testAminoAcids.add(new Pair<>("AA", false));
            testAminoAcids.add(new Pair<>("z", false));
            testAminoAcids.add(new Pair<>("a", false));
            testAminoAcids.add(new Pair<>("[", false));
            testAminoAcids.add(new Pair<>("]", false));
            testAminoAcids.add(new Pair<>("0", false));
            testAminoAcids.add(new Pair<>("9", false));
            testAminoAcids.add(new Pair<>("hello", false));
            testAminoAcids.add(new Pair<>("$", false));

            MS2Modification mod = new MS2Modification();

            // Test all characters
            for (Pair<String, Boolean> pair : testAminoAcids)
            {
                Boolean success;

                try
                {
                    mod.setAminoAcid(pair.first);
                    success = true;
                }
                catch (Exception e)
                {
                    success = false;
                }

                if (pair.second != success)
                {
                    throw new RuntimeException("Amino acid \"" + pair.first + "\" failed validation in setAminoAcid()");
                }
            }
        }
    }
}
