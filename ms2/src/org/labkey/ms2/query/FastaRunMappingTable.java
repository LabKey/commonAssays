/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.ms2.query;

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.protein.query.ProteinUserSchema;

/**
 * Created by: jeckels
 * Date: 2/21/16
 */
public class FastaRunMappingTable extends FilteredTable
{
    private static final FieldKey CONTAINER_FIELD_KEY = FieldKey.fromParts("Container");

    public FastaRunMappingTable(MS2Schema schema)
    {
        super(MS2Manager.getTableInfoFastaRunMapping(), schema);
        wrapAllColumns(true);
        setDescription("Contains a row for each FASTA file used for a given imported MS2 search");

        getColumn("FastaId").setFk(new QueryForeignKey(ProteinUserSchema.NAME, schema.getContainer(), null, schema.getUser(), ProteinUserSchema.FASTA_FILE_TABLE_NAME, null, null, false));
        getColumn("Run").setFk(new QueryForeignKey(schema, null, MS2Schema.TableType.MS2RunDetails.name(), null, "Description"));
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        clearConditions(CONTAINER_FIELD_KEY);
        SQLFragment sql = new SQLFragment("Run IN (SELECT r.Run FROM ");
        sql.append(MS2Manager.getTableInfoRuns(), "r");
        sql.append(" WHERE r.Deleted = ? AND ");
        sql.append(filter.getSQLFragment(getSchema(), new SQLFragment("r.Container"), _userSchema.getContainer()));
        sql.append(")");
        sql.add(false);
        addCondition(sql, CONTAINER_FIELD_KEY);
    }
}
