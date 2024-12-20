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
package org.labkey.test.pages.luminex;

import org.labkey.test.Locator;
import org.labkey.test.components.luminex.exclusionreport.ExcludedSinglepointUnknownsWebpart;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WrapsDriver;

import static org.labkey.test.components.luminex.exclusionreport.ExcludedSinglepointUnknownsWebpart.ExcludedSinglepointUnknownsWebpart;

/**
 * Created by iansigmon on 12/29/16.
 */
public class ExclusionReportPage extends LabKeyPage<ExclusionReportPage.Elements>
{
    private static final String SinglePoint_TITLE = "Excluded Singlepoint Unknowns";

    private ExclusionReportPage(WebDriver driver)
    {
        super(driver);
    }

    public static ExclusionReportPage beginAt(WrapsDriver driver)
    {
        ExclusionReportPage page = new ExclusionReportPage(driver.getWrappedDriver());
        page.clickAndWait(Locator.linkWithText("view excluded data"));

        return page;
    }

    public void assertSinglepointUnknownExclusion(String runName, String description, String dilution, String...analytes)
    {
        elementCache().singlepointUnknownsWebpart.assertExclusionPresent(runName, description, dilution, analytes);
    }

    public void assertSinglepointUnknownExclusionNotPresent(String runName, String description, String dilution, String...analytes)
    {
        elementCache().singlepointUnknownsWebpart.assertExclusionNotPresent(runName, description, dilution, analytes);
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public class Elements extends LabKeyPage<?>.ElementCache
    {
        final ExcludedSinglepointUnknownsWebpart singlepointUnknownsWebpart = ExcludedSinglepointUnknownsWebpart(getDriver()).findWhenNeeded();

    }

}
