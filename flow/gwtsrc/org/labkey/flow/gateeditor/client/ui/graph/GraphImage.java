package org.labkey.flow.gateeditor.client.ui.graph;

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.DOM;

public class GraphImage extends Image
{
    public void onBrowserEvent(Event event)
    {
        super.onBrowserEvent(event);
        if (DOM.eventGetType(event) == Event.ONMOUSEDOWN)
        {
            DOM.eventPreventDefault(event);
        }
    }
}
