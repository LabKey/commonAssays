package cpas.ms2.pipeline;

import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.view.ViewContext;
import org.labkey.api.ms2.pipeline.MS2PipelineManager;

import java.io.File;
import java.io.FileFilter;
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

            File dir = new File(entry.getURI());
            addFileActions("MS2", "importProteinProphet", "Import ProteinProphet",
                    entry, dir.listFiles(new ProteinProphetFilenameFilter()));
        }
    }


    private static class ProteinProphetFilenameFilter implements FileFilter
    {
        public boolean accept(File f)
        {
            return MS2PipelineManager.isProtXMLFile(f);
        }
    }

}
