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
