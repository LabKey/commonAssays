package org.labkey.flow.gateeditor.client.ui;

import org.labkey.flow.gateeditor.client.GateEditor;
import org.labkey.flow.gateeditor.client.model.GWTPopulation;
import org.labkey.flow.gateeditor.client.model.GWTPopulationSet;
import com.google.gwt.user.client.ui.*;

public class NewPopulationDialog extends GateComponent
{
    DialogBox dialog;
    VerticalPanel verticalPanel;
    TextBox nameBox;
    ListBox populationListBox;



    public NewPopulationDialog(GateEditor editor)
    {
        super(editor);
        dialog = new DialogBox(true);
        verticalPanel = new VerticalPanel();
        verticalPanel.add(new Label("What do you want to call the new population?"));
        nameBox = new TextBox();
        verticalPanel.add(nameBox);
        verticalPanel.add(new Label("What should be the parent of this new population?"));
        populationListBox = new ListBox();
        populationListBox.addItem("Ungated", "");
        addPopulations(populationListBox, editor.getState().getScriptComponent().getPopulations(), 0);
        verticalPanel.add(populationListBox);
        verticalPanel.add(new Button("Create", new ClickListener()
        {
            public void onClick(Widget sender)
            {
                GWTPopulationSet parent;
                String parentName = populationListBox.getValue(populationListBox.getSelectedIndex());
                if (parentName != null && parentName.length() > 0)
                {
                    parent = getEditor().getState().getScriptComponent().findPopulation(parentName);
                }
                else
                {
                    parent = getEditor().getState().getScriptComponent();
                }
                GWTPopulation[] populations = parent.getPopulations();
                GWTPopulation[] newPopulations = new GWTPopulation[populations.length + 1];
                for (int i = 0; i < populations.length; i ++)
                {
                    newPopulations[i] = populations[i];
                }
                GWTPopulation newPopulation = new GWTPopulation();
                newPopulation.setName(nameBox.getText());
                newPopulations[newPopulations.length - 1] = newPopulation;
                getEditor().save(getEditor().getState().getScript());
            }
        }));
        dialog.setWidget(verticalPanel);
        dialog.addPopupListener(new PopupListener() {

            public void onPopupClosed(PopupPanel sender, boolean autoClosed)
            {
                getEditor().getRootPanel().remove(dialog);
            }
        });
        dialog.show();
    }

    private void addPopulations(ListBox listBox, GWTPopulation[] populations, int depth)
    {
        for (int i = 0; i < populations.length; i ++)
        {
            GWTPopulation population = populations[i];
            StringBuffer display = new StringBuffer();
            for (int indent = 0; indent < depth; indent ++)
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
}
