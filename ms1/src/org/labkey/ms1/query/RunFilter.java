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
    public static final String NAME = "runId";

    private int[] _runIds = null;

    public RunFilter(String runIds)
    {
        //comma-delimited list
        String[] ids = runIds.split(",");
        _runIds =  new int[ids.length];

        for(int idx = 0; idx < ids.length; ++idx)
            _runIds[idx] = Integer.parseInt(ids[idx]);
    }

    public RunFilter(int runId)
    {
        _runIds = new int[]{runId};
    }

    public RunFilter(int[] runIds)
    {
        _runIds = runIds;
    }

    public void setFilters(FeaturesTableInfo tinfo)
    {
        if(null != _runIds)
            tinfo.addCondition(new SQLFragment("FileId IN (SELECT FileId FROM ms1.Files AS f INNER JOIN Exp.Data AS d ON (f.ExpDataFileId=d.RowId) WHERE d.RunId IN(" + genListSQL(_runIds) +"))"),
                                "FileId");
    }

}
