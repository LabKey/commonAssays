/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

package org.labkey.test.tests.ms2;

import org.junit.experimental.categories.Category;
import org.labkey.api.data.ColumnHeaderType;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.TextSearcher;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.Locator.NBSP;

@Category({MS2.class, DailyA.class})
public class MS2Test extends AbstractMS2ImportTest
{
    protected static final String RUN_GROUP1_NAME1 = "Test Run Group 1";
    //Issue #16260, "Exception when including run group with tricky characters in name," has been updated
    protected static final String RUN_GROUP1_NAME2 = "Test Run Group 1 New Name" + TRICKY_CHARACTERS;
    protected static final String RUN_GROUP1_CONTACT = "Test Contact";
    protected static final String RUN_GROUP1_DESCRIPTION = "This is a description";
    protected static final String RUN_GROUP1_HYPOTHESIS = "I think this is happening";
    protected static final String RUN_GROUP1_COMMENTS = "Here are comments.";
    protected static final String RUN_GROUP2_NAME = "Test Run Group 2";
    protected static final String RUN_GROUP3_NAME = "Test Run Group 3";
    protected static final String PROTOCOL_NAME = "Protocol Rollup 1";
    protected static final String PROTOCOL_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<bioml>\n" +
                    "    <note label=\"pipeline, database\" type=\"input\">Bovine_mini1.fasta</note>\n" +
                    "    <note label=\"protein, cleavage site\" type=\"input\">[KR]|{P}</note>\n" +
                    "</bioml>";

    @Override
    @LogMethod
    protected void setupMS2()
    {
        cleanPipeline();
        super.setupMS2();
        importMS2Run("DRT2", 2);
    }

    @Override
    @LogMethod
    protected void verifyMS2()
    {
        verifyFirstRun();

        validateSecondRun();

        validateRunGroups();

        queryValidationTest();
        pepXMLtest();
    }

