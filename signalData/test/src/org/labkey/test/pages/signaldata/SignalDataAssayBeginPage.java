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

import org.jetbrains.annotations.NotNull;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;

import java.util.List;

public class SignalDataAssayBeginPage
{
    private BaseWebDriverTest _test;

    public SignalDataAssayBeginPage(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void setSearchBox(String value)
    {
        _test.setFormElement(Locators.searchBox, value);
        _test.waitForText("Search CONTAINS " + value);
        _test.waitForFormElementToEqual(Locators.searchBox, value);
    }

    public void clearSearchBox()
    {
        setSearchBox("");
    }

    public void waitForPageLoad()
    {
        _test.waitForElement(Locators.searchBox);
    }

    public void waitForGridValue(String value, int qty)
    {
        _test.waitForElements(Locators.gridCell(value), qty);
    }

    @NotNull
    private DataRegionTable getDataRegionTable()
    {
        return new DataRegionTable("aqwp101", _test.getDriver());
    }

    public int getRowCount()
    {
        return getDataRegionTable().getDataRowCount();
    }

    public void resetUploadedData(String runToKeep)
    {
        DataRegionTable table = getDataRegionTable();
        table.uncheckAll();
        int i = 0;
        boolean somethingToDelete = false;
        for (String runIdentifier : table.getColumnDataAsText("Run Identifier"))
        {
            if (!runToKeep.equals(runIdentifier))
            {
                table.checkCheckbox(i);
                somethingToDelete = true;
            }
            i++;
        }
        if (somethingToDelete)
        {
            // TODO: Should use DRT.deleteSelectedRows() but the assay runs returnUrl is mangled
            _test.doAndWaitForPageToLoad(() ->
            {
                table.clickHeaderButton("Delete");
                _test.acceptAlert();
            });
        }

        // TODO: Validate the delete?
    }

    public void resetSelections()
    {
        getDataRegionTable().uncheckAll();
    }

    public void selectData(@NotNull String name, @NotNull String run)
    {
        DataRegionTable table = getDataRegionTable();
        int i = 0;
        for (List<String> rowData : table.getRows("Name", "Run Identifier"))
        {
            if (name.equalsIgnoreCase(rowData.get(0)) && run.equalsIgnoreCase(rowData.get(1)))
            {
                table.checkCheckbox(i);
                break;
            }
            i++;
        }
    }

    public SignalDataUploadPage navigateToImportPage()
    {
        _test.clickButton("Import Data");
        return new SignalDataUploadPage(_test);
    }

    public SignalDataRunViewerPage viewRuns()
    {
        _test.clickButton("View Selected Runs");
        SignalDataRunViewerPage page = new SignalDataRunViewerPage(_test);
        page.waitForPageLoad();
        return page;
    }

    private static class Locators
    {
        private static final Locator.XPathLocator searchBox = Locator.tagWithAttributeContaining("input", "name","runs-search");
        static Locator.XPathLocator gridCell(String contents)
        {
            return Locator.tag("tr[contains(@class,'labkey-row') or contains(@class,'labkey-alternate-row')]").withDescendant(Locator.tagWithText("td", contents));
        }
    }
}
