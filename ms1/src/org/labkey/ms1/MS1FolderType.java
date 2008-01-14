package org.labkey.ms1;

import org.labkey.api.module.DefaultFolderType;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.view.Portal;
import static org.labkey.api.util.PageFlowUtil.set;

import java.util.Arrays;

/**
 * Implements an MS1 Folder type
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Nov 2, 2007
 * Time: 11:11:50 AM
 */
public class MS1FolderType extends DefaultFolderType
{
    public MS1FolderType(MS1Module module)
    {
        super("MS1",
                "View and analyze MS1 quantitation results along with MS2 data.",
            Arrays.asList(
                Portal.getPortalPart("Data Pipeline").createWebPart(),
                Portal.getPortalPart("MS1 Runs").createWebPart()
            ), null,
            getDefaultModuleSet(module, getModule("MS2"), getModule("Pipeline")),
            module);
    }
}
