/*
 * Copyright (c) 2012 LabKey Corporation
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
package org.labkey.ms2.pipeline.client;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.labkey.api.gwt.client.pipeline.GWTPipelineConfig;
import org.labkey.api.gwt.client.pipeline.GWTPipelineLocation;
import org.labkey.api.gwt.client.pipeline.GWTPipelineTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UI to let a user choose between multiple clusters (if there's more than one) and/or set the cluster queue for execution 
 * User: jeckels
 * Date: Jan 20, 2012
 */
public class LocationComposite extends SearchFormComposite implements PipelineConfigCallback
{
    protected VerticalPanel instance = new VerticalPanel();
    private List<GWTPipelineLocation> _pipelineLocations;
    private List<TaskUserInterface> _taskUserInterfaces;
    private ChangeHandler _handler;
    private FlexTable _editableTable;
    private FlexTable _readOnlyTable;
    private final InputXmlComposite _inputXml;

    public LocationComposite(InputXmlComposite inputXml)
    {
        _inputXml = inputXml;
        initWidget(instance);
    }

    @Override
    public void setWidth(String width)
    {
    }

    @Override
    public Widget getLabel()
    {
        Label result = new Label("Execution location");
        result.setStyleName(LABEL_STYLE_NAME);
        return result;
    }

    @Override
    public String validate()
    {
        return null;
    }

    public void setName(String name)
    {
        
    }

    public String getName()
    {
        return null;
    }

    /** Callback from requesting info on the pipeline tasks and potential execution locations */
    public void setPipelineConfig(GWTPipelineConfig result)
    {
        _pipelineLocations = result.getLocations();
        _taskUserInterfaces = new ArrayList<TaskUserInterface>();

        _editableTable = new FlexTable();

        int row = initializeTable(_editableTable);

        List<String> clusterLocations = getClusterLocations();
        for (final GWTPipelineTask task : result.getTasks())
        {
            if (task.isCluster())
            {
                int col = 0;
                _editableTable.setText(row, col++, task.getGroupName());

                final TextBox queueTextBox = new TextBox();
                final ListBox queuesListBox = new ListBox();
                queueTextBox.setVisible(false);
                TaskUserInterface taskUserInterface;
                if (clusterLocations.size() > 1)
                {
                    // More than one cluster location available, so show a column with a combo box to choose their favorite
                    final ListBox locationListBox = new ListBox();
                    for (String clusterLocation : clusterLocations)
                    {
                        locationListBox.addItem(clusterLocation);
                        if (task.getDefaultLocation().getLocation().equals(clusterLocation))
                        {
                            locationListBox.setSelectedIndex(locationListBox.getItemCount() - 1);
                        }
                    }
                    _editableTable.setWidget(row, col++, locationListBox);

                    // When the user changes the location, update the queues drop down to match
                    locationListBox.addChangeHandler(new ChangeHandler()
                    {
                        public void onChange(ChangeEvent event)
                        {
                            updateQueues(queuesListBox, locationListBox.getItemText(locationListBox.getSelectedIndex()));
                        }
                    });
                    taskUserInterface = new TaskUserInterface(task, locationListBox, queuesListBox, queueTextBox);
                }
                else
                {
                    taskUserInterface = new TaskUserInterface(task, null, queuesListBox, queueTextBox);
                }
                
                updateQueues(queuesListBox, task.getDefaultLocation().getLocation());
                _taskUserInterfaces.add(taskUserInterface);
                // Let the user choose one of the predetermined queues from the drop down, or type in their own queue name
                HorizontalPanel queuePanel = new HorizontalPanel();
                queuePanel.add(queuesListBox);
                queuePanel.add(queueTextBox);
                _editableTable.setWidget(row, col++, queuePanel);
                queuesListBox.addChangeHandler(new ChangeHandler()
                {
                    public void onChange(ChangeEvent event)
                    {
                        if (queuesListBox.getSelectedIndex() == queuesListBox.getItemCount() - 1)
                        {
                            queueTextBox.setVisible(true);
                        }
                    }
                });
                row++;
            }
        }

        instance.add(_editableTable);
        if (_inputXml.params != null)
        {
            syncXmlToForm(_inputXml.params);
        }
        if (readOnly)
        {
            setReadOnly(true);
        }
        
        setVisibilityInParentTable();
    }

    private List<String> getClusterLocations()
    {
        List<String> clusterLocations = new ArrayList<String>();
        for (GWTPipelineLocation location : _pipelineLocations)
        {
            if (location.isCluster())
            {
                clusterLocations.add(location.getLocation());
            }
        }
        Collections.sort(clusterLocations);
        return clusterLocations;
    }

