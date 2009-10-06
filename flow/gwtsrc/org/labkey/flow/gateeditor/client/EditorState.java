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

package org.labkey.flow.gateeditor.client;

import org.labkey.flow.gateeditor.client.model.*;
import org.labkey.flow.gateeditor.client.ui.GateEditorListener;

import java.util.List;
import java.util.ArrayList;

public class EditorState
{
    private GWTEditingMode editingMode;
    private GWTScript script;
    private String subsetName;
    private GWTPopulation population;
    private GWTRun run;
    private GWTWell well;
    private GWTGate gate;
    private GWTCompensationMatrix comp;
    private String yAxis = "";
    private GWTRun[] runs = new GWTRun[0];


    public GWTEditingMode getEditingMode()
    {
        return editingMode;
    }

    public void setEditingMode(GWTEditingMode editingMode)
    {
        this.editingMode = editingMode;
    }

    public boolean isRunMode()
    {
        return getEditingMode().isRunMode();
    }

    public GWTScriptComponent getScriptComponent()
    {
        return editingMode.getScriptComponent(script);
    }

    public GWTWorkspace getWorkspace()
    {
        return workspace;
    }

    public void setWorkspace(GWTWorkspace workspace)
    {
        if (this.workspace == workspace)
            return;
        this.workspace = workspace;
        for (GateEditorListener listener : listeners)
        {
            listener.onWorkspaceChanged();
        }
        setScript(workspace.getScript());
        setRun(workspace.getRun());
    }

    public void fireBeforeWorkspaceChanged()
    {
        for (GateEditorListener listener : listeners)
        {
            listener.onBeforeWorkspaceChanged();
        }
    }

    private GWTWorkspace workspace;
    private List<GateEditorListener> listeners = new ArrayList<GateEditorListener>();

    public GWTScript getScript()
    {
        return script;
    }

    public void setSubsetName(String subsetName)
    {
        this.subsetName = subsetName;
    }

    public void setScript(GWTScript script)
    {
        if (this.script == script)
            return;
        this.script = script;
        for (GateEditorListener listener : listeners)
        {
            listener.onScriptChanged();
        }
        if (subsetName != null)
        {
            setPopulation(getScriptComponent().findPopulation(subsetName));
        }
    }

    public GWTPopulation getPopulation()
    {
        return population;
    }

    public void setPopulation(GWTPopulation population)
    {
        if (this.population == population)
            return;

        GWTPopulation populationDelete = null;
        if (this.population != null && this.population.isIncomplete())
        {
            populationDelete = this.population;
        }
        this.population = population;
        for (GateEditorListener listener : listeners)
        {
            listener.onPopulationChanged();
        }
        setGate(population.getGate());
        if (populationDelete != null)
        {
            deletePopulation(populationDelete);
        }
        if (population != null)
        {
            subsetName = population.getFullName();
            GWTWell well = getWorkspace().getSubsetReleventWellMap().get(population.getFullName());
            if (well != null)
            {
                setWell(well);
            }
        }
        else
        {
            subsetName = null;
        }
    }

    private void deletePopulation(GWTPopulation population)
    {
        if (population == null)
            return;
        GWTScript script = getScript();
        GWTScriptComponent scriptComponent = getScriptComponent();
        GWTPopulation popCompare = scriptComponent.findPopulation(population.getFullName());
        if (popCompare != population)
            return;
        GWTScript newScript = script.duplicate();
        scriptComponent = getEditingMode().getScriptComponent(newScript);
        String parentName = population.getParentFullName();
        GWTPopulationSet parent;
        if (parentName == null)
        {
            parent = scriptComponent;
        }
        else
        {
            parent = scriptComponent.findPopulation(parentName);
        }
        List<GWTPopulation> lstPopulations = new ArrayList<GWTPopulation>();
        for (int i = 0; i < parent.getPopulations().length; i ++)
        {
            GWTPopulation child = parent.getPopulations()[i];
            if (!child.getName().equals(population.getName()))
            {
                lstPopulations.add(child);
            }
        }
        GWTPopulation[] arrPopulation = new GWTPopulation[lstPopulations.size()];
        for (int i = 0; i < arrPopulation.length; i ++)
        {
            arrPopulation[i] = lstPopulations.get(i);
        }
        parent.setPopulations(arrPopulation);
        setScript(newScript);
    }

    public GWTRun getRun()
    {
        return run;
    }

    public void setRun(GWTRun run)
    {
        if (this.run == run)
            return;
        this.run = run;
        for (GateEditorListener listener : listeners)
        {
            listener.onRunChanged();
        }
    }

    public GWTWell getWell()
    {
        return well;
    }

    public void setWell(GWTWell well)
    {
        if (this.well == well)
            return;
        
        this.well = well;
        if (isRunMode())
        {
            GWTScript script = well.getScript();
            if (script == null)
            {
                script = getWorkspace().getScript();
            }
            setScript(script);
            setCompensationMatrix(well.getCompensationMatrix());
        }
        for (GateEditorListener listener : listeners)
        {
            listener.onWellChanged();
        }
    }

    public GWTGate getGate()
    {
        return gate;
    }

    public void setGate(GWTGate gate)
    {
        if (this.gate == gate)
            return;
        this.gate = gate;
        for (GateEditorListener listener : listeners)
        {
            listener.onGateChanged();
        }
    }

    public String getYAxis()
    {
        return yAxis;
    }

    public void setYAxis(String yAxis)
    {
        if (this.yAxis != null && this.yAxis.equals(yAxis))
            return;
        this.yAxis = yAxis;
        for (GateEditorListener listener : listeners)
        {
            listener.onYAxisChanged();
        }
    }

    public void addListener(GateEditorListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(GateEditorListener listener)
    {
        listeners.remove(listener);
    }

    public GWTCompensationMatrix getCompensationMatrix()
    {
        return comp;
    }

    public void setCompensationMatrix(GWTCompensationMatrix comp)
    {
        if (this.comp == comp)
            return;
        this.comp = comp;
        for (GateEditorListener listener : listeners)
        {
            listener.onCompMatrixChanged();
        }

    }
}
