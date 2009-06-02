/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;
import org.labkey.api.gwt.client.util.PropertyUtil;

/**
 * User: brittp
 * Date: Jun 20, 2007
 * Time: 2:23:06 PM
 */
public class MicroarrayAssayDesigner implements EntryPoint
{
    public static final String CHANNEL_COUNT_PARAMETER_URI = "labkey.org#MicroarrayAssay.ChannelCountXPath";
    public static final String BARCODE_PARAMETER_URI = "labkey.org#MicroarrayAssay.BarcodeXPath";
    public static final String BARCODE_FIELD_NAMES_PARAMETER_URI = "labkey.org#MicroarrayAssay.SampleSetBarcodeFieldNames";
    public static final String CY3_SAMPLE_NAME_COLUMN_PARAMETER_URI = "labkey.org#MicroarrayAssay.Cy3SampleNameColumn";
    public static final String CY5_SAMPLE_NAME_COLUMN_PARAMETER_URI = "labkey.org#MicroarrayAssay.Cy5SampleNameColumn";

    public void onModuleLoad()
    {
        RootPanel panel = RootPanel.get("org.labkey.microarray.designer.MicroarrayAssayDesigner-Root");
        if (panel != null)
        {
            MicroarrayDesignerMainPanel view = new MicroarrayDesignerMainPanel(panel);
            view.showAsync();
        }
    }
}
