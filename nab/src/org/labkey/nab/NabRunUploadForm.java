/*
 * Copyright (c) 2007 LabKey Corporation
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

package org.labkey.nab;

import org.labkey.api.study.actions.AssayRunUploadForm;

/**
 * User: brittp
 * Date: Sep 27, 2007
 * Time: 4:00:02 PM
 */
public class NabRunUploadForm extends AssayRunUploadForm
{
    private Integer _replaceRunId;

    public Integer getReplaceRunId()
    {
        return _replaceRunId;
    }

    public void setReplaceRunId(Integer replaceRunId)
    {
        _replaceRunId = replaceRunId;
    }
}
