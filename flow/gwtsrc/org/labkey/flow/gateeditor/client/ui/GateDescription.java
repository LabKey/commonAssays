package org.labkey.flow.gateeditor.client.ui;

import com.google.gwt.user.client.ui.*;
import org.labkey.flow.gateeditor.client.model.*;
import org.labkey.flow.gateeditor.client.GateEditor;

import java.util.*;

public class GateDescription extends GateComponent
{
    FlexTable widget;

    Map labelMap = new HashMap();
    ListBox xAxis;
    ListBox yAxis;
    Button btnClearAllPoints = new Button("Clear All Points", new ClickListener() {
        public void onClick(Widget sender)
        {
            clearAllPoints();
        }
    });
    Button btnSave = new Button("Save", new ClickListener() {
        public void onClick(Widget sender)
        {
            save();
        }
    });
    GateEditorListener listener = new GateEditorListener()
    {
        public void onGateChanged()
        {
            setGate(getGate());
        }
        public void onWorkspaceChanged()
        {
            updateAxisDropdowns();
        }
        public void onCompMatrixChanged()
        {
            updateAxisDropdowns();
        }
    };

    ChangeListener listBoxListener = new ChangeListener()
    {
        public void onChange(Widget sender)
        {
            GWTGate gate = getGate();
            if (gate instanceof GWTPolygonGate)
            {
                gate = new GWTPolygonGate(xAxis.getValue(xAxis.getSelectedIndex()), new double[0], yAxis.getValue(yAxis.getSelectedIndex()), new double[0]);
                gate.setOpen(true);
                editor.getState().setGate(gate);
            }
            else if (gate instanceof GWTIntervalGate)
            {
                editor.getState().setYAxis(yAxis.getValue(yAxis.getSelectedIndex()));
            }
        }
    };
    public GateDescription(GateEditor editor)
    {
        super(editor);
        widget = new FlexTable();
        editor.addListener(listener);
        xAxis = new ListBox();
        xAxis.addChangeListener(listBoxListener);
        yAxis = new ListBox();
        yAxis.addChangeListener(listBoxListener);
    }

    public Widget getWidget()
    {
        return widget;
    }

