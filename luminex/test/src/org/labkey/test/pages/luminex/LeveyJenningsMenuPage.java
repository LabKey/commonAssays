/*
 * Copyright (c) 2026 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Levey-Jennings Reports menu page (luminex-leveyJenningsMenu.view): the Antigens filter
 * and the Titrations / Single Point Controls lists that link to the Levey-Jennings report.
 */
public class LeveyJenningsMenuPage extends LabKeyPage<LeveyJenningsMenuPage.ElementCache>
{
    public LeveyJenningsMenuPage(WebDriver driver)
    {
        super(driver);
    }

    public static LeveyJenningsMenuPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath, String protocolRowId, @Nullable String analyte)
    {
        Map<String, String> params = new HashMap<>();
        params.put("rowId", protocolRowId);
        if (analyte != null)
            params.put("analyte", analyte);
        webDriverWrapper.beginAt(WebTestHelper.buildURL("luminex", containerPath, "leveyJenningsMenu", params));
        return new LeveyJenningsMenuPage(webDriverWrapper.getDriver());
    }

    @Override
    protected void waitForPage()
    {
        waitForElement(Locators.antigensTable);
    }

    public List<String> getAntigens()
    {
        return getLinkTexts(elementCache().antigensRow);
    }

    public int getAntigenColumnCount()
    {
        return Locator.xpath("./td").findElements(elementCache().antigensRow).size();
    }

    public String getAntigensText()
    {
        return elementCache().antigensRow.getText().trim();
    }

    public LeveyJenningsMenuPage selectAntigen(String antigen)
    {
        clickAndWait(Locator.linkWithText(antigen).findElement(elementCache().antigensRow));
        return new LeveyJenningsMenuPage(getDriver());
    }

    public LeveyJenningsMenuPage clearAntigen()
    {
        clickAndWait(Locator.tagWithAttribute("a", "aria-label", "Clear selection").findElement(elementCache().antigensRow));
        return new LeveyJenningsMenuPage(getDriver());
    }

    public List<String> getTitrations()
    {
        return getLinkTexts(elementCache().titrationsCell);
    }

    public String getTitrationsText()
    {
        return elementCache().titrationsCell.getText().trim();
    }

    public List<String> getSinglePointControls()
    {
        return getLinkTexts(elementCache().singlePointControlsCell);
    }

    // labkey-text-link uppercases its text with CSS, so read textContent rather than the rendered text
    private List<String> getLinkTexts(WebElement container)
    {
        return Locator.tag("a").findElements(container).stream()
                .map(link -> StringUtils.trimToEmpty(link.getDomProperty("textContent")))
                .toList();
    }

    public String getSinglePointControlsText()
    {
        return elementCache().singlePointControlsCell.getText().trim();
    }

    public String getTitrationHref(String titration)
    {
        return Locator.linkWithText(titration).findElement(elementCache().titrationsCell).getAttribute("href");
    }

    public void clickTitration(String titration)
    {
        clickAndWait(Locator.linkWithText(titration).findElement(elementCache().titrationsCell));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<ElementCache>.ElementCache
    {
        final WebElement antigensRow = Locator.xpath(".//tr[2]").findWhenNeeded(Locators.antigensTable.findWhenNeeded(this));
        final WebElement controlsTable = Locators.controlsTable.findWhenNeeded(this);
        final WebElement titrationsCell = Locator.xpath(".//tr[2]/td[1]").findWhenNeeded(controlsTable);
        final WebElement singlePointControlsCell = Locator.xpath(".//tr[2]/td[2]").findWhenNeeded(controlsTable);
    }

    private static class Locators
    {
        static final Locator.XPathLocator antigensTable = Locator.xpath("//table[.//th[normalize-space()='Antigens']]");
        static final Locator.XPathLocator controlsTable = Locator.xpath("//table[.//td[normalize-space()='Titrations']]");
    }
}
