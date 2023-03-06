/*
 * Copyright (c) 2015-2019 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ui.lineage.LineageGraph;
import org.openqa.selenium.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractMS2SearchEngineTest extends MS2TestBase
{
    protected boolean _useOnlyOneFasta = false;

    @Override
    abstract protected void doCleanup(boolean afterTest) throws TestTimeoutException;

    abstract protected void setupEngine();

    abstract protected void basicChecks();

    protected boolean isQuickTest()
    {
        return false;
    }

    public void basicMS2Check()
    {
        createProjectAndFolder();

        log("Start analysis running.");
        navigateToFolder(FOLDER_NAME);

        clickButton("Process and Import Data");
        _fileBrowserHelper.selectFileBrowserItem("bov_sample/");
        setupEngine();

        waitForElement(Locator.xpath("//select[@name='sequenceDB']/option[.='" + DATABASE1 + "']"), WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Minimum PeptideProphet prob", "Minimum ProteinProphet prob", "Quantitation engine");

        searchMS2LibraCheck();

        log("Set analysis parameters.");
        setFormElement(Locator.name("protocolName"), "test2");
        setFormElement(Locator.name("protocolDescription"), "This is a test protocol for Verify.");
        selectOptionByText(Locator.name("sequenceDB"), DATABASE1);

        if (!_useOnlyOneFasta)
        {
            selectOptionByText(Locator.name("sequenceDB"), DATABASE2);
            selectOptionByText(Locator.name("sequenceDB"), DATABASE3);
        }

        setFormElement(Locator.name("configureXml"), "");
        waitAndClick(Locator.xpath("//a[@class='labkey-button']/span[text() = 'OK']"));
        setFormElement(Locator.name("configureXml"), INPUT_XML);
        assertTextPresent("Quantitation mass tolerance", "Quantitation residue mass label");
        setFormElement(Locator.name("minPeptideProphetProb"), "0");
        clickButton("Search");
        // Search is submitted as AJAX, and upon success the browser is redirected to a new page. Wait for it to load
        waitForElement(Locator.linkWithText("Data Pipeline"), WAIT_FOR_JAVASCRIPT);
        sleep(5000); // without this sleep, some machines try to redirect back to the begin.view page after the Data Pipeline link is clicked
        log("View the analysis log.");
        clickAndWait(Locator.linkWithText("Data Pipeline"));

        waitForPipelineJobsToComplete(1, SAMPLE_BASE_NAME + " (test2)", false);

        clickAndWait(Locator.xpath("//a[contains(text(), '" + SAMPLE_BASE_NAME + " (test2)')]/../../td/a"));

        log("View log file.");

        pushLocation();
        clickAndWait(Locator.linkContainingText(LOG_BASE_NAME).containing(".log")); //Filename Link has a time stamp

        log("Verify log.");
        assertTextPresent("search");
        popLocation();

        if (isQuickTest())
            return;

        log("Analyze again.");
        navigateToFolder(FOLDER_NAME);
        clickButton("Process and Import Data");
        _fileBrowserHelper.selectFileBrowserItem("bov_sample/");

        setupEngine();

        log("Make sure new protocol is listed.");
        waitForElement(Locator.xpath("//select[@name='protocol']/option[.='test2']"), WAIT_FOR_JAVASCRIPT);
        assertEquals("test2", getSelectedOptionText(Locator.name("protocol")));

        if (!isElementPresent(Locator.linkWithText("running")) && isElementPresent(Locator.linkWithText("completed")))
            assertTextPresent("running");

        log("Verify no work for protocol.");
        boolean result;
        try
        {
            findButton("Search");
            result = true;
        }
        catch (NoSuchElementException notPresent)
        {
            result = false;
        }
        assertFalse("Button '" + "Search" + "' was present", result);

        log("View full status.");
        clickFolder(FOLDER_NAME);

        assertElementPresent(Locator.linkContainingText(SAMPLE_BASE_NAME + " (test2)"));

        clickAndWait(Locator.tagWithAttribute("a", "title", "Experiment run graph"));

        log("Verify graph view");
        pushLocation();
        Locator.linkWithSpan("Toggle Beta Graph (new!)").waitForElement(getDriver(), 4000)
                .click();
        LineageGraph graphComponent = new LineageGraph.LineageGraphFinder(getDriver()).waitFor();

        // navigate to the details page for CAexample_mini.mzXML.image..itms.png
        graphComponent.getDetailGroup("Data Children")
                .getItem(SAMPLE_BASE_NAME + ".mzXML.image..itms.png")
                .clickOverViewLink(true);
        assertElementPresent(Locator.linkWithText("msPicture"), 2);
        beginAt(getAttribute(Locator.xpath("//img[contains(@src, 'showFile.view')]"), "src"));
        // Firefox sets the title of the page when we view an image separately from an HTML page, so use that to verify
        // that we got something that matches what we expect. IE doesn't do this, so assume that we're good if we don't
        // get a 404, error message, etc
        if (getBrowserType() == BrowserType.FIREFOX)
            assertTitleContains("(PNG Image");
        popLocation();

        log("Verify experiment view");
        clickAndWait(Locator.imageMapLinkByTitle("graphmap", "bov_sample/" + SAMPLE_BASE_NAME + " (test2) (CAexample_mini.mzXML)"));

        log("Verify experiment run view.");
        String dataHref = Locator.imageMapLinkByTitle("graphmap", "Data: CAexample_mini.mzXML").findElement(getDriver()).getAttribute("href");
        beginAt(dataHref); // Clicking this is unreliable. Possibly because the image is so large. Just navigate.
        assertTextPresent(
                "bov_sample/" + SAMPLE_BASE_NAME,
                "Data CAexample_mini.mzXML");

        navigateToFolder(FOLDER_NAME);
        clickAndWait(Locator.linkWithImage(WebTestHelper.getContextPath() + "/MS2/images/runIcon.gif"));

        // Make sure we're not using a custom default view for the current user
        selectOptionByText(Locator.name("viewParams"), "<Standard View>");
        clickButton("Go");

        basicChecks();
    }

    protected void searchMS2LibraCheck()
    {
        selectOptionByText(Locator.xpath("//tr[td/table/tbody/tr/td/div[contains(text(),'Quantitation engine')]]/td/select"),"Libra");
        assertTextPresent("Libra config name", "Libra normalization channel");
        setFormElement(Locator.xpath("//tr[td/table/tbody/tr/td/div[text()='Libra config name']]/td/input"), "foo");
        fireEvent(Locator.xpath("//tr[td/table/tbody/tr/td/div[text()='Libra config name']]/td/input"), SeleniumEvent.change);
        String text = getFormElement(Locator.name("configureXml"));
        assertTrue(text.contains("foo"));
    }
}
