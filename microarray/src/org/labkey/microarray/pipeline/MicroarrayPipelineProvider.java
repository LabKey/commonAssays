package org.labkey.microarray.pipeline;

import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.security.ACL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.labkey.microarray.MicroarrayController;

import java.io.File;
import java.util.List;
import java.sql.SQLException;


public class MicroarrayPipelineProvider extends PipelineProvider
{
    public static final String NAME = "Array";

    public MicroarrayPipelineProvider()
    {
        super(NAME);
    }

    public void updateFileProperties(ViewContext context, List<FileEntry> entries)
    {
        if (!context.hasPermission(ACL.PERM_INSERT))
            return;

        try
        {
            PipeRoot root = PipelineService.get().findPipelineRoot(context.getContainer());
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
                            ActionURL url = MicroarrayController.getUploadRedirectAction(context.getContainer(), protocol, root.relativePath(new File(entry.getURI())));
                            addAction(url, "Import MAGEML using " + protocol.getName(),
                                    entry, files);
                        }
                    }
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

}

