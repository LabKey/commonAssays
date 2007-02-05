package org.labkey.flow.script;

import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.security.ACL;
import org.labkey.flow.util.PFUtil;
import org.apache.log4j.Logger;

import java.util.*;
import java.io.File;

import org.labkey.flow.controllers.FlowModule;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;

public class FlowPipelineProvider extends PipelineProvider
{
    private static final Logger _log = Logger.getLogger(FlowPipelineProvider.class);
    public static final String NAME = "flow";

    public FlowPipelineProvider()
    {
        super(NAME);
    }

    private boolean hasFlowModule(ViewContext context)
    {
        return FlowModule.isActive(context.getContainer());
    }

    public void updateFileProperties(ViewContext context, List<FileEntry> entries)
    {
        if (!context.hasPermission(ACL.PERM_INSERT))
            return;
        if (!hasFlowModule(context))
            return;
        if (entries.size() == 0)
            return;
        FileEntry entry = entries.get(0);
        ViewURLHelper url = entries.get(0).cloneHref();
        url.setPageFlow(PFUtil.getPageFlowName(AnalysisScriptController.Action.chooseRunsToUpload));
        url.setAction(AnalysisScriptController.Action.chooseRunsToUpload.toString());
        FileAction action = new FileAction("Upload FCS files", url, null);
        action.setDescription("<p><b>Flow Instructions:</b><br>Navigate to the directories containing FCS files.  Click the button to upload FCS files in the directories shown.</p>");
        entry.addAction(action);
    }

    public boolean suppressOverlappingRootsWarning(ViewContext context)
    {
        if (!hasFlowModule(context))
            return super.suppressOverlappingRootsWarning(context);
        return true;
    }
}
