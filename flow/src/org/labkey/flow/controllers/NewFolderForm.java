/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.flow.controllers;

import org.labkey.api.view.ViewForm;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Arrays;

public class NewFolderForm extends ViewForm
{
    public String ff_folderName;
    public Set<String> ff_copyAnalysisScript = Collections.emptySet();
    public boolean ff_copyProtocol;

    public void setFf_folderName(String name)
    {
        ff_folderName = name;
    }

    public void setFf_copyAnalysisScript(String[] analysisScript)
    {
        ff_copyAnalysisScript = new HashSet<String>(Arrays.asList(analysisScript));
    }

    public void setFf_copyProtocol(boolean b)
    {
        ff_copyProtocol = b;
    }
}