    /** Create a row with column headers and set some CSS. Don't create a Location column if there's only one cluster */
    private int initializeTable(FlexTable table)
    {
        int col = 0;
        int row = 0;

        table.setStyleName("labkey-show-borders");
        
        table.setText(row, col, "Group Name");
        table.getCellFormatter().addStyleName(row, col++, "labkey-strong");
        List<String> clusterLocations = getClusterLocations();
        if (clusterLocations.size() > 1)
        {
            table.setText(row, col, "Location");
            table.getCellFormatter().addStyleName(row, col++, "labkey-strong");
        }
        table.setText(row, col, "Queue");
        table.getCellFormatter().addStyleName(row, col++, "labkey-strong");
        return row + 1;
    }

    /** Update the combo box with the queues that are known for the current cluster location */
    private void updateQueues(ListBox queuesListBox, String locationName)
    {
        GWTPipelineLocation selectedLocation = null;
        for (GWTPipelineLocation location : _pipelineLocations)
        {
            if (location.getLocation().equals(locationName))
            {
                selectedLocation = location;
                break;
            }
        }

        if (selectedLocation != null)
        {
            queuesListBox.clear();
            for (String queueName : selectedLocation.getQueues())
            {
                queuesListBox.addItem(queueName);
            }
            // Let the user type in their own queue name 
            queuesListBox.addItem("Other...");
        }
    }

    @Override
    public void syncFormToXml(ParamParser params) throws SearchFormException
    {
        if (_taskUserInterfaces != null)
        {
            for (TaskUserInterface taskUserInterface : _taskUserInterfaces)
            {
                List<String> clusterLocations = getClusterLocations();
                String locationValue = clusterLocations.size() > 1 ? taskUserInterface._locationListBox.getValue(taskUserInterface._locationListBox.getSelectedIndex()) : clusterLocations.get(0);
                if (taskUserInterface._locationListBox != null)
                {
                    // There's more than one cluster available
                    String locationParamName = getLocationParamName(taskUserInterface);
                    params.removeInputParameter(locationParamName);
                    if (!locationValue.equals(taskUserInterface._task.getDefaultLocation().getLocation()))
                    {
                        // It's not the default location, so include it in the XML
                        params.setInputParameter(locationParamName, locationValue);
                    }
                }
                String queueParamName = getQueueParamName(taskUserInterface);
                params.removeInputParameter(queueParamName);
                String queueParamValue;
                if (taskUserInterface._queueListBox.getSelectedIndex() == taskUserInterface._queueListBox.getItemCount() - 1)
                {
                    // The user has chosen "Other..." and typed in a name
                    queueParamValue = taskUserInterface._queueTextBox.getValue();
                }
                else
                {
                    // The user has selected one of the options in the combo box
                    queueParamValue = taskUserInterface._queueListBox.getValue(taskUserInterface._queueListBox.getSelectedIndex());
                }
                GWTPipelineLocation location = findLocation(locationValue);
                if (location != null && !queueParamValue.equals(location.getQueues().get(0)))
                {
                    params.setInputParameter(queueParamName, queueParamValue);
                }
            }
        }
    }

    private String getLocationParamName(TaskUserInterface taskUserInterface)
    {
        return taskUserInterface._task.getGroupName() + ", globus location";
    }

    private String getQueueParamName(TaskUserInterface taskUserInterface)
    {
        return taskUserInterface._task.getGroupName() + ", globus queue";
    }

    @Override
    public String syncXmlToForm(ParamParser params)
    {
        if (_taskUserInterfaces != null)
        {
            for (TaskUserInterface taskUserInterface : _taskUserInterfaces)
            {
                if (taskUserInterface._locationListBox != null)
                {
                    // If we have multiple possible locations, sync the combo box
                    String locationValue = params.getInputParameter(getLocationParamName(taskUserInterface));
                    setValue(taskUserInterface._locationListBox, locationValue);
                    updateQueues(taskUserInterface._queueListBox, locationValue);
                }
                else
                {
                    updateQueues(taskUserInterface._queueListBox, taskUserInterface._task.getDefaultLocation().getLocation());
                }

                // Then sync the queue, into the combo box if it's in the list, or show the text field for user-entered names
                String queueValue = params.getInputParameter(getQueueParamName(taskUserInterface));
                if (queueValue == null || "".equals(queueValue))
                {
                    // Use the default queue, which is the first one in the list
                    taskUserInterface._queueListBox.setSelectedIndex(0);
                    taskUserInterface._queueTextBox.setVisible(false);
                }
                else
                {
                    int index = findValue(taskUserInterface._queueListBox, queueValue);
                    if (index != -1)
                    {
                        // We have the right queue in the combo box
                        taskUserInterface._queueListBox.setSelectedIndex(index);
                        taskUserInterface._queueTextBox.setVisible(false);
                    }
                    else
                    {
                        // We don't have it in the combo box, so populate the text field
                        taskUserInterface._queueListBox.setSelectedIndex(taskUserInterface._queueListBox.getItemCount() - 1);
                        taskUserInterface._queueTextBox.setValue(queueValue);
                        taskUserInterface._queueTextBox.setVisible(true);
                    }
                }
            }
        }
        if (readOnly)
        {
            setReadOnly(true);
        }
        return "";
    }

