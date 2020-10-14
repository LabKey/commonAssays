package org.labkey.nab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.nab.pipeline.NabPopulateFitParametersPipelineJob;

public class NabUpgradeCode implements UpgradeCode
{
    private static final Logger _log = LogManager.getLogger(NabUpgradeCode.class);

    // Invoked by nab-20.000-20.001.sql
    @SuppressWarnings({"UnusedDeclaration"})
    @DeferredUpgrade
    public static void populateFitParameters(final ModuleContext context)
    {
        if (!context.isNewInstall())
        {
            try
            {
                ViewBackgroundInfo info = new ViewBackgroundInfo(ContainerManager.getRoot(), null, null);
                PipeRoot root = PipelineService.get().findPipelineRoot(ContainerManager.getRoot());
                PipelineService.get().queueJob(new NabPopulateFitParametersPipelineJob(info, root));
            }
            catch (PipelineValidationException e)
            {
                _log.error("Unexpected error during NabPopulateFitParametersPipelineJob", e);
            }
        }
    }
}
