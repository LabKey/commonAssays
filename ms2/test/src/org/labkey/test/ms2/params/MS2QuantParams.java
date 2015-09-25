/*
 * Copyright (c) 2007-2015 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.pipeline.PipelineWebTestBase;
import org.labkey.test.util.ProteinRegionTable;

import java.util.HashMap;
import java.util.Map;

/**
 * MS2QuantParams class
* <p/>
* Created: Aug 15, 2007
*
* @author bmaclean
*/
class MS2QuantParams extends MS2TestParams
{
    protected Map<String, MS2QuantRatio> ratios = new HashMap<>();

    public MS2QuantParams(PipelineWebTestBase test, String dataPath, String protocol)
    {
        this(test, dataPath, protocol, null);
    }

    public MS2QuantParams(PipelineWebTestBase test, String dataPath, String protocol, String sampleName)
    {
        super(test, dataPath, protocol, sampleName);
    }

    public void addRatio(String protName, MS2QuantRatio ratio)
    {
        ratios.put(protName, ratio);
    }
    
    public void addAllRatios(MS2QuantParams params)
    {
        ratios.putAll(params.ratios);
    }

    public void validate()
    {
        String link = getExperimentLinks()[0];
        _test.log("***** " + link + " *****");

        // Navigate to the peptides view by clicking the experiment name link.
        _test.clickAndWait(Locator.linkWithText(link));

        setGrouping("Peptides (Legacy)");
        _test.clearAllFilters("MS2Peptides", "Scan");

        setGrouping("ProteinProphet (Legacy)");
        _test.clearAllFilters("ProteinGroupsWithQuantitation", "GroupNumber");

        _test.log("Pick protein columns");
        _test.clickButton("Pick Protein Columns");
        _test.setFormElement(Locator.name("columns"), "GroupNumber, GroupProbability, Protein, RatioMean, RatioStandardDev, RatioNumberPeptides, AACoverage, BestName, BestGeneName, Description");
        _test.clickButton("Pick Columns");
        ProteinRegionTable tableProt = new ProteinRegionTable(0.75, _test);
        tableProt.setFilter("RatioNumberPeptides", "Is Greater Than", "1");

        int colRatio = tableProt.getColumn("Ratio Peps");
        int colProtein = tableProt.getColumn("Protein");
        int colMean = tableProt.getColumn("L2H Mean");
        int colSD = tableProt.getColumn("L2H StdDev");

        int protsActual = 0;

        while (tableProt.nextPage())
        {
            int protRows = tableProt.getProtCount(); // Account for peptide table rows
            protsActual += protRows;

            //log("QuantParams qp = new QuantParams(\"" + dataPath + "\", \"" + sampleName + "\", \"" + protocol +"\");");
            for (int i = 0; i < protRows; i++)
            {
                int row = tableProt.getProtRow(i);
                String peps = tableProt.getDataAsText(row, colRatio);

                // Check all the proteins with more than 1 peptide contributing to the _ratio.
                if (peps == null || peps.length() == 0 || Integer.valueOf(peps) < 2)
                    continue;

                String prot = tableProt.getDataAsText(row, colProtein);
                String[] protParts = prot.split("\n");
                prot = protParts[0].trim();
                double ratio = Double.parseDouble(tableProt.getDataAsText(row, colMean));
                double stddev = Double.parseDouble(tableProt.getDataAsText(row, colSD));

                MS2QuantRatio qr = ratios.get(prot);
                if (qr == null)
                {
                    //validateTrue("Unexpected ratio for " + prot + " (" + ratio + ", " + stddev + ")",
                    //        false);
                    validateTrue("qp.addRatio(\"" + prot + "\", new MS2QuantRatio(" + ratio + ", " + stddev + "));",
                            false);
                }
                else
                {
                    validateTrue("Protein ratio for " + prot + " (" + ratio + ", " + stddev + ") " +
                                " does not match expected (" + qr.getRatio() + ", " + qr.getStddev() + ")",
                            qr.isMatch(ratio, stddev));
                }
            }
        }

        validateTrue("Protein ratios " + protsActual + " do not match expected " + ratios.size(),
                protsActual == ratios.size());
    }
}
