package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GWTWorkspaceOptions implements IsSerializable
{
    public int scriptId;
    public int runId;
    public GWTEditingMode editingMode;
}
