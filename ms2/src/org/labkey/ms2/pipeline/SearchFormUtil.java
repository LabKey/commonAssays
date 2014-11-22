/*
 * Copyright (c) 2008-2014 LabKey Corporation
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

package org.labkey.ms2.pipeline;

import org.labkey.ms2.pipeline.client.CutSite;
import org.labkey.ms2.pipeline.client.Enzyme;

import java.util.*;

/**
 * User: billnelson@uky.edu
 * Date: Apr 24, 2008
 */

/**
 * <code>EnzymeUtil</code>
 */
public class SearchFormUtil
{
    private static List<Enzyme> tppEnzymeList;
    private static Map<String, String> mod0Map;
    private static Map<String, String> mod1Map;
    private static Set<String> residues;

    public static List<Enzyme> getDefaultEnzymeList()
    {
        if(tppEnzymeList != null) return tppEnzymeList;
        tppEnzymeList = new ArrayList<>();
        tppEnzymeList.add(new Enzyme("Trypsin",new String[]{"trypsin"},
                new CutSite[]{new CutSite( new char[]{'K','R'}, new char[]{'P'},"[KR]|{P}", false)}));
        tppEnzymeList.add(new Enzyme("Strict Trypsin",new String[]{"stricttrypsin"},
                new CutSite[]{new CutSite(new char[]{'K','R'}, new char[]{},"[KR]|[X]", false)}));
        tppEnzymeList.add(new Enzyme("ArgC",new String[]{"argc", "arg-c"},
                new CutSite[]{new CutSite( new char[]{'R'}, new char[]{'P'},"[R]|{P}", false)}));
        tppEnzymeList.add(new Enzyme("AspN",new String[]{"aspn","asp-n"},
                new CutSite[]{new CutSite(new char[]{'D'}, new char[]{},"[X]|[D]", true)}));
        tppEnzymeList.add(new Enzyme("Chymotrypsin", new String[]{"chymotrypsin"},
                new CutSite[]{new CutSite( new char[]{'Y','W','F','M','L'}, new char[]{'P'},"[FLMWY]|{P}", false), new CutSite(new char[]{'Y','W','F','M'}, new char[]{'P'},"[YWFM]|{P}", false)}));
        tppEnzymeList.add(new Enzyme("Clostripain", new String[]{"clostripain"},
                new CutSite[]{new CutSite( new char[]{'R'}, new char[]{'-'},"[R]|[X]", false)}));
        tppEnzymeList.add(new Enzyme("CNBr", new String[]{"cnbr"},
                new CutSite[]{new CutSite(new char[]{'M'}, new char[]{'P'},"[M]|{P}", false)}));
        tppEnzymeList.add(new Enzyme("Elastase", new String[]{"elastase"},
                new CutSite[]{new CutSite( new char[]{'G','V','L','I','A'}, new char[]{'P'},"[GVLIA]|{P}", false)}));
        tppEnzymeList.add(new Enzyme("Formic acid", new String[]{"formicacid","formic_acid"},
                new CutSite[]{new CutSite( new char[]{'D'}, new char[]{'P'},"[D]|{P}", false)}));
        tppEnzymeList.add(new Enzyme("GluC", new String[]{"gluc"},
                new CutSite[]{new CutSite( new char[]{'D','E'}, new char[]{'P'},"[DE]|{P}", false)}));
        tppEnzymeList.add(new Enzyme("GluC bicarb", new String[]{"gluc_bicarb"},
                new CutSite[]{new CutSite( new char[]{'E'}, new char[]{'P'},"[E]|{P}", false)}));
        tppEnzymeList.add(new Enzyme("Iodosobenzoate",new String[]{"iodosobenzoate"},
                new CutSite[]{new CutSite( new char[]{'W'}, new char[]{'-'},"[W]|[X]", false)}));
        tppEnzymeList.add(new Enzyme("LysC", new String[]{"lysc","lys-c"},
                new CutSite[]{new CutSite( new char[]{'K'}, new char[]{'P'},"[K]|{P}", false)}));
        tppEnzymeList.add(new Enzyme("Strict LysC", new String[]{"lysc-p","lys-c/p"},
                new CutSite[]{new CutSite(new char[]{'K'}, new char[]{},"[K]|[X]", false)}));
        tppEnzymeList.add(new Enzyme("LysN", new String[]{"lysn"},
                new CutSite[]{new CutSite(new char[]{'K'}, new char[]{},"[X]|[K]", true)}));
        tppEnzymeList.add(new Enzyme("LysN promisc", new String[]{"lysn_promisc"},
                new CutSite[]{new CutSite(new char[]{'K','A','S','R'}, new char[]{},"[X]|[KASR]", true)}));
        tppEnzymeList.add(new Enzyme("None", new String[]{"nonspecific"},
                new CutSite[]{new CutSite( new char[]{}, new char[]{},"[X]|[X]", true)}));
        tppEnzymeList.add(new Enzyme("PepsinA", new String[]{"pepsina"},
                new CutSite[]{new CutSite(new char[]{'F','L'}, new char[]{'-'},"[FL]|[X]", false)}));
        tppEnzymeList.add(new Enzyme("Protein endopeptidase", new String[]{"protein_endopeptidase"},
                new CutSite[]{new CutSite( new char[]{'P'}, new char[]{'-'},"[P]|[X]", false)}));
        tppEnzymeList.add(new Enzyme("Staph protease", new String[]{"staph_protease"},
                new CutSite[]{new CutSite(new char[]{'E'}, new char[]{'-'},"[E]|[X]", false)}));
        //Sequest cannot handle mixed sence
//        tppEnzymeList.add(new Enzyme("TCA", new String[]{"tca"},
//                new CutSite[]{new CutSite( new char[]{'K','R'}, new char[]{'P'},"[KR]|{P}", false),
//                              new CutSite( new char[]{'Y','W','F','M'}, new char[]{'P'},"[YWFM]|{P}", false),
//                              new CutSite( new char[]{'D'}, new char[]{},"[X]|[D]", true)}));
        tppEnzymeList.add(new Enzyme("Trypsin/CnBr", new String[]{"trypsin/cnbr","tryp-cnbr"},
                new CutSite[]{new CutSite( new char[]{'K','R'}, new char[]{'P'},"[KR]|{P}", false),
                              new CutSite( new char[]{'M'}, new char[]{'P'},"[M]|{P}", false)}));
        tppEnzymeList.add(new Enzyme("Trypsin/GluC", new String[]{"trypsin_gluc"},
                new CutSite[]{new CutSite(new char[]{'D','E','K','R'}, new char[]{'P'},"[DEKR]|{P}", false)}));
        tppEnzymeList.add(new Enzyme("TrypsinK", new String[]{"trypsin_k"},
                new CutSite[]{new CutSite(new char[]{'K'}, new char[]{'P'},"[K]|{P}", false)}));
        tppEnzymeList.add(new Enzyme("TrypsinR", new String[]{"trypsin_r"},
                new CutSite[]{new CutSite(new char[]{'R'}, new char[]{'P'},"[R]|{P}", false)}));
        tppEnzymeList.add(new Enzyme("Trypsin/Chymotrypsin", new String[]{"trypsin_chymotrypsin"},
                new CutSite[]{new CutSite(new char[]{'R', 'K', 'W', 'Y', 'F'}, new char[]{'P'},"[RKWYF]|{P}", false)}));
        // Not currently defined correctly in TPP; no cut is at n-term DE not c-term P
//        tppEnzymeList.add(new Enzyme("Thermolysin", new String[]{"thermolysin"},
//                new CutSite[]{new CutSite(new char[]{'A','L','I','V','F','M'}, new char[]{'P'},"[ALIVFM]|{P}", true)}));
        return tppEnzymeList;
    }

