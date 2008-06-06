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
import java.util.Set;

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

    public String getSupportedEnzyme(String enzyme) throws SequestParamsException
    {
        Set<String> enzymeSigs = supportedEnzymes.keySet();
        enzyme = removeWhiteSpace(enzyme);
        enzyme = combineEnzymes(enzyme.split(","));
        for(String supportedEnzyme:enzymeSigs)
        {
            if(sameEnzyme(enzyme,supportedEnzyme))
            {
                if (supportedEnzymes.get(supportedEnzyme).equals("nonspecific\t\t\t\t0\t-\t\t-"))
                {
                    _params.getParam("enzyme_number").setValue("0");
                    return "nonspecific";
                }
                else
                {
                    _params.getParam("enzyme_number").setValue("1");
                    String paramsString = supportedEnzymes.get(supportedEnzyme);
                    _params.getParam("enzyme1").setValue(paramsString);
                    StringTokenizer st = new StringTokenizer(paramsString);
                    return st.nextToken();
                }
            }
        }
        return "";
    }
}
