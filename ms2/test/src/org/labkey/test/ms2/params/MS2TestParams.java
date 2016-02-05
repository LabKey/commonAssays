/*
 * Copyright (c) 2007-2016 LabKey Corporation
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
package org.labkey.test.ms2.params;

import org.labkey.test.Locator;
import org.labkey.test.pipeline.AbstractPipelineTestParams;
import org.labkey.test.pipeline.PipelineWebTestBase;

public class MS2TestParams extends AbstractPipelineTestParams
{
    public MS2TestParams(PipelineWebTestBase test, String dataPath, String protocolName, String... sampleNames)
    {
        super(test, dataPath, "xtandem", protocolName, sampleNames);

        setParametersFile("tandem.xml");
        if (sampleNames.length == 0)
        {
            // todo: more info on inputs and outputs
            setOutputExtensions(".pep.xml", ".prot.xml");
        }
        else
        {
            setInputExtensions(".mzXML");
            setOutputExtensions(".xtan.xml", ".pep.xml", ".prot.xml");
        }
    }

    public String getExperimentRunTableName()
    {
        return "MS2SearchRuns";
    }

    public void clickActionButton()
    {
        _test.log("X! Tandem Search");
        _test._fileBrowserHelper.selectImportDataAction("X!Tandem Peptide Search");
    }

    protected void setGrouping(String grouping)
    {
        _test.log("Set grouping to " + grouping);
        _test.selectOptionByText(Locator.name("grouping"), grouping);
        _test.clickAndWait(Locator.id("viewTypeSubmitButton"));
    }

    @Override
    protected void clickSubmitButton()
    {
        _test.clickAndWait(Locator.id("button_Search"));
    }
}
