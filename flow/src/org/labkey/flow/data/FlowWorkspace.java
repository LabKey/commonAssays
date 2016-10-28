/*
 * Copyright (c) 2011-2016 LabKey Corporation
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

package org.labkey.flow.data;

import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.controllers.FlowParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * User: kevink
 * Date: 9/29/11
 *
 * Represents an imported FlowJo Workspace ExpData object.
 *
 * Unlike the other FlowDataObject types, there is no row in flow.object table for the FlowWorkspace.
 * It is only represented by a row in the exp.data table.
 *
 * @since 11.3
 */
public class FlowWorkspace extends FlowDataObject
{
    public static List<FlowWorkspace> getWorkspaces(Container container)
    {
        return (List)FlowDataObject.fromDataType(container, FlowDataType.Workspace);
    }

    public static FlowWorkspace fromWorkspaceId(int id)
    {
        if (id == 0)
            return null;

        return new FlowWorkspace(ExperimentService.get().getExpData(id));
    }

    public static FlowWorkspace fromRunId(int runId)
    {
        FlowRun run = FlowRun.fromRunId(runId);
        if (run == null)
            return null;

        return run.getWorkspace();
    }

    static public FlowWorkspace fromLSID(String lsid)
    {
        ExpData data = ExperimentService.get().getExpData(lsid);
        if (data == null)
            return null;
        return new FlowWorkspace(data);
    }

    static public FlowWorkspace fromURL(ActionURL url, Container actionContainer, User user)
    {
        return fromURL(url, null, actionContainer, user);
    }

    static public FlowWorkspace fromURL(ActionURL url, HttpServletRequest request, Container actionContainer, User user)
    {
        FlowWorkspace ret = FlowWorkspace.fromWorkspaceId(getIntParam(url, request, FlowParam.workspaceId));
        if (ret == null || ret.getExpObject() == null)
            return null;
        ret.checkContainer(actionContainer, user, url);
        return ret;
    }
    public FlowWorkspace(ExpData data)
    {
        super(data);
    }

    public int getWorkspaceId()
    {
        return getRowId();
    }

    @Override
    public void addParams(Map<FlowParam, Object> map)
    {
        map.put(FlowParam.workspaceId, getWorkspaceId());
    }

    @Override
    public ActionURL urlShow()
    {
        // UNDONE: Need a details page for the imported workspace.
        return null;
    }

    @Override
    public String getLabel()
    {
        return "Workspace '" + getName() + "'";
    }

    @Override
    public ActionURL urlDownload()
    {
        if (!getData().isFileOnDisk())
            return null;
        ActionURL url = PageFlowUtil.urlProvider(ExperimentUrls.class).getShowFileURL(getData(), false);
        return url;
    }

}
