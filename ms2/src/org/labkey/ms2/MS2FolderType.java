package org.labkey.ms2;

import org.labkey.api.module.DefaultFolderType;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import static org.labkey.api.util.PageFlowUtil.set;
import org.labkey.ms2.search.ProteinSearchWebPart;

import java.util.Arrays;

public class MS2FolderType extends DefaultFolderType
{
    public MS2FolderType(MS2Module module)
    {
        //TODO: Get rid of these strings.. Should be part of some service
        super("MS2",
                "Manage tandem mass spectrometry analyses using a variety of popular search engines, " +
                        "including Mascot, Sequest, and X!Tandem. " +
                        "Use existing analytic tools like PeptideProphet and ProteinProphet.",
            Arrays.asList(
                Portal.getPortalPart("Data Pipeline").createWebPart(),
                Portal.getPortalPart(MS2Module.MS2_RUNS_ENHANCED_NAME).createWebPart()
            ),
            Arrays.asList(
                Portal.getPortalPart(ProteinSearchWebPart.NAME).createWebPart(),
                Portal.getPortalPart(MS2Module.MS2_SAMPLE_PREPARATION_RUNS_NAME).createWebPart(),
                Portal.getPortalPart("Run Groups").createWebPart(),
                Portal.getPortalPart("Run Types").createWebPart(),
                Portal.getPortalPart("Sample Sets").createWebPart(),
                Portal.getPortalPart("Protocols").createWebPart()
            ),
            getDefaultModuleSet(module, getModule("MS1"), getModule("Pipeline")),
            module);
    }


    public String getStartPageLabel(ViewContext ctx)
    {
        return super.getStartPageLabel(ctx);
    }
}
