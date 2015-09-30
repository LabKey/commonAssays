/*
 * Copyright (c) 2015 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

public class MascotConfigPage extends LabKeyPage
{
    private final Elements _elements;

    public MascotConfigPage(BaseWebDriverTest test)
    {
        super(test);
        _elements = new Elements();
    }

    BaseWebDriverTest getTest()
    {
        return _test;
    }

    public static MascotConfigPage beginAt(BaseWebDriverTest test)
    {
        test.beginAt(WebTestHelper.buildURL("ms2", "mascotConfig"));
        return new MascotConfigPage(test);
    }

    public MascotConfigPage setMascotServer(String serverUrl)
    {
        _test.setFormElement(elements().serverUrlInput, serverUrl);
        return this;
    }

    public MascotConfigPage setMascotUser(String user)
    {
        _test.setFormElement(elements().userInput, user);
        return this;
    }

    public MascotConfigPage setMascotPassword(String password)
    {
        _test.setFormElement(elements().passwordInput, password);
        return this;
    }

    public MascotConfigPage setMascotProxy(String proxyUrl)
    {
        _test.setFormElement(elements().proxyUrlInput, proxyUrl);
        return this;
    }

    public MascotTestPage testMascotSettings()
    {
        elements().testLink.click();
        return new MascotTestPage(this);
    }

    public LabKeyPage save()
    {
        _test.clickAndWait(elements().saveButton);
        return new LabKeyPage(_test);
    }

    public LabKeyPage cancel()
    {
        _test.clickAndWait(elements().cancelButton);
        return new LabKeyPage(_test);
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

        WebElement serverUrlInput = new LazyWebElement(Locator.name("mascotServer"), this);
        WebElement userInput = new LazyWebElement(Locator.name("mascotUserAccount"), this);
        WebElement passwordInput = new LazyWebElement(Locator.name("mascotUserPassword"), this);
        WebElement proxyUrlInput = new LazyWebElement(Locator.name("mascotHTTPProxy"), this);

        WebElement testLink = new LazyWebElement(Locator.linkWithText("Test Mascot settings"), this);
        WebElement saveButton = new LazyWebElement(Locator.lkButton("Save"), this);
        WebElement cancelButton = new LazyWebElement(Locator.lkButton("Cancel"), this);
    }
}