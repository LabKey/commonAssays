package org.labkey.microarray;

import org.labkey.api.module.DefaultFolderType;
import org.labkey.api.view.Portal;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.exp.api.ExperimentService;

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
                Portal.getPortalPart(MicroarrayModule.WEBPART_PENDING_FILES).createWebPart(),
                Portal.getPortalPart(MicroarrayModule.WEBPART_MICROARRAY_STATISTICS).createWebPart()
            ),
            Arrays.asList(
                Portal.getPortalPart(MicroarrayModule.WEBPART_MICROARRAY_RUNS).createWebPart(),
                Portal.getPortalPart("Assay Details").createWebPart(),
                Portal.getPortalPart("Assay List").createWebPart()
            ),
            getDefaultModuleSet(module, getModule(MicroarrayModule.NAME), getModule(PipelineService.MODULE_NAME), getModule(ExperimentService.MODULE_NAME)),
            module);
    }
}
