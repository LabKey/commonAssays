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

package org.labkey.flow.gateeditor.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.labkey.flow.gateeditor.client.model.*;

public interface GateEditorServiceAsync
{
    void getRuns(AsyncCallback asyncCallback);
    void getWorkspace(GWTWorkspaceOptions workspaceOptions, AsyncCallback asyncCallback);
    void getGraphInfo(GWTGraphOptions graphOptions, AsyncCallback asyncCallback);
    void getCompensationMatrices(AsyncCallback asyncCallback);
    void save(GWTScript script, AsyncCallback asyncCallback);
    void save(GWTWell well, GWTScript script, AsyncCallback asyncCallback);
}
