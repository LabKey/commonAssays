package org.labkey.flow.gateeditor.client;

import com.google.gwt.user.client.rpc.RemoteService;
import org.labkey.flow.gateeditor.client.model.*;

public interface GateEditorService extends RemoteService
{
    GWTRun[] getRuns();
    GWTCompensationMatrix[] getCompensationMatrices();
    GWTWorkspace getWorkspace(GWTWorkspaceOptions workspaceOptions);
    GWTGraphInfo getGraphInfo(GWTGraphOptions graphOptions);
    GWTScript save(GWTScript script);
    GWTWell save(GWTWell well, GWTScript script);
}