    public void setValue(ListBox listBox, String value)
    {
        for (int i = 0; i < listBox.getItemCount(); i ++)
        {
            String compare = listBox.getValue(i);
            if (compare.equals(value))
            {
                listBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private boolean canSave(GWTGate gate)
    {
        if (gate == null)
            return false;
        if (!gate.isDirty())
            return false;
        if (gate instanceof GWTIntervalGate)
            return true;
        if (gate instanceof GWTPolygonGate)
        {
            return ((GWTPolygonGate) gate).length() >= 2;
        }
        return false;
    }

    public void setGate(GWTGate gate)
    {
        while (widget.getRowCount() > 0)
        {
            widget.removeRow(0);
        }
        int row = -1;
        widget.setText(++row, 0, getPopulation().getFullName());
        widget.getFlexCellFormatter().setColSpan(row, 0, 2);
        if (gate instanceof GWTIntervalGate)
        {
            GWTIntervalGate interval = (GWTIntervalGate) gate;
            widget.setText(++row, 0, "Axis:");
            widget.setText(row, 1, getLabel(interval.getAxis()));
            widget.setText(++row, 0, "Plotted Against:");
            widget.setWidget(row, 1, yAxis);
            setValue(yAxis, editor.getState().getYAxis());
            widget.setText(++row, 0, "Min:");
            widget.setText(row, 1, Double.toString(interval.getMinValue()));
            widget.setText(++row, 0, "Max:");
            widget.setText(row, 1, Double.toString(interval.getMaxValue()));
        }
        else if (gate instanceof GWTPolygonGate)
        {
            GWTPolygonGate polygon = (GWTPolygonGate) gate;
            setValue(xAxis, polygon.getXAxis());
            setValue(yAxis, polygon.getYAxis());
            if (polygon.length() == 0)
            {
                widget.setText(++row, 0, "X Axis");
                widget.setText(row, 1, "Y Axis");
                widget.setWidget(++row, 0, xAxis);
                widget.setWidget(row, 1, yAxis);
            }
            else
            {
                widget.setText(++row, 0, getLabel(polygon.getXAxis()));
                widget.setText(row, 1, getLabel(polygon.getYAxis()));
                double[] arrX = polygon.getArrX();
                double[] arrY = polygon.getArrY();
                for (int i = 0; i < arrX.length; i ++)
                {
                    widget.setText(++row, 0, Double.toString(arrX[i]));
                    widget.setText(row, 1, Double.toString(arrY[i]));
                }
            }
        }
        else
        {
            widget.setText(++row, 0, "This gate is too complex to be edited.");
            widget.getFlexCellFormatter().setColSpan(row, 0, 2);
        }
        if (!isReadOnly())
        {
            widget.setWidget(++row, 0, btnClearAllPoints);
            widget.getFlexCellFormatter().setColSpan(row, 0, 2);
            if (canSave(gate))
            {
                widget.setWidget(++row, 0, btnSave);
                widget.getFlexCellFormatter().setColSpan(row, 0, 2);
            }
        }
    }

    public void clearAllPoints()
    {
        GWTGate gate = getGate();
        GWTPolygonGate newGate;
        if (gate instanceof GWTIntervalGate)
        {
            GWTIntervalGate interval = (GWTIntervalGate) gate;
            newGate = new GWTPolygonGate(interval.getAxis(), new double[0], null, new double[0]);
        }
        else if (gate instanceof GWTPolygonGate)
        {
            GWTPolygonGate polygon = (GWTPolygonGate) gate;
            newGate = new GWTPolygonGate(polygon.getXAxis(), new double[0], polygon.getYAxis(), new double[0]);
        }
        else
        {
            newGate = new GWTPolygonGate(getWell().getParameters()[0], new double[0], getWell().getParameters()[1], new double[0]);
        }
        newGate.setOpen(true);
        editor.getState().setGate(newGate);
    }

    public void updateAxisDropdowns()
    {
        List parameters = new ArrayList();
        List labels = new ArrayList();
        labelMap = new HashMap();
        GWTWorkspace workspace = getWorkspace();
        if (workspace != null)
        {
            parameters.addAll(Arrays.asList(workspace.getParameterNames()));
            labels.addAll(Arrays.asList(workspace.getParameterLabels()));
            for (int i = 0; i < parameters.size(); i ++)
            {
                labelMap.put(parameters.get(i), labels.get(i));
            }
        }
        GWTCompensationMatrix comp = getCompensationMatrix();
        if (comp != null)
        {
            for (int i = 0; i < comp.getParameterNames().length; i ++)
            {
                String plainName = comp.getParameterNames()[i];
                String compName = GWTCompensationMatrix.PREFIX + plainName + GWTCompensationMatrix.SUFFIX;
                String plainLabel = (String) labelMap.get(plainName);
                if (plainLabel == null)
                {
                    plainLabel = plainName;
                }
                String compLabel = "comp-" + plainLabel;
                parameters.add(compName);
                labels.add(compLabel);
            }
        }
        updateDropdown(xAxis, parameters, labels);
        parameters.add(0, "");
        labels.add(0, "[[histogram]]");
        updateDropdown(yAxis, parameters, labels);
    }

    protected String getLabel(String axis)
    {
        if (axis == null || axis.length() == 0)
            return "[[histogram]]";
        String ret = (String) labelMap.get(axis);
        if (ret == null)
            return axis;
        return ret;
    }

    private void updateDropdown(ListBox listbox, List values, List labels)
    {
        String curValue = "";
        if (listbox.getSelectedIndex() >= 0)
        {
            curValue = listbox.getValue(listbox.getSelectedIndex());
        }
        listbox.clear();
        for (int i = 0; i < values.size(); i ++)
        {
            String value = (String) values.get(i);
            listbox.addItem((String) labels.get(i), value);
        }
        setValue(listbox, curValue);
    }

    public void save()
    {
        GWTGate gate = getGate();
        if (gate instanceof GWTPolygonGate)
        {
            GWTPolygonGate polygon = (GWTPolygonGate) gate;
            if (polygon.length() == 2)
            {
                GWTIntervalGate interval = new GWTIntervalGate(polygon.getXAxis(),
                    Math.min(polygon.getArrX()[0], polygon.getArrX()[1]),
                    Math.max(polygon.getArrX()[0], polygon.getArrX()[1]));
                interval.setDirty(true);
                gate = interval;
            }
        }
        if (getEditor().getState().isRunMode())
        {
            GWTWell well = getWell();
            GWTScript script = well.getScript();
            if (script == null)
            {
                GWTScript newScript = new GWTScript();
                newScript.setAnalysis((GWTAnalysis) getEditor().getState().getScript().getAnalysis().duplicate());
                script = newScript;
            }
            GWTPopulation population = script.getAnalysis().findPopulation(getPopulation().getFullName());
            population.setGate(gate);
            getEditor().save(well, script);
        }
        else
        {
            getEditor().getState().getPopulation().setGate(gate);
            getEditor().save(getEditor().getState().getScript());
        }
        setGate(gate);

    }
}
