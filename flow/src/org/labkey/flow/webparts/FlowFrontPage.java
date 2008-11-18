/*
 * Copyright (c) 2008 LabKey Corporation
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
import org.labkey.api.security.User;

/**
 * Created by IntelliJ IDEA.
 * User: matthewb
 * Date: Nov 10, 2008
 * Time: 11:07:10 AM
 */
public class FlowFrontPage extends JspView<FlowFrontPage>
{
    // web parts shouldn't assume the current container
    public Container c;

    public FlowFrontPage(ViewContext c) throws Exception
    {
        this(c.getContainer());
    }

    public FlowFrontPage(Container c) throws Exception
    {
        super(FlowFrontPage.class, "frontpage.jsp", null);
        setTitle("Flow Front Page");
        setFrame(WebPartView.FrameType.PORTAL);
        setModelBean(this);
        this.c = c;
    }

    public static WebPartFactory FACTORY = new SimpleWebPartFactory("Flow Front Page", FlowFrontPage.class);
}