    public void setReadOnly(boolean readOnly)
    {
        super.setReadOnly(readOnly);

        if(readOnly)
        {
            // Create a read-only version of the table based on the current values in the editable copy
            if (_editableTable != null)
            {
                if (_readOnlyTable != null)
                {
                    instance.remove(_readOnlyTable);
                }
                _readOnlyTable = createReadOnlyTable();

                instance.remove(_editableTable);
                instance.insert(_readOnlyTable, 0);
            }

        }
        else
        {
            // Yank out the read only version and replace it with the editable one
            if (_readOnlyTable != null)
            {
                instance.remove(_readOnlyTable);
                instance.add(_editableTable);
                _readOnlyTable = null;
            }
        }
    }

    /** Create a read-only table based on the values currently in the editable version of the UI */
    private FlexTable createReadOnlyTable()
    {
        FlexTable flexTable = new FlexTable();

        int row = initializeTable(flexTable);
        if (_taskUserInterfaces != null)
        {
            for (TaskUserInterface taskUserInterface : _taskUserInterfaces)
            {
                int col = 0;
                flexTable.setText(row, col++, taskUserInterface._task.getGroupName());
                if (taskUserInterface._locationListBox != null)
                {
                    flexTable.setText(row, col++, taskUserInterface._locationListBox.getItemText(taskUserInterface._locationListBox.getSelectedIndex()));
                }
                if (taskUserInterface._queueListBox.getSelectedIndex() == taskUserInterface._queueListBox.getItemCount() - 1)
                {
                    flexTable.setText(row, col++, taskUserInterface._queueTextBox.getValue());
                }
                else
                {
                    flexTable.setText(row, col++, taskUserInterface._queueListBox.getItemText(taskUserInterface._queueListBox.getSelectedIndex()));
                }
                row++;
            }
        }

        return flexTable;
        
    }


    private boolean setValue(ListBox listBox, String value)
    {
        if (value != null)
        {
            int index = findValue(listBox, value);
            if (index >= 0)
            {
                boolean result = listBox.getSelectedIndex() == index;
                listBox.setSelectedIndex(index);
                return result;
            }
        }
        return false;
    }

    private int findValue(ListBox listBox, String value)
    {
        for (int i = 0; i < listBox.getItemCount(); i++)
        {
            if (value.equals(listBox.getItemText(i)))
            {
                return i;
            }
        }
        return -1;
    }

    public void addChangeListener(ChangeHandler handler)
    {
        _handler = handler;
    }

    private GWTPipelineLocation findLocation(String name)
    {
        if (_pipelineLocations == null)
        {
            return null;
        }
        for (GWTPipelineLocation pipelineLocation : _pipelineLocations)
        {
            if (pipelineLocation.getLocation().equals(name))
            {
                return pipelineLocation;
            }
        }
        return null;
    }

    private class TaskUserInterface
    {
        private GWTPipelineTask _task;
        private ListBox _locationListBox;
        private ListBox _queueListBox;
        private TextBox _queueTextBox;

        private TaskUserInterface(GWTPipelineTask task, @Nullable ListBox locationListBox, @NotNull ListBox queueListBox, TextBox queueTextBox)
        {
            _task = task;
            _locationListBox = locationListBox;
            _queueListBox = queueListBox;
            _queueTextBox = queueTextBox;

            if (_locationListBox != null)
            {
                _locationListBox.addChangeHandler(_handler);
            }
            _queueListBox.addChangeHandler(_handler);
            _queueTextBox.addChangeHandler(_handler);
        }
    }

    @Override
    public void configureCompositeRow(FlexTable table, int row)
    {
        super.configureCompositeRow(table, row);
        setVisibilityInParentTable();
    }

    private void setVisibilityInParentTable()
    {
        boolean visible = _taskUserInterfaces != null && !_taskUserInterfaces.isEmpty();
        _parentTable.getRowFormatter().setVisible(_parentTableRow, visible);
    }

    @Override
    public boolean isHandledParameterName(String name)
    {
        name = name.toLowerCase();
        return name.endsWith(", globus location") || name.endsWith(", globus queue");
    }
}
