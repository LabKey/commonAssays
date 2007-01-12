package org.labkey.ms1.pipeline;

import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.view.ViewContext;

import java.util.List;
import java.io.File;

/**
 * Pipeline provider for importing msInspect analysis.
 *  
 * User: jeckels
 * Date: Nov 3, 2006
 */
public class MSInspectPipelineProvider extends PipelineProvider
{
    static String NAME = "msInspect";

    public MSInspectPipelineProvider()
    {
        super(NAME);
    }

    public void updateFileProperties(ViewContext context, List<FileEntry> entries)
    {
        for (FileEntry entry : entries)
        {
            if (!entry.isDirectory())
                continue;

            File dir = new File(entry.getURI());
            addFileActions("ms1", "importMsInspect", "Import msInspect Data",
                    entry, dir.listFiles(new MS1FileFilter()));
        }
    }

}
