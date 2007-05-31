package org.labkey.flow.gateeditor.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.Window;

abstract public class GateCallback implements AsyncCallback
{

    public void onFailure(Throwable caught)
    {
        Window.alert(caught.getMessage());
    }
}
