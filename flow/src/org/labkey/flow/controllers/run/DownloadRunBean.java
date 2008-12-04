/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.flow.controllers.run;

import org.labkey.flow.data.FlowRun;

import java.io.File;
import java.util.Map;
import java.util.List;

/**
 * User: kevink
 * Date: Nov 24, 2008 11:14:38 AM
 */
public class DownloadRunBean
{
    public FlowRun run;
    public Map<String, File> files;
    public List<File> missing;

    public DownloadRunBean(FlowRun run, Map<String, File> files, List<File> missing)
    {
        this.run = run;
        this.files = files;
        this.missing = missing;
    }
}
