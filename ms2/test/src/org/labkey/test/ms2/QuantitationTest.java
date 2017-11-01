/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
package org.labkey.test.ms2;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;

import java.io.File;

@Category({DailyA.class})
public class QuantitationTest extends AbstractXTandemTest
{
    protected static final String LIBRA_PROTOCOL_NAME = "BasicLibra";

    protected static final String LIBRA_INPUT_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> \n" +
        "<bioml>\n" +
            "  <note label=\"pipeline, protocol name\" type=\"input\">" + LIBRA_PROTOCOL_NAME + "</note> \n" +
            "  <note label=\"pipeline, protocol description\" type=\"input\">Search with Libra quantitation</note> \n" +
            "  <note label=\"pipeline prophet, min peptide probability\" type=\"input\">0</note> \n" +
            "  <note label=\"pipeline prophet, min protein probability\" type=\"input\">0</note> \n" +
            "  <note label=\"pipeline quantitation, algorithm\" type=\"input\">libra</note> \n" +
            "  <note label=\"pipeline quantitation, libra normalization channel\" type=\"input\">2</note> \n" +
            "  <note label=\"pipeline quantitation, libra config name\" type=\"input\">LibraConfig1</note> \n" +
        "</bioml>";

    @Test
    public void testSteps()
    {
        createProjectAndFolder();

        log("Start analysis running.");
        navigateToFolder(getProjectName(),"ms2folder");
        clickButton("Process and Import Data");

        _fileBrowserHelper.selectFileBrowserItem("bov_sample/" + SAMPLE_BASE_NAME + ".mzXML");

        setupEngine();

        waitForElement(Locator.xpath("//select[@name='sequenceDB']/option[.='" + DATABASE1 + "']" ), WAIT_FOR_JAVASCRIPT);
        log("Set analysis parameters.");
        setFormElement(Locator.name("protocolName"), LIBRA_PROTOCOL_NAME);
        setFormElement(Locator.name("protocolDescription"), "Search with Libra quantitation");
        selectOptionByText(Locator.name("sequenceDB"), DATABASE1);
        setFormElement(Locator.name("configureXml"), "");
        waitAndClick(Locator.xpath("//a[@class='labkey-button']/span[text() = 'OK']"));
        setFormElement(Locator.name("configureXml"), LIBRA_INPUT_XML);
        clickButton("Search");
        log("View the analysis log.");
        // Search is submitted as AJAX, and upon success the browser is redirected to a new page. Wait for it to load
        waitForElement(Locator.linkWithText("Data Pipeline"), WAIT_FOR_JAVASCRIPT);
        sleep(5000); // without this sleep, some machines try to redirect back to the begin.view page after the Data Pipeline link is clicked
        clickAndWait(Locator.linkWithText("Data Pipeline"));

        String runDescription = SAMPLE_BASE_NAME + " (" + LIBRA_PROTOCOL_NAME + ")";
        waitForPipelineJobsToComplete(1, runDescription, false);

        clickFolder(FOLDER_NAME);
        clickAndWait(Locator.linkContainingText(runDescription));
        selectOptionByText(Locator.name("viewParams"), "<Standard View>");
        clickButton("Go");
        assertTextPresent(PEPTIDE3, PEPTIDE5);

        clickFolder(FOLDER_NAME);

        // Jump to the flow chart view
        clickAndWait(Locator.tagWithAttribute("a", "title", "Experiment run graph"));

        clickAndWait(Locator.imageMapLinkByTitle("graphmap", "Data: " + SAMPLE_BASE_NAME + ".libra.tsv (Run Output)"));
        assertElementPresent(Locator.linkWithText("libra Protein Quantitation"));

        clickAndWait(Locator.linkWithText("Lineage for " + SAMPLE_BASE_NAME + ".libra.tsv"));
        beginAt(Locator.imageMapLinkByTitle("graphmap", "libra Peptide Quantitation").findElement(getDriver()).getAttribute("href"));
        // Check to see that arguments to xinteract are showing
        assertTextPresent("-LLibraConfig1.xml-2");
    }

    @Override
    protected void basicChecks()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void cleanPipe(String search_type)
    {
        File rootDir = new File(PIPELINE_PATH);
        delete(new File(rootDir, ".labkey/protocols/"+search_type+"/" + LIBRA_PROTOCOL_NAME + ".xml"));
        delete(new File(rootDir, "bov_sample/"+search_type+"/" + LIBRA_PROTOCOL_NAME));
    }
}
