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

import java.io.File;

public class RunPropertiesWebPart extends WebPartPanel
{
    private static final String TITLE = "Run Properties";
    public static final String ASSAY_ID_FIELD  = "name";
    public static final String ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD = "__primaryFile__";
    private Elements _elements;

    protected RunPropertiesWebPart(WebElement componentElement, WebDriver driver)
    {
        super(componentElement, driver);
    }

    public static RunPropertiesWebPartFinder RunPropertiesWebPart(WebDriver driver)
    {
        return new RunPropertiesWebPartFinder(driver).withTitle(TITLE);
    }

    public void addFilesToAssayRun(File firstFile, File... additionalFiles)
    {
        getWrapper().setFormElement(Locator.name(ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD), firstFile);

        int index = 1;
        for (File additionalFile : additionalFiles)
        {
            getWrapper().sleep(500);
            getWrapper().click(Locators.plusButton);

            String fieldName = ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD + (index++);
            getWrapper().setFormElement(Locator.name(fieldName), additionalFile);
        }
    }

    /*Note: Do not use for multiple file replacement*/
    public void replaceFileInAssayRun(File original, File newFile)
    {
        //Add spot for new file
        getWrapper().click(Locators.plusButton);
        //remove old file
        getWrapper().click(Locators.minusButton(original.getName()));

        String fieldName = ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD;
        getWrapper().setFormElement(Locator.name(fieldName), newFile);
    }

    public void setRunId(String runId)
    {
        getWrapper().setFormElement(Locator.inputById(ASSAY_ID_FIELD), runId);
    }

    public static class RunPropertiesWebPartFinder extends WebPartFinder<RunPropertiesWebPart, RunPropertiesWebPartFinder>
    {
        public RunPropertiesWebPartFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected RunPropertiesWebPart construct(WebElement el, WebDriver driver)
        {
            return new RunPropertiesWebPart(el, driver);
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
        protected static final Locator plusButton = Locator.xpath("//a[contains(@class, 'labkey-file-add-icon-enabled')]");
        protected static Locator minusButton(String fileName)
        {
            return Locator.xpath("//tr[td/span/text() = '" + fileName + "']" +
                    "/td/a[contains(@class, 'labkey-file-remove-icon-enabled')]");
        }
    }
}