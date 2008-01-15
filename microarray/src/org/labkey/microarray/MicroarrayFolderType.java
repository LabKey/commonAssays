package org.labkey.microarray;

import org.labkey.api.module.DefaultFolderType;
import org.labkey.api.view.Portal;

import java.util.Arrays;

/**
 * User: jeckels
 * Date: Jan 2, 2008
 */
public class MicroarrayFolderType extends DefaultFolderType
{
    public MicroarrayFolderType(MicroarrayModule module)
    {
        super("Microarray",
                "Import and analyze microarray data",
            Arrays.asList(
                Portal.getPortalPart("Data Pipeline").createWebPart(),
                Portal.getPortalPart(MicroarrayModule.WEBPART_MICROARRAY_RUNS).createWebPart(),
                Portal.getPortalPart(MicroarrayModule.WEBPART_PENDING_FILES).createWebPart()
            ), null,
            getDefaultModuleSet(module, getModule(MicroarrayModule.NAME), getModule("Pipeline")),
            module);
    }
}
