/*
 * Copyright (c) 2007 LabKey Corporation
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

package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;
import java.io.Serializable;

public class GWTGraphInfo implements IsSerializable, Serializable
{
    public GWTGraphOptions graphOptions;
    public GWTRectangle rcChart;
    public GWTRectangle rcData;
    public GWTRange rangeX;
    public GWTRange rangeY;
    public String graphURL;

    public GWTPoint toScreen(GWTPoint point)
    {
        return new GWTPoint(rangeX.toScreen(point.x, rcData.width) + rcData.x,
                rcData.y + rcData.height - rangeY.toScreen(point.y, rcData.height));
    }

    public GWTPoint toValue(GWTPoint point)
    {
        return new GWTPoint(rangeX.toValue(point.x - rcData.x, rcData.width),
                rangeY.toValue(rcData.y + rcData.height - point.y, rcData.height));
    }
}
