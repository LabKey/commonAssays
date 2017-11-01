/*
 * Copyright (c) 2010-2017 LabKey Corporation
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

package org.labkey.flow;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.flow.api.FlowService;
import org.labkey.flow.persist.FlowManager;

import java.util.LinkedList;
import java.util.List;


/**
 * User: matthewb
 * Date: Oct 25, 2010
 * Time: 11:16:08 AM
 */

public class FlowServiceImpl implements FlowService
{
    @Override
    public List<ExpData> getExpDataByURL(String canonicalURL, @Nullable Container container)
    {
        List<ExpData> ret = new LinkedList<>();
        SQLFragment sql = new SQLFragment("SELECT dataid FROM ").append(FlowManager.get().getTinfoObject().getFromSQL("O")).append(" WHERE uri=?");
        sql.add(canonicalURL);

        if (null != container)
        {
            sql.append(" AND container=?");
            sql.add(container);
        }

        sql.append(" ORDER BY 1");

        Integer[] dataids = new SqlSelector(FlowManager.get().getSchema(), sql).getArray(Integer.class);

        for (Integer dataid : dataids)
        {
            ExpData data = ExperimentService.get().getExpData(dataid);
            if (null != data)
                ret.add(data);
        }

        return ret;
    }

    @Override
    public int getTempTableCount()
    {
        return new SqlSelector(DbScope.getLabKeyScope(), "SELECT COUNT(*) FROM information_schema.tables \n" +
                "WHERE TABLE_SCHEMA = 'temp' and (table_name LIKE 'ffo%' or table_name LIKE 'fbg%')").getArrayList(Integer.class).get(0);
    }
}