    public static Map<String, List<String>> getDefaultEnzymeMap()
    {
        Map<String, List<String>> enzymeMap = new HashMap<>();

        for(Enzyme enz: getDefaultEnzymeList())
        {
            String name = enz.getDisplayName();
            List<String> signatures = new ArrayList<>();
            for (CutSite site : enz.getCutSite())
            {
                signatures.add(site.getSignature());
            }
            enzymeMap.put(name, signatures);
        }
        return enzymeMap;
    }

    public static Map<String, List<String>> mascot2Tpp(List<Enzyme> mascotEnzymeList)
    {
        Map<String, List<String>> enzymeMap = new HashMap<>();
        getDefaultEnzymeList();
        for(Enzyme mascotEnz: mascotEnzymeList)
        {
            if(mascotEnz.getCutSite()[0].getSignature().equalsIgnoreCase("None"))
            {
                List<String> values = new ArrayList<>();
                values.add(mascotEnz.getCutSite()[0].getSignature());
                enzymeMap.put("None", values);
                continue;
            }
            for(Enzyme tppEnz: tppEnzymeList)
            {
                if(mascotEnz.equals(tppEnz))
                {
                    String displayName = tppEnz.getDisplayName();
                    String signature = mascotEnz.getCutSite()[0].getSignature();
                    List<String> values = new ArrayList<>();
                    values.add(signature);
                    enzymeMap.put(displayName, values);
                }
            }
        }
        return enzymeMap;
    }

