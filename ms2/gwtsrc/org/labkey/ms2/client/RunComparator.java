package org.labkey.ms2.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.labkey.api.gwt.client.util.PropertyUtil;
import org.labkey.api.gwt.client.util.ServiceUtil;

/**
 * User: jeckels
 * Date: Jun 6, 2007
 */
public class RunComparator implements EntryPoint
{

    public void onModuleLoad()
    {
        RootPanel panel = RootPanel.get("org.labkey.ms2.RunComparator");
        RunComparatorView view = new RunComparatorView(panel);
        view.requestComparison();
    }
}
