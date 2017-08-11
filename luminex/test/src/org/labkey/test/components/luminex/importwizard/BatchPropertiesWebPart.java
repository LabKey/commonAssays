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