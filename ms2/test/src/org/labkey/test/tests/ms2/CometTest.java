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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.categories.MS2;
import org.labkey.test.ms2.AbstractMS2SearchEngineTest;

import java.io.File;

import static org.junit.Assert.fail;

@Category({MS2.class, DailyB.class, FileBrowser.class})
public class CometTest extends AbstractMS2SearchEngineTest
{
    protected static final String SEARCH_BUTTON = "Comet";
    protected static final String SEARCH_TYPE = "comet";

    @Test
    public void testSteps()
    {
        log("Verifying that pipeline files were cleaned up properly");
        File test2 = new File(PIPELINE_PATH + "/bov_sample/" + SEARCH_TYPE + "/test2");
        if (test2.exists())
            fail("Pipeline files were not cleaned up; test2(" + test2.toString() + ") directory still exists");

        _useOnlyOneFasta = true;

        basicMS2Check();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        cleanPipe(SEARCH_TYPE);
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    protected void setupEngine()
    {
        log("Analyze " + SEARCH_BUTTON + " sample data.");
        _fileBrowserHelper.selectImportDataAction(SEARCH_BUTTON +  " Peptide Search");
    }

    protected void basicChecks()
    {
        goToModule("Query");
        selectQuery("ms2", "Fractions");
        waitForElement(Locator.linkWithText("view data"), WAIT_FOR_JAVASCRIPT);
        clickAndWait(Locator.linkWithText("view data"));
        assertTextPresent("CAexample_mini.mzXML");
        // There should be 200 scans total
        assertTextPresent("200");
        // There should be 100 MS1 scans and 100 MS2 scans
        assertTextPresent("100");

        navigateToFolder(getProjectName(), getCurrentContainer()); // with tabs invisible, navigate to the container
        clickAndWait(Locator.linkWithImage(WebTestHelper.getContextPath() + "/MS2/images/runIcon.gif"));

        // Make sure we're not using a custom default view for the current user
        selectOptionByText(Locator.name("viewParams"), "<Standard View>");
        clickButton("Go");

        // Check for a few high-scoring peptides
        assertTextPresent("K.MSKIRQVIAAR.L", "K.LRRPDPYK.G", "K.KSGRSSDLTSVR.L");
    }
}
