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
package org.labkey.test.pages.luminex;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.luminex.importwizard.AnalytePropertiesWebPart;
import org.labkey.test.components.luminex.importwizard.BatchPropertiesWebPart;
import org.labkey.test.components.luminex.importwizard.DefineWellRoleWebPart;
import org.labkey.test.components.luminex.importwizard.RunPropertiesWebPart;
import org.labkey.test.pages.LabKeyPage;

import java.io.File;
import java.util.function.Consumer;

import static org.labkey.test.components.luminex.importwizard.AnalytePropertiesWebPart.AnalytePropertiesWebPart;
import static org.labkey.test.components.luminex.importwizard.BatchPropertiesWebPart.BatchPropertiesWebPart;
import static org.labkey.test.components.luminex.importwizard.DefineWellRoleWebPart.DefineWellRoleWebPart;
import static org.labkey.test.components.luminex.importwizard.RunPropertiesWebPart.RunPropertiesWebPart;

/**
 * Created by iansigmon on 12/28/16.
 */
public class LuminexImportWizard extends LabKeyPage<LuminexImportWizard.Elements>
{
    public static final String ASSAY_ID_FIELD  = "name";


    Elements _elements;
    public LuminexImportWizard(WebDriverWrapper driver)
    {
        super(driver);
    }

    /*
        preCondition: starts from Luminex Assay Run or Assay Data table
     */
    public void startImport()
    {
        clickButton("Import Data");
    }

    public void clickNext()
    {
        clickButton("Next");
    }

    public void clickCancel()
    {
        clickButton("Cancel");
    }

    public void clickResetDefaultValues()
    {
        clickButton("Reset Default Values");
    }

    public void setRunId(String runId)
    {
        elements().runProperties.setRunId(runId);
    }

    public void addFilesToAssayRun(File firstFile, File... additionalFiles)
    {
        elements().runProperties.addFilesToAssayRun(firstFile, additionalFiles);
    }

    /*Note: Do not use for multiple file replacement*/
    public void replaceFileInAssayRun(File original, File newFile)
    {
        elements().runProperties.replaceFileInAssayRun(original, newFile);
    }

    public void setStandardRole(String name, boolean checked)
    {
        elements().defineWellRoles.setStandardTitrationRole(name, checked);
    }

    public void setQCControlRole(String name, boolean checked)
    {
        elements().defineWellRoles.setQCControlTitrationRole(name, checked);
    }

    public void setOtherControlRole(String name, boolean checked)
    {
        elements().defineWellRoles.setOtherControlTitrationRole(name, checked);
    }

    public Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    public void checkParticipantVisitResolver()
    {
        elements().batchproperties.checkSampleInfo();
    }

    /**
     * Create a new luminex assay run
     * Run from the Assay Run or Assay Results page as starting point
     * @param runId name of run
     * @param step1 function that takes this wizard and sets step1 fields, can be null if no fields are to be set
     * @param step2 function that takes this wizard and sets step2 fields, can be null if no fields are to be set
     *              (will fail since Run Data files are required)
     * @param step3 function that takes this wizard and sets step3 fields, can be null if no fields are to be set
     */
    public void createNewAssayRun( String runId, Consumer<LuminexImportWizard> step1, Consumer<LuminexImportWizard> step2, Consumer<LuminexImportWizard> step3)
    {
        startImport();
        checkParticipantVisitResolver();

        if (step1 != null)
            step1.accept(this);
        clickNext();

        setFormElement(Locator.name(ASSAY_ID_FIELD), runId);
        if (step2 != null)
            step2.accept(this);
        clickNext();

        if (step3 != null)
            step3.accept(this);
        clickButton("Save and Finish", longWaitForPage);
    }

    public class Elements extends LabKeyPage.ElementCache
    {
        //Page 1 WebParts
        final BatchPropertiesWebPart batchproperties = BatchPropertiesWebPart(getDriver()).findWhenNeeded();

        //Page 2 WebParts
        final RunPropertiesWebPart runProperties = RunPropertiesWebPart(getDriver()).findWhenNeeded();

        //Page 3 WebParts
        final DefineWellRoleWebPart defineWellRoles = DefineWellRoleWebPart(getDriver()).findWhenNeeded();
        final AnalytePropertiesWebPart analyteProperties = AnalytePropertiesWebPart(getDriver()).findWhenNeeded();
    }

    public static class Locators extends org.labkey.test.Locators
    {
    }
}
