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
package org.labkey.test.ms2.params;

import org.labkey.test.pipeline.PipelineWebTestBase;

/**
 * TestsBaseline class
* <p/>
* Created: Aug 15, 2007
*
* @author bmaclean
*/
public class MS2TestsBaseline extends MS2TestsBase
{
    public MS2TestsBaseline(PipelineWebTestBase test)
    {
        super(test);
    }

    public void addTestsScoringOrganisms()
    {
        // Scoring tests
        _listParams.add(new MS2ScoringParams(_test, "yeast/Paulovich_101705_ltq", "xt_yeastp",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "yeast/Paulovich_101705_ltq", "xc_yeastp",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "yeast/Paulovich_101705_ltq", "xk_yeastp",
                new String[] {  },
                0.0, 0, 0.0, 0));

        _listParams.add(new MS2ScoringParams(_test, "yeast/comp12vs12standSCX", "xt_yeast",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "yeast/comp12vs12standSCX", "xc_yeast",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "yeast/comp12vs12standSCX", "xk_yeast",
                new String[] {  },
                0.0, 0, 0.0, 0));

        _listParams.add(new MS2ScoringParams(_test, "human/Hupo_PPP", "xt_hupo",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "human/Hupo_PPP", "xc_hupo",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "human/Hupo_PPP", "xk_hupo",
                new String[] {  },
                0.0, 0, 0.0, 0));
    }

    public void addTestsISBMix()
    {
        _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/FT", "xt_isbmix",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/FT", "xk_isbmix",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/LCQ", "xt_isbmix",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/LCQ", "xk_isbmix",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/LTQ", "xt_isbmix",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/LTQ", "xk_isbmix",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/QSTAR", "xt_isbmix",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/QSTAR", "xk_isbmix",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/QTOF", "xt_isbmix",
                new String[] {  },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/QTOF", "xk_isbmix",
                new String[] {  },
                0.0, 0, 0.0, 0));
    }

    public void addTestsScoringMix()
    {
        _listParams.add(new MS2ScoringParams(_test, "mix/Keller_omics", "xt_komics",
                new String[] { },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "mix/Keller_omics", "xc_komics",
                new String[] { },
                0.0, 0, 0.0, 0));
        _listParams.add(new MS2ScoringParams(_test, "mix/Keller_omics", "xk_komics",
                new String[] { },
                0.0, 0, 0.0, 0));
    }

    public void addTestsIPAS()
    {
/*        listParams.add(new MS2ScoringParams(test, "human/IPAS0080", "xt_ipas",
                new String[] { },
                0.0, 0, 0.0, 0));
        listParams.add(new MS2ScoringParams(test, "human/HFHs100IFN", "xc_hfh",
                new String[] { },
                0.0, 0, 0.0, 0));
        listParams.add(new MS2ScoringParams(test, "human/IPAS0080", "xk_ipas",
                new String[] { },
                0.0, 0, 0.0, 0));
        listParams.add(new MS2ScoringParams(test, "human/IPAS0080_raw", "xk_ipas",
                new String[] { },
                0.0, 0, 0.0, 0));
        listParams.add(new MS2ScoringParams(test, "human/IPAS0029", "xt_ipas",
                new String[] { },
                0.0, 0, 0.0, 0));
        listParams.add(new MS2ScoringParams(test, "human/IPAS0029", "xc_ipas",
                new String[] { },
                0.0, 0, 0.0, 0));
        listParams.add(new MS2ScoringParams(test, "human/IPAS0029", "xk_ipas",
                new String[] { },
                0.0, 0, 0.0, 0)); */
        _listParams.add(new MS2ScoringParams(_test, "human/IPAS0029_raw", "xk_ipas",
                new String[] { },
                0.0, 0, 0.0, 0));
    }

    public void addTestsQuant()
    {
        // Q3 quantitation
        MS2QuantParams qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_q3_75", "BSA_10_1");
        _listParams.add(qp);

        qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_q3_75", "BSA_5_1");
        _listParams.add(qp);

        qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_q3_75", "L_04_BSA_D0-D3_3-1");
        _listParams.add(qp);

        qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_q3_75", "L_04_BSA_D0-D3_1-1");
        _listParams.add(qp);

        qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_q3_75", "L_04_BSA_D0-D3_1-3");
        _listParams.add(qp);

        qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_q3_75", "L_04_BSA_D0-D3_1-5");
        _listParams.add(qp);

        qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_q3_75", "L_04_BSA_D0-D3_1-10");
        _listParams.add(qp);

        // Xpress quantitation
        qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_qc_5", "BSA_10_1");
        _listParams.add(qp);

        qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_qc_5", "BSA_5_1");
        _listParams.add(qp);

        qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_qc_5", "L_04_BSA_D0-D3_3-1");
        _listParams.add(qp);

        qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_qc_5", "L_04_BSA_D0-D3_1-1");
        _listParams.add(qp);

        qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_qc_5", "L_04_BSA_D0-D3_1-3");
        _listParams.add(qp);

        qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_qc_5", "L_04_BSA_D0-D3_1-5");
        _listParams.add(qp);

        qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_qc_5", "L_04_BSA_D0-D3_1-10");
        _listParams.add(qp);
        //*/
    }
}
