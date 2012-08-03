/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

package org.labkey.ms2;

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.ms2.MS2Service;
import org.labkey.api.ms2.SearchClient;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.security.User;
import org.labkey.api.query.FieldKey;
import org.labkey.ms2.pipeline.mascot.MascotClientImpl;
import org.labkey.ms2.query.PeptidesTableInfo;
import org.labkey.ms2.query.MS2Schema;
import org.apache.log4j.Logger;

/**
 * User: jeckels
 * Date: Jan 9, 2007
 */
public class MS2ServiceImpl implements MS2Service.Service
{
    public String getRunsTableName()
    {
        return MS2Manager.getTableInfoRuns().toString();
    }

    public SearchClient createSearchClient(String server, String url, Logger instanceLogger, String userAccount, String userPassword)
    {
        if(server.equalsIgnoreCase("mascot"))
            return new MascotClientImpl(url, instanceLogger, userAccount, userPassword);
        return null;
    }

    public TableInfo createPeptidesTableInfo(User user, Container container)
    {
        return createPeptidesTableInfo(user, container, true, ContainerFilter.CURRENT, null, null);
    }

    public TableInfo createSequencesTableInfo(User user, Container container)
    {
        return new MS2Schema(user, container).getTable(MS2Schema.TableType.Sequences.toString());
    }

    public MS2Schema createSchema(User user, Container container)
    {
        return new MS2Schema(user, container);
    }

    public TableInfo createPeptidesTableInfo(User user, Container container, boolean includeFeatureFk, ContainerFilter containerFilter, SimpleFilter filter, Iterable<FieldKey> defaultColumns)
    {
        PeptidesTableInfo table = new PeptidesTableInfo(new MS2Schema(user, container), includeFeatureFk, containerFilter, MS2RunType.values());
        if(null != filter)
            table.addCondition(filter);
        if(null != defaultColumns)
            table.setDefaultVisibleColumns(defaultColumns);
        return table;
    }

}
