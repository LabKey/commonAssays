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
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Window;

public class NewPopulationDialog extends GateComponent
{
    DialogBox dialog;
    TextBox nameBox;
    ListBox populationListBox;
    KeyboardListener keyboardListener = new KeyboardListener()
    {
        public void onKeyDown(Widget sender, char keyCode, int modifiers)
        {
        }

        public void onKeyPress(Widget sender, char keyCode, int modifiers)
        {
            if (keyCode == KeyboardListener.KEY_ESCAPE)
            {
                cancel();
                return;
            }
            if (keyCode == KeyboardListener.KEY_ENTER)
            {
                createPopulation();
                return;
            }
        }

        public void onKeyUp(Widget sender, char keyCode, int modifiers)
        {
        }
    };

    public NewPopulationDialog(GateEditor editor)
    {
        super(editor);
        dialog = new DialogBox(true)
        {
            public void show()
            {
                super.show();
                nameBox.setFocus(true);
            }
        };
        dialog.setText("New Population");
        VerticalPanel verticalPanel = new VerticalPanel();
        verticalPanel.add(new Label("What do you want to call the new population?"));
        nameBox = new TextBox();
        nameBox.addKeyboardListener(keyboardListener);
        verticalPanel.add(nameBox);
        verticalPanel.add(new Label("What should be the parent of this new population?"));
        populationListBox = new ListBox();
        populationListBox.addItem("Ungated", "");
        addPopulations(populationListBox, editor.getState().getScriptComponent().getPopulations(), 0);
        if (getPopulation() != null)
        {
            String fullName = getPopulation().getFullName();
            for (int i = 0; i < populationListBox.getItemCount(); i ++)
            {
                if (fullName.equals(populationListBox.getValue(i)))
                {
                    populationListBox.setSelectedIndex(i);
                    break;
                }
            }
        }
        populationListBox.addKeyboardListener(keyboardListener);
        verticalPanel.add(populationListBox);
        HorizontalPanel horizontalPanel = new HorizontalPanel();
        Button createButton = new Button("Create", new ClickListener()
        {
            public void onClick(Widget sender)
            {
                createPopulation();
            }
        });
        createButton.addKeyboardListener(keyboardListener);
        horizontalPanel.add(createButton);
        horizontalPanel.add(new Button("Cancel", new ClickListener() {
            public void onClick(Widget sender)
            {
                cancel();
            }
        }));
        verticalPanel.add(horizontalPanel);
        dialog.setWidget(verticalPanel);
    }

    private void addPopulations(ListBox listBox, GWTPopulation[] populations, int depth)
    {
        for (GWTPopulation population : populations)
        {
            if (population.isIncomplete())
                continue;
            StringBuffer display = new StringBuffer();
            for (int indent = 0; indent < depth; indent++)
            {
                display.append(".");
            }
            display.append(population.getName());
            listBox.addItem(display.toString(), population.getFullName());
            addPopulations(listBox, population.getPopulations(), depth + 1);
        }
    }

    public Widget getWidget()
    {
        return dialog;
    }

    public DialogBox getDialog()
    {
        return dialog;
    }

    public boolean createPopulation()
    {
        GWTPopulationSet parent;
        GWTScript script = getEditor().getState().getScript().duplicate();
        GWTScriptComponent scriptComponent = getEditor().getState().getEditingMode().getScriptComponent(script);
        String parentName = populationListBox.getValue(populationListBox.getSelectedIndex());
        String name = nameBox.getText();
        String fullName;
        if (parentName != null && parentName.length() > 0)
        {
            parent = scriptComponent.findPopulation(parentName);
            fullName = parentName + "/" + name;
        }
        else
        {
            parent = scriptComponent;
            fullName = name;
        }
        GWTPopulation[] populations = parent.getPopulations();
        GWTPopulation[] newPopulations = new GWTPopulation[populations.length + 1];
        for (int i = 0; i < populations.length; i ++)
        {
            if (populations[i].getName().equals(name))
            {
                Window.alert("There is already a population named '" + name + "'.");
                return false;
            }
            newPopulations[i] = populations[i];
        }
        GWTPopulation newPopulation = new GWTPopulation();
        GWTPolygonGate gate = new GWTPolygonGate();
        gate.setOpen(true);
        gate.setXAxis(getEditor().getState().getWorkspace().getParameterNames()[0]);
        gate.setArrX(new double[0]);
        gate.setArrY(new double[0]);
        newPopulation.setGate(gate);
        newPopulation.setName(name);
        newPopulation.setFullName(fullName);
        newPopulation.setGate(gate);
        newPopulations[newPopulations.length - 1] = newPopulation;
        parent.setPopulations(newPopulations);
        getEditor().getState().setScript(script);
        getEditor().getState().setPopulation(newPopulation);
        dialog.hide();
        return true;
    }

    public void cancel()
    {
        dialog.hide();
    }
}
