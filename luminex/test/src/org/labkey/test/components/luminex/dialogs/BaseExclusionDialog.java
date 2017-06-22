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
