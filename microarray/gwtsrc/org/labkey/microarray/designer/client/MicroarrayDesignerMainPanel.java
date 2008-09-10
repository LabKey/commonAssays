/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.microarray.designer.client;

import org.labkey.api.gwt.client.assay.AssayDesignerMainPanel;
import org.labkey.api.gwt.client.assay.model.GWTProtocol;
import org.labkey.api.gwt.client.ui.PropertiesEditor;
import org.labkey.api.gwt.client.ui.WidgetUpdatable;
import org.labkey.api.gwt.client.ui.BoundTextAreaBox;
import org.labkey.api.gwt.client.ui.BoundListBox;
import org.labkey.api.gwt.client.model.GWTDomain;
import org.labkey.microarray.sampleset.client.SampleSetService;
import org.labkey.microarray.sampleset.client.model.GWTSampleSet;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.Window;

import java.util.List;

/**
 * User: jeckels
 * Date: Feb 6, 2008
 */
public class MicroarrayDesignerMainPanel extends AssayDesignerMainPanel
{
    private BoundTextAreaBox _channelCountTextBox;
    private BoundTextAreaBox _barcodeTextBox;
    private BoundListBox _sampleSetsList;
    private BoundListBox _sampleColumnsList;

    private GWTSampleSet[] _sampleSets;

    public MicroarrayDesignerMainPanel(RootPanel panel, String providerName, Integer protocolId, boolean copyAssay)
    {
        super(panel, providerName, protocolId, copyAssay);
    }

    protected FlexTable createAssayInfoTable(final GWTProtocol assay)
    {
        FlexTable result = super.createAssayInfoTable(assay);
        int row = result.getRowCount();

        final String channelCountValue = (String)assay.getProtocolParameters().get(MicroarrayAssayDesigner.CHANNEL_COUNT_PARAMETER_URI);
        _channelCountTextBox = new BoundTextAreaBox("Channel Count XPath", "ChannelCountXPath", channelCountValue, new WidgetUpdatable()
        {
            public void update(Widget widget)
            {
                assay.getProtocolParameters().put(MicroarrayAssayDesigner.CHANNEL_COUNT_PARAMETER_URI, ((TextBox) widget).getText());
                if (!((TextBox) widget).getText().equals(channelCountValue))
                {
                    setDirty(true);
                }
            }
        }, this);
        result.setHTML(row, 0, "Channel Count XPath");
        result.setWidget(row++, 1, _channelCountTextBox);

        final String barcodeXPathValue = (String)assay.getProtocolParameters().get(MicroarrayAssayDesigner.BARCODE_PARAMETER_URI);
        _barcodeTextBox = new BoundTextAreaBox("Barcode XPath", "BarcodeXPath", barcodeXPathValue, new WidgetUpdatable()
        {
            public void update(Widget widget)
            {
                assay.getProtocolParameters().put(MicroarrayAssayDesigner.BARCODE_PARAMETER_URI, ((TextBox) widget).getText());
                if (!((TextBox) widget).getText().equals(barcodeXPathValue))
                {
                    setDirty(true);
                }
            }
        }, this);
        result.setHTML(row, 0, "Barcode XPath");
        result.setWidget(row++, 1, _barcodeTextBox);

        _sampleSetsList = new BoundListBox(false, new WidgetUpdatable()
        {
            public void update(Widget widget)
            {
                ListBox list = (ListBox)widget;
                int index = list.getSelectedIndex();
                String lsid = list.getSelectedIndex() == -1 ? null : list.getValue(index);
                assay.getProtocolParameters().put(MicroarrayAssayDesigner.SAMPLE_SET_LSID_PARAMETER_URI, lsid);
                if (_sampleSets != null)
                {
                    for (int i = 0; i < _sampleSets.length; i++)
                    {
                        GWTSampleSet sampleSet = _sampleSets[i];
                        if (sampleSet.getLsid().equals(lsid))
                        {
                            updateSampleSetColumns(sampleSet);
                            return;
                        }
                    }
                }
                updateSampleSetColumns(null);
            }
        }, this);

        _sampleColumnsList = new BoundListBox(false, new WidgetUpdatable()
        {
            public void update(Widget widget)
            {
                ListBox list = (ListBox)widget;
                int index = list.getSelectedIndex();
                String columnName = list.getSelectedIndex() == -1 ? null : list.getValue(index);
                assay.getProtocolParameters().put(MicroarrayAssayDesigner.SAMPLE_BARCODE_COLUMN_NAME_PARAMETER_URI, columnName);
            }
        }, this);

        result.setHTML(row, 0, "Barcode Source");
        HorizontalPanel sampleSetPanel = new HorizontalPanel();
        sampleSetPanel.add(_sampleSetsList);
        sampleSetPanel.add(_sampleColumnsList);
        result.setWidget(row++, 1, sampleSetPanel);

        updateSampleSets();

        return result;
    }

