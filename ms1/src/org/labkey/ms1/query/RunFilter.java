/*
 * Copyright (c) 2007 LabKey Software Foundation
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
package org.labkey.ms1.query;

import org.labkey.api.data.SQLFragment;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.ms1.MS1Controller;

import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Use with the FeaturesView and TableInfo to filter on one or more runs
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 14, 2008
 * Time: 9:48:41 AM
 */
public class RunFilter extends ListFilterBase implements FeaturesFilter
{
    private int[] _runIds = null;

    public RunFilter(String runIds)
    {
        //comma-delimited list
        _runIds = PageFlowUtil.toInts(runIds.split(","));
    }

    public RunFilter(int runId)
    {
        _runIds = new int[]{runId};
    }

    public RunFilter(int[] runIds)
    {
        _runIds = runIds;
    }

    public String getRunIdString()
    {
        if(null == _runIds)
            return "";
        else
            return genListSQL(_runIds);
    }

    public void setFilters(FeaturesTableInfo tinfo)
    {
        if(null != _runIds)
            tinfo.addCondition(new SQLFragment("FileId IN (SELECT FileId FROM ms1.Files AS f INNER JOIN Exp.Data AS d ON (f.ExpDataFileId=d.RowId) WHERE d.RunId IN(" + genListSQL(_runIds) +"))"),
                                "FileId");
    }

}
