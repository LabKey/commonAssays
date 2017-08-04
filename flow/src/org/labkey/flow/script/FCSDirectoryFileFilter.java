package org.labkey.flow.script;

import org.labkey.flow.analysis.model.FCS;

import java.io.File;
import java.io.FileFilter;

public class FCSDirectoryFileFilter implements FileFilter
{
    @Override
    public boolean accept(File dir)
    {
        if (!dir.isDirectory())
            return false;

        File[] fcsFiles = dir.listFiles((FileFilter) FCS.FCSFILTER);
        return null != fcsFiles && 0 != fcsFiles.length;
    }
}