    public void showAsync()
    {
        super.showAsync();
        SampleSetService.App.getService().getSampleSets(new AsyncCallback()
        {
            public void onFailure(Throwable caught)
            {
                Window.alert("Failed to get sample sets: " + caught);
            }

            public void onSuccess(Object result)
            {
                _sampleSets = (GWTSampleSet[])result;
                updateSampleSets();
            }
        });
    }

    private void updateSampleSets()
    {
        if (_sampleSetsList == null)
        {
            return;
        }

        _sampleSetsList.clear();
        _sampleSetsList.addItem("<No default sample set>", "");
        _sampleSetsList.setSelectedIndex(0);

        // Make sure that we both have the sample sets and the assay definition since they're both populated async
        if (_sampleSets != null && _assay != null)
        {
            String selectedLSID = (String)_assay.getProtocolParameters().get(MicroarrayAssayDesigner.SAMPLE_SET_LSID_PARAMETER_URI);
            for (int i = 0; i < _sampleSets.length; i++)
            {
                GWTSampleSet sampleSet = _sampleSets[i];
                _sampleSetsList.addItem(sampleSet.getName(), sampleSet.getLsid());
                if (selectedLSID != null && selectedLSID.equals(sampleSet.getLsid()))
                {
                    _sampleSetsList.setSelectedIndex(_sampleSetsList.getItemCount() - 1);
                    updateSampleSetColumns(sampleSet);
                }
            }
        }
        if (_sampleSetsList.getSelectedIndex() <= 0)
        {
            updateSampleSetColumns(null);
        }

        _sampleSetsList.setEnabled(_sampleSets != null);
    }

    private void updateSampleSetColumns(GWTSampleSet sampleSet)
    {
        _sampleColumnsList.clear();
        _sampleColumnsList.addItem("<No barcode column>", "");
        _sampleColumnsList.setSelectedIndex(0);

        if (sampleSet != null)
        {
            String selectedColumnName = (String)_assay.getProtocolParameters().get(MicroarrayAssayDesigner.SAMPLE_BARCODE_COLUMN_NAME_PARAMETER_URI);
            for (int j = 0; j < sampleSet.getColumnNames().size(); j++)
            {
                String columnName = (String)sampleSet.getColumnNames().get(j);
                _sampleColumnsList.addItem(columnName, columnName);
                if ((selectedColumnName == null && "barcode".equalsIgnoreCase(columnName)) || (selectedColumnName != null && selectedColumnName.equals(columnName)))
                {
                    _sampleColumnsList.setSelectedIndex(_sampleColumnsList.getItemCount() - 1);
                    _assay.getProtocolParameters().put(MicroarrayAssayDesigner.SAMPLE_BARCODE_COLUMN_NAME_PARAMETER_URI, columnName);
                }
            }
        }
        _sampleColumnsList.setEnabled(sampleSet != null);
    }


    protected PropertiesEditor createPropertiesEditor(GWTDomain domain)
    {
        return super.createPropertiesEditor(domain);
    }
}
