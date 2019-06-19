/*
 * Copyright (c) 2008-2018 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.ms2.MS2PipelineFolder;
import org.labkey.test.ms2.params.MS2EmailSuccessParams;
import org.labkey.test.pipeline.PipelineFolder;
import org.labkey.test.pipeline.PipelineTestParams;
import org.labkey.test.pipeline.PipelineTestsBase;
import org.labkey.test.pipeline.PipelineWebTestBase;
import org.labkey.test.util.PipelineToolsHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({BVT.class, FileBrowser.class})
@BaseWebDriverTest.ClassTimeout(minutes = 15)
public class PipelineTest extends PipelineWebTestBase
{
    protected static final int MAX_WAIT_SECONDS = 60*5;

    protected PipelineTestsBase _testSetMS2 = new PipelineTestsBase(this);
    private final PipelineToolsHelper _pipelineToolsHelper = new PipelineToolsHelper(this);

    public PipelineTest()
    {
        super("Pipeline BVT");

        MS2PipelineFolder folder = new MS2PipelineFolder(this, "pipe1",
                TestFileUtils.getLabKeyRoot() + "/sampledata/xarfiles/ms2pipe");
        folder.setFolderType("None");
        folder.setTabs("Pipeline", "MS2", "Dumbster");
        folder.setWebParts("Data Pipeline", "MS2 Runs", "Mail Record");

        PipelineFolder.MailSettings mail = new PipelineFolder.MailSettings(this);
        mail.setNotifyOnSuccess(true, true, "brother@pipelinebvt.test");
        mail.setNotifyOnError(true, true);
        mail.setEscalateUsers("momma@pipelinebvt.test");
        folder.setMailSettings(mail);

        String[] sampleNames = new String[] { "CAexample_mini1", "CAexample_mini2" };
        _testSetMS2.setFolder(folder);
        _testSetMS2.addParams(mailParams(new MS2EmailSuccessParams(this, "bov_fract", "test1", sampleNames), mail));
        _testSetMS2.addParams(mailParams(new MS2EmailSuccessParams(this, "bov_fract", "test_fract"), mail));
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

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("pipeline", "ms2");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        _testSetMS2.clean();
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

        _testSetMS2.setup();

        enableEmailRecorder();
        runProcessing(_testSetMS2);

        // Repeat to make sure retry works
        enableEmailRecorder();
        runProcessing(_testSetMS2);
        checkEmail(2);

        // Make sure there haven't been any errors yet.
        checkErrors();
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
        assertEquals("Expected " + countExpect + " notification emails, found " + count, count, countExpect);
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
