package org.labkey.opensso;

import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.security.AuthenticationManager;
import org.labkey.api.security.AuthenticationProvider;
import org.apache.log4j.Logger;

public class OpenSSOModule extends DefaultModule
{
    public static final String NAME = "OpenSSO";
    private static Logger _log = Logger.getLogger(OpenSSOModule.class);

    public OpenSSOModule()
    {
        super(NAME, 2.30, null, false);
        addController("opensso", OpenSSOController.class);
        AuthenticationManager.registerProvider(new OpenSSOProvider());
    }

    public TabDisplayMode getTabDisplayMode()
    {
        return TabDisplayMode.DISPLAY_NEVER;
    }
}