/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import org.labkey.api.util.HtmlString;
import org.labkey.api.view.*;
import org.labkey.flow.controllers.FlowController;

public class OverviewWebPart extends HtmlView
{
    static public final BaseWebPartFactory FACTORY = new SimpleWebPartFactory("Flow Experiment Management", OverviewWebPart.class);
    static
    {
        FACTORY.addLegacyNames("Flow Overview");
    }

    public OverviewWebPart(ViewContext portalCtx) throws Exception
    {
        super(HtmlString.unsafe(new FlowOverview(portalCtx.getUser(), portalCtx.getContainer()).toString()));
        setTitle("Flow Experiment Management");

        // slight hackery see initFlow()
        FlowController.initFlow(portalCtx.getContainer());
    }
}