    private void verifyFirstRun()
    {
        log("Verify run view.");
        navigateToFolder(FOLDER_NAME);
        clickAndWait(Locator.linkContainingText("DRT1"));

        // Make sure we're not using a custom default view for the current user
        selectOptionByText(Locator.name("viewParams"), "<Standard View>");
        clickButton("Go");

        assertTextPresent(
                SEARCH_NAME3,
                "databases",
                MASS_SPEC,
                ENZYME);
        assertElementPresent(Locator.linkWithText(PEPTIDE1));

        log("Test Navigation Bar for Run");
        log("Test Show Modifications");
        click(Locator.linkWithText("Show Modifications"));
        // Wait for tooltip to show up
        waitForText(2000, "Variable");
        assertTextPresent(
                "E^",
                "Q^");

        log("Test Show Peptide Prophet Details");
        pushLocation();
        beginAt(getLinkHref("Show Peptide Prophet Details", "MS2", "/" + getProjectName() + "/" + FOLDER_NAME));
        assertTextPresent(
                "Minimum probability",
                "Error rate",
                "Sensitivity");
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Charge 1+ Distribution"));
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Sensitivity Plot"));
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Charge 1+ Cumulative Distribution"));
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Charge 3+ Distribution"));
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Charge 3+ Cumulative Observed vs. Model"));
        popLocation();

        log("Test Show Protein Prophet Details");
        pushLocation();
        beginAt(getLinkHref("Show Protein Prophet Details", "MS2", "/" + getProjectName() + "/" + FOLDER_NAME));
        assertTextPresent(
                "Minimum probability",
                "Error rate",
                "Sensitivity");
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Sensitivity Plot"));
        popLocation();

        // Make sure we're not using a custom default view for the current user
        selectOptionByText(Locator.name("viewParams"), "<Standard View>");
        clickButton("Go");
        selectOptionByText(Locator.name("grouping"), "Peptides (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));

        log("Test export selected");
        DataRegionTable peptidesTable = new DataRegionTable(REGION_NAME_PEPTIDES, this);
        pushLocation();
        peptidesTable.checkCheckbox(0);
        File peptides = doAndWaitForDownload(() -> peptidesTable.clickHeaderMenu("Export Selected", false, "TSV"));

        TextSearcher txtSearcher = new TextSearcher(peptides);
        assertTextPresent(txtSearcher, "K.LLASMLAK.A");
        assertTextNotPresent(txtSearcher, "R.Q^YALHVDGVGTK.A");
        assertTextPresent(txtSearcher, "\n", 2);
        popLocation();
        pushLocation();
        peptidesTable.checkAllOnPage();
        File allPeptides = doAndWaitForDownload(() -> peptidesTable.clickHeaderMenu("Export Selected", false, "AMT"));
        TextSearcher allPeptideSearcher = new TextSearcher(allPeptides);
        assertTextPresent(allPeptideSearcher, "\n", "60");
        assertTextPresent(allPeptideSearcher,
                "Run",
                "CalcMHPlus",
                "RetTime",
                "Fraction",
                "PepProphet",
                "Peptide",
                "1373.4690",
                "846.5120",
                "K.LLASMLAK.A",
                "K.EEEESDEDMGFG.-");
        popLocation();

        log("Test sort");
        pushLocation();
        peptidesTable.setSort("Hyper", SortDirection.DESC);
        assertTextPresent("Hyper DESC");
        assertTextBefore("R.Q^YALHVDGVGTK.A", "K.LLASMLAK.A");
        assertTextBefore("14.9", "13.0");
        peptidesTable.setSort("Charge", SortDirection.ASC);
        assertTextPresent("Charge ASC, Hyper DESC");
        assertTextBefore("K.KLHQK.L", "R.GGNEESTK.T");
        assertTextBefore("1272.5700", "1425.6860");
        peptidesTable.setSort("Charge", SortDirection.DESC);
        assertTextPresent("Charge DESC, Hyper DESC");
        peptidesTable.setSort("Scan", SortDirection.ASC);
        assertTextPresent("Scan ASC, Charge DESC, Hyper DESC");
        assertTextBefore("R.Q^YALHVDGVGTK.A", "K.LLASMLAK.A");
        assertTextBefore("R.SLADVARR.R", "-.MELFSNELLYK.T");

        log("Test export");
        pushLocation();
        File allPepTSV = doAndWaitForDownload(() -> peptidesTable.clickHeaderMenu("Export All", false, "TSV"));
        TextSearcher allPeptsvSearcher = new TextSearcher(allPepTSV);
        assertTextPresent(allPeptsvSearcher,
                "Scan",
                "IonPercent",
                "Protein",
                "gi|4689022|ribosomal_protein_",
                "1373.4690");
        assertTextPresentInThisOrder(allPeptsvSearcher, "R.Q^YALHVDGVGTK.A", "K.LLASMLAK.A");
        assertTextPresentInThisOrder(allPeptsvSearcher, "R.SLADVARR.R", "-.MELFSNELLYK.T");
        assertTextPresent(allPeptsvSearcher, "\n", 58);
        popLocation();

        File allAMT = doAndWaitForDownload(() -> peptidesTable.clickHeaderMenu("Export All", false, "AMT"));
        TextSearcher allAmtSrch = new TextSearcher(allAMT);
        assertTextPresentInThisOrder(allAmtSrch, "R.Q^YALHVDGVGTK.A", "K.LLASMLAK.A");
        assertTextPresent(allAmtSrch, "Run", "Peptide");
        assertTextPresent(allAmtSrch, "\n", 60); // todo: verify 60 rows
        popLocation();

        log("Test Scan, Z, Hyper, Next, B, Y, and Expect filters");
        pushLocation();
        selectOptionByText(Locator.name("viewParams"), LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME);
        clickButton("Go");
        assertTextPresent("-.MELFSNELLYK.T",
                "R.EADKVLVQMPSGK.Q");
        assertTextNotPresent("K.FANIGDVIVASVK.Q",
                "K.TESGYGSESSLR.R");

        log("Test filter was remembered");
        assertTextPresent("Scan DESC");

        log("Continue with filters");
        peptidesTable.setFilter("Charge", "Equals", "2");
        assertTextNotPresent("R.APPSTQESESPR.Q");
        assertTextPresent("R.TIDPVIAR.K");
        peptidesTable.setFilter("Hyper", "Is Greater Than or Equal To", "14.6");
        assertTextNotPresent("K.RLLRSMVK.F");
        assertTextPresent("R.AEIDYANK.T");
        peptidesTable.setFilter("Next", "Does Not Equal", "9.5");
        assertTextNotPresent("R.AEIDYANK.T");
        peptidesTable.setFilter("B", "Is Less Than", "11.6");
        assertTextNotPresent("R.TIDPVIAR.K");
        peptidesTable.setFilter("Y", "Is Less Than", "11.3");
        assertTextNotPresent("R.QPNSGPYKK.Q");
        peptidesTable.setFilter("Expect", "Is Greater Than", "1.2");
        assertTextNotPresent("K.FVKKSNDVR.L");
        Set<String> expectedFilters = new HashSet<>(Arrays.asList("(Scan > 6)", "(Scan <= 100)", "(Charge = 2)", "(Hyper >= 14.6)", "(Next <> 9.5)", "(B < 11.6)", "(Y < 11.3)", "(Expect > 1.2)"));
        assertEquals("Wrong peptide filters", expectedFilters, getFilters(ViewType.peptide));

        log("Test spectrum page");
        assertElementPresent(Locator.linkWithText("R.LSSMRDSR.S"));
        String address = getAttribute(Locator.linkWithText("R.LSSMRDSR.S"), "href");
        beginAt(address);

        log("Verify spectrum page.");
        assertTextPresent("R.LSSMRDSR.S",
                "gi|29650192|ribosomal_protein",
                "56",
                "0.000");
        clickAndWait(Locator.linkWithText("Next"));
        assertTextPresent("R.GGNEESTK.T", "gi|442754|A_Chain_A,_Superoxi");

        log("Return to run.");
        goBack();
        goBack();

        log("Verify still filtered.");
        assertEquals("Wrong peptide filters", expectedFilters, getFilters(ViewType.peptide));

        log("Test pick peptide columns");
        clickButton("Pick Peptide Columns");
        clickButton("Pick", 0);
        clickButton("Pick Columns");
        assertTextPresent("RetTime");

        log("Test export");
        File peptideFilteredRows = doAndWaitForDownload(() -> peptidesTable.clickHeaderMenu("Export All", false, "TSV"));
        TextSearcher pepFilteredRowsSearcher = new TextSearcher(peptideFilteredRows);
        assertTextPresent(pepFilteredRowsSearcher,
                "Scan",
                "Run Description",
                "Fraction Name",
                "dMassPPM",
                "PPErrorRate",
                "SeqId",
                "56",
                "gi|442754|A_Chain_A,_Superoxi");
        assertTextPresent(pepFilteredRowsSearcher, "\n", 3);
        assertTextPresentInThisOrder(pepFilteredRowsSearcher, "R.LSSMRDSR.S", "R.GGNEESTK.T");
        assertTextNotPresent(pepFilteredRowsSearcher, "K.FVKKSNDVR.L");

        File allAmt = doAndWaitForDownload(() -> peptidesTable.clickHeaderMenu("Export All", false, "AMT"));
        TextSearcher allAmtSearcher = new TextSearcher(allAmt);
        assertTextPresent(allAmtSearcher, "Run", "Peptide");
        assertTextPresentInThisOrder(allAmtSearcher, "R.LSSMRDSR.S", "R.GGNEESTK.T");
        assertTextNotPresent(allAmtSearcher, "K.FVKKSNDVR.L");
        assertTextPresent(allAmtSearcher, "\n", 5);
        popLocation();

        log("Test using saved view");
        pushLocation();
        selectOptionByText(Locator.name("viewParams"), LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME);
        clickButton("Go");

        log("Test hyper charge filters too");
        setFormElement(Locator.id("Charge1"), "11");
        setFormElement(Locator.id("Charge2"), "13");
        setFormElement(Locator.id("Charge3"), "14");
        clickAndWait(Locator.id("AddChargeScoreFilterButton"));
        assertTextPresent("R.KVTTGR.A");
        assertTextNotPresent("K.KLHQK.L",
                "K.MEVDQLK.K",
                "-.MELFSNELLYK.T");

        log("Test Protein View and if viewParams hold");
        selectOptionByText(Locator.name("grouping"), "Protein (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        assertTextPresent("Description",
                "Coverage",
                "Best Gene Name",
                "Scan DESC");
        assertTextNotPresent("K.LLASMLAK.A");
        assertEquals("Wrong protein filters",
                new HashSet<>(Arrays.asList("(Scan > 6)", "(Scan <= 100)", "(+1:Hyper >= 11.0, +2:Hyper >= 13.0, +3:Hyper >= 14.0)")),
                getFilters(ViewType.peptide));

        log("Test filters in Protein View");
        DataRegionTable proteinsTable = new DataRegionTable(REGION_NAME_PROTEINS, this);
        proteinsTable.setFilter("SequenceMass", "Is Greater Than", "17000", "Is Less Than", "50000");
        assertTextNotPresent("gi|15925226|30S_ribosomal_pro",
                "gi|19703691|Nicotinate_phosph");
        proteinsTable.setFilter("Description", "Does Not Contain", "Uncharacterized conserved protein");
        assertTextNotPresent("Uncharacterized conserved protein [Thermoplasma acidophilum]");
        assertEquals("Wrong protein filters",
                new HashSet<>(Arrays.asList("(SequenceMass > 17000)", "(SequenceMass < 50000)", "(Description DOES NOT CONTAIN Uncharacterized conserved protein)")),
                getFilters(ViewType.protein));

        log("Test Single Protein View");
        String href = getAttribute(Locator.linkContainingText("gi|13541159|30S_ribosomal_pro"), "href");
        pushLocation();
        beginAt(href);

        log("Verify peptides.");
        assertTextPresent("gi|13541159|ref|NP_110847.1|");
        final HashSet<String> missingFilters = new HashSet<>(Arrays.asList("(Scan > 6)", "(Scan <= 100)", "(+1:Hyper >= 11.0, +2:Hyper >= 13.0, +3:Hyper >= 14.0)"));
        missingFilters.removeAll(getFilters(ViewType.peptide));
        assertTrue("Missing protein filters: " + missingFilters, missingFilters.isEmpty());

        log("Return to run.");
        popLocation();

        log("Test sorting in Protein View");
        proteinsTable.setSort("SequenceMass", SortDirection.ASC);
        assertTextPresent("SequenceMass ASC");
        assertTextBefore("gi|15668549|LSU_ribosomal_pro", "gi|14318169|AF379640_1_riboso");

        log("Test export Protein View");
        File proteinViewFile = doAndWaitForDownload(() -> proteinsTable.clickHeaderMenu("Export All", false, "TSV"));
        TextSearcher proteinViewFileSearcher = new TextSearcher(proteinViewFile);
        assertTextPresent(proteinViewFileSearcher, "Protein",
                "Description",
                "gi|13541159|30S_ribosomal_pro",
                "ribosomal protein S19 [Thermoplasma volcanium]",
                "gi|29650192|ribosomal_protein",
                "ribosomal protein S6 [Anopheles stephensi]");
        assertTextPresent(proteinViewFileSearcher, "\n", 18);
        assertTextPresentInThisOrder(proteinViewFileSearcher, "gi|15668549|LSU_ribosomal_pro", "gi|14318169|AF379640_1_riboso");
        goBack();

        log("Test export expanded view");
        selectOptionByText(Locator.name("grouping"), "Protein (Legacy)");
        checkCheckbox(Locator.checkboxByName("expanded"));
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        File proteinsFile = doAndWaitForDownload(() -> proteinsTable.clickHeaderMenu("Export All", false, "TSV"));
        TextSearcher protFileSearch = new TextSearcher(proteinsFile);
        assertTextPresent(protFileSearch,
                "Protein",
                "IonPercent",
                "Protein",
                "gi|13541159|30S_ribosomal_pro",
                "R.KVTTGR.A",
                "gi|29650192|ribosomal_protein",
                "R.E^PVSPWGTPAKGYR.T");
        assertTextPresent(protFileSearch, "\n", 18);
        // TODO: Verify/fix these values: order changed in r43461
        assertTextPresentInThisOrder(protFileSearch, "gi|14318169|AF379640_1_riboso", "gi|15668549|LSU_ribosomal_pro");
        goBack();
        File allProtsFile = doAndWaitForDownload(() -> proteinsTable.clickHeaderMenu("Export All", false, "AMT"));
        TextSearcher allProtsSearcher = new TextSearcher(allProtsFile);
        assertTextPresent(allProtsSearcher, "Run", "Peptide");
        assertTextPresent(allProtsSearcher, "\n", 20);
        // TODO: Verify/fix these values: order changed in r43461
        assertTextPresentInThisOrder(allProtsSearcher, "R.RDYLHYLPKYNR.F", "K.TKDYEGMQVPVK.V");
        assertTextNotPresent(allProtsSearcher, "K.LLASMLAK.A", "R.KKVAIVPEPLR.K");

        log("Test Protein Prophet");
        selectOptionByText(Locator.name("grouping"), "ProteinProphet (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        assertTextPresent("Group",
                "Prob",
                "Spectrum Ids",
                // TODO: Verify/fix these values: changed in r43461
                "gi|16078254|similar_to_riboso",    // gi|4689022|ribosomal_protein_
                "12.71%",                           // 14.06%
                "gi|27684893|similar_to_60S_RI");   // gi|4883902|APETALA3_homolog_R

        log("Test Protein Prophet with filters");
        selectOptionByText(Locator.name("viewParams"), LEGACY_PROTEIN_PROPHET_VIEW_NAME);
        clickButton("Go");
        assertEquals("Wrong peptide filters", new HashSet<>(Arrays.asList("(Scan > 6)", "(Scan <= 100)")), getFilters(ViewType.peptide));
        assertEquals("Wrong peptide sort", Arrays.asList("Scan DESC"), getSorts(ViewType.peptide));
        assertTextNotPresent("gi|30089158|low_density_lipop");

        DataRegionTable quantitationTable = new DataRegionTable(REGION_NAME_QUANTITATION, this);
        quantitationTable.setFilter("PercentCoverage", "Is Not Blank", null);
        assertTextNotPresent("gi|13442951|MAIL");
        assertEquals("Wrong protein group filters",
                new HashSet<>(Arrays.asList("(GroupProbability > 0.7)", "(PercentCoverage is not blank)")),
                getFilters(ViewType.proteinGroup));

        validateLegacySingleRunExport();

        log("Create saved view to test query groupings");
        selectOptionByText(Locator.name("grouping"), "Peptides (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        selectOptionByText(Locator.id("views"), LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME);
        clickButton("Go");

        log("Test Query - Peptides Grouping");
        selectOptionByText(Locator.name("grouping"), "Standard");
        checkCheckbox(Locator.checkboxByName("expanded"));
        clickAndWait(Locator.id("viewTypeSubmitButton"));

        log("Check that saved view is working");
        assertTextNotPresent("K.KTEENYTLVFIVDVK.A");
        assertTextBefore("R.EADKVLVQMPSGK.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");

        log("Test adding a sort and a filter");
        peptidesTable.setFilter("Hyper", "Is Greater Than", "10.6");
        assertTextNotPresent("K.RFSGTVKLK.Y");
        peptidesTable.setSort("Next", SortDirection.ASC);
        assertTextBefore("K.ERQPPPR.L", "K.KLHQK.L");
        // Explicitly clear out the sorts, since we want to be just dealing with the ones set in Customize View
        peptidesTable.clearSort("Next");
        peptidesTable.clearSort("Scan");

        log("Test customize view");
        peptidesTable.clearAllFilters();
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addSort("Charge", "Z", SortDirection.DESC);
        _customizeViewsHelper.addSort("Mass", "CalcMH+", SortDirection.DESC);
        _customizeViewsHelper.addFilter("DeltaMass", "dMass", "Is Less Than", "0");
        _customizeViewsHelper.addFilter("RowId", "Row Id", "Is Greater Than", "3");
        _customizeViewsHelper.addColumn("NextAA", "Next AA");
        _customizeViewsHelper.removeColumn("Expect");
        _customizeViewsHelper.removeColumn("ProteinHits");
        _customizeViewsHelper.saveCustomView(VIEW4);

        log("Test that the sorting and filtering worked and that the columns were changed");
        assertTextPresent("Next AA",
                "K.TESGYGSESSLR.R",
                "Protein",
                "gi|27805893|guanine_nucleotid");
        assertTextBefore(PEPTIDE1, PEPTIDE2);
        assertTextBefore(PEPTIDE3, PEPTIDE4);
        assertTextNotPresent("K.LLASMLAK.A",
                "R.GGNEESTK.T",
                "Orig Score",
                "Expect",
                "SeqHits");

        log("Test Ignore View Filter");
        peptidesTable.clickApplyGridFilter();
        assertTextPresent("K.LLASMLAK.A",
                "R.GGNEESTK.T",
                "Next AA");
        assertTextBefore(PEPTIDE2, PEPTIDE1);
        assertTextBefore(PEPTIDE4, PEPTIDE3);

        log("Test Apply View Filter");
        peptidesTable.clickApplyGridFilter();
        assertTextPresent("Next AA");
        assertTextBefore(PEPTIDE1, PEPTIDE2);
        assertTextBefore(PEPTIDE3, PEPTIDE4);
        assertTextNotPresent("K.LLASMLAK.A",
                "R.GGNEESTK.T",
                "Expect",
                "SeqHits");

        log("Test exporting Query - Peptides grouping");
        log("Test exporting in TSV");
        File allPeptidesFile = doAndWaitForDownload(() -> peptidesTable.clickHeaderMenu("Export All", false, "TSV"));
        TextSearcher allPeptidesSearch = new TextSearcher(allPeptidesFile);
        assertTextPresent(allPeptidesSearch, "Scan",
                "dMass",
                "Next AA",
                "Protein",
                "gi|27805893|guanine_nucleotid");
        assertTextPresent(allPeptidesSearch, "\n", 24);
        assertTextPresentInThisOrder(allPeptidesSearch, PEPTIDE1, PEPTIDE2);
        assertTextPresentInThisOrder(allPeptidesSearch, PEPTIDE3, PEPTIDE4);
        assertTextNotPresent(allPeptidesSearch, "K.LLASMLAK.A",
                "R.GGNEESTK.T",
                "Expect",
                "SeqHits");

        log("Test exporting in AMT");
        File allAMTFile = doAndWaitForDownload(() -> peptidesTable.clickHeaderMenu("Export All", false, "AMT"));
        TextSearcher allAmtFileSrch = new TextSearcher(allAMTFile);
        assertTextPresent(allAmtFileSrch, "Run",
                "Peptide",
                "RetTime");
        assertTextPresent(allAmtFileSrch, "\n", 26);
        assertTextPresentInThisOrder(allAmtFileSrch, PEPTIDE1, PEPTIDE2);
        assertTextPresentInThisOrder(allAmtFileSrch, PEPTIDE3, PEPTIDE4);
        assertTextNotPresent(allAmtFileSrch, "K.LLASMLAK.A",
                "R.GGNEESTK.T",
                "Protein");

        log("Test exporting selected in TSV");
        peptidesTable.uncheckAll();
        peptidesTable.checkCheckbox(0);
        peptidesTable.checkCheckbox(1);
        File selectedStuffFile = doAndWaitForDownload(() -> peptidesTable.clickHeaderMenu("Export Selected", false, "TSV"));
        TextSearcher selectedStuffSrch = new TextSearcher(selectedStuffFile);
        assertTextPresent(selectedStuffSrch, "Next AA",
                "gi|25027045|putative_50S_ribo");
        assertTextPresent(selectedStuffSrch, "\n", 3);
        assertTextPresentInThisOrder(selectedStuffSrch, "K.ISNFIANNDCRYYIDAEHQKIISDEINR.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");
        assertTextNotPresent(selectedStuffSrch, "Expect", "SeqHits");

        log("Test exporting selected in AMT");
        peptidesTable.uncheckAll();
        peptidesTable.checkCheckbox(0);
        peptidesTable.checkCheckbox(1);
        File selectedAmtFile = doAndWaitForDownload(() ->  peptidesTable.clickHeaderMenu("Export Selected", false, "AMT"));
        TextSearcher selectedAmtSrch = new TextSearcher(selectedAmtFile);
        assertTextPresent(selectedAmtSrch, "Peptide");
        assertTextPresent(selectedAmtSrch, "\n", 5);
        assertTextPresentInThisOrder(selectedAmtSrch, "K.ISNFIANNDCRYYIDAEHQKIISDEINR.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");
        assertTextNotPresent(selectedAmtSrch, "Next AA");

        log("Test default view");
        peptidesTable.goToView("default");
        assertTextPresent("K.LLASMLAK.A",
                "R.GGNEESTK.T",
                "Expect",
                "SeqHits");
        assertTextBefore(PEPTIDE2, PEPTIDE1);
        assertTextBefore(PEPTIDE4, PEPTIDE3);
        assertTextNotPresent("Next AA");

        log("Test load saved view");
        peptidesTable.goToView(VIEW4);
        assertTextBefore(PEPTIDE1, PEPTIDE2);
        assertTextBefore(PEPTIDE3, PEPTIDE4);
        assertTextNotPresent("R.GGNEESTK.T",
                "Expect",
                "SeqHits");

        log("Test changing default view");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.clearFilters();
        _customizeViewsHelper.clearSorts();
        _customizeViewsHelper.addSort("DeltaMass", "dMass", SortDirection.ASC);
        _customizeViewsHelper.addFilter("Mass", "CalcMH+", "Is Greater Than", "1000");
        _customizeViewsHelper.addColumn("Fraction");
        _customizeViewsHelper.removeColumn("IonPercent");
        _customizeViewsHelper.saveDefaultView();
        peptidesTable.goToView("default");
        assertTextPresent("Fraction");
        assertTextBefore("K.TKDYEGMQVPVK.V", "R.LGARRVSPVR.A");
        assertTextNotPresent("K.LLASMLAK.A",
                "Ion%");

        log("Test restoring default view");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.revertUnsavedView();
        assertTextPresent("K.LLASMLAK.A",
                "Ion%");
        assertTextBefore("R.LGARRVSPVR.A", "K.TKDYEGMQVPVK.V");
        assertTextNotPresent("Fraction");

        log("Test delete view");
        peptidesTable.goToView(VIEW4);
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.deleteView();
        assertTextPresent("K.LLASMLAK.A",
                "R.GGNEESTK.T",
                "Expect",
                "SeqHits");
        assertTextBefore(PEPTIDE2, PEPTIDE1);
        assertTextBefore(PEPTIDE4, PEPTIDE3);
        assertTextNotPresent("Next AA");

        log("Test Protein Prophet view in Query - Peptides grouping");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("ProteinProphetData/ProteinGroupId/Group", "Group");
        _customizeViewsHelper.addColumn("ProteinProphetData/ProteinGroupId/TotalNumberPeptides", "Peptides");
        _customizeViewsHelper.addColumn("ProteinProphetData/ProteinGroupId/GroupProbability", "Prob");
        _customizeViewsHelper.addColumn("ProteinProphetData/ProteinGroupId/BestName", "Best Name");
        _customizeViewsHelper.removeColumn("Mass");
        _customizeViewsHelper.addFilter("DeltaMass", "dMass", "Is Greater Than", "0");
        _customizeViewsHelper.addFilter("ProteinProphetData/ProteinGroupId/GroupProbability", "Prob", "Is Greater Than", "0.7");
        _customizeViewsHelper.addSort("ProteinProphetData/ProteinGroupId/GroupProbability", "Prob", SortDirection.ASC);
        _customizeViewsHelper.saveCustomView(VIEW4);

        log("Test that Protein Prophet view is displayed and that it sorts and filters correctly");
        assertTextPresent("Group",
                "Peptides",
                "Prob",
                "gi|4689022|",
                "gi|23619029|ref|NP_704991.1|",
                "PepProphet",
                "Scan",
                "K.MLNMAKSKMHK.M");
        assertTextBefore("gi|16078254|ref|NP_389071.1|", "gi|18311790|ref|NP_558457.1|");
        assertTextNotPresent("CalcMH+",
                "K.EIRQRQGDDLDGLSFAELR.G",
                "K.GSDSLSDGPACKR.S");

        log("Test exporting from Protein Prophet view");
        log("Test exporting in TSV");
        File allTSVFile = doAndWaitForDownload(() -> peptidesTable.clickHeaderMenu("Export All", false, "TSV"));
        TextSearcher allTsvSrch = new TextSearcher(allTSVFile);
        assertTextPresent(allTsvSrch, "Group",
                "Peptides",
                "Prob",
                "gi|4689022|",
                "gi|23619029|ref|NP_704991.1|",
                "PepProphet",
                "Scan",
                "K.MLNMAKSKMHK.M");
        assertTextPresent(allTsvSrch, "\n", 6);
        assertTextPresentInThisOrder(allTsvSrch, "gi|16078254|ref|NP_389071.1|", "gi|18311790|ref|NP_558457.1|");
        assertTextNotPresent(allTsvSrch, "CalcMH+",
                "K.EIRQRQGDDLDGLSFAELR.G",
                "K.GSDSLSDGPACKR.S");

        log("Test exporting in AMT");
        File allAMTPepFile = doAndWaitForDownload(() -> peptidesTable.clickHeaderMenu("Export All", false, "AMT"));
        TextSearcher allAMTPepSrch = new TextSearcher(allAMTPepFile);
        assertTextPresent(allAMTPepSrch, "Run",
                "Peptide",
                "RetTime");
        assertTextPresent(allAMTPepSrch, "\n", 8);
        assertTextPresentInThisOrder(allAMTPepSrch, "R.KKVAIVPEPLR.K", "R.Q^YALHVDGVGTK.A");
        assertTextNotPresent(allAMTPepSrch, "Best Name",
                "K.EIRQRQGDDLDGLSFAELR.G",
                "K.GSDSLSDGPACKR.S");

        log("Test Query - Proteins Grouping");
        selectOptionByText(Locator.name("grouping"), "Protein Groups");
        checkCheckbox(Locator.checkboxByName("expanded"));
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        assertTextPresent("Protein",
                "Description",
                "Group",
                "APETALA3 homolog RbAP3-2 [Ranunculus bulbosus]",
                "gi|4883902|APETALA3_homolog_R");

        log("Test customize view");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeColumn("UniquePeptidesCount");
        _customizeViewsHelper.addColumn("Proteins/Protein/ProtSequence", "Protein Sequence");
        _customizeViewsHelper.addFilter("GroupProbability", "Prob", "Is Greater Than", "0.7");
        _customizeViewsHelper.addSort("ErrorRate", "Error", SortDirection.DESC);
        _customizeViewsHelper.saveCustomView(VIEW4);

        log("Test that sorting, filtering, and columns are correct");
        assertTextPresent("Sequence",
                "MSASELATSYSALILADEGIEIKSDKLLSLTKAANVDVEPIWATIFAKALEGKDLKELLLNIGSGAGAAPVAGGAGAPAAADGERPAEEKEEAKEEEESDEDMGFG");
        assertTextBefore("gi|16078254|similar_to_riboso", "gi|18311790|phosphoribosylfor");
        assertTextNotPresent("Unique",
                "gi|30089158|low_density_lipop");

        log("Test exporting in Query - Protein View");
        DataRegionTable proteinGroupsTable = new DataRegionTable(REGION_NAME_PROTEINGROUPS, this);

        log("Test exporting in TSV");
        File proteinGroupFile = doAndWaitForDownload(() -> proteinGroupsTable.clickHeaderMenu("Export All", false, "TSV"));
        TextSearcher proteinGroupSrch = new TextSearcher(proteinGroupFile);
        assertTextPresent(proteinGroupSrch, "Sequence",
                "MSASELATSYSALILADEGIEIKSDKLLSLTKAANVDVEPIWATIFAKALEGKDLKELLLNIGSGAGAAPVAGGAGAPAAADGERPAEEKEEAKEEEESDEDMGFG");
        assertTextPresent(proteinGroupSrch, "\n", 8);
        assertTextPresentInThisOrder(proteinGroupSrch, "gi|16078254|similar_to_riboso", "gi|18311790|phosphoribosylfor");
        assertTextNotPresent(proteinGroupSrch, "Unique",
                "gi|30089158|low_density_lipop");

        log("Test exporting selected and non-expanded view");
        uncheckCheckbox(Locator.checkboxByName("expanded"));
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        proteinGroupsTable.uncheckAll();
        proteinGroupsTable.checkCheckbox(0);
        proteinGroupsTable.checkCheckbox(1);
        File selectedProtGroupFile = doAndWaitForDownload(() -> proteinGroupsTable.clickHeaderMenu("Export Selected", false, "TSV"));
        TextSearcher selectedProtGroupSrch = new TextSearcher(selectedProtGroupFile);
        assertTextPresentInThisOrder(selectedProtGroupSrch, "0.74", "0.78");
        assertTextPresent(selectedProtGroupSrch, "\n", 3);
    }

    private void validateLegacySingleRunExport()
    {
        log("Test export");
        DataRegionTable quantitationTable = new DataRegionTable(REGION_NAME_QUANTITATION, this);
        File qTableFile = doAndWaitForDownload(() -> quantitationTable.clickHeaderMenu("Export All", false, "AMT"));
        TextSearcher qTableSrch = new TextSearcher(qTableFile);
        assertTextPresent(qTableSrch, "Run",
                "Peptide",
                "1318.6790",
                "1435.6810");
        assertTextPresent(qTableSrch, "\n", 5);
        assertTextPresentInThisOrder(qTableSrch, "K.MLNMAKSKMHK.M", "R.E^VNAEDLAPGEPGR.L");
        assertTextNotPresent(qTableSrch, "gi|27684893|similar_to_60S_RI");

        log("Test export selected in expanded view with different protein and peptide columns and sorting");
        log("Test sorting in Protein Prophet");
        quantitationTable.setSort("GroupProbability", SortDirection.ASC);
        assertTextPresent("GroupProbability ASC");
        assertTextBefore("gi|548772|RL4_HALHA_50S_RIBOS", "gi|23619029|60S_ribosomal_pro");
        clickButton("Pick Peptide Columns");
        clickButton("Pick", 0);
        clickButton("Pick Columns");
        clickButton("Pick Peptide Columns");
        clickButton("Pick", 0);
        clickButton("Pick Columns");
        selectOptionByText(Locator.name("grouping"), "ProteinProphet (Legacy)");
        checkCheckbox(Locator.checkboxByName("expanded"));
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        quantitationTable.checkCheckbox(0);
        File qSelected = doAndWaitForDownload(() -> quantitationTable.clickHeaderMenu("Export Selected", false, "TSV"));
        TextSearcher qSelectedSearch = new TextSearcher(qSelected);
        assertTextPresent(qSelectedSearch, "Group",
                "PP Unique",
                "Run Description",
                "IonPercent",
                "ObsMHPlus",
                "Peptide",
                "SeqId",
                "gi|548772|RL4_HALHA_50S_RIBOS",
                "EVNAEDLAPGEPGR");
        assertTextNotPresent(qSelectedSearch, "gi|23619029|60S_ribosomal_pro");
        assertTextPresent(qSelectedSearch, "\n", 2);

        log("Make sure sort is exported correctly too");
        File quantFile = doAndWaitForDownload(() -> quantitationTable.clickHeaderMenu("Export All", false, "TSV"), 1)[0];
        TextSearcher quantSrch = new TextSearcher(quantFile);
        assertTextPresent(quantSrch, "MLNMAKSKMHK");
        assertTextPresent(quantSrch, "\n", 3);
        assertTextPresentInThisOrder(quantSrch, "gi|548772|RL4_HALHA_50S_RIBOS", "gi|23619029|60S_ribosomal_pro");
    }

    protected void validateSecondRun()
    {
        navigateToFolder(FOLDER_NAME);
        clickAndWait(Locator.linkWithText("drt/CAexample_mini (DRT2)"));

        selectOptionByText(Locator.name("viewParams"), "<Standard View>");
        clickButton("Go");

        log("Test peptide filtering on protein page");
        assertElementPresent(Locator.linkWithText("gi|15645924|ribosomal_protein"));
        String address = getAttribute(Locator.linkWithText("gi|15645924|ribosomal_protein"), "href");
        pushLocation();
        beginAt(address);

        log("Verify protein page.");
        assertTextPresent("gi|15645924|ribosomal_protein",
                "7,683");
        String selectedValue = getSelectedOptionValue(Locator.name("allPeps"));
        boolean userPref = selectedValue == null || "".equals(selectedValue) || "false".equals(selectedValue);
        if (!userPref)
        {
            // User last viewed all peptides, regardless of search engine assignment, so flip to the other option
            // before checking that the values match our expectations
            doAndWaitForPageToLoad(() -> selectOptionByValue(Locator.name("allPeps"), "false"));
        }
        assertTextPresent("27% (18 / 66)",
                "27% (2,050 / 7,683)",
                "1 total, 1 distinct",
                "R.VKLKAMQLSNPNEIKKAR.N");
        assertTextNotPresent("K.YTELK.D");

        doAndWaitForPageToLoad(() -> selectOptionByValue(Locator.name("allPeps"), "true"));

        assertTextPresent("35% (23 / 66)",
                "35% (2,685 / 7,683)",
                "Matches sequence of",
                "2 total, 2 distinct",
                "R.VKLKAMQLSNPNEIKKAR.N",
                "K.YTELK.D");

        log("Return to run and set a filter");
        popLocation();
        DataRegionTable peptidesTable = new DataRegionTable(REGION_NAME_PEPTIDES, this);
        peptidesTable.setFilter("Scan", "Is Less Than", "25");
        address = getAttribute(Locator.linkWithText("gi|15645924|ribosomal_protein"), "href");
        pushLocation();
        beginAt(address);

        // Be sure that our selection is sticky
        assertTextPresent("Matches sequence of",
                // Be sure that our scan filter was propagated to the protein page
                "1 total, 1 distinct",
                "27% (18 / 66)",
                "27% (2,050 / 7,683)",
                "R.VKLKAMQLSNPNEIKKAR.N");
        assertTextNotPresent("K.YTELK.D");

        if (userPref)
        {
            // User last only peptides assigned by the search engine, so flip back to restore their preference
            doAndWaitForPageToLoad(() -> selectOptionByValue(Locator.name("allPeps"), "false"));
        }

        popLocation();
        navigateToFolder(FOLDER_NAME);

        validateCompare();
        navigateToFolder(FOLDER_NAME);

        String retTimeColumn = "RetentionTime";
        String retMinsColumn = "RetentionTimeMinutes";

        clickAndWait(Locator.linkWithText("drt/CAexample_mini (DRT2)"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn(retTimeColumn);
        _customizeViewsHelper.addColumn(retMinsColumn);
        _customizeViewsHelper.saveCustomView(VIEW6);

        DataRegionTable drt = new DataRegionTable(REGION_NAME_PEPTIDES, this);
        int row = drt.getRowIndex("Peptide","K.KLYNEELK.A");
        Double retTime = Double.parseDouble( drt.getDataAsText(row, retTimeColumn));
        Double retTimeMinutes = Double.parseDouble(drt.getDataAsText(row, retMinsColumn));

        Double eRetTime = 16.95;
        Double eRetTimeMinutes = 0.28;
        List<Double> eTimes = Arrays.asList(eRetTime, eRetTimeMinutes);
        assertEquals("Wrong retention times", eTimes, Arrays.asList(retTime, retTimeMinutes));
    }

    private void validateRunGroups()
    {
        log("Test creating run groups");
        navigateToFolder(FOLDER_NAME);
        clickAndWait(Locator.linkWithImage(WebTestHelper.getContextPath() + "/Experiment/images/graphIcon.gif"));
        clickAndWait(Locator.id("expandCollapse-experimentRunGroup"), 0);
        clickButton("Create new group");
        setFormElement(Locator.name("name"), RUN_GROUP1_NAME1);
        setFormElement(Locator.name("contactId"), RUN_GROUP1_CONTACT);
        setFormElement(Locator.name("experimentDescriptionURL"), RUN_GROUP1_DESCRIPTION);
        setFormElement(Locator.name("hypothesis"), RUN_GROUP1_HYPOTHESIS);
        setFormElement(Locator.name("comments"), RUN_GROUP1_COMMENTS);
        clickButton("Submit");
        clickAndWait(Locator.id("expandCollapse-experimentRunGroup"), 0);
        assertTextPresent(RUN_GROUP1_NAME1,
                RUN_GROUP1_HYPOTHESIS,
                RUN_GROUP1_COMMENTS);
        navigateToFolder(FOLDER_NAME);
        assertTextPresent(RUN_GROUP1_NAME1);

        clickAndWait(Locator.linkWithText("Run Groups"));
        clickButton("Create Run Group");
        clickButton("Submit");
        setFormElement(Locator.name("name"), RUN_GROUP3_NAME);
        clickButton("Submit");

        clickButton("Create Run Group");
        setFormElement(Locator.name("name"), RUN_GROUP2_NAME);
        clickButton("Submit");

        log("Test editing run group info");
        clickAndWait(Locator.linkWithText(RUN_GROUP1_NAME1));
        assertTextPresent(
                RUN_GROUP1_NAME1,
                RUN_GROUP1_CONTACT,
                RUN_GROUP1_DESCRIPTION,
                RUN_GROUP1_HYPOTHESIS,
                RUN_GROUP1_COMMENTS);
        clickButton("Edit");
        setFormElement(Locator.name("name"), RUN_GROUP1_NAME2);
        clickButton("Submit");

        log("Test customizing view to include the run groups");
        navigateToFolder(FOLDER_NAME);
        clickAndWait(Locator.linkWithText("MS2 Runs"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn(new String[] { "RunGroupToggle", EscapeUtil.fieldKeyEncodePart(RUN_GROUP1_NAME2) }, RUN_GROUP1_NAME2);
        _customizeViewsHelper.addColumn(new String[]{"RunGroupToggle", RUN_GROUP2_NAME}, "Run Groups " + RUN_GROUP2_NAME);
        _customizeViewsHelper.addColumn(new String[]{"RunGroupToggle", "Default Experiment"}, "Run Groups Default Experiment");
        _customizeViewsHelper.applyCustomView();

        assertTextPresent(new TextSearcher(this).setSearchTransformer(TextSearcher.TextTransformers.FIELD_LABEL),
                RUN_GROUP1_NAME2,
                RUN_GROUP2_NAME,
                DEFAULT_EXPERIMENT);

        for (int i = 0; i <= 5; i++)
        {
            checkCheckbox(Locator.checkboxByName("experimentMembership").index(i));
        }

        log("Test editing a run group's runs");
        navigateToFolder(FOLDER_NAME);
        clickAndWait(Locator.linkWithText("Run Groups"));
        clickAndWait(Locator.linkWithText(RUN_GROUP2_NAME));
        assertTextPresent(RUN_GROUP1_NAME2,
                RUN_GROUP2_NAME,
                DEFAULT_EXPERIMENT);
        new DataRegionTable("XTandemSearchRuns", getDriver()).checkCheckbox(1);
        clickButton("Remove");

        assertTextPresent("DRT2");
        assertTextNotPresent("DRT1");

        verifyRunGroupMapQuery();
        navigateToFolder(FOLDER_NAME);

        log("Test that the compare run groups works");
        DataRegionTable searchRunsTable = new DataRegionTable(REGION_NAME_SEARCH_RUNS, getDriver());
        searchRunsTable.checkAllOnPage();
        searchRunsTable.clickHeaderMenu("Compare", "ProteinProphet");
        clickButton("Compare");

        click(Locator.linkWithText("Comparison Overview"));
        waitForText(1000, RUN_GROUP1_NAME2);
        assertTextPresent(RUN_GROUP1_NAME2,
                RUN_GROUP2_NAME,
                DEFAULT_EXPERIMENT);
        selectOptionByValue(Locator.xpath("//div[text() = 'A']/../../../td/select"), "group1");

        log("Test Customize View");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("SeqId/Mass", "Protein Mass");
        _customizeViewsHelper.addFilter("SeqId/Mass", "Protein Mass", "Is Less Than", "30000");
        _customizeViewsHelper.saveCustomView(VIEW5);

        DataRegionTable peptidesTable = new DataRegionTable("query", this);
        Locator seqIdMassHeader = DataRegionTable.Locators.columnHeader("query", "SeqId/Mass");
        log("Make sure the filtering and new columns worked");
        assertElementPresent(seqIdMassHeader);
        assertTextNotPresent("gi|34849400|gb|AAP58899.1|");

        log("Check default view works");
        peptidesTable.goToView("default");
        assertElementNotPresent(seqIdMassHeader);
        assertTextPresent("gi|34849400|");

        log("Check sorting");
        peptidesTable.goToView(VIEW5);
        peptidesTable.setSort("SeqId", SortDirection.ASC);
        assertTextBefore("gi|13470573|ref|NP_102142.1|", "gi|15828808|ref|NP_326168.1|");

        log("Test exporting Compare Runs in Query");
        File compareRunExportFile = new DataRegionExportHelper(new DataRegionTable("query", getDriver())).exportText();
        TextSearcher compareRunExportSearcher = new TextSearcher(compareRunExportFile);
        assertTextPresent(compareRunExportSearcher, "Mass",
                "0.89");
        assertTextPresentInThisOrder(compareRunExportSearcher, "gi|13470573|ref|NP_102142.1|", "gi|15828808|ref|NP_326168.1|");
        assertTextNotPresent(compareRunExportSearcher, "gi|34849400|");

        log("Test delete run groups");
        navigateToFolder(FOLDER_NAME);
        clickAndWait(Locator.linkWithText("Run Groups"));
        DataRegionTable runGroupTable = new DataRegionTable("RunGroupWide", getDriver());
        runGroupTable.checkAll();
        runGroupTable.clickHeaderButton("Delete");
        clickButton("Confirm Delete");
        assertTextNotPresent(RUN_GROUP1_NAME2,
                RUN_GROUP2_NAME,
                DEFAULT_EXPERIMENT);
        navigateToFolder(FOLDER_NAME);
        assertTextNotPresent(RUN_GROUP1_NAME2);

        verifyGroupAudit();
    }

        //verify audit trail registers runs added to or removed from groups.
    private void verifyGroupAudit()
    {
        List<Map<String, Object>> rows = executeSelectRowCommand("auditLog", "ExperimentAuditEvent").getRows();
        assertEquals("Unexpected number of audit rows", 9, rows.size());
        int addedCount = 0;
        int removedCount = 0;
        for (Map row : rows)
        {
            if (((String)row.get("Comment")).contains("was added to the run group"))
                addedCount++;
            else if (((String)row.get("Comment")).contains("was removed from the run group"))
                removedCount++;
        }

        assertEquals(8, addedCount);
        assertEquals(1, removedCount);

        navigateToFolder(FOLDER_NAME);
    }

    private void verifyRunGroupMapQuery()
    {
        goToSchemaBrowser();
        viewQueryData("exp", "RunGroupMap");

        List<Map<String, Object>> rows = executeSelectRowCommand("exp", "RunGroupMap").getRows();
        assertEquals("Unexpected number of rows in RunGroupMap", 5, rows.size());

        Set<String> keys = rows.get(0).keySet();
        for (String header : new String[] {"RunGroup", "Created", "CreatedBy", "Run"})
        {
            assertTrue("Run Group Map missing column: " + header, keys.contains(header));
        }
        Map<String, Integer> textAndCount = new HashMap<>();
        textAndCount.put(DEFAULT_EXPERIMENT, 2);
        textAndCount.put("Test Run Group 1 New Name", 2); // Intentionally don't include the special characters because it's hard to match up the HTML encoding exactly
        textAndCount.put(RUN_GROUP2_NAME, 1);
        textAndCount.put("DRT2", 3);
        textAndCount.put("DRT1", 2);

        for (String key : textAndCount.keySet())
        {
            assertTextPresent(key, textAndCount.get(key).intValue());
        }
    }


    private void validateCompare()
    {
        log("Test Compare MS2 Runs");

        log("Test Compare Peptides using Query");
        DataRegionTable searchRunsTable = new DataRegionTable(REGION_NAME_SEARCH_RUNS, this);
        searchRunsTable.checkAllOnPage();
        searchRunsTable.clickHeaderMenu("Compare", "Peptide");
        click(Locator.radioButtonByNameAndValue("peptideFilterType", "none"));
        setFormElement(Locator.input("targetProtein"), "");
        clickButton("Compare");
        assertTextPresent(
                "K.EEEESDEDMGFG.-",
                "R.Q^YALHVDGVGTK.A",
                "K.GSDSLSDGPACKR.S",
                "K.EYYLLHKPPKTISSTK.D");

        // verify the bulk protein coverage map export
        File proteinCoverageFile = doAndWaitForDownload(() -> new DataRegionTable("query", this).clickHeaderButton("Export Protein Coverage"));
        TextSearcher coverageSearcher = new TextSearcher(proteinCoverageFile);
        assertTextPresent(coverageSearcher,
                "22001886|sp|Q963B6",
                "29827410|ref|NP_822044.1",
                "17508693|ref|NP_492384.1",
                "27716987|ref|XP_233992.1",
                "(search engine matches)");
        assertTextNotPresent(coverageSearcher, "(all matching peptides)");
        assertTextPresent(coverageSearcher, "57 Total qualifying peptides in run", 56); // two peptides have the same search engine protein
        assertTextPresent(coverageSearcher, "57 Distinct qualifying peptides in run", 56); // two peptides have the same search engine protein
        assertTextPresent(coverageSearcher, "59 Total qualifying peptides in run", 59);
        assertTextPresent(coverageSearcher, "59 Distinct qualifying peptides in run", 59);
        assertTextPresent(coverageSearcher, "peptide-marker", 117);


        clickAndWait(Locator.linkWithText("Setup Compare Peptides"));
        click(Locator.radioButtonByNameAndValue("peptideFilterType", "probability"));
        setFormElement(Locator.input("peptideProphetProbability"), "0.9");
        clickButton("Compare");
        assertTextPresent(
                "K.EEEESDEDMGFG.-",
                "R.Q^YALHVDGVGTK.A",
                "K.EYYLLHKPPKTISSTK.D");
        assertTextNotPresent("K.GSDSLSDGPACKR.S");

        // verify the bulk protein coverage map export for the peptideProphet probability filter
        File protCoverageFile = doAndWaitForDownload(() -> new DataRegionTable("query", this).clickHeaderButton("Export Protein Coverage"));
        TextSearcher protCoverageFileSearcher = new TextSearcher(protCoverageFile);
        assertTextPresent(protCoverageFileSearcher,
                "4689022|emb|CAA80880.2",
                "18311790|ref|NP_558457.1",
                "15828808|ref|NP_326168.1",
                "34849400|gb|AAP58899.1",
                "(search engine matches)");
        assertTextNotPresent(protCoverageFileSearcher,
                "BAB39767.1", // for peptide K.GSDSLSDGPACKR.S
                "(all matching peptides)");
        assertTextPresent(protCoverageFileSearcher, "2 Total qualifying peptides in run", 4);
        assertTextPresent(protCoverageFileSearcher, "2 Distinct qualifying peptides in run", 4);
        assertTextPresent(protCoverageFileSearcher, "peptide-marker", 4);
        assertTextPresent(protCoverageFileSearcher, " 1  / 1(Q^) ", 1); // TODO: how do we verify the location of the match in the coverage map table?

        clickAndWait(Locator.linkWithText("Setup Compare Peptides"));
        setFormElement(Locator.input("targetProtein"), "gi|18311790|phosphoribosylfor");
        clickButton("Compare");
        assertTextPresent("R.Q^YALHVDGVGTK.A");
        assertTextNotPresent(
                "K.EEEESDEDMGFG.-",
                "K.GSDSLSDGPACKR.S",
                "K.EYYLLHKPPKTISSTK.D");

        // verify the bulk protein coverage map export for peptideProphet filter with target protein
        File peptideProphetFile = doAndWaitForDownload(() -> new DataRegionTable("query", this).clickHeaderButton("Export Protein Coverage"));
        TextSearcher peptideProphetSearcher = new TextSearcher(peptideProphetFile);
        assertTextPresent(peptideProphetSearcher,
                "18311790|ref|NP_558457.1",
                "(all matching peptides)");
        assertTextNotPresent(peptideProphetSearcher,
                "CAA80880.2", // for peptide K.EEEESDEDMGFG.-
                "(search engine matches)");
        assertTextPresent(peptideProphetSearcher, "(PeptideProphet >= 0.9) AND (Matches sequence of ", 2);
        assertTextPresent(peptideProphetSearcher, "Peptide Counts:", 2);
        assertTextPresent(peptideProphetSearcher, "1 Total peptide matching sequence", 1);
        assertTextPresent(peptideProphetSearcher, "1 Distinct peptide matching sequence", 1);
        assertTextPresent(peptideProphetSearcher, "0 Total peptides matching sequence", 1);
        assertTextPresent(peptideProphetSearcher, "0 Distinct peptides matching sequence", 1);
        assertTextPresent(peptideProphetSearcher, "2 Total qualifying peptides in run", 2);
        assertTextPresent(peptideProphetSearcher, "2 Distinct qualifying peptides in run", 2);
        assertTextPresent(peptideProphetSearcher, "peptide-marker", 1);
        assertTextPresent(peptideProphetSearcher, " 1  / 1(Q^) ", 1); // TODO: how do we verify the location of the match in the coverage map table?

        clickAndWait(Locator.linkWithText("Setup Compare Peptides"));
        setFormElement(Locator.input("targetProtein"), "gi|15645924|ribosomal_protein");
        click(Locator.radioButtonByNameAndValue("peptideFilterType", "none"));
        clickButton("Compare");
        assertTextPresent(
                "K.YTELK.D",
                "R.VKLKAMQLSNPNEIKKAR.N");
        assertTextNotPresent(
                "R.Q^YALHVDGVGTK.A",
                "K.EEEESDEDMGFG.-",
                "K.GSDSLSDGPACKR.S",
                "K.EYYLLHKPPKTISSTK.D");

        // verify the bulk protein coverage map export for target protein
        File bulkProteinCoverageMap = doAndWaitForDownload(() -> new DataRegionTable("query", this).clickHeaderButton("Export Protein Coverage"));
        TextSearcher bulkProteinCoverageSearcher = new TextSearcher(bulkProteinCoverageMap);
        assertTextPresent(bulkProteinCoverageSearcher,
                "15645924|ref|NP_208103.1",
                "(all matching peptides)");
        assertTextNotPresent(bulkProteinCoverageSearcher,
                "15612296",
                "NP_223949.1",
                "(search engine matches)");
        assertTextPresent(bulkProteinCoverageSearcher, "NP_208103.1", 4);
        assertTextPresent(bulkProteinCoverageSearcher, "Peptide Counts:", 2);
        assertTextPresent(bulkProteinCoverageSearcher, "0 Total peptides matching sequence", 1);
        assertTextPresent(bulkProteinCoverageSearcher, "0 Distinct peptides matching sequence", 1);
        assertTextPresent(bulkProteinCoverageSearcher, "57 Total qualifying peptides in run", 1);
        assertTextPresent(bulkProteinCoverageSearcher, "57 Distinct qualifying peptides in run", 1);
        assertTextPresent(bulkProteinCoverageSearcher, "2 Total peptides matching sequence", 1);
        assertTextPresent(bulkProteinCoverageSearcher, "2 Distinct peptides matching sequence", 1);
        assertTextPresent(bulkProteinCoverageSearcher, "59 Total qualifying peptides in run", 1);
        assertTextPresent(bulkProteinCoverageSearcher, "59 Distinct qualifying peptides in run", 1);
        assertTextPresent(bulkProteinCoverageSearcher, "peptide-marker", 2);

        log("Test Compare Runs using Query Peptides");
        navigateToFolder(FOLDER_NAME);
        DataRegionTable ms2Runs = new DataRegionTable(REGION_NAME_SEARCH_RUNS, getDriver());
        ms2Runs.checkAll();
        ms2Runs.clickHeaderMenu("Compare", "Peptide");
        checkRadioButton(Locator.radioButtonByNameAndValue("peptideFilterType", "none"));
        setFormElement(Locator.name("targetProtein"), "");
        clickButton("Compare");
        assertTextPresent(
                "K.EIRQRQGDDLDGLSFAELR.G",
                "R.TQMPAASICVNYK.G",
                "Avg PepProphet");

        log("Test Customize View in Query Peptides");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("CTAGG_AVG_XCorr");
        _customizeViewsHelper.removeColumn("InstanceCount");
        _customizeViewsHelper.addFilter("CTAGG_AVG_XCorr", "Avg XCorr", "Is Greater Than", "10");
        _customizeViewsHelper.saveCustomView();

        log("Check filtering and columns were added correctly");
        assertTextPresent(
                "Avg XCorr",
                "K.EIRQRQGDDLDGLSFAELR.G",
                "11.200",
                "13.800");
        assertTextNotPresent(
                "R.TQMPAASICVNYK.G");

        log("Check Ignore/Apply View Filter");
        DataRegionTable table = new DataRegionTable("query", this);
        table.clickApplyGridFilter();
        assertTextPresent(
                "K.EIRQRQGDDLDGLSFAELR.G",
                "R.TQMPAASICVNYK.G",
                "Avg XCorr");

        table.clickApplyGridFilter();
        assertTextPresent(
                "Avg XCorr",
                "K.EIRQRQGDDLDGLSFAELR.G");
        assertTextNotPresent(
                "R.TQMPAASICVNYK.G");

        log("Test exporting in Query Peptides Comparision");
        File lastPeptideFile = new DataRegionExportHelper(table).exportText(ColumnHeaderType.Caption, DataRegionExportHelper.TextSeparator.TAB);
        TextSearcher lastPeptideSearcher = new TextSearcher(lastPeptideFile);
        assertTextPresent(lastPeptideSearcher,
                "Avg XCorr",
                "K.EIRQRQGDDLDGLSFAELR.G",
                "11.200",
                "13.800");
        assertTextNotPresent(lastPeptideSearcher,
                "R.TQMPAASICVNYK.G");
        goBack();
    }

    private void pepXMLtest()
    {
        clickButton("Process and Import Data");
        _fileBrowserHelper.importFile("pepXML/truncated.pep.xml", "Import Search Results");
        String ms2Run = "ms2pipe/truncated (pepXML)";
        waitForRunningPipelineJobs(defaultWaitForPage);
        clickAndWait(Locator.linkWithText(ms2Run));

        click(Locator.linkWithText("Show Peptide Prophet Details"));
        Object[] windows = getDriver().getWindowHandles().toArray();
        getDriver().switchTo().window((String) windows[1]);
        waitForElement(Locator.tagWithAttribute("img", "alt", "Charge 3+ Cumulative Observed vs. Model"));
        assertEquals("Incorrect number of graphs", 1, getElementCount(Locator.tag("img").withAttributeContaining("src", WebTestHelper.buildRelativeUrl("ms2", "MS2VerifyProject/ms2folder", "showPeptideProphetSensitivityPlot"))));
        assertEquals("Incorrect number of graphs", 6, getElementCount(Locator.tag("img").withAttributeContaining("src", WebTestHelper.buildRelativeUrl("ms2", "MS2VerifyProject/ms2folder", "showPeptideProphetDistributionPlot"))));
        assertEquals("Incorrect number of graphs", 6, getElementCount(Locator.tag("img").withAttributeContaining("src", WebTestHelper.buildRelativeUrl("ms2", "MS2VerifyProject/ms2folder", "showPeptideProphetObservedVsModelPlot"))));
        assertTextPresent("PeptideProphet Details: ms2pipe/truncated (pepXML)");
        getDriver().close();
        getDriver().switchTo().window((String) windows[0]);
    }

    //issue 12342
    private void queryValidationTest()
    {
        log("Validate previously failing queries");

        String  sqlGroupNumberDisplay =    "SELECT ProteinGroups.\"Group\", \n" +
                "ProteinGroups.GroupProbability, \n" +
                "ProteinGroups.ErrorRate, \n" +
                "ProteinGroups.UniquePeptidesCount, \n" +
                "ProteinGroups.TotalNumberPeptides \n" +
                "FROM ProteinGroups ";

        String expectedError = "Could not resolve IndistinguishableCollectionId column";

        createQuery(getProjectName() + "/ms2folder", "GroupNumberTest", "ms2", sqlGroupNumberDisplay, "", false);
        _ext4Helper.clickExt4Tab("Source");
        clickButtonContainingText("Execute Query", 0);
        waitForText(expectedError);
        assertTextPresent(expectedError, 13);

        //add correct text
        String  sqlGroupNumberDisplay2 =    "SELECT ProteinGroups.\"Group\", \n" +
                "ProteinGroups.GroupProbability, \n" +
                "ProteinGroups.ErrorRate, \n" +
                "ProteinGroups.UniquePeptidesCount, \n" +
                "ProteinGroups.TotalNumberPeptides, \n" +
                "ProteinGroups.IndistinguishableCollectionId \n" +
                "FROM ProteinGroups ";

        createQuery(getProjectName() + "/ms2folder", "GroupNumberTestCorrect", "ms2", sqlGroupNumberDisplay2 + "\n", "", false);
        _ext4Helper.clickExt4Tab("Source");
        clickButtonContainingText("Execute Query", 0);
        assertTextNotPresent(expectedError);

        navigateToFolder(FOLDER_NAME);
    }

    private void fractionRollupTest()
    {
        navigateToFolder(FOLDER_NAME);
        clickButton("Process and Import Data");
        _fileBrowserHelper.selectFileBrowserItem("bov_fract/xtandem/test1/CAexample_mini1.xtan.xml");
        _fileBrowserHelper.checkFileBrowserFileCheckbox("CAexample_mini2.xtan.xml");
        if (isElementPresent(Locator.tagWithClassContaining("span", "x4-toolbar-more-icon")))
        {
            click(Locator.tagWithClassContaining("span", "x4-toolbar-more-icon"));
            click(Locator.tagWithText("span", "Fraction Rollup Analysis"));
        }
        else
        {
            clickButton(" Fraction Rollup Analysis");
        }
        waitForElement(Locator.input("protocolName"));
        setFormElement(Locator.input("protocolName"), PROTOCOL_NAME);
        setFormElement(Locator.textarea("configureXml"), PROTOCOL_XML);
        clickButton("Search");
        waitForRunningPipelineJobs(defaultWaitForPage);
        waitForElementWithRefresh(Locator.linkContainingText(PROTOCOL_NAME), WAIT_FOR_JAVASCRIPT);
        waitAndClick(Locator.linkContainingText(PROTOCOL_NAME));
        waitForElement(Locator.linkWithText("K.LLASMLAK.A"));
        DataRegionTable peptidesTable = new DataRegionTable(REGION_NAME_PEPTIDES, this);
        List<String> peptides = peptidesTable.getColumnDataAsText("Peptide");
        assertEquals(Arrays.asList("K.LLASMLAK.A", "R.Q^YALHVDGVGTK.A", "R.EFAEVVSKIRR.S", "A.KKVVAVIK.L", "K.ELQAAQAR.L", "R.EYDTSKIEAAIWK.E", "K.TEGVIPSR.E", "R.LGRHPNK.A", "K.LLASMLAK.A", "R.Q^YALHVDGVGTK.A", "R.EFAEVVSKIRR.S", "A.KKVVAVIK.L", "K.ELQAAQAR.L", "R.EYDTSKIEAAIWK.E", "K.TEGVIPSR.E", "R.LGRHPNK.A"), peptides);
    }

    private void cleanPipeline()
    {
        if (PIPELINE_PATH == null)
            return;
        File rootDir = new File(PIPELINE_PATH);
        delete(new File(rootDir, ".labkey/protocols/rollup/Protocol Rollup 1.xml"));
    }

    //TODO: Create MS2RunView component for this stuff
    public Set<String> getFilters(ViewType type)
    {
        String filter = Locator.tag("td").withText(type + NBSP + "Filter:").followingSibling("td").findElement(getDriver()).getText().trim();
        return new HashSet<>(Arrays.asList(filter.split(" +AND +")));
    }

    public List<String> getSorts(ViewType type)
    {
        String peptideFilter = Locator.tag("td").withText(type + NBSP + "Sort:").followingSibling("td").findElement(getDriver()).getText().trim();
        return Arrays.asList(peptideFilter.split(" +, +"));
    }

    public enum ViewType
    {
        peptide{
            @Override
            public String toString()
            {
                return "Peptide";
            }
        },
        protein{
            @Override
            public String toString()
            {
                return "Protein";
            }
        },
        proteinGroup{
            @Override
            public String toString()
            {
                return "Protein" + NBSP + "Group";
            }
        };
    }
}
