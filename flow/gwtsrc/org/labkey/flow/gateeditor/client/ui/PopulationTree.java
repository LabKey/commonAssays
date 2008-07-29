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

import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.DOM;
import org.labkey.flow.gateeditor.client.model.*;
import org.labkey.flow.gateeditor.client.GateEditor;
import org.labkey.flow.gateeditor.client.FlowUtil;
import org.labkey.api.gwt.client.ui.WindowUtil;
import org.labkey.api.gwt.client.ui.ImageButton;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class PopulationTree extends GateComponent
{
    VerticalPanel widget;
    ScrollPanel scrollPanel;
    VerticalPanel tree;
    ImageButton newPopulationButton;
    Map populationNameLabelMap = new HashMap();
    Map labelPopulationMap = new HashMap();
    Label currentLabel;

    GateEditorListener listener = new GateEditorListener()
    {
        public void onScriptChanged()
        {
            setScriptComponent(getEditor().getState().getEditingMode().getScriptComponent(getScript()));
        }

        public void onPopulationChanged()
        {
            selectPopulation(getPopulation());
        }
    };

    ClickListener clickListener = new ClickListener()
    {
        public void onClick(Widget sender)
        {
            GWTPopulation population = (GWTPopulation) labelPopulationMap.get(sender);
            if (population != null)
            {
                editor.getState().setPopulation(population);
            }
        }
    };

    public Widget getWidget()
    {
        return widget;
    }

    public PopulationTree(GateEditor editor)
    {
        super(editor);
        this.widget = new VerticalPanel();
        Image spacer = new Image();
        spacer.setUrl(FlowUtil._gif());
        spacer.setHeight("1px");
        spacer.setWidth("150px");
        widget.add(spacer);
        scrollPanel = new ScrollPanel();
        scrollPanel.setWidth("150px");
        scrollPanel.setHeight("400px");
        tree = new VerticalPanel();
        scrollPanel.add(tree);
        widget.add(scrollPanel);
        editor.addListener(listener);
        if (!editor.getState().isRunMode())
        {
            newPopulationButton = new ImageButton("New Population")
            {
                public void onClick(Widget sender)
                {
                    NewPopulationDialog dlg = new NewPopulationDialog(getEditor());
                    dlg.getDialog().setPopupPosition(widget.getAbsoluteLeft(), widget.getAbsoluteTop());
                    dlg.getDialog().show();
                    WindowUtil.scrollIntoView(dlg.getDialog());
                }
            };
            widget.add(newPopulationButton);
        }
    }




    private boolean isBold(GWTPopulation population)
    {
        if (!editor.getState().getEditingMode().isRunMode())
        {
            return population.isIncomplete();
        }
        GWTScript baseScript = editor.getState().getWorkspace().getScript();
        GWTPopulation basePopulation = editor.getState().getEditingMode().getScriptComponent(baseScript).findPopulation(population.getFullName());
        if (basePopulation == null)
            return true;
        GWTGate baseGate = basePopulation.getGate();
        if (baseGate == null)
        {
            return population.getGate() != null;
        }
        return !baseGate.equals(population.getGate());
    }

    private void makeBold(GWTPopulation population, Label label)
    {
        if (isBold(population))
        {
            DOM.setStyleAttribute(label.getElement(), "fontWeight", "bold");
        }
        else
        {
            DOM.setStyleAttribute(label.getElement(), "fontWeight", "normal");
        }
    }

    private void addLabels(GWTPopulation population, int depth)
    {
        Label label = new Label(population.getName());
        DOM.setStyleAttribute(label.getElement(), "paddingLeft", (depth * 10) + "px");
        DOM.setStyleAttribute(label.getElement(), "cursor", "default");
        tree.add(label);
        label.addClickListener(clickListener);
        makeBold(population, label);
        populationNameLabelMap.put(population.getFullName(), label);
        labelPopulationMap.put(label, population);
        GWTPopulation[] children = population.getPopulations();
        for (int i = 0; i < children.length; i++)
        {
            addLabels(children[i], depth + 1);
        }
    }

    public void setScriptComponent(GWTScriptComponent scriptComponent)
    {
        for (Iterator it = labelPopulationMap.keySet().iterator(); it.hasNext();)
        {
            Label label = (Label) it.next();
            label.removeFromParent();
        }
        labelPopulationMap.clear();
        populationNameLabelMap.clear();
        currentLabel = null;
        for (int i = 0; i < scriptComponent.getPopulations().length; i++)
        {
            addLabels(scriptComponent.getPopulations()[i], 0);
        }
        selectPopulation(getPopulation());
    }

    public void selectPopulation(GWTPopulation population)
    {
        if (currentLabel != null)
        {
            DOM.setStyleAttribute(currentLabel.getElement(), "backgroundColor", "white");
            currentLabel = null;
        }
        if (population != null)
        {
            currentLabel = (Label) populationNameLabelMap.get(population.getFullName());
            if (currentLabel != null)
            {
                makeBold(population, currentLabel);
                DOM.setStyleAttribute(currentLabel.getElement(), "backgroundColor", "#FFDF8C");
            }
        }
    }
}
