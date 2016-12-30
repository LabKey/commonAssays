package org.labkey.test.components.luminex.dialogs;

import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

/**
 * Created by iansigmon on 12/29/16.
 */
public abstract class BaseExclusionDialog extends LabKeyPage
{
    protected static final String MENU_BUTTON = "Exclusions";

    public BaseExclusionDialog(WebDriver driver)
    {
        super(driver);
    }

    protected abstract void openDialog();
}
