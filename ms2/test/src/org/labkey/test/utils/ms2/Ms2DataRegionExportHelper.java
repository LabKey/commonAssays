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
            return getTest().doAndWaitForDownload(() -> getDataRegionTable().clickHeaderMenu("Export Selected", false, fileType.toString()));
        }
        else
        {
            return getTest().doAndWaitForDownload(() -> getDataRegionTable().clickHeaderMenu("Export All", false, fileType.toString()));
        }

    }

    public static enum FileDownloadType
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
