/*
 * Copyright (c) 2013 LabKey Corporation
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

package org.labkey.nab.query;

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;

import java.util.HashSet;
import java.util.Set;


public class NAbSpecimenTable extends FilteredTable<NabProtocolSchema>
{
    private static final FieldKey CONTAINER_FIELD_KEY = FieldKey.fromParts("Container");

    private Set<Float> _cutoffValues;

    public NAbSpecimenTable(NabProtocolSchema schema)
    {
        super(NabProtocolSchema.getTableInfoNAbSpecimen(), schema);

        wrapAllColumns(true);

        // TODO - add columns for all of the different cutoff values

        addCondition(getRealTable().getColumn("ProtocolID"), _userSchema.getProtocol().getRowId());
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        // We need to do our filtering based on the run since we don't have a container column of our own
        clearConditions(CONTAINER_FIELD_KEY);
        SQLFragment sql = new SQLFragment("RunId IN (SELECT RowId FROM ");
        sql.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        sql.append(" WHERE ");
        sql.append(filter.getSQLFragment(getSchema(), CONTAINER_FIELD_KEY, _userSchema.getContainer()));
        sql.append(")");
        addCondition(sql, CONTAINER_FIELD_KEY);
    }

    public Set<Float> getCutoffValues()
    {
        if (_cutoffValues == null)
        {
            SQLFragment sql = new SQLFragment("SELECT DISTINCT Cutoff FROM ");
            sql.append(NabProtocolSchema.getTableInfoCutoffValue(), "cv");
            sql.append(", ");
            sql.append(NabProtocolSchema.getTableInfoNAbSpecimen(), "ns");
            sql.append(" WHERE ns.RowId = cv.NAbSpecimenID AND ns.ProtocolId = ?");
            sql.add(_userSchema.getProtocol().getRowId());
            return new HashSet<Float>(new SqlSelector(NabProtocolSchema.getSchema(), sql).getCollection(Float.class));
        }
        return _cutoffValues;
    }
}
