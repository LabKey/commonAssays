/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
package org.labkey.test.pages.luminex;

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SetAnalyteDefaultValuesPage
{
    private BaseWebDriverTest _test;

    public SetAnalyteDefaultValuesPage(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void importDefaults(File defaultsFile)
    {
        importDefaultsExpectError(defaultsFile, null);
    }

    public void importDefaultsExpectError(File defaultsFile, String errorText)
    {
        ensureOnImportPage();
        _test.click(Locator.tagWithClass("h3", "panel-title").containing("Copy/paste text"));
        _test.click(Locator.tagWithClass("h3", "panel-title").containing("Upload file"));
        _test.setFormElement(Locator.name("file"), defaultsFile);
        submitDefaults(errorText);
    }

    private void ensureOnImportPage()
    {
        if (!_test.isElementPresent(Locator.pageHeader("Import Default Values")))
        {
            _test.clickButton("Import Data");
        }
    }

    private void submitDefaults(String errorText)
    {
        if (errorText != null)
        {
            _test.clickButton("Submit", 0);
            _test.waitForElement(Locators.labkeyError.withText(errorText));
        }
        else
        {
            _test.clickButton("Submit");
        }
    }

    public List<AnalyteDefault> getAnalyteDefaults()
    {
        List<WebElement> defaultValueRows = Locator.id("defaultValues").append(Locator.tag("tr").withAttribute("id")).findElements(_test.getDriver());
        List<AnalyteDefault> defaultValues = new ArrayList<>();

        for (WebElement row : defaultValueRows)
        {
            String analyteName = Locator.name("analytes").findElement(row).getAttribute("value");
            String positivityThresholdValue = Locator.name("positivityThresholds").findElement(row).getAttribute("value");
            String negativeBead = Locator.name("negativeBeads").findElement(row).getAttribute("value");

            Integer positivityThreshold = null;

            try
            {
                positivityThreshold = Integer.parseInt(positivityThresholdValue);
            }
            catch (NumberFormatException ex)
            {
                Assert.fail("Illegal positivity threshold for '" + analyteName + "': " + positivityThresholdValue);
            }

            defaultValues.add(new AnalyteDefault(analyteName, positivityThreshold, negativeBead));
        }

        return defaultValues;
    }

    public static class AnalyteDefault
    {
        public static final Integer DEFAULT_POSITIVITY_THRESHOLD = 100;

        private String _analyteName;
        private Integer _positivityThreshold;
        private String _negativeBead;

        public AnalyteDefault(String analyteName)
        {
            this(analyteName, DEFAULT_POSITIVITY_THRESHOLD);
        }

        public AnalyteDefault(String analyteName, Integer positivityThreshold)
        {
            this(analyteName, positivityThreshold, "");
        }

        public AnalyteDefault(String analyteName, String negativeBead)
        {
            this(analyteName, DEFAULT_POSITIVITY_THRESHOLD, negativeBead);
        }

        public AnalyteDefault(String analyteName, Integer positivityThreshold, String negativeBead)
        {
            _analyteName = analyteName;
            _positivityThreshold = positivityThreshold;
            _negativeBead = negativeBead;
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof AnalyteDefault &&
                    _analyteName.equals(((AnalyteDefault) obj).getAnalyteName()) &&
                    _positivityThreshold.equals(((AnalyteDefault) obj).getPositivityThreshold()) &&
                    _negativeBead.equals(((AnalyteDefault) obj).getNegativeBead());
        }

        @Override
        public int hashCode()
        {
            int result = _analyteName != null ? _analyteName.hashCode() : 0;
            result = 31 * result + (_positivityThreshold != null ? _positivityThreshold.hashCode() : 0);
            result = 31 * result + (_negativeBead != null ? _negativeBead.hashCode() : 0);
            return result;
        }

        public String getAnalyteName()
        {
            return _analyteName;
        }

        public Integer getPositivityThreshold()
        {
            return _positivityThreshold;
        }

        public String getNegativeBead()
        {
            return _negativeBead;
        }

        @Override
        public String toString()
        {
            return String.format("[%s][%d][%s]", _analyteName, _positivityThreshold, _negativeBead);
        }
    }
}
