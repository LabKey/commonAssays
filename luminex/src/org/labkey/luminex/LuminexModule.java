package org.labkey.luminex;

import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.apache.log4j.Logger;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class LuminexModule extends DefaultModule implements ContainerManager.ContainerListener
{
    private static final Logger _log = Logger.getLogger(LuminexModule.class);
    public static final String NAME = "Luminex";

    public LuminexModule()
    {
        super(NAME, 8.10, null, true);

        addController("luminex", LuminexController.class);
    }

    public void containerCreated(Container c)
    {
    }

    public void containerDeleted(Container c, User user)
    {
    }

    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
    }

    public void startup(ModuleContext moduleContext)
    {
        super.startup(moduleContext);
        // add a container listener so we'll know when our container is deleted:
        ContainerManager.addContainerListener(this);

        AssayService.get().registerAssayProvider(new LuminexAssayProvider());
        ExperimentService.get().registerExperimentDataHandler(new LuminexExcelDataHandler());
    }

    public Set<String> getSchemaNames()
    {
        return Collections.singleton("luminex");
    }

    public Set<DbSchema> getSchemasToTest()
    {
        return PageFlowUtil.set(LuminexSchema.getSchema());
    }

    public Set<String> getModuleDependencies()
    {
        return Collections.singleton("Experiment");
    }
}