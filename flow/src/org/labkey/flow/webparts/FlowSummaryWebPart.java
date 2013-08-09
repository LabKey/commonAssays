/*
 * Copyright (c) 2009-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.flow.webparts;

import org.labkey.api.view.*;
import org.labkey.api.data.Container;

/**
 * User: kevink
 * Date: May 11, 2009 5:52:17 PM
 */
public class FlowSummaryWebPart extends JspView<FlowSummaryWebPart>
{
    public static WebPartFactory FACTORY =
            new SimpleWebPartFactory("Flow Summary", WebPartFactory.LOCATION_RIGHT, FlowSummaryWebPart.class, null)
            {
                @Override
                public boolean isAvailable(Container c, String location)
                {
                    if (location.equals(getDefaultLocation()) || location.equals(WebPartFactory.LOCATION_MENUBAR))
                    {
                        return c.getFolderType() instanceof FlowFolderType ||
                               c.getActiveModules().contains(getModule());
                    }
                    return false;
                }
            };

    // web parts shouldn't assume the current container
    public Container c;

    public FlowSummaryWebPart(ViewContext portalCtx)
    {
        this(portalCtx.getContainer());
    }

    public FlowSummaryWebPart(Container c)
    {
        super(FlowSummaryWebPart.class, "FlowSummary.jsp", null);
        setTitle("Flow Summary");
        setFrame(WebPartView.FrameType.PORTAL);
        setModelBean(this);
        this.c = c;
    }
}
