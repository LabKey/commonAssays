package org.labkey.microarray.pipeline;

import org.apache.log4j.Logger;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.security.ACL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.microarray.assay.MicroarrayAssayProvider;

import java.io.File;
import java.util.List;


public class MicroarrayPipelineProvider extends PipelineProvider
{
    private static final Logger _log = Logger.getLogger(MicroarrayPipelineProvider.class);

    public static String name = "Array";

    public MicroarrayPipelineProvider()
    {
        super(name);
    }

    public void updateFileProperties(ViewContext context, List<FileEntry> entries)
    {
        if (!context.hasPermission(ACL.PERM_INSERT))
            return;

        try
        {
            PipeRoot root = PipelineService.get().findPipelineRoot(context.getContainer());
            File rootDir = root.getRootPath();
            for (FileEntry entry : entries)
            {
                File[] files = entry.listFiles(ArrayPipelineManager.getImageFileFilter());
                if (files != null)
                    addAction("Microarray", "importImageFiles", "Import Images",
                            entry, files);
                
                files = entry.listFiles(ArrayPipelineManager.getMageFileFilter());
                if (files != null)
                {
                    for (ExpProtocol protocol : AssayService.get().getAssayProtocols(context.getContainer()))
                    {
                        if (AssayService.get().getProvider(protocol) instanceof MicroarrayAssayProvider)
                        {
                            ViewURLHelper url = AssayService.get().getUploadWizardURL(context.getContainer(), protocol);
                            url.addParameter(".pipelinePath", root.relativePath(new File(entry.getURI())));
                            addAction(url, "Import MAGEML using " + protocol.getName(),
                                    entry, files);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            _log.error("Exception", e);
        }
    }

}

