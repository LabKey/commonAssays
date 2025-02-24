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
package org.labkey.test.pages.signaldata;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebElement;

/**
 * User: tgaluhn
 * Date: 9/6/2016
 */
public class SignalDataRunViewerPage extends LabKeyPage
{
    public SignalDataRunViewerPage(WebDriverWrapper test)
    {
        super(test);
    }

    public void waitForPageLoad()
    {
        waitForElement(Locator.id("startqcbtn"));
    }

    public void checkRunViewerCheckbox(String resultName)
    {
        _ext4Helper.checkGridRowCheckbox(resultName);
    }

    public WebElement showPlot()
    {
        return doAndWaitForElementToRefresh(() -> {
            clickButton("Overlay Selected", 0);
            waitForElementToDisappear(Locator.id("sampleinputs").notHidden());
        }, Locator.tag("svg"), shortWait());
    }
}
