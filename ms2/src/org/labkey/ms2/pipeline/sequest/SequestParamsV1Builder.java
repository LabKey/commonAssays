/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.ms2.pipeline.sequest;

import java.net.URI;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * User: billnelson@uky.edu
 * Date: Jan 23, 2007
 * Time: 4:57:14 PM
 */
public class SequestParamsV1Builder extends SequestParamsBuilder
{

    public SequestParamsV1Builder(Map<String, String> sequestInputParser, URI uriSequenceRoot)
    {
        super(sequestInputParser, uriSequenceRoot);

        this._params = new SequestParamsV1();

        supportedEnzymes.put("[KR]|{P}", "trypsin\t\t\t\t1\tKR\t\tP");
        supportedEnzymes.put("[KR]|[X]", "stricttrypsin\t\t\t\t1\tKR\t\t-");
        supportedEnzymes.put("[R]|{P}", "argc\t\t\t\t1\tR\t\tP");
        supportedEnzymes.put("[X]|[D]", "aspn\t\t\t\t0\tD\t\t-");
        supportedEnzymes.put("[FMWY]|{P}", "chymotrypsin\t\t\t\t1\tFMWY\t\tP");
        supportedEnzymes.put("[R]|[X]", "clostripain\t\t\t\t1\tR\t\t-");
        supportedEnzymes.put("[M]|{P}", "cnbr\t\t\t\t1\tM\t\tP");
        supportedEnzymes.put("[AGILV]|{P}", "elastase\t\t\t\t1\tAGILV\t\tP");
        supportedEnzymes.put("[D]|{P}", "formicacid\t\t\t\t1\tD\t\tP");
        supportedEnzymes.put("[ED]|{P}", "gluc\t\t\t\t1\tED\t\tP");
        supportedEnzymes.put("[E]|{P}", "gluc_bicarb\t\t\t\t1\tE\t\tP");
        supportedEnzymes.put("[W]|[X]", "iodosobenzoate\t\t\t\t1\tW\t\t-");
        supportedEnzymes.put("[K]|{P}", "lysc\t\t\t\t1\tK\t\tP");
        supportedEnzymes.put("[K]|[X]", "lysc-p\t\t\t\t1\tK\t\t-");
        supportedEnzymes.put("[X]|[K]", "lysn\t\t\t\t0\tK\t\t-");
        supportedEnzymes.put("[X]|[KASR]", "lysn_promisc\t\t\t\t0\tK\t\t-");
        supportedEnzymes.put("[X]|[X]", "nonspecific\t\t\t\t0\t-\t\t-");
        supportedEnzymes.put("[FL]|[X]", "pepsina\t\t\t\t1\tFL\t\t-");
        supportedEnzymes.put("[P]|[X]", "protein_endopeptidase\t\t\t\t1\tP\t\t-");
        supportedEnzymes.put("[E]|[X]", "staph_protease\t\t\t\t1\tE\t\t-");
        supportedEnzymes.put("[KMR]|{P}", "trypsin/cnbr\t\t\t\t1\tKMR\t\tP");
        supportedEnzymes.put("[DEKR]|{P}", "trypsin_gluc\t\t\t\t1\tDEKR\t\tP");


    }

    public String initXmlValues()
    {
        StringBuilder parserError = new StringBuilder();
        parserError.append(initDatabases(false));
        parserError.append(initPeptideMassTolerance());
        parserError.append(initMassUnits());
        parserError.append(initIonScoring());
        parserError.append(initEnzymeInfo());
        parserError.append(initDynamicMods());
        parserError.append(initMassType());
        parserError.append(initStaticMods());
        parserError.append(initPassThroughs());

        return parserError.toString();
    }

    String initEnzymeInfo()
    {
        String parserError = "";
        String cleavageSites;
        String cleavageBlockers = "-";
        String offset;
        String enzyme = sequestInputParams.get("protein, cleavage site");
        if (enzyme == null) return parserError;
        if (enzyme.equals(""))
        {
            parserError = "protein, cleavage site did not contain a value.\n";
            return parserError;
        }
        enzyme = enzyme.trim();
        if (enzyme.startsWith("{"))
        {
            parserError = "protein, cleavage site does not support n-terminal blocking AAs(" + enzyme + ").\n";
            return parserError;
        }
        String supportedEnzyme = lookUpEnzyme(enzyme);
        if (supportedEnzyme != null)
        {
            if (supportedEnzyme.equals("No_Enzyme\t\t\t\t0\t-\t\t-"))
            {
                _params.getParam("enzyme_number").setValue("0");
                return parserError;
            }
            else
            {
                _params.getParam("enzyme_number").setValue("1");
                _params.getParam("enzyme1").setValue(supportedEnzyme);
                return parserError;
            }
        }

        if (enzyme.indexOf(',') != -1)
        {
            parserError = "protein, cleavage site contained more than one cleavage site(" + enzyme + ").\n";
            return parserError;
        }
        StringTokenizer sites = new StringTokenizer(enzyme, "|");
        if (sites.countTokens() != 2)
        {
            parserError = "protein, cleavage site contained invalid format(" + enzyme + ").\n";
            return parserError;
        }
        String cTermInfo = sites.nextToken().trim();
        String nTermInfo = sites.nextToken().trim();

        if (cTermInfo.startsWith("[") & cTermInfo.endsWith("]"))
        {
            cTermInfo = cTermInfo.substring(1, (cTermInfo.length() - 1)).trim();
            if (cTermInfo.equals("X"))
            {
                // [X]|[X]   is looked up in the hashtable above.
                if (nTermInfo.startsWith("[") & nTermInfo.endsWith("]"))
                {
                    nTermInfo = nTermInfo.substring(1, (nTermInfo.length() - 1)).trim();
                    if (isValidResidue(nTermInfo))
                    {
                        cleavageSites = nTermInfo;
                        offset = "0";
                    }
                    else
                    {
                        parserError = "protein, cleavage site contained invalid residue(" + nTermInfo + ").\n";
                        return parserError;
                    }

                }
                else
                {
                    parserError = "protein, cleavage site contained invalid format(" + enzyme + ").\n";
                    return parserError;
                }
            }
            else
            {
                if (isValidResidue(cTermInfo))
                {
                    cleavageSites = cTermInfo;
                    offset = "1";
                    if (nTermInfo.startsWith("{") & nTermInfo.endsWith("}"))
                    {
                        nTermInfo = nTermInfo.substring(1, (nTermInfo.length() - 1)).trim();
                        if (isValidResidue(nTermInfo))
                        {
                            cleavageBlockers = nTermInfo;
                        }
                        else
                        {
                            parserError = "protein, cleavage site contained invalid format(" + enzyme + ").\n";
                            return parserError;
                        }
                    }
                    else if (nTermInfo.equals("[X]"))
                    {
                        cleavageBlockers = "-";
                    }
                }
                else
                {
                    parserError = "protein, cleavage site contained invalid residue(" + cTermInfo + ").\n";
                    return parserError;
                }
            }

        }
        else
        {
            parserError = "protein, cleavage site contained invalid format(" + enzyme + ").\n";
            return parserError;
        }
        _params.getParam("enzyme1").setValue("Unknown(" + enzyme + ")\t\t\t\t" + offset + "\t" + cleavageSites + "\t\t" + cleavageBlockers);
        return parserError;
    }
}
