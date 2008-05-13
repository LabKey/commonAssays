/*
 * Copyright (c) 2006-2007 LabKey Corporation
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

import org.labkey.flow.controllers.editscript.ScriptController.Action;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;

import java.io.Writer;

abstract public class TemplatePage extends ScriptController.Page
{
    public ScriptController.Page body;
    public Action curAction;
    public void renderBody(Writer out) throws Exception
    {
        HttpView view = new JspView(body);
        ((HttpView) HttpView.currentView()).include(view, out);
    }
}
