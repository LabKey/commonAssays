package org.labkey.flow.webparts;

import org.labkey.api.view.JspView;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.SimpleWebPartFactory;

/**
 * Created by IntelliJ IDEA.
 * User: matthewb
 * Date: Nov 13, 2008
 * Time: 10:32:43 AM
 */
public class FlowFiles extends JspView<Object>
{
    public FlowFiles()
    {
        super(FlowFiles.class, "flowfiles.jsp", null);
        setTitle("Flow Files");
    }

    public static WebPartFactory FACTORY  = new SimpleWebPartFactory("Flow Files", FlowFiles.class);
}
