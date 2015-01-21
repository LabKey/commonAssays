package org.labkey.test.pages;

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
            labels.add(element.getText());

        return labels;
    }

    public List<String> getYAxis()
    {
        List<String> labels = new ArrayList<>();
        for ( WebElement element : getAxisTextElements(2) )
            labels.add(element.getText());

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
        _test.click( Locator.xpath("//div[contains(@class, 'x-tool-close')]") );
    }

}
