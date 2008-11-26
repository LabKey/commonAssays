/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.flow.controllers.editscript;

import org.labkey.flow.controllers.SpringFlowController;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.data.FlowScript;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.action.BeehivePortingActionResolver;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.RedirectAction;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewForward;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.util.PageFlowUtil;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

/**
 * User: kevink
 * Date: Nov 25, 2008 5:27:35 PM
 */
public class SpringScriptController extends SpringFlowController<SpringScriptController.Action>
{
    static Logger _log = Logger.getLogger(SpringScriptController.class);

    public enum Action
    {
        begin,
        editScript,
        newProtocol,
        editSettings,
        editProperties,
        editCompensationCalculation,
        showCompensationCalulation,
        uploadCompensationCalculation,
        chooseCompensationRun,
        editAnalysis,
        editGateTree,
        uploadAnalysis,
        graphImage,
        copy,
        delete,
        gateEditor,
    }

    static SpringActionController.DefaultActionResolver _actionResolver =
            //new SpringActionController.DefaultActionResolver(SpringScriptController.class);
            new BeehivePortingActionResolver(ScriptController.class, SpringScriptController.class);

    public SpringScriptController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleViewAction<EditScriptForm>
    {
        public ModelAndView getView(EditScriptForm form, BindException errors) throws Exception
        {
            FlowScript script = form.analysisScript;
            if (script == null)
            {
                HttpView.throwRedirect(new ActionURL(FlowController.BeginAction.class, getContainer()));
            }
            HttpView.throwRedirect(script.urlShow());
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }
}
