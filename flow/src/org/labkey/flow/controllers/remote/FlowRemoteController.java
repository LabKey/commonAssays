/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.flow.controllers.remote;

import org.labkey.flow.controllers.editscript.GateEditorServiceImpl;
import org.labkey.api.action.InterfaceAction;
import org.labkey.flow.FlowModule;
import org.labkey.flow.gateeditor.client.GateEditorService;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.HttpView;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;

/**
 * Created by IntelliJ IDEA.
 * User: matthewb
 * Date: Sep 24, 2007
 * Time: 4:06:44 PM
 */
public class FlowRemoteController extends SpringActionController
{
    static DefaultActionResolver _actionResolver = new DefaultActionResolver(FlowRemoteController.class);

    public FlowRemoteController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    // UNDONE: InfoAction { version number, login status, etc }

    @RequiresPermission(ACL.PERM_READ)
    public class GateEditorServiceAction extends InterfaceAction<GateEditorService>
    {
        public GateEditorServiceAction()
        {
            super(GateEditorService.class, FlowModule.NAME);
        }

        public GateEditorServiceImpl getInstance(int version)   // version * 1000
        {
            ViewContext context = HttpView.currentContext();
            switch (version)
            {
                case 2300:
                    return new GateEditorServiceImpl(context);
                default:
                    return new GateEditorServiceImpl(context);
            }
        }
    }
}
