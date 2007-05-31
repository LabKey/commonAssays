package org.labkey.flow.gateeditor.client.ui.graph;

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.DOM;

public class TransparentEventImage extends Image
{
    Widget widgetDefer;
    public TransparentEventImage(Widget widgetDefer)
    {
        this.widgetDefer = widgetDefer;
    }


    public void onBrowserEvent(Event event)
    {
        switch (DOM.eventGetType(event))
        {
        case Event.ONCLICK:
        case Event.ONMOUSEDOWN:
        case Event.ONMOUSEUP:
        case Event.ONMOUSEMOVE:
        case Event.ONMOUSEOVER:
        case Event.ONMOUSEOUT:
            widgetDefer.onBrowserEvent(event);
            break;
        }
        super.onBrowserEvent(event);
    }
}
