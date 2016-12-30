package org.labkey.test.components.luminex.importwizard;

import org.labkey.test.components.WebPartPanel;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Created by iansigmon on 12/29/16.
 */
public class AnalytePropertiesWebPart extends WebPartPanel
{
    private static final String TITLE = "Analyte Properties";

    private Elements _elements;
    protected AnalytePropertiesWebPart(WebElement componentElement, WebDriver driver)
    {
        super(componentElement, driver);
    }

    public static AnalytePropertiesWebPartFinder AnalytePropertiesWebPart(WebDriver driver)
    {
        return new AnalytePropertiesWebPartFinder(driver).withTitle(TITLE);
    }

    public static class AnalytePropertiesWebPartFinder extends WebPartFinder<AnalytePropertiesWebPart, AnalytePropertiesWebPartFinder>
    {
        public AnalytePropertiesWebPartFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected AnalytePropertiesWebPart construct(WebElement el, WebDriver driver)
        {
            return new AnalytePropertiesWebPart(el, driver);
        }
    }

    public Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    public class Elements extends WebPartPanel.ElementCache
    {

    }

    public static class Locators extends org.labkey.test.Locators
    {
    }
}
