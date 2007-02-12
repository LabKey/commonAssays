package org.labkey.nab;

import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.AssayService;
import org.labkey.api.view.ViewContext;
import org.labkey.nab.NabController;
import org.labkey.nab.NabManager;

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
        super("Nab", 1.70, null, null);
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
