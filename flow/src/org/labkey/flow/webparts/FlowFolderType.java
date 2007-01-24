package org.labkey.flow.webparts;

import org.labkey.api.module.DefaultFolderType;
import org.labkey.flow.controllers.FlowModule;

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
}
