/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.ms2.MS2PipelineFolder;
import org.labkey.test.ms2.params.MS2EmailSuccessParams;
import org.labkey.test.params.ms1.FeaturesTestParams;
import org.labkey.test.params.ms1.PepMatchTestParams;
import org.labkey.test.pipeline.PipelineFolder;
import org.labkey.test.pipeline.PipelineTestParams;
import org.labkey.test.pipeline.PipelineTestsBase;
import org.labkey.test.pipeline.PipelineWebTestBase;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.util.PipelineToolsHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

@Category({BVT.class, FileBrowser.class})
public class PipelineTest extends PipelineWebTestBase
{
    protected static final int MAX_WAIT_SECONDS = 60*5;

    protected PipelineTestsBase _testSetMS2 = new PipelineTestsBase(this);
    protected PipelineTestsBase _testSetMS1 = new PipelineTestsBase(this);
    private final PipelineToolsHelper _pipelineToolsHelper = new PipelineToolsHelper(this);

    public PipelineTest()
    {
        super("Pipeline BVT");

        MS2PipelineFolder folder = new MS2PipelineFolder(this, "pipe1",
                TestFileUtils.getLabKeyRoot() + "/sampledata/xarfiles/ms2pipe");
        folder.setFolderType("None");
        folder.setTabs("Pipeline", "MS1", "MS2", "Dumbster");
        folder.setWebParts("Data Pipeline", "MS1 Runs", "MS2 Runs", "Mail Record");

        PipelineFolder.MailSettings mail = new PipelineFolder.MailSettings(this);
        mail.setNotifyOnSuccess(true, true, "brother@pipelinebvt.test");
        mail.setNotifyOnError(true, true);
        mail.setEscalateUsers("momma@pipelinebvt.test");
        folder.setMailSettings(mail);

        String[] sampleNames = new String[] { "CAexample_mini1", "CAexample_mini2" };
        _testSetMS2.setFolder(folder);
        _testSetMS2.addParams(mailParams(new MS2EmailSuccessParams(this, "bov_fract", "test1", sampleNames), mail));
        _testSetMS2.addParams(mailParams(new MS2EmailSuccessParams(this, "bov_fract", "test_fract"), mail));

        _testSetMS1.setFolder(folder);
        _testSetMS1.addParams(mailParams(new FeaturesTestParams(this, "bov_fract", "find_minmax",
                false, sampleNames), mail, true));
        _testSetMS1.addParams(mailParams(new PepMatchTestParams(this, "bov_fract/xtandem/test1", "match1",
                true, sampleNames), mail, true));
    }

    private PipelineTestParams mailParams(PipelineTestParams params, PipelineFolder.MailSettings mail)
    {
        return mailParams(params, mail, false);
    }

    private PipelineTestParams mailParams(PipelineTestParams params, PipelineFolder.MailSettings mail,
                                          boolean expectError)
    {
        params.setMailSettings(mail);
        params.setExpectError(expectError);
        return params;
    }

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("pipeline", "ms1", "ms2");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _testSetMS2.clean();
        _testSetMS1.clean();
        super.doCleanup(afterTest);
    }

    @Before
    public void preTest()
    {
        _pipelineToolsHelper.setToolsDirToTestDefault();
    }

    @Test
    public void testSteps()
    {
        _testSetMS2.verifyClean();
        _testSetMS1.verifyClean();

        _testSetMS2.setup();

        enableEmailRecorder();
        runProcessing(_testSetMS2);

        // Repeat to make sure retry works
        enableEmailRecorder();
        runProcessing(_testSetMS2);
        checkEmail(2);

        // Make sure there haven't been any errors yet.
        checkErrors();

        // Break the pipeline tools directory setting to cause errors.
        _pipelineToolsHelper.setPipelineToolsDirectory(TestFileUtils.getLabKeyRoot() + "/external/noexist");
        runProcessing(_testSetMS1);
        checkEmail(4);

        // Make sure the expected errors have been logged.
        checkExpectedErrors(4);

        // Test pipeline error escalation email.
        _testSetMS1.getParams()[0].validateEmailEscalation(0);
        checkEmail(1);
        // Fix the pipeline tools directory.
        _pipelineToolsHelper.resetPipelineToolsDirectory();

        PipelineTestParams tpRetry = _testSetMS1.getParams()[0];
        tpRetry.setExpectError(false);
        for (String sampleExp : tpRetry.getExperimentLinks())
        {
            pushLocation();
            clickAndWait(Locator.linkWithText("All"));
            log("Trying to view status info for " + sampleExp);
            // Create a fresh PipelineStatusTable every time through the loop so that we're looking at a current set
            // of cached table info
            PipelineStatusTable statusTable = new PipelineStatusTable(this);
            statusTable.clickStatusLink(sampleExp);
            log("Now on job with URL " + getURL());
            clickButton("Retry");
            popLocation();
        }

        waitToComplete(_testSetMS1);

        // Could validate here more, but the final validation should be enough.
        checkEmail(1);

        for (PipelineTestParams tp : _testSetMS1.getParams())
            tp.setExpectError(false);
        runProcessing(_testSetMS1);
        checkEmail(2);
    }

    public void checkEmail(int countExpect)
    {
        EmailRecordTable emailTable = new EmailRecordTable(this);
        int sleepCount = 0;
        // Wait up to 15 seconds for the email to be sent and show up in the table
        while (emailTable.getEmailCount() < countExpect && sleepCount < 3)
        {
            sleep(5000);
            refresh();
            sleepCount++;
        }
        int count = emailTable.getEmailCount();
        assertTrue("Expected " + countExpect + " notification emails, found " + count,
                count == countExpect);
        enableEmailRecorder();
    }

    private void runProcessing(PipelineTestsBase testSet)
    {
        testSet.runAll();

        waitToComplete(testSet);
        
        for (PipelineTestParams tp : testSet.getParams())
        {
            pushLocation();
            tp.validate();
            popLocation();

            // Quit if a test was not valid.
            assertTrue(tp.isValid());
        }
    }

    private void waitToComplete(PipelineTestsBase testSet)
    {
        // Just wait for everything to complete.
        int seconds = 0;
        int sleepInterval = 2;
        do
        {
            log("Waiting for tests processing to complete");
            sleep(sleepInterval * 1000);
            seconds += sleepInterval;
            refresh();
        }
        while (testSet.getCompleteParams().length != testSet.getParams().length &&
                seconds < MAX_WAIT_SECONDS);
    }
}
