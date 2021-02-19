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
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebElement;

public class MascotConfigPage extends LabKeyPage<MascotConfigPage.Elements>
{
    public MascotConfigPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public static MascotConfigPage beginAt(BaseWebDriverTest test)
    {
        test.beginAt(WebTestHelper.buildURL("ms2", "mascotConfig"));
        return new MascotConfigPage(test);
    }

    public MascotConfigPage setMascotServer(String serverUrl)
    {
        setFormElement(elementCache().serverUrlInput, serverUrl);
        return this;
    }

    public MascotConfigPage setMascotUser(String user)
    {
        setFormElement(elementCache().userInput, user);
        return this;
    }

    public MascotConfigPage setMascotPassword(String password)
    {
        setFormElement(elementCache().passwordInput, password);
        return this;
    }

    public MascotConfigPage setMascotProxy(String proxyUrl)
    {
        setFormElement(elementCache().proxyUrlInput, proxyUrl);
        return this;
    }

    public MascotTestPage testMascotSettings()
    {
        elementCache().testLink.click();
        return new MascotTestPage(this);
    }

    public LabKeyPage save()
    {
        clickAndWait(elementCache().saveButton);
        return null;
    }

    public LabKeyPage cancel()
    {
        clickAndWait(elementCache().cancelButton);
        return null;
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    protected class Elements extends LabKeyPage<?>.ElementCache
    {
        WebElement serverUrlInput = new LazyWebElement(Locator.name("mascotServer"), this);
        WebElement userInput = new LazyWebElement(Locator.name("mascotUserAccount"), this);
        WebElement passwordInput = new LazyWebElement(Locator.name("mascotUserPassword"), this);
        WebElement proxyUrlInput = new LazyWebElement(Locator.name("mascotHTTPProxy"), this);

        WebElement testLink = new LazyWebElement(Locator.linkWithText("Test Mascot settings"), this);
        WebElement saveButton = new LazyWebElement(Locator.lkButton("Save"), this);
        WebElement cancelButton = new LazyWebElement(Locator.lkButton("Cancel"), this);
    }
}
