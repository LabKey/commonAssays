/*
 * Copyright (c) 2016-2026 LabKey Corporation
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
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.ext4.Checkbox.Ext4Checkbox;

public class SignalDataRunViewerPage extends LabKeyPage
{
    private static final String SELECT_ALL_LABEL = "Select All";
    private static final String OVERLAY_SELECTED_BUTTON_ID = "startqcbtn";

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

    /**
     * Check or uncheck the "Select All" checkbox on the QC tool's Available Inputs toolbar. Checking it selects every
     * input in the grid; unchecking it deselects them all. Waits for the "Overlay Selected" button to reflect the new
     * selection state before returning.
     */
    public void setSelectAll(boolean checked)
    {
        Checkbox selectAll = Ext4Checkbox().withLabel(SELECT_ALL_LABEL).waitFor(getDriver());
        if (checked)
            selectAll.check();
        else
            selectAll.uncheck();

        // 'Overlay Selected' is enabled only when at least one input is selected, so it is a reliable signal that the
        // grid selection has settled after toggling Select All.
        waitFor(() -> isOverlaySelectedEnabled() == checked,
                "'Overlay Selected' button did not reflect the Select All state (expected enabled=" + checked + ")",
                WAIT_FOR_JAVASCRIPT);
    }

    /**
     * @param resultName the input (result) name shown in the Available Inputs grid
     * @return whether that input's grid row is currently selected
     */
    public boolean isInputSelected(String resultName)
    {
        return _ext4Helper.isGridRowSelected(resultName, 0);
    }

    /**
     * @return whether the "Overlay Selected" button is currently enabled
     */
    public boolean isOverlaySelectedEnabled()
    {
        WebElement button = Locator.id(OVERLAY_SELECTED_BUTTON_ID).findElement(getDriver());
        return Ext4Helper.elementIfEnabled(button) != null;
    }

    public WebElement showPlot()
    {
        return doAndWaitForElementToRefresh(() -> {
            clickButton("Overlay Selected", 0);
            waitForElementToDisappear(Locator.id("sampleinputs").notHidden());
        }, Locator.tag("svg"), shortWait());
    }
}
