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

import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.ContainerManager;

/**
 * Use with FeaturesView and TableInfo to filter on a given set of containers
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 14, 2008
 * Time: 9:58:27 AM
 */
public class ContainerFilter implements FeaturesFilter
{
    public static final String NAME = "container";

    private Container[] _containers;

    public ContainerFilter(String containerList)
    {
        String[] ids = containerList.split(",");
        _containers = new Container[ids.length];

        for(int idx = 0; idx < ids.length; ++idx)
        {
            _containers[idx] = ContainerManager.getForId(ids[idx]);
        }
    }

    public ContainerFilter(Container container)
    {
        _containers = new Container[]{container};
    }
    
    public ContainerFilter(Container[] containers)
    {
        _containers = containers;
    }

    public void setFilters(FeaturesTableInfo tinfo)
    {
        tinfo.addCondition(new SQLFragment("FileId IN (SELECT FileId FROM ms1.Files AS f INNER JOIN Exp.Data AS d ON (f.ExpDataFileId=d.RowId) WHERE d.Container IN(" + genListSQL() +"))"),
                "FileId");

    }

    //can't use ListFilterBase for this because Container.toString() returns more than just the container id!
    public String genListSQL()
    {
        StringBuilder sql = new StringBuilder();
        for(Container container : _containers)
        {
            if(null != container)
            {
                sql.append("'");
                sql.append(container.getId());
                sql.append("',");
            }
        }

        if(sql.length() > 0)
            sql.deleteCharAt(sql.length() - 1);

        return sql.toString();
    }
}
