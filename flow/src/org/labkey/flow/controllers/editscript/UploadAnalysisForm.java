/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.labkey.api.util.SessionHelper;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.StatisticSet;
import org.labkey.flow.analysis.model.Workspace;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class UploadAnalysisForm extends EditScriptForm
{
    private static final Logger _log = LogManager.getLogger(UploadAnalysisForm.class);

    private String token;
    private int existingStatCount = 0;
    public Workspace _workspaceObject;
    public Set<StatisticSet> ff_statisticSet;


    @Override
    public void reset()
    {
        super.reset();
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

    // Stash the workspace in session and reference it via a token
    public String getToken()
    {
        if (_workspaceObject != null && token == null)
        {
            token = SessionHelper.stashAttribute(getRequest(), _workspaceObject, TimeUnit.MINUTES.toMillis(10));
        }
        return token;
    }

    // On form POST, get the workspace from session using the token
    public void setToken(String token)
    {
        this.token = token;
        if (token != null)
        {
            _workspaceObject = (Workspace)SessionHelper.getStashedAttribute(getRequest(), token);
        }
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
