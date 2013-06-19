/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.security.User;

import java.util.List;
import java.util.Map;

/**
 * Use with FeaturesView and TableInfo to filter on a given set of containers
 *
 * User: Dave
 * Date: Jan 14, 2008
 * Time: 9:58:27 AM
 */
public class ContainerFeaturesFilter implements FeaturesFilter
{
    private Container[] _containers;

    public ContainerFeaturesFilter(Container container)
    {
        _containers = new Container[]{container};
    }
    
    public ContainerFeaturesFilter(Container[] containers)
    {
        _containers = containers;
    }

    public ContainerFeaturesFilter(Container container, boolean includeDescendants, User user)
    {
        if(includeDescendants)
        {
            List<Container> containers = ContainerManager.getAllChildren(container, user);
            containers.add(container);
            _containers = new Container[containers.size()];
            containers.toArray(_containers);
        }
        else
            _containers = new Container[]{container};
    }

    public SQLFragment getWhereClause(Map<String, String> aliasMap, SqlDialect dialect)
    {
        String dataTableAlias = aliasMap.get("exp.Data");
        assert(null != dataTableAlias);
        return new SQLFragment(dataTableAlias + ".Container IN (" + genListSQL() + ")");
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
