package MS2;

import org.labkey.api.module.DefaultFolderType;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.view.Portal;
import static org.labkey.api.util.PageFlowUtil.set;
import java.util.Arrays;

public class MS2FolderType extends DefaultFolderType
{
    public MS2FolderType(MS2Module module)
    {
        //TODO: Get rid of these strings.. Should be part of some service
        super("MS2", Arrays.asList(Portal.getPortalPart(MS2Module.MS2_RUNS_ENHANCED_NAME).createWebPart()),
                Arrays.asList(
                    Portal.getPortalPart(MS2Module.MS2_SAMPLE_PREPARATION_RUNS_NAME).createWebPart(),
                    Portal.getPortalPart("Data Pipeline").createWebPart(),
                    Portal.getPortalPart("Narrow Experiments").createWebPart(),
                    Portal.getPortalPart("Narrow Sample Sets").createWebPart(),
                    Portal.getPortalPart("Narrow Protocols").createWebPart()
                    ),
                set(module, ModuleLoader.getInstance().getModule("Pipeline"),
                        ModuleLoader.getInstance().getModule("Experiment"),
                        ModuleLoader.getInstance().getModule("Portal")),
                module);
    }
}
