package org.labkey.microarray.designer.client;

import org.labkey.api.gwt.client.assay.AssayDesignerMainPanel;
import org.labkey.api.gwt.client.ui.PropertiesEditor;
import org.labkey.api.gwt.client.model.GWTDomain;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * User: jeckels
 * Date: Feb 6, 2008
 */
public class MicroarrayDesignerMainPanel extends AssayDesignerMainPanel
{
    public MicroarrayDesignerMainPanel(RootPanel panel, String providerName, Integer protocolId, boolean copyAssay)
    {
        super(panel, providerName, protocolId, copyAssay);
    }

    protected PropertiesEditor createPropertiesEditor(GWTDomain domain)
    {
        return super.createPropertiesEditor(domain);
    }
}
