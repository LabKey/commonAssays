package org.labkey.test.components.luminex.importwizard;

import org.labkey.test.Locator;
import org.labkey.test.components.WebPartPanel;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Created by iansigmon on 12/29/16.
 */
public class BatchPropertiesWebPart extends WebPartPanel
{
    private static final String TITLE = "Batch Properties";

    private Elements _elements;
    protected BatchPropertiesWebPart(WebElement componentElement, WebDriver driver)
    {
        super(componentElement, driver);
    }

    public static BatchPropertiesWebPartFinder BatchPropertiesWebPart(WebDriver driver)
    {
        return new BatchPropertiesWebPartFinder(driver).withTitle(TITLE);
    }

    public void checkSampleInfo()
    {
        elements().sampleInfoRadioButton.click();
    }

    public static class BatchPropertiesWebPartFinder extends WebPartFinder<BatchPropertiesWebPart, BatchPropertiesWebPartFinder>
    {
        public BatchPropertiesWebPartFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected BatchPropertiesWebPart construct(WebElement el, WebDriver driver)
        {
            return new BatchPropertiesWebPart(el, driver);
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
        protected final WebElement sampleInfoRadioButton = Locators.sampleInfoRadio.findWhenNeeded(this);

    }

    public static class Locators extends org.labkey.test.Locators
    {
        protected static final Locator sampleInfoRadio = Locator.radioButtonByNameAndValue("participantVisitResolver", "SampleInfo");
    }
}