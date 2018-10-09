package org.labkey.flow;

import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.module.ModuleContext;
import org.labkey.flow.persist.FlowManager;

public class FlowUpgradeCode implements UpgradeCode
{
    // called from flow-18.20-18.21.sql
    @DeferredUpgrade // must run after startup so the FlowProperty.FileDate has been created
    public static void addFileDate(ModuleContext context)
    {
        if (context.isNewInstall())
            return;

        FlowManager.get().setFileDateForAllFCSFiles(context.getUpgradeUser());
    }
}
