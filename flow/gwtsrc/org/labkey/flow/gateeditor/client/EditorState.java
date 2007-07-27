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

    public GWTRun[] getRuns()
    {
        return runs;
    }

    public void setRuns(GWTRun[] runs)
    {
        this.runs = runs;
        for (int i = 0; i < listeners.size(); i ++)
        {
            ((GateEditorListener) listeners.get(i)).onRunsChanged();
        }
    }

    public GWTWorkspace getWorkspace()
    {
        return workspace;
    }

    public void setWorkspace(GWTWorkspace workspace)
    {
        this.workspace = workspace;
        for (int i = 0; i < listeners.size(); i ++)
        {
            ((GateEditorListener) listeners.get(i)).onWorkspaceChanged();
        }
        setScript(workspace.getScript());
    }

    private GWTWorkspace workspace;
    private List listeners = new ArrayList();

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
        this.script = script;
        for (int i = 0; i < listeners.size(); i ++)
        {
            ((GateEditorListener) listeners.get(i)).onScriptChanged();
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
        GWTPopulation populationDelete = null;
        if (this.population != null && this.population.isIncomplete())
        {
            populationDelete = this.population;
        }
        this.population = population;
        for (int i = 0; i < listeners.size(); i ++)
        {
            ((GateEditorListener) listeners.get(i)).onPopulationChanged();
        }
        setGate(population.getGate());
        if (populationDelete != null)
        {
            deletePopulation(populationDelete);
        }
        if (population != null)
        {
            subsetName = population.getFullName();
            GWTWell well = (GWTWell) getWorkspace().getSubsetReleventWellMap().get(population.getFullName());
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
        List lstPopulations = new ArrayList();
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
            arrPopulation[i] = (GWTPopulation) lstPopulations.get(i);
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
        this.run = run;
        for (int i = 0; i < listeners.size(); i ++)
        {
            ((GateEditorListener) listeners.get(i)).onRunChanged();
        }
    }

    public GWTWell getWell()
    {
        return well;
    }

    public void setWell(GWTWell well)
    {
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
        for (int i = 0; i < listeners.size(); i ++)
        {
            ((GateEditorListener) listeners.get(i)).onWellChanged();
        }
    }

    public GWTGate getGate()
    {
        return gate;
    }

    public void setGate(GWTGate gate)
    {
        this.gate = gate;
        for (int i = 0; i < listeners.size(); i ++)
        {
            ((GateEditorListener) listeners.get(i)).onGateChanged();
        }
    }

    public String getYAxis()
    {
        return yAxis;
    }

    public void setYAxis(String yAxis)
    {
        this.yAxis = yAxis;
        for (int i = 0; i < listeners.size(); i ++)
        {
            ((GateEditorListener) listeners.get(i)).onYAxisChanged();
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
        this.comp = comp;
        for (int i = 0; i < listeners.size(); i ++)
        {
            ((GateEditorListener) listeners.get(i)).onCompMatrixChanged();
        }

    }
}
