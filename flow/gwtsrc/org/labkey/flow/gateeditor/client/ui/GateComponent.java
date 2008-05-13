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
