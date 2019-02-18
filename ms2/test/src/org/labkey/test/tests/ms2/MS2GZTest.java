/*
 * Copyright (c) 2007-2018 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

/** exercises the gzip handling */
@Category({MS2.class, DailyA.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class MS2GZTest extends AbstractMS2ImportTest
{
    @Override
    @LogMethod
    protected void setupMS2()
    {
        super.setupMS2();

        importMS2Run("DRT3", 2);
    }

    @Override
    @LogMethod
    protected void verifyMS2()
    {
        DataRegionTable searchRunsTable = new DataRegionTable(REGION_NAME_SEARCH_RUNS, this);

        log("Test Compare Search Engine Proteins");
        navigateToFolder(FOLDER_NAME);

        searchRunsTable.checkAllOnPage();
        searchRunsTable.clickHeaderMenu("Compare", "Search Engine Protein");
        selectOptionByText(Locator.name("viewParams"), QUERY_PROTEINPROPHET_VIEW_NAME);
        checkCheckbox(Locator.checkboxByName("total"));
        clickButton("Compare");
        assertTextPresent("Total",
                "gi|33241155|ref|NP_876097.1|",
                "Pattern");
        assertTextNotPresent("gi|32307556|ribosomal_protein", "gi|136348|TRPF_YEAST_N-(5'-ph");
        DataRegionTable compareTable = new DataRegionTable("MS2Compare",getDriver());
        compareTable.setSort("Protein", SortDirection.ASC);
        assertTextBefore("gi|11499506|ref|NP_070747.1|", "gi|13507919|");

    }
}