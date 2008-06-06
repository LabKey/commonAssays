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
 * Time: 4:33:29 PM
 */
public class SequestParamsV2Builder extends SequestParamsBuilder
{


    public SequestParamsV2Builder(Map<String, String> sequestInputParser, URI uriSequenceRoot)
    {
        super(sequestInputParser, uriSequenceRoot);

        this._params = new SequestParamsV2();

        supportedEnzymes.put("[KR]|{P}", "trypsin 1 1 KR P");
        supportedEnzymes.put("[KR]|[X]", "stricttrypsin 1 1 KR -");
        supportedEnzymes.put("[R]|{P}", "argc 1 1 R P");
        supportedEnzymes.put("[X]|[D]", "aspn 1 0 D -");
        supportedEnzymes.put("[FMWY]|{P}", "chymotrypsin 1 1 FMWY P");
        supportedEnzymes.put("[R]|[X]", "clostripain 1 1 R -");
        supportedEnzymes.put("[M]|{P}", "cnbr 1 1 M P");
        supportedEnzymes.put("[AGILV]|{P}", "elastase 1 1 AGILV P");
        supportedEnzymes.put("[D]|{P}", "formicacid 1 1 D P");
        supportedEnzymes.put("[K]|{P}", "trypsin_k 1 1 K P");
        supportedEnzymes.put("[ED]|{P}", "gluc 1 1 ED P");
        supportedEnzymes.put("[E]|{P}", "gluc_bicarb 1 1 E P");
        supportedEnzymes.put("[W]|[X]", "iodosobenzoate 1 1 W -");
        supportedEnzymes.put("[K]|[X]", "lysc 1 1 K P");
        supportedEnzymes.put("[K]|[X]", "lysc-p 1 1 K -");
        supportedEnzymes.put("[X]|[K]", "lysn 1 0 K -");
        supportedEnzymes.put("[X]|[KASR]", "lysn_promisc 1 0 KASR -");
        supportedEnzymes.put("[X]|[X]", "nonspecific 0 0 - -");
        supportedEnzymes.put("[FL]|[X]", "pepsina 1 1 FL -");
        supportedEnzymes.put("[P]|[X]", "protein_endopeptidase 1 1 P -");
        supportedEnzymes.put("[E]|[X]", "staph_protease 1 1 E -");
        supportedEnzymes.put("[KMR]|{P}", "trypsin/cnbr 1 1 KMR P");
        supportedEnzymes.put("[DEKR]|{P}", "trypsin_gluc 1 1 DEKR P");

    }

    public String initXmlValues()
    {
        StringBuilder parserError = new StringBuilder();
        parserError.append(initDatabases(true));
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

    protected String getSupportedEnzyme(String enzyme) throws SequestParamsException
    {
        Set<String> enzymeSigs = supportedEnzymes.keySet();
        enzyme = removeWhiteSpace(enzyme);
        enzyme = combineEnzymes(enzyme.split(","));
        for(String supportedEnzyme:enzymeSigs)
        {
            if(sameEnzyme(enzyme,supportedEnzyme))
            {
                String paramsString = supportedEnzymes.get(supportedEnzyme);
                _params.getParam("enzyme_info").setValue(paramsString);
                StringTokenizer st = new StringTokenizer(paramsString);
                return st.nextToken();
            }
        }
        return "";
    }
}
