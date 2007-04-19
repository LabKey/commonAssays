package org.labkey.biotrue;

import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.query.DefaultSchema;
import org.labkey.biotrue.controllers.BtController;
import org.labkey.biotrue.controllers.BtOverviewWebPart;
import org.labkey.biotrue.query.BtSchema;
import org.labkey.biotrue.task.ScheduledTask;
import org.labkey.biotrue.task.BtThreadPool;

import java.util.Set;
import java.util.Collections;

public class BtModule extends DefaultModule
{
    static public final String NAME = "BioTrue";
    public BtModule()
    {
        super(NAME, 0.03, null, "/biotrue", BtOverviewWebPart.FACTORY);
        addController("biotrue", BtController.class);
        DefaultSchema.registerProvider("biotrue", BtSchema.PROVIDER);
    }
    
    public Set<String> getSchemaNames()
    {
        return Collections.singleton("biotrue");
    }

    public void startup(ModuleContext moduleContext)
    {
        BtThreadPool.get();
        ScheduledTask.startTimer();
    }
}
