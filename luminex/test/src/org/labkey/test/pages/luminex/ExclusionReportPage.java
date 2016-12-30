package org.labkey.test.pages.luminex;

import org.labkey.test.Locator;
import org.labkey.test.components.luminex.exclusionreport.ExcludedSinglepointUnknownsWebpart;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.internal.WrapsDriver;

import static org.labkey.test.components.luminex.exclusionreport.ExcludedSinglepointUnknownsWebpart.ExcludedSinglepointUnknownsWebpart;

/**
 * Created by iansigmon on 12/29/16.
 */
public class ExclusionReportPage extends LabKeyPage
{
    private static final String SinglePoint_TITLE = "Excluded Singlepoint Unknowns";

    Elements _elements;
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
        elements().singlepointUnknownsWebpart.assertExclusionPresent(runName, description, dilution, analytes);
    }

    public void assertSinglepointUnknownExclusionNotPresent(String runName, String description, String dilution, String...analytes)
    {
        elements().singlepointUnknownsWebpart.assertExclusionNotPresent(runName, description, dilution, analytes);
    }

    public Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    public class Elements extends LabKeyPage.ElementCache
    {
        final ExcludedSinglepointUnknownsWebpart singlepointUnknownsWebpart = ExcludedSinglepointUnknownsWebpart(getDriver()).findWhenNeeded();

    }

    public static class Locators extends org.labkey.test.Locators
    {

    }
}
