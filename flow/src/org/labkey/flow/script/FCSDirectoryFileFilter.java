/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.flow.script;

import org.labkey.api.pipeline.file.FileAnalysisTaskPipeline;
import org.labkey.flow.analysis.model.FCS;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;

public class FCSDirectoryFileFilter implements FileAnalysisTaskPipeline.FilePathFilter
{
    @Override
    public boolean accept(Path path)
    {
        return accept(path.toFile());
    }

    @Override
    public boolean accept(File dir)
    {
        if (!dir.isDirectory())
            return false;

        File[] fcsFiles = dir.listFiles((FileFilter) FCS.FCSFILTER);
        return null != fcsFiles && 0 != fcsFiles.length;
    }
}
