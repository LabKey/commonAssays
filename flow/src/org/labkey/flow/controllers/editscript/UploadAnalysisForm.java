/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.flow.controllers.editscript;

import org.apache.struts.upload.FormFile;
import org.apache.struts.action.ActionMapping;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.StatisticSet;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class UploadAnalysisForm extends EditScriptForm
{
    private static final Logger _log = Logger.getLogger(UploadAnalysisForm.class);

    private int existingStatCount = 0;
    public FlowJoWorkspace workspaceObject;
    public Set<StatisticSet> ff_statisticSet;


    public void reset(ActionMapping actionMapping, HttpServletRequest servletRequest)
    {
        super.reset(actionMapping, servletRequest);
        ff_statisticSet = EnumSet.of(StatisticSet.existing, StatisticSet.workspace, StatisticSet.count, StatisticSet.frequencyOfParent);
        try
        {
            Analysis analysis = (Analysis) getAnalysis();
            if (analysis != null)
            {
                existingStatCount = analysis.getStatistics().size();
                if (existingStatCount != 0)
                {
                    ff_statisticSet = EnumSet.of(StatisticSet.existing);
                }
            }
        }
        catch (Exception e)
        {
            _log.error("Error", e);
        }
    }

    public void setWorkspaceObject(String object) throws Exception
    {
        workspaceObject = (FlowJoWorkspace) PageFlowUtil.decodeObject(object);
    }
    public String groupName;
    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    public String sampleId;
    public void setSampleId(String sampleName)
    {
        this.sampleId = sampleName;
    }

    public void setFf_statisticSet(String[] values)
    {
        ff_statisticSet = EnumSet.noneOf(StatisticSet.class);
        for (String value : values)
        {

            if (StringUtils.isEmpty(value))
                continue;
            ff_statisticSet.add(StatisticSet.valueOf(value));
        }
    }

    public int getExistingStatCount()
    {
        return existingStatCount;
    }
}
