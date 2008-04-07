package org.labkey.elispot;

import org.apache.log4j.Logger;
import org.labkey.api.data.*;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.security.User;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.ViewContext;
import org.labkey.elispot.plate.ElispotPlateReaderService;
import org.labkey.elispot.plate.ExcelPlateReader;
import org.labkey.elispot.plate.TextPlateReader;

import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class ElispotModule extends DefaultModule implements ContainerManager.ContainerListener
{
    public static final String NAME = "ELISpotAssay";
    private static final Logger _log = Logger.getLogger(ElispotModule.class);

    public ElispotModule()
    {
        super(NAME, 0.01, null, false);
        addController("elispot-assay", ElispotController.class);
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

        PlateService.get().registerPlateTypeHandler(new ElispotPlateTypeHandler());
        ExperimentService.get().registerExperimentDataHandler(new ElispotDataHandler());
        AssayService.get().registerAssayProvider(new ElispotAssayProvider());

        ElispotPlateReaderService.registerProvider(new ExcelPlateReader());
        ElispotPlateReaderService.registerProvider(new TextPlateReader());
    }

    public Set<String> getModuleDependencies()
    {
        return Collections.singleton("Experiment");
    }

    public void afterSchemaUpdate(ModuleContext moduleContext, ViewContext viewContext)
    {
        // Issue #5689, module was renamed to avoid a collision with a 3rd party module,
        // need to clean up the module entry
        if (moduleContext.getModuleState() == ModuleLoader.ModuleState.InstallRequired)
        {
            try {
                SimpleFilter filter = new SimpleFilter("Name", "Elispot");
                filter.addCondition("ClassName", ElispotModule.class.getName());
                Table.delete(CoreSchema.getInstance().getTableInfoModules(), filter);
            }
            catch (SQLException e)
            {
                _log.error("Unable to remove old Elispot module entry", e);
            }
        }
    }
}