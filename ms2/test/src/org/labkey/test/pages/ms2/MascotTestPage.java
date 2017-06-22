/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
package org.labkey.test.pages.ms2;

import org.labkey.test.Locator;
import org.labkey.test.Locators;
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