package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.view.ViewContext;
import org.labkey.api.util.FileUtil;
import org.labkey.ms2.pipeline.MS2PipelineManager;

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
            if (TPPTask.isProtXMLFile(f))
            {
                File parent = f.getParentFile();
                String basename = FileUtil.getBaseName(f, 2);
                
                return !fileExists(MS2PipelineManager.getSearchExperimentFile(parent, basename));
            }

            return false;
        }
    }

}
