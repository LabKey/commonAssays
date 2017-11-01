/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
package org.labkey.test.ms2;

import org.labkey.test.TestTimeoutException;

public abstract class AbstractXTandemTest extends AbstractMS2SearchEngineTest
{
    protected static final String SEARCH_BUTTON = "X!Tandem";
    protected static final String SEARCH_TYPE = "xtandem";

    protected static final String PEPTIDE = "K.LLASMLAK.A";
    protected static final String PEPTIDE2 = "K.EEEESDEDMGFG.-";
    protected static final String PEPTIDE3 = "K.GSDSLSDGPACKR.S";
    protected static final String PEPTIDE4 = "K.EEEESDEDMGFG.-";
    protected static final String PEPTIDE5 = "K.LHRIEAGVMPR.N";
    protected static final String PROTEIN = "gi|18311790|phosphoribosylfor";
    
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        cleanPipe(SEARCH_TYPE);
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    protected void setupEngine()
    {
        log("Analyze " + SEARCH_BUTTON + " sample data.");
        sleep(1500);
        _fileBrowserHelper.selectImportDataAction(SEARCH_BUTTON +  " Peptide Search");
    }

}
