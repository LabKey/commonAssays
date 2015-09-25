package org.labkey.test.pages.ms2;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

public class MascotTestPage extends LabKeyPage
{
    private final Elements _elements;
    private final MascotConfigPage _configPage;

    public MascotTestPage(MascotConfigPage configPage)
    {
        super(configPage.getTest());
        _elements = new Elements();
        _configPage = configPage;

        _test.switchToWindow(1);
    }

    public String getError()
    {
        try
        {
            return elements().testError.getText();
        }
        catch (NoSuchElementException ignore)
        {
            return null;
        }
    }

    public String getConfigurationText()
    {
        return elements().configurationTextArea.getText();
    }

    public MascotConfigPage close()
    {
        _test.getDriver().close();
        _test.switchToMainWindow();

        return _configPage;
    }

    private Elements elements()
    {
        return _elements;
    }

    private class Elements extends ComponentElements
    {
        @Override
        protected SearchContext getContext()
        {
            return getDriver();
        }

        WebElement testError = new LazyWebElement(Locators.labkeyError, this);
        WebElement configurationTextArea = new LazyWebElement(Locator.tag("textarea"), this);
    }
}