package org.labkey.microarray.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;
import org.labkey.api.gwt.client.util.PropertyUtil;
import org.labkey.api.gwt.client.assay.AssayView;

/**
 * User: brittp
 * Date: Jun 20, 2007
 * Time: 2:23:06 PM
 */
public class MicroarrayAssayDesigner implements EntryPoint
{
    public void onModuleLoad()
    {
        RootPanel panel = RootPanel.get("org.labkey.microarray.designer.MicroarrayAssayDesigner-Root");
        if (panel != null)
        {
            String protocolIdStr = PropertyUtil.getServerProperty("protocolId");
            String providerName = PropertyUtil.getServerProperty("providerName");
            String copyStr = PropertyUtil.getServerProperty("copy");
            boolean copyAssay = copyStr != null && Boolean.TRUE.toString().equals(copyStr);
            AssayView view = new AssayView(panel, providerName, protocolIdStr != null ? new Integer(Integer.parseInt(protocolIdStr)) : null, copyAssay);
            view.showAsync();
        }
    }
}
