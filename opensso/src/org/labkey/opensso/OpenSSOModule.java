package org.labkey.opensso;

import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;

public class OpenSSOModule extends DefaultModule
{
    public static final String NAME = "OpenSSO";

    public OpenSSOModule()
    {
        super(NAME, 0.01, null, false);
        addController("opensso", OpenSSOController.class);
    }

    public void startup(ModuleContext moduleContext)
    {
        super.startup(moduleContext);
    }
}