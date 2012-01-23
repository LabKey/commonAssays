package org.labkey.ms2.pipeline.client;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.labkey.api.gwt.client.pipeline.GWTPipelineConfig;
import org.labkey.api.gwt.client.pipeline.GWTPipelineLocation;
import org.labkey.api.gwt.client.pipeline.GWTPipelineTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: jeckels
 * Date: Jan 20, 2012
 */
public class LocationComposite extends SearchFormComposite implements AsyncCallback<GWTPipelineConfig>
{
    protected VerticalPanel instance = new VerticalPanel();
    private GWTPipelineConfig _pipelineConfig;

    public LocationComposite()
    {
        init();
    }

    @Override
    public void init()
    {
        initWidget(instance);
    }

    @Override
    public void setWidth(String width)
    {
    }

    @Override
    public Widget getLabel(String style)
    {
        Label result = new Label("Location");
        result.setStyleName(style);
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

    public void onFailure(Throwable caught)
    {
        instance.add(new Label(caught.toString()));
    }

    public void onSuccess(GWTPipelineConfig result)
    {
        _pipelineConfig = result;

        List<String> clusterLocations = new ArrayList<String>();
        for (GWTPipelineLocation location : _pipelineConfig.getLocations())
        {
            if (location.isCluster())
            {
                clusterLocations.add(location.getLocation());
            }
        }
        Collections.sort(clusterLocations);

        FlexTable table = new FlexTable();
        int col = 0;
        table.setHTML(0, col++, "<strong>Task Name</strong>");
        table.setHTML(0, col++, "<strong>Group Name</strong>");
        if (clusterLocations.size() > 1)
        {
            table.setHTML(0, col++, "<strong>Location</strong>");
        }
        table.setHTML(0, col++, "<strong>Queue</strong>");

        int row = 1;

        for (final GWTPipelineTask task : result.getTasks())
        {
            if (task.isCluster())
            {
                col = 0;
                table.setText(row, col++, task.getName());
                table.setText(row, col++, task.getGroupName());

                final ListBox queuesListBox = new ListBox();
                if (clusterLocations.size() > 1)
                {

                    final ListBox locationListBox = new ListBox();
                    for (String clusterLocation : clusterLocations)
                    {
                        locationListBox.addItem(clusterLocation);
                        if (task.getDefaultLocation().getLocation().equals(clusterLocation))
                        {
                            locationListBox.setSelectedIndex(locationListBox.getItemCount() - 1);
                        }
                    }
                    table.setWidget(row, col++, locationListBox);

                    locationListBox.addChangeHandler(new ChangeHandler()
                    {
                        public void onChange(ChangeEvent event)
                        {
                            updateQueues(queuesListBox, locationListBox.getItemText(locationListBox.getSelectedIndex()), task);
                        }
                    });
                    updateQueues(queuesListBox, task.getDefaultLocation().getLocation(), task);
                }
                table.setWidget(row, col++, queuesListBox);
                row++;
            }
        }

        if (table.getRowCount() > 1)
        {
            instance.add(table);
        }
        else
        {
            instance.add(new Label("No tasks to be configured"));
        }
    }

    private void updateQueues(ListBox queuesListBox, String locationName, GWTPipelineTask task)
    {
        GWTPipelineLocation selectedLocation = null;
        for (GWTPipelineLocation location : _pipelineConfig.getLocations())
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
        }
        queuesListBox.addItem("Other...");
    }
}
