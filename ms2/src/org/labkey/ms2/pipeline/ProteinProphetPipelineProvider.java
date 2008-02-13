package org.labkey.ms2.pipeline;

import org.labkey.api.exp.pipeline.XarGeneratorId;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.view.ViewContext;
import org.labkey.api.util.FileType;

import java.io.File;
import java.util.List;

/**
 * User: jeckels
 * Date: Feb 17, 2006
 */
public class ProteinProphetPipelineProvider extends PipelineProvider
{
    static final String NAME = "ProteinProphet";

    public ProteinProphetPipelineProvider()
    {
        super(NAME);
    }

    public void updateFileProperties(ViewContext context, List<FileEntry> entries)
    {
        for (FileEntry entry : entries)
        {
            if (!entry.isDirectory())
                continue;

            addFileActions("MS2", "importProteinProphet", "Import ProteinProphet",
                    entry, entry.listFiles(new ProteinProphetFilenameFilter()));
        }
    }


    private static class ProteinProphetFilenameFilter extends FileEntryFilter
    {
        public boolean accept(File f)
        {
            FileType fileType = TPPTask.getProtXMLFileType(f);
            if (fileType != null)
            {
                File parent = f.getParentFile();
                String basename = fileType.getBaseName(f);
                
                return !fileExists(XarGeneratorId.FT_SEARCH_XAR.newFile(parent, basename));
            }

            return false;
        }
    }

}
