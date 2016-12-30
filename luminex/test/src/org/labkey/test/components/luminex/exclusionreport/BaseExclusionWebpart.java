package org.labkey.test.components.luminex.exclusionreport;

import org.labkey.test.components.WebPartPanel;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Created by iansigmon on 12/29/16.
 */
public abstract class BaseExclusionWebpart extends WebPartPanel
{
    protected BaseExclusionWebpart(WebElement componentElement, WebDriver driver)
    {
        super(componentElement, driver);
    }

    protected DataRegionTable getTable()
    {
        return new DataRegionTable(getTableName(), getDriver());
    }

    protected abstract String getTableName();
}
