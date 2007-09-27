package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class GWTWorkspaceOptions implements IsSerializable, Serializable
{
    public int scriptId;
    public int runId;
    public GWTEditingMode editingMode;
}
