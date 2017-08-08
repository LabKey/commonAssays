/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ExtHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Created by iansigmon on 12/29/16.
 */
public class SinglepointExclusionDialog extends BaseExclusionDialog
{
    protected static final String MENU_BUTTON_ITEM = "Exclude Singlepoint Unknowns";
    private static final String TITLE = "Exclude Singlepoint Unknowns from Analysis";

    Elements _elements;
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
        return dialog;
    }

    public void selectDilution(String description, String dilution)
    {
        click(Locator.xpath("//div[contains(@class, 'x-grid3-row')]").withDescendant(Locator.tagWithText("td", description).followingSibling("td").withText(dilution)));
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

    protected Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    public class Elements extends LabKeyPage.ElementCache
    {
    }

    public static class Locators extends org.labkey.test.Locators
    {
    }
}
