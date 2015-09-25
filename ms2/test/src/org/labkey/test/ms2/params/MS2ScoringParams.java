/*
 * Copyright (c) 2007-2013 LabKey Corporation
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
import org.labkey.test.SortDirection;
import org.labkey.test.pipeline.PipelineWebTestBase;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ProteinRegionTable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * MS2ScoringParams class
* <p/>
* Created: Aug 15, 2007
*
* @author bmaclean
*/
class MS2ScoringParams extends MS2TestParams
{
    private double maxFPPeptideProb;
    private int maxFPPeptideAbove;
    private double maxFPProteinProb;
    private int maxFPProteinAbove;
    private Set<String> positiveProteins;

    MS2ScoringParams(PipelineWebTestBase test, String dataPath,
                  String protocol,
                  String[] positiveProteins,
                  double maxFPPeptideProb,
                  int maxFPPeptideAbove,
                  double maxFPProteinProb,
                  int maxFPProteinAbove)
    {
        super(test, dataPath, protocol);

        if (positiveProteins != null)
        {
            this.positiveProteins = new HashSet<>();
            this.positiveProteins.addAll(Arrays.asList(positiveProteins));
        }
        this.maxFPPeptideProb = maxFPPeptideProb;
        this.maxFPPeptideAbove = maxFPPeptideAbove;
        this.maxFPProteinProb = maxFPProteinProb;
        this.maxFPProteinAbove = maxFPProteinAbove;
    }

    public void validate()
    {
        String link = getExperimentLinks()[0];
        _test.log("***** " + link + " *****");

        // Navigate to the peptides view by clicking the experiment name link.
        _test.clickAndWait(Locator.linkWithText(link));

        setGrouping("Peptides (Legacy)");
        _test.clearAllFilters("MS2Peptides", "Scan");

        int protsActual = 0;

        if (positiveProteins != null)
        {
            setGrouping("ProteinProphet (Legacy)");
            _test.clearAllFilters("ProteinGroupsWithQuantitation", "GroupNumber");

            int protsExpect = positiveProteins.size();
            HashSet<String> protSet = new HashSet<>();

            ProteinRegionTable tableProt = new ProteinRegionTable(0.995, _test);

            int colProtein = tableProt.getColumn("Protein");

            while (tableProt.nextPage())
            {
                int protsRows = tableProt.getProtCount();

                protsActual += protsRows;

                for (int i = 0; i < protsRows; i++)
                {
                    String prot = tableProt.getDataAsText(tableProt.getProtRow(i),
                            colProtein);
                    String[] protParts = prot.split("\n");
                    prot = protParts[0].trim();
                    protSet.add(prot);
                    validateTrue("Identified proteins contain unexpected result " + prot,
                            positiveProteins.contains(prot));
                }
            }

            for (String prot : positiveProteins)
            {
                validateTrue("Expected proteins contain missing result " + prot,
                        protSet.contains(prot));
            }

            validateTrue("Actual positive protein IDs " + protsActual + " does not equal expected " + protsExpect,
                    protsExpect == protsActual);

            setGrouping("Peptides (Legacy)");
        }

        DataRegionTable tablePep = new DataRegionTable("MS2Peptides", _test);

        _test.setFilter("MS2Peptides", "PeptideProphet", "Is Greater Than or Equal To", "0.9");
        _test.setFilter("MS2Peptides", "Protein", "Starts With", "rev_");
        _test.setSort("MS2Peptides", "PeptideProphet", SortDirection.DESC);

        double maxFound = Double.parseDouble(tablePep.getDataAsText(0, tablePep.getColumn("PepProphet")));

        validateTrue("Maximum PeptideProphet false-positive value " + maxFound + " does not match " + maxFPPeptideProb,
                Math.abs(maxFound - maxFPPeptideProb) < 0.0001);

        validateTrue("Total false-positive peptides above PeptideProphet probability 0.9 " + tablePep.getDataRowCount() +
                    " does not match " + maxFPPeptideAbove,
                tablePep.getDataRowCount() == maxFPPeptideAbove);

        setGrouping("ProteinProphet (Legacy)");
        _test.clearAllFilters("ProteinGroupsWithQuantitation", "GroupNumber");

        protsActual = 0;

        ProteinRegionTable tableProt = new ProteinRegionTable(0.5, _test);

        int colProb = tableProt.getColumn("Prob");

        while (tableProt.nextPage())
            protsActual += tableProt.getProtCount();

        _test.setSort("ProteinGroupsWithQuantitation", "GroupProbability", SortDirection.DESC);
        maxFound = Double.parseDouble(tableProt.getDataAsText(0, colProb));

        validateTrue("Maximum ProteinProphet false-positive value " + maxFound + " does not match " + maxFPProteinProb + ".",
                Math.abs(maxFound - maxFPProteinProb) < 0.0001);

        validateTrue("Total false-positive proteins above ProteinProphet probability 0.5 " + protsActual +
                    " does not match " + maxFPProteinAbove,
                protsActual == maxFPProteinAbove);
    }
}
