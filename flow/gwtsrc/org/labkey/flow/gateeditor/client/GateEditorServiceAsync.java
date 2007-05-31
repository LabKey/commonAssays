package org.labkey.flow.gateeditor.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.labkey.flow.gateeditor.client.model.*;

public interface GateEditorServiceAsync
{
    void getRuns(AsyncCallback asyncCallback);
    void getWorkspace(GWTWorkspaceOptions workspaceOptions, AsyncCallback asyncCallback);
    void getGraphInfo(GWTGraphOptions graphOptions, AsyncCallback asyncCallback);
    void getCompensationMatrices(AsyncCallback asyncCallback);
    void save(GWTScript script, AsyncCallback asyncCallback);
    void save(GWTWell well, GWTScript script, AsyncCallback asyncCallback);
}
