package org.labkey.cabig;

import org.apache.log4j.Logger;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.security.SecurityManager;
import org.labkey.api.util.PageFlowUtil;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

public class caBIGModule extends DefaultModule
{
    private static final Logger _log = Logger.getLogger(caBIGModule.class);
    public static final String NAME = "caBIG";

    private static final String[] RECOGNIZED_PAGEFLOWS = new String[]{"cabig"};

    public caBIGModule()
    {
        super(NAME, 0.01, null, "/cabig");
        addController("cabig", caBIGController.class);
    }

    public void startup(ModuleContext moduleContext)
    {
        super.startup(moduleContext);
        // add a container listener to unpublish containers when they're deleted
        ContainerManager.addContainerListener(caBIGManager.get());
        SecurityManager.addPermissionsView(caBIGController.caBIGPermissionsView.class);
    }

    public Set<String> getSchemaNames()
    {
        return Collections.singleton("cabig");
    }

/*    public Set<DbSchema> getSchemasToTest()
    {
        return PageFlowUtil.set(caBIGSchema.getInstance().getSchema());
    }
    */ // TODO: Need to add caBIG.xml


    @Override
    public Set<String> getModuleDependencies()
    {
        Set<String> result = new HashSet<String>();
        result.add("Experiment");
        return result;
    }
}