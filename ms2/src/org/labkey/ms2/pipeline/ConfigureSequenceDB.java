/*
 * Copyright (c) 2006-2011 LabKey Corporation
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

package org.labkey.ms2.pipeline;

import org.labkey.api.jsp.FormPage;

/**
 * User: brittp
 * Date: Dec 13, 2005
 * Time: 1:15:47 PM
 */
public abstract class ConfigureSequenceDB extends FormPage<PipelineController.SequenceDBRootForm>
{
    private String _localPathRoot;

    public String getLocalPathRoot()
    {
        return _localPathRoot;
    }

    public void setLocalPathRoot(String localPathRoot)
    {
        _localPathRoot = localPathRoot;
    }
}
