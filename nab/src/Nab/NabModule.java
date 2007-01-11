package Nab;

import org.fhcrc.cpas.module.DefaultModule;
import org.fhcrc.cpas.module.ModuleContext;
import org.fhcrc.cpas.data.Container;
import org.fhcrc.cpas.data.ContainerManager;
import org.fhcrc.cpas.data.RuntimeSQLException;
import org.fhcrc.cpas.study.PlateService;
import org.fhcrc.cpas.study.AssayService;
import org.fhcrc.cpas.view.ViewContext;
import cpas.assays.nab.NabManager;

import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.*;

/**
 * Copyright (C) 2004 Fred Hutchinson Cancer Research Center. All Rights Reserved.
 * User: migra
 * Date: Feb 15, 2006
 * Time: 10:39:44 PM
 */
public class NabModule extends DefaultModule implements ContainerManager.ContainerListener
{
    public NabModule()
    {
        super("Nab", 1.70, null);
        addController("Nab", NabController.class);
    }

    //void wantsToDelete(Container c, List<String> messages);
    public void containerCreated(Container c)
    {
    }

    public void containerDeleted(Container c)
    {
        try
        {
            NabManager.get().deleteContainerData(c);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
    }


    @Override
    public void afterSchemaUpdate(ModuleContext moduleContext, ViewContext viewContext)
    {

        double version = moduleContext.getInstalledVersion();
        if (version > 0 && version <= 1.6)
        {
            try
            {
                List<Container> containers = AssayService.get().getAssayContainers();
                for (Container container : containers)
                    NabManager.get().convert(viewContext.getUser(), container);
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
    }

    @Override
    public void startup(ModuleContext moduleContext)
    {
        PlateService.get().registerDetailsLink(NabManager.PLATE_TEMPLATE_NAME, "Nab", "display");
    }


    @Override
    public Set<String> getModuleDependencies()
    {
        Set<String> result = new HashSet<String>();
        result.add("Study");
        return result;
    }
}
