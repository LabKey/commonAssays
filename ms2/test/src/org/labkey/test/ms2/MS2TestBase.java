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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

abstract public class MS2TestBase extends BaseWebDriverTest
{
    protected static final String FOLDER_NAME = "ms2folder";
    protected static final String SAMPLE_BASE_NAME = "CAexample_mini";
    protected static final String VIEW = "filterView";
    protected static final String LOG_BASE_NAME = "CAexample_mini";
    protected static final String DATABASE1 = "Bovine_mini1.fasta";
    protected static final String DATABASE2 = "Bovine_mini2.fasta";
    protected static final String DATABASE3 = "Bovine_mini3.fasta";
    protected static final String INPUT_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> \n" +
        "<bioml>\n" +
            "  <note label=\"pipeline, protocol name\" type=\"input\">test2</note> \n" +
            "  <note label=\"pipeline, protocol description\" type=\"input\">This is a test protocol using the defaults.</note> \n" +
            "  <note label=\"pipeline prophet, min peptide probability\" type=\"input\">0</note> \n" +
            "  <note label=\"pipeline prophet, min protein probability\" type=\"input\">0</note> \n" +
            "  <note label=\"spectrum, minimum peaks\" type=\"input\">10</note> \n" +
            "  <note label=\"mzxml2search, charge\" type=\"input\">1,3</note> \n" +
            "  <note label=\"pipeline mspicture, enable\" type=\"input\">true</note>  \n" +
            "  <note label=\"pipeline quantitation, residue label mass\" type=\"input\">9.0@C</note> \n" +
            "  <note label=\"pipeline quantitation, algorithm\" type=\"input\">xpress</note> \n" +
        "</bioml>";

    public static final String PIPELINE_PATH = TestFileUtils.getSampleData("xarfiles/ms2pipe").getAbsolutePath();

    protected static final String REGION_NAME_PEPTIDES = "MS2Peptides";
    protected static final String REGION_NAME_PROTEINGROUPS = "ProteinGroups";
    protected static final String REGION_NAME_SEARCH_RUNS = "MS2SearchRuns";

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("ms2");
    }

    @Override
    protected String getProjectName()
    {
        return "MS2VerifyProject";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        cleanPipe(PIPELINE_PATH);
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    protected void createProjectAndFolder()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME, "MS2");

        log("Set bad pipeline root.");
        String errorMessage = setPipelineRootExpectingError("/bogus");
        assertTrue(errorMessage.contains("does not exist"));

        log("Set good pipeline root.");
        setPipelineRoot(PIPELINE_PATH);
    }

    protected void delete(File file)
    {
        if (file.isDirectory())
        {
            for (File child : file.listFiles())
            {
                delete(child);
            }
        }
        log("Deleting " + file.getPath());
        file.delete();
    }

    protected void cleanPipe(String search_type)
    {
        if (PIPELINE_PATH == null)
            return;

        File rootDir = new File(PIPELINE_PATH);
        delete(new File(rootDir, "bov_sample/xars"));
        delete(new File(rootDir, "bov_sample/"+search_type+"/test1/CAexample_mini.log"));
        delete(new File(rootDir, "bov_sample/"+search_type+"/test2"));
        delete(new File(rootDir, ".labkey/protocols/mass_spec/TestMS2Protocol.xml"));
        delete(new File(rootDir, ".labkey/protocols/"+search_type+"/default.xml"));
        delete(new File(rootDir, ".labkey/protocols/"+search_type+"/test2.xml"));
    }

    protected void navigateToFolder(String folderName)
    {
        navigateToFolder(getProjectName(), folderName);
    }
}