    public static Map<String, String> getDefaultDynamicMods()
    {
        if( mod1Map == null)
        {
            mod1Map = new HashMap<>();
            mod1Map.put("Oxidation (15.994915@M)", "15.994915@M");
            mod1Map.put("Oxidation (15.994915@W)", "15.994915@W");
            mod1Map.put("Deamidation (0.984016@N)", "0.984016@N");
            mod1Map.put("Deamidation (0.984016@Q)", "0.984016@Q");
            mod1Map.put("Phospho (79.966331@S)","79.966331@S");
            mod1Map.put("Phospho (79.966331@T)", "79.966331@T");
            mod1Map.put("Phospho (79.966331@Y)", "79.966331@Y");
            mod1Map.put("Sulfo (79.956815@Y)", "79.956815@Y");
            mod1Map.put("Acetyl (42.010565@K)", "42.010565@K");
            mod1Map.put("Carbamyl (43.005814@N-term)", "43.005814@[");
            mod1Map.put("Carbamyl (43.005814@K)","43.005814@K");
            mod1Map.put("Carbamidomethyl (57.021464@N-term)", "57.021464@[");
            mod1Map.put("Carbamidomethyl (57.021464@K)", "57.021464@K");
            mod1Map.put("ICAT-D:2H(8) (8.0502@C)", "8.0502@C");
            mod1Map.put("ICAT-C:13C(9) (9.0302@C)", "9.0302@C");
            mod1Map.put("iTRAQ (144.102063@N-term,K)", "144.102063@[,144.102063@K");
            mod1Map.put("iTRAQ 8-plex (304.2@N-term,K)", "304.2@[,304.2@K");
            mod1Map.put("Label:13C(6) (6.020129@L)", "6.020129@L");
            mod1Map.put("Label:13C(6)15N(2) (8.014199@K)", "8.014199@K");
            mod1Map.put("Label:13C(6) (6.020129@R)", "6.020129@R");
            mod1Map.put("Label:2H(4) (4.025107@K)", "4.025107@K");
            mod1Map.put("Nic-h4 (-4.026655@N-term,K)", "-4.026655@[,-4.026655@K");
            mod1Map.put("Nic-d4 (4.026655@N-term,K)", "4.026655@[,4.026655@K");
        }
        return mod1Map;
    }

    public static Map<String, String> getDefaultStaticMods()
    {
        if( mod0Map == null)
        {
            mod0Map = new HashMap<>();
            mod0Map.put("Carbamidomethyl (57.021464@C)", "57.021464@C");
            mod0Map.put("Carboxymethyl (58.005479@C)", "58.005479@C");
            mod0Map.put("ICAT-D (442.224991@C)","442.224991@C");
            mod0Map.put("ICAT-C (227.126991@C)", "227.126991@C");
            mod0Map.put("iTRAQ (144.102063@N-term,K)", "144.102063@[,144.102063@K");
            mod0Map.put("Propionamide (71.037114@C)", "71.037114@C");
            mod0Map.put("Pyridylethyl (105.057849@C)", "105.057849@C");
            mod0Map.put("Nic-h4 (105.021464@N-term,K)", "105.021464@[,105.021464@K");
            mod0Map.put("Nic-d4 (109.048119@N-term,K)", "109.048119@[,109.048119@K");
        }
        return mod0Map;
    }

    public static Set getValidResidues()
    {
        if(residues == null)
        {
            residues = new TreeSet<>();
            residues.add("A");
            residues.add("B");
            residues.add("C");
            residues.add("D");
            residues.add("E");
            residues.add("F");
            residues.add("G");
            residues.add("H");
            residues.add("I");
            residues.add("K");
            residues.add("L");
            residues.add("M");
            residues.add("N");
            residues.add("O");
            residues.add("P");
            residues.add("Q");
            residues.add("R");
            residues.add("S");
            residues.add("T");
            residues.add("V");
            residues.add("W");
            residues.add("X");
            residues.add("Y");
            residues.add("Z");
        }
        return residues;
    }
}
