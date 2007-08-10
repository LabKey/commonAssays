package org.labkey.flow.gateeditor.client.ui;

import org.labkey.flow.gateeditor.client.GateEditor;
import org.labkey.flow.gateeditor.client.model.*;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.http.client.URL;

abstract public class GateComponent
{
    protected GateEditor editor;

    public GateComponent(GateEditor editor)
    {
        this.editor = editor;
    }

    abstract public Widget getWidget();

    public GateEditor getEditor()
    {
        return editor;
    }

    public void setEditor(GateEditor editor)
    {
        this.editor = editor;
    }

    public String u(String s)
    {
        return URL.encodeComponent(s);
    }

    public GWTScript getScript()
    {
        return editor.getState().getScript();
    }

    public GWTRun getRun()
    {
        return editor.getState().getRun();
    }

    public GWTWell getWell()
    {
        return editor.getState().getWell();
    }

    public GWTPopulation getPopulation()
    {
        return editor.getState().getPopulation();
    }

    public GWTGate getGate()
    {
        return editor.getState().getGate();
    }

    public String getYAxis()
    {
        return editor.getState().getYAxis();
    }

    public GWTWorkspace getWorkspace()
    {
        return editor.getState().getWorkspace();
    }

    public GWTWell[] getWells()
    {
        GWTWorkspace workspace = getWorkspace();
        if (workspace == null)
        {
            return new GWTWell[0];
        }
        return workspace.getWells();
    }

    public GWTCompensationMatrix getCompensationMatrix()
    {
        return editor.getState().getCompensationMatrix();
    }

    public boolean isReadOnly()
    {
        GWTWorkspace workspace = editor.getState().getWorkspace();
        if (workspace == null)
            return true;
        if (workspace.isReadOnly())
            return true;
        return false;
    }
}
