/*
 * Copyright (c) 2015-2016 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by aaronr on 1/20/15.
 */
public class LeveyJenningsPlotWindow
{
    private BaseWebDriverTest _test;
    private final String divCls = "ljplotdiv";

    public LeveyJenningsPlotWindow(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void waitTillReady()
    {
        _test.waitForText("Levey-Jennings Plot");
        // note this is the only place ticklabel is used... consider fixing this and dropping the class from plot.js
        _test.waitForElement( Locator.xpath("//*[contains(@class, 'ticklabel')]") );
    }

    private Locator.XPathLocator getSvgRoot()
    {
        return Locator.xpath("//div[@class='ljplotdiv']/*");
    }

    private Locator.XPathLocator getLabels()
    {
        return getSvgRoot().append( Locator.xpath("/*[@class='ext-gen33-labels']") );
    }

    public String getTitle()
    {
        WebElement el = getLabels().append( Locator.xpath("/*[1]") ).findElement(_test.getDriver());
        return el.getText();
    }

    public String getXTitle()
    {
        WebElement el = getLabels().append( Locator.xpath("/*[2]") ).findElement(_test.getDriver());
        return el.getText();
    }

    public String getYTitle()
    {
        WebElement el = getLabels().append( Locator.xpath("/*[3]") ).findElement(_test.getDriver());
        return el.getText();
    }

    private List<WebElement> getAxisTextElements(int index)
    {
        Locator.XPathLocator xAxisLocator = getSvgRoot().append( Locator.xpath("/*[@class='axis']["+index+"]/*[@class='tick-text']/*/*") );
        return xAxisLocator.findElements(_test.getDriver());
    }

    public List<String> getXAxis()
    {
        List<String> labels = new ArrayList<>();
        for ( WebElement element : getAxisTextElements(1) )
        {
            String text = element.getText();
            if (!text.equals(""))
                labels.add(text);
        }

        return labels;
    }

    public List<String> getYAxis()
    {
        List<String> labels = new ArrayList<>();
        for ( WebElement element : getAxisTextElements(2) )
        {
            String text = element.getText();
            if (!text.equals(""))
                labels.add(text);
        }

        return labels;
    }

    public String getXTickTagElementText()
    {
        for ( WebElement element : getAxisTextElements(1) )
        {
            String cls = element.getAttribute("class");
            if (cls != null && Arrays.asList(cls.split(" ")).contains("xticktag") )
                return element.getText();
        }
        return null;
    }

    public void closeWindow()
    {
        _test.click(Locator.xpath("//div[contains(@class, 'x-tool-close')]"));
    }

}
