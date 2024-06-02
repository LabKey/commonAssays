/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
import org.labkey.api.protein.query.ProteinUserSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.ms2.MS2Manager;

public class FastaRunMappingTable extends FilteredTable<MS2Schema>
{
    private static final FieldKey CONTAINER_FIELD_KEY = FieldKey.fromParts("Container");

    public FastaRunMappingTable(MS2Schema schema, ContainerFilter cf)
    {
        super(MS2Manager.getTableInfoFastaRunMapping(), schema, cf);
        wrapAllColumns(true);
        setDescription("Contains a row for each FASTA file used for a given imported MS2 search");

        getMutableColumn("FastaId").setFk( QueryForeignKey
                .from(schema, getContainerFilter())
                .schema(ProteinUserSchema.NAME, schema.getContainer())
                .table(ProteinUserSchema.FASTA_FILE_TABLE_NAME));
        getMutableColumn("Run").setFk( QueryForeignKey
                .from(schema, getContainerFilter())
                .to(MS2Schema.TableType.MS2RunDetails.name(), null, "Description"));
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        clearConditions(CONTAINER_FIELD_KEY);
        SQLFragment sql = new SQLFragment("Run IN (SELECT r.Run FROM ");
        sql.append(MS2Manager.getTableInfoRuns(), "r");
        sql.append(" WHERE r.Deleted = ? AND ");
        sql.add(false);
        sql.append(filter.getSQLFragment(getSchema(), new SQLFragment("r.Container")));
        sql.append(")");
        addCondition(sql, CONTAINER_FIELD_KEY);
    }
}
