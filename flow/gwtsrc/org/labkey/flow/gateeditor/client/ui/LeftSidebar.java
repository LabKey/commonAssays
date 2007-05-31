package org.labkey.flow.gateeditor.client.ui;

import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.Widget;
import org.labkey.flow.gateeditor.client.GateEditor;

public class LeftSidebar extends GateComponent
{
    StackPanel widget;
    PopulationTree populationTree;
    RunList runList;
    WellList wellList;

    public LeftSidebar(GateEditor editor)
    {
        super(editor);
        widget = new StackPanel();
        widget.setWidth("200px");
        runList = new RunList(editor);
        wellList = new WellList(editor);
        populationTree = new PopulationTree(editor);
        widget.add(runList.getWidget(), "Runs");
        widget.add(wellList.getWidget(), "Wells");
        widget.add(populationTree.getWidget(), "Populations");
    }

    public Widget getWidget()
    {
        return widget;
    }
}
