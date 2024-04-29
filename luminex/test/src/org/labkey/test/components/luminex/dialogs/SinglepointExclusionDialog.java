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
package org.labkey.test.components.luminex.dialogs;

import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;
import org.openqa.selenium.WebDriver;

/**
 * Created by iansigmon on 12/29/16.
 */
public class SinglepointExclusionDialog extends BaseExclusionDialog
{
    protected static final String MENU_BUTTON_ITEM = "Exclude Singlepoint Unknowns";
    private static final String TITLE = "Exclude Singlepoint Unknowns from Analysis";

    protected SinglepointExclusionDialog(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected void openDialog()
    {
        DataRegionTable table = new DataRegionTable("Data", getDriver());
        table.clickHeaderMenu(MENU_BUTTON, false, MENU_BUTTON_ITEM);
        _extHelper.waitForExtDialog(TITLE);
    }

    public static SinglepointExclusionDialog beginAt(WebDriver driver)
    {
        SinglepointExclusionDialog dialog = new SinglepointExclusionDialog(driver);
        dialog.openDialog();
        dialog.waitForText("Exclusions", "Analyte Name");

        // Wait until there are rows in the singlepoint grid...
        dialog.waitForElements(Locator.xpath("//div[not(contains(@class, 'x-masked-relative'))][contains(@class, 'x-grid-panel')]/descendant::div[contains(@class, 'x-grid3-row')]"));
        // and the analyte grid...
        dialog.waitForElements(Locator.xpath("//div[contains(@class, 'x-masked-relative')][contains(@class, 'x-grid-panel')]/descendant::div[contains(@class, 'x-grid3-row')]"));

        return dialog;
    }

    public void selectDilution(String description, String dilution)
    {
        Locator element = Locator.xpath("//div[contains(@class, 'x-grid3-row')]").withDescendant(Locator.tagWithText("td", description).followingSibling("td").withText(dilution));
        waitForElement(element);
        scrollIntoView(element);
        click(element);
    }

    public void checkAnalyte(String analyte)
    {
        //TODO: do something more robust
        clickAnalyteGridRowCheckbox(analyte);
    }

    public void uncheckAnalyte(String analyte)
    {
        //TODO: do something more robust
        clickAnalyteGridRowCheckbox(analyte);
    }

    private void clickAnalyteGridRowCheckbox(String analyte)
    {
        click(ExtHelper.locateGridRowCheckbox(analyte));
        sleep(500);
    }

}
