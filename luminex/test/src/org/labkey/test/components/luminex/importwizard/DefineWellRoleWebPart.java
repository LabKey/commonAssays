/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.components.luminex.importwizard;

import org.labkey.test.Locator;
import org.labkey.test.components.WebPartPanel;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Created by iansigmon on 12/29/16.
 */
public class DefineWellRoleWebPart extends WebPartPanel
{
    private static final String TITLE = "Define Well Roles";
    private Elements _elements;
    protected DefineWellRoleWebPart(WebElement componentElement, WebDriver driver)
    {
        super(componentElement, driver);
    }

    public static DefineWellRoleWebPartFinder DefineWellRoleWebPart(WebDriver driver)
    {
        return new DefineWellRoleWebPartFinder(driver).withTitle(TITLE);
    }

    public void setOtherControlTitrationRole(String name, boolean checked)
    {
        setTitrationRole("othercontrol", name, checked);
    }

    public void setQCControlTitrationRole(String name, boolean checked)
    {
        setTitrationRole("qccontrol", name, checked);
    }

    public void setStandardTitrationRole(String name, boolean checked)
    {
        setTitrationRole("standard", name, checked);
    }

    private void setTitrationRole(String role, String titration, boolean checked)
    {
        if (checked)
            getWrapper().checkCheckbox(Locators.titrationRole(role, titration));
        else
            getWrapper().uncheckCheckbox(Locators.titrationRole(role, titration));
    }

    public static class DefineWellRoleWebPartFinder extends WebPartFinder<DefineWellRoleWebPart, DefineWellRoleWebPartFinder>
    {
        public DefineWellRoleWebPartFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected DefineWellRoleWebPart construct(WebElement el, WebDriver driver)
        {
            return new DefineWellRoleWebPart(el, driver);
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
        protected static final Locator titrationRole(String controlType, String name)
        {
            return Locator.tagWithName("input", "_titrationRole_" + controlType + "_" + name);
        }
    }
}