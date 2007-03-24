package org.labkey.flow.webparts;

import org.labkey.api.module.DefaultFolderType;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.security.User;
import org.labkey.flow.controllers.FlowModule;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;

public class FlowFolderType extends DefaultFolderType
{
    public FlowFolderType(FlowModule module)
    {
        super("Flow", Arrays.asList(OverviewWebPart.FACTORY.createWebPart()),
                Arrays.asList(AnalysesWebPart.FACTORY.createWebPart(),
                        AnalysisScriptsWebPart.FACTORY.createWebPart()),
                module.getActiveModulesForOwnedFolder(), module);
    }

    public ViewURLHelper getStartURL(HttpServletRequest request, Container c, User user)
    {
        ViewURLHelper ret = super.getStartURL(request, c, user);
        ret.replaceParameter(DataRegion.LAST_FILTER_PARAM, "true");
        return ret;
    }
}
