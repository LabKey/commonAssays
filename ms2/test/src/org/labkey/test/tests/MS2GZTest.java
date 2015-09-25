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

package org.labkey.test.tests;

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

/** exercises the gzip handling */
@Category({MS2.class, DailyA.class})
public class MS2GZTest extends AbstractMS2ImportTest
{
    @Override
    @LogMethod
    protected void setupMS2()
    {
        super.setupMS2();

        importMS2Run("DRT3", 2);
    }

    @Override
    @LogMethod
    protected void verifyMS2()
    {
        DataRegionTable searchRunsTable = new DataRegionTable("MS2SearchRuns", this);
        log("Test Protein Prophet Compare");
        searchRunsTable.checkAllOnPage();
        _ext4Helper.clickExt4MenuButton(true, Locator.lkButton("Compare"), false, "ProteinProphet (Legacy)");
        selectOptionByText(Locator.name("viewParams"), LEGACY_PROTEIN_PROPHET_VIEW_NAME);
        clickButton("Compare");
        assertTextPresent("(GroupProbability > 0.7)", "GroupNumber", "0.78");
        assertTextNotPresent("gi|30089158|emb|CAD89505.1|");
        setSort("MS2Compare", "Protein", SortDirection.ASC);
        assertTextBefore("gi|13442951|dbj|BAB39767.1|", "gi|13470573|ref|NP_102142.1|");
        setSort("MS2Compare", "Run0GroupProbability", SortDirection.DESC);
        if (!isTextBefore("gi|13470573|ref|NP_102142.1|", "gi|13442951|dbj|BAB39767.1|"))
            setSort("MS2Compare", "Run0GroupProbability", SortDirection.ASC);
        assertTextBefore("gi|13470573|ref|NP_102142.1|", "gi|13442951|dbj|BAB39767.1|");

        log("Test adding columns");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));

        searchRunsTable.checkAllOnPage();
        _ext4Helper.clickExt4MenuButton(true, Locator.lkButton("Compare"), false, "ProteinProphet (Legacy)");
        checkCheckbox(Locator.checkboxByName("light2HeavyRatioMean"));
        uncheckCheckbox(Locator.checkboxByName("groupProbability"));
        clickButton("Compare");
        assertTextPresent("ratiomean");
        assertTextNotPresent("GroupProbability");

        log("Test Compare Search Engine Proteins");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));

        searchRunsTable.checkAllOnPage();
        _ext4Helper.clickExt4MenuButton(true, Locator.lkButton("Compare"), false, "Search Engine Protein");
        selectOptionByText(Locator.name("viewParams"), LEGACY_PROTEIN_VIEW_NAME);
        checkCheckbox(Locator.checkboxByName("total"));
        clickButton("Compare");
        assertTextPresent(
                "(SequenceMass > 20000)",
                "(DeltaMass > 0.0)",
                "Total",
                "gi|33241155|ref|NP_876097.1|",
                "Pattern");
        assertTextNotPresent("gi|32307556|ribosomal_protein", "gi|136348|TRPF_YEAST_N-(5'-ph");
        setSort("MS2Compare", "Protein", SortDirection.ASC);
        assertTextBefore("gi|11499506|ref|NP_070747.1|", "gi|13507919|");

        log("Test Compare Peptides (Legacy)");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));

        searchRunsTable.checkAllOnPage();
        _ext4Helper.clickExt4MenuButton(true, Locator.lkButton("Compare"), false, "Peptide (Legacy)");
        selectOptionByText(Locator.name("viewParams"), LEGACY_PROTEIN_VIEW_NAME);
        clickButton("Compare");
        assertTextPresent("(DeltaMass > 0.0)", "K.VYLADPVVFTVKHIK.Q", "Pattern");
        assertTextNotPresent("R.TIDPVIAR.K", "K.KLYNEELK.A", "K.EIRQRQGDDLDGLSFAELR.G");
        setSort("MS2Compare", "Peptide", SortDirection.DESC);
        if (!isTextBefore("-.MELFSNELLYK.T", "K.VYLADPVVFTVKHIK.Q"))
            setSort("MS2Compare", "Peptide", SortDirection.ASC);
        assertTextBefore("-.MELFSNELLYK.T", "K.VYLADPVVFTVKHIK.Q");

    }
}