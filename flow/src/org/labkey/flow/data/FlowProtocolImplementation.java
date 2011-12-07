/*
 * Copyright (c) 2006-2011 LabKey Corporation
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
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Table;
import org.labkey.api.exp.api.ProtocolImplementation;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.security.User;

import java.util.List;
import java.sql.SQLException;

public class FlowProtocolImplementation extends ProtocolImplementation
{
    static public final String NAME = "flow";
    static public void register()
    {
        ExperimentService.get().registerProtocolImplementation(new FlowProtocolImplementation());
    }
    public FlowProtocolImplementation()
    {
        super(NAME);
    }

    public void onSamplesChanged(User user, ExpProtocol expProtocol, ExpMaterial[] materials) throws SQLException
    {
        FlowProtocol protocol = new FlowProtocol(expProtocol);
        protocol.updateSampleIds(user);
    }

    @Override
    public void onRunDeleted(Container container, User user) throws SQLException
    {
        SQLFragment sql = new SQLFragment();
        sql.append("DELETE FROM exp.data WHERE rowid IN (\n");
        sql.append("  SELECT d.rowid\n");
        sql.append("  FROM exp.data d\n");
        sql.append("  WHERE\n");
        sql.append("    d.container = ? AND\n").add(container.getId());
        sql.append("    d.sourceapplicationid IS NULL AND\n");
        sql.append("    d.runid IS NULL AND\n");
        sql.append("    (d.lsid LIKE 'urn:lsid:%:Flow-%' OR d.lsid LIKE 'urn:lsid:%:Data.Folder-%') AND\n");
        sql.append("    d.rowid NOT IN (\n");
        sql.append("      SELECT dataid FROM exp.datainput\n");
        sql.append("      UNION\n");
        sql.append("      SELECT dataid FROM flow.object\n");
        sql.append("    )\n");
        sql.append(")\n");

        Table.execute(ExperimentService.get().getSchema(), sql);
    }
}
