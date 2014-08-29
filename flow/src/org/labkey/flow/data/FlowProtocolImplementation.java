/*
 * Copyright (c) 2006-2014 LabKey Corporation
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
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ProtocolImplementation;
import org.labkey.api.security.User;

import java.sql.SQLException;
import java.util.List;

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

    @Override
    public void onSamplesChanged(User user, ExpProtocol expProtocol, List<? extends ExpMaterial> materials) throws SQLException
    {
        FlowProtocol protocol = new FlowProtocol(expProtocol);
        protocol.updateSampleIds(user);
    }

    @Override
    public void onRunDeleted(Container container, User user)
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
        sql.append("    NOT EXISTS (SELECT di.dataid FROM exp.datainput di WHERE di.dataid = d.rowid) AND\n");
        sql.append("    NOT EXISTS (SELECT fo.dataid FROM flow.object fo WHERE fo.dataid = d.rowid AND fo.container = ?)\n").add(container.getId());
        sql.append(")\n");

        new SqlExecutor(ExperimentService.get().getSchema()).execute(sql);
    }
}
