/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.test.utils.ms2;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;

import java.io.File;

public class Ms2DataRegionExportHelper extends DataRegionExportHelper
{
    public Ms2DataRegionExportHelper(DataRegionTable drt)
    {
        super(drt);
    }

    public File exportText()
    {
        return exportText(FileDownloadType.TSV, null);
    }

    public File exportText(FileDownloadType fileType, @Nullable Boolean exportSelected)
    {
        if (null != exportSelected && exportSelected)
        {
            return getWrapper().doAndWaitForDownload(() -> getDataRegionTable().clickHeaderMenu("Export Selected", false, fileType.toString()));
        }
        else
        {
            return getWrapper().doAndWaitForDownload(() -> getDataRegionTable().clickHeaderMenu("Export All", false, fileType.toString()));
        }

    }

    public enum FileDownloadType
    {
        EXCEL("Excel"),
        TSV("TSV"),
        AMT("AMT"),
        MS2("MS2 Ions TSV"),
        BIBLIOSPEC("Bibliospec");

        private String _label;

        private FileDownloadType(String label)
        {
            _label = label;
        }
    }
}
