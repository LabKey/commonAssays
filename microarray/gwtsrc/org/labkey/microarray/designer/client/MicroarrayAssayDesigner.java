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
    public void onModuleLoad()
    {
        RootPanel panel = RootPanel.get("org.labkey.microarray.designer.MicroarrayAssayDesigner-Root");
        if (panel != null)
        {
            String protocolIdStr = PropertyUtil.getServerProperty("protocolId");
            String providerName = PropertyUtil.getServerProperty("providerName");
            String copyStr = PropertyUtil.getServerProperty("copy");
            boolean copyAssay = copyStr != null && Boolean.TRUE.toString().equals(copyStr);
            MicroarrayDesignerMainPanel view = new MicroarrayDesignerMainPanel(panel, providerName, protocolIdStr != null ? new Integer(Integer.parseInt(protocolIdStr)) : null, copyAssay);
            view.showAsync();
        }
    }
}
