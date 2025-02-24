/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.util.signaldata;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.core.webdav.WebDavUploadHelper;

import java.io.File;

public class SignalDataInitializer
{
    private final BaseWebDriverTest _test;
    private final String _project;

    public static final String RAW_SignalData_ASSAY = "RawSignalData";
    public static final String RAW_SignalData_DESC = "SignalData Raw Assay Data";

    public static final File RAW_SignalData_SAMPLE_DATA = TestFileUtils.getSampleData("signaldata");

    public SignalDataInitializer(BaseWebDriverTest test, String projectName)
    {
        _test = test;
        _project = projectName;
    }

    @LogMethod
    public void setupProject()
    {
        _test._containerHelper.createProject(_project, "Assay");
        _test._containerHelper.enableModule(_project, "SignalData");

        defineRawSignalDataAssay();
        mockUploadRunData();

        _test.goToProjectHome();
    }

    @LogMethod
    private void defineRawSignalDataAssay()
    {
        _test.goToProjectHome();

        _test.log("Defining Raw SignalData Assay");
        _test.goToManageAssays();

        ReactAssayDesignerPage assayDesigner = _test._assayHelper.createAssayDesign("Signal Data", RAW_SignalData_ASSAY);
        assayDesigner.setDescription(RAW_SignalData_DESC);
        assayDesigner.setEditableRuns(true);
        assayDesigner.setEditableResults(true);
        assayDesigner.clickFinish();

        new WebDavUploadHelper(_test.getPrimaryTestProject()).uploadDirectoryContents(RAW_SignalData_SAMPLE_DATA);
    }

    @LogMethod
    private void mockUploadRunData()
    {
        _test.beginAt("/" + _project + "/signaldata-mockSignalDataWatch.view");
        _test.waitForElement(Locator.tag("li").withText("Ready to Load"));
        _test.click(Locator.tagWithClass("input", "signaldata-run-btn"));
        _test.waitForElement(Locator.tagWithText("li", "Test Run Upload Complete"));
    }
}
