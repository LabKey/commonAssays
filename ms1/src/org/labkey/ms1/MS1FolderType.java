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
            Arrays.asList(
                Portal.getPortalPart("Data Pipeline").createWebPart(),
                Portal.getPortalPart("MS1 Runs").createWebPart()
            ), null,
            set(module, ModuleLoader.getInstance().getModule("Pipeline"),
                    ModuleLoader.getInstance().getModule("Experiment"),
                    ModuleLoader.getInstance().getModule("Portal"),
                    ModuleLoader.getInstance().getModule("MS2")),
            module);
    }
}
