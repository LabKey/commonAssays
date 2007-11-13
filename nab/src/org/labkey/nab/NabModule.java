package org.labkey.nab;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.security.User;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;

import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Copyright (C) 2004 Fred Hutchinson Cancer Research Center. All Rights Reserved.
 * User: migra
 * Date: Feb 15, 2006
 * Time: 10:39:44 PM
 */
public class NabModule extends DefaultModule implements ContainerManager.ContainerListener
{
    private static final String NAME = "Nab";

    public NabModule()
    {
        super(NAME, 2.20, null, false);
        addController("Nab", NabController.class);
        addController("NabAssay", NabAssayController.class);
    }

    //void wantsToDelete(Container c, List<String> messages);
    public void containerCreated(Container c)
    {
    }

    public void containerDeleted(Container c, User user)
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
        PlateService.get().registerDetailsLinkResolver(new PlateService.PlateDetailsResolver()
        {
            public ViewURLHelper getDetailsURL(Plate plate)
            {
                // for 2.0, we'll accept all plate types: only NAB uses the plate service.
                ViewURLHelper url = new ViewURLHelper("Nab", "display", plate.getContainer());
                url.addParameter("rowId", "" + plate.getRowId());
                return url;
            }
        });

        PlateService.get().registerPlateTypeHandler(new NabPlateTypeHandler());
        AssayService.get().registerAssayProvider(new NabAssayProvider());
        ExperimentService.get().registerExperimentDataHandler(new NabDataHandler());
    }


    @Override
    public Set<String> getModuleDependencies()
    {
        Set<String> result = new HashSet<String>();
        result.add("Study");
        return result;
    }

    public ViewURLHelper getTabURL(HttpServletRequest request, Container c, User user)
    {
        ViewURLHelper defaultURL = super.getTabURL(request, c, user);

        // this is a bit of a hack: while we're supporting both old and new assay-based NAB
        // implementations, it's less confusing to the user if the NAB tab keeps them from switching
        // from the new implementation to the old, so we swap out the pageflow of the tab URL:
        ViewContext context = HttpView.getRootContext();
        String pageFlow = context != null ? context.getViewURLHelper().getPageFlow() : null;
        if ("assay".equals(pageFlow) || "NabAssay".equals(pageFlow))
            defaultURL.setPageFlow("assay");

        return defaultURL;
    }
}
