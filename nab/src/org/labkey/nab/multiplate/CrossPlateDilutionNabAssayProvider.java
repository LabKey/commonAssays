/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.nab.multiplate;

import org.labkey.api.util.PageFlowUtil;
import org.labkey.nab.NabDataHandler;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 2/24/13
 */
public class CrossPlateDilutionNabAssayProvider extends HighThroughputNabAssayProvider
{
    private static final String NAB_RUN_LSID_PREFIX = "HighThroughputNabAssayRun";
    private static final String NAB_ASSAY_PROTOCOL = "HighThroughputNabAssayProtocol";

    public CrossPlateDilutionNabAssayProvider()
    {
        super(NAB_ASSAY_PROTOCOL, NAB_RUN_LSID_PREFIX, CrossPlateDilutionNabDataHandler.NAB_HIGH_THROUGHPUT_DATA_TYPE);
    }

    @Override
    public String getName()
    {
        return "TZM-bl Neutralization (NAb), High-throughput (Cross Plate Dilution)";
    }

    @Override
    public String getResourceName()
    {
        return "HighThroughputNAb";
    }

    public String getDescription()
    {
        return "Imports a specially formatted CSV or XLS file that contains data from multiple plates.  The high-throughput NAb " +
                "assay differs from the standard NAb assay in that dilutions are assumed to occur across plates, rather than " +
                "within a single plate.  Both NAb assay types measure neutralization in TZM-bl cells as a function of a " +
                "reduction in Tat-induced luciferase (Luc) reporter gene expression after a single round of infection. Montefiori, D.C. 2004" +
                PageFlowUtil.helpPopup("NAb", "<a href=\"http://www.ncbi.nlm.nih.gov/pubmed/18432938\">" +
                        "Evaluating neutralizing antibodies against HIV, SIV and SHIV in luciferase " +
                        "reporter gene assays</a>.  Current Protocols in Immunology, (Coligan, J.E., " +
                        "A.M. Kruisbeek, D.H. Margulies, E.M. Shevach, W. Strober, and R. Coico, eds.), John Wiley & Sons, 12.11.1-12.11.15.", true);
    }

    public NabDataHandler getDataHandler()
    {
        return new CrossPlateDilutionNabDataHandler();
    }
}
