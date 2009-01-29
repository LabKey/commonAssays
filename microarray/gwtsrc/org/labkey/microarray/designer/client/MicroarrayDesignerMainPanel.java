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
import org.labkey.api.gwt.client.ui.*;
import org.labkey.api.gwt.client.model.GWTDomain;
import org.labkey.api.gwt.client.model.GWTPropertyDescriptor;
import com.google.gwt.user.client.ui.*;

/**
 * User: jeckels
 * Date: Feb 6, 2008
 */
public class MicroarrayDesignerMainPanel extends AssayDesignerMainPanel
{
    private BoundTextAreaBox _channelCountTextBox;
    private BoundTextAreaBox _barcodeTextBox;
    private BoundTextBox _sampleSetBarcodeFieldNames;

    public MicroarrayDesignerMainPanel(RootPanel panel, String providerName, Integer protocolId, boolean copyAssay)
    {
        super(panel, providerName, protocolId, copyAssay);
    }

    protected FlexTable createAssayInfoTable(final GWTProtocol assay)
    {
        FlexTable result = super.createAssayInfoTable(assay);
        int row = result.getRowCount();

        final String channelCountValue = assay.getProtocolParameters().get(MicroarrayAssayDesigner.CHANNEL_COUNT_PARAMETER_URI);
        _channelCountTextBox = new BoundTextAreaBox("Channel Count XPath", "ChannelCountXPath", channelCountValue, new WidgetUpdatable()
        {
            public void update(Widget widget)
            {
                assay.getProtocolParameters().put(MicroarrayAssayDesigner.CHANNEL_COUNT_PARAMETER_URI, _channelCountTextBox.getBox().getText());
            }
        }, this);
        result.setHTML(row, 0, "Channel Count XPath");
        result.setWidget(row++, 1, _channelCountTextBox);

        final String barcodeXPathValue = assay.getProtocolParameters().get(MicroarrayAssayDesigner.BARCODE_PARAMETER_URI);
        _barcodeTextBox = new BoundTextAreaBox("Barcode XPath", "BarcodeXPath", barcodeXPathValue, new WidgetUpdatable()
        {
            public void update(Widget widget)
            {
                assay.getProtocolParameters().put(MicroarrayAssayDesigner.BARCODE_PARAMETER_URI, _barcodeTextBox.getBox().getText());
            }
        }, this);
        result.setHTML(row, 0, "Barcode XPath");
        result.setWidget(row++, 1, _barcodeTextBox);

        String barcodeFieldNames = assay.getProtocolParameters().get(MicroarrayAssayDesigner.BARCODE_FIELD_NAMES_PARAMETER_URI);
        _sampleSetBarcodeFieldNames = new BoundTextBox("Barcode Field Names", "barcodeFieldNames", barcodeFieldNames, new WidgetUpdatable()
        {
            public void update(Widget widget)
            {
                assay.getProtocolParameters().put(MicroarrayAssayDesigner.BARCODE_FIELD_NAMES_PARAMETER_URI, _sampleSetBarcodeFieldNames.getBox().getText());
            }
        }, this);

        result.setHTML(row, 0, "Barcode Field Names");
        result.setWidget(row++, 1, _sampleSetBarcodeFieldNames);

        return result;
    }

    protected PropertiesEditor<GWTDomain<GWTPropertyDescriptor>, GWTPropertyDescriptor> createPropertiesEditor(GWTDomain domain)
    {
        return super.createPropertiesEditor(domain);
    }
}
