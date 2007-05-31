package org.labkey.flow.gateeditor.client.ui;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.DockPanel;
import org.labkey.flow.gateeditor.client.GateEditor;
import org.labkey.flow.gateeditor.client.model.GWTEditingMode;
import org.labkey.flow.gateeditor.client.ui.graph.GraphWindow;

public class GateEditorPanel extends GateComponent
{
    DockPanel widget;
    WellList wellList;
    PopulationTree populationTree;
    RunList runList;
    CompensationMatrixList compList;
    GraphWindow graphWindow;
    GateDescription gateDescription;
    HorizontalPanel bottomStrip;

    public GateEditorPanel(GateEditor editor)
    {
        super(editor);
        this.widget = new DockPanel();
        if (!editor.getState().isRunMode())
        {
            bottomStrip = new HorizontalPanel();
            runList = new RunList(editor);
            bottomStrip.add(runList.getWidget());
            compList = new CompensationMatrixList(editor);
            bottomStrip.add(compList.getWidget());
            this.widget.add(bottomStrip, DockPanel.SOUTH);
        }
        wellList = new WellList(editor);
        this.widget.add(wellList.getWidget(), DockPanel.WEST);
        populationTree = new PopulationTree(editor);
        this.widget.add(populationTree.getWidget(), DockPanel.WEST);
        graphWindow = new GraphWindow(editor);
        this.widget.add(graphWindow.getWidget(), DockPanel.CENTER);
        gateDescription = new GateDescription(editor);
        this.widget.add(gateDescription.getWidget(), DockPanel.EAST);

    }

    public Widget getWidget()
    {
        return widget;
    }

    public GateDescription getGateDescription()
    {
        return gateDescription;
    }

    public void setGateDescription(GateDescription gateDescription)
    {
        this.gateDescription = gateDescription;
    }

}
