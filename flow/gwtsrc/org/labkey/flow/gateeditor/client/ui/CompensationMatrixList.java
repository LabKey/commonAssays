package org.labkey.flow.gateeditor.client.ui;

import com.google.gwt.user.client.ui.*;
import org.labkey.flow.gateeditor.client.GateEditor;
import org.labkey.flow.gateeditor.client.GateCallback;
import org.labkey.flow.gateeditor.client.model.GWTCompensationMatrix;

public class CompensationMatrixList extends GateComponent
{
    HorizontalPanel widget;
    ListBox listBox;
    GWTCompensationMatrix[] matrices;

    GateEditorListener listener = new GateEditorListener() {

        public void onCompMatrixChanged()
        {
            updateCompensationMatrix();
        }
    };

    ChangeListener changeListener = new ChangeListener() {

        public void onChange(Widget sender)
        {
            getEditor().getState().setCompensationMatrix(matrices[listBox.getSelectedIndex()]);
        }
    };

    public CompensationMatrixList(GateEditor editor)
    {
        super(editor);
        widget = new HorizontalPanel();
        widget.add(new Label("Compensation Matrix:"));
        listBox = new ListBox();
        listBox.addChangeListener(changeListener);
        widget.add(listBox);
        editor.getService().getCompensationMatrices(new GateCallback() {

            public void onSuccess(Object result)
            {
                setCompensationMatrices((GWTCompensationMatrix[]) result);
            }
        });
    }


    public Widget getWidget()
    {
        return widget;
    }

    public void updateCompensationMatrix()
    {
        for (int i = 0; i < matrices.length; i ++)
        {
            if (matrices[i].equals(getCompensationMatrix()))
            {
                listBox.setSelectedIndex(1);
            }
        }
    }

    public void setCompensationMatrices(GWTCompensationMatrix[] comps)
    {
        listBox.clear();
        for (int i = 0; i < comps.length; i++)
        {
            listBox.addItem(comps[i].getLabel(), Integer.toString(comps[i].getCompId()));
        }
        if (comps.length > 0)
        {
            editor.getState().setCompensationMatrix(comps[0]);
        }
    }
}
