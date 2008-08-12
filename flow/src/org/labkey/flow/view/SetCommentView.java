package org.labkey.flow.view;

import org.labkey.api.view.JspView;
import org.labkey.flow.data.FlowObject;

/**
 * User: kevink
 * Date: Aug 11, 2008 10:59:09 PM
 */
public class SetCommentView extends JspView<FlowObject>
{
    public SetCommentView(FlowObject model)
    {
        super("/org/labkey/flow/view/setComment.jsp", model);
        setFrame(FrameType.NONE);
    }
}
