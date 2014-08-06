/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.microarray.matrix;

import org.labkey.api.data.AbstractTableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.exp.query.SamplesSchema;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.microarray.query.MicroarrayUserSchema;

import java.util.ArrayList;
import java.util.List;

public class FeatureDataTable extends FilteredTable<ExpressionMatrixProtocolSchema>
{
    public FeatureDataTable(ExpressionMatrixProtocolSchema schema)
    {
        super(ExpressionMatrixProtocolSchema.getTableInfoFeatureData(), schema);

        setDetailsURL(AbstractTableInfo.LINK_DISABLER);
        setImportURL(AbstractTableInfo.LINK_DISABLER);

        addColumn(wrapColumn(getRealTable().getColumn("RowId"))).setHidden(true);

        ColumnInfo valueColumn = addColumn(wrapColumn(getRealTable().getColumn("Value")));
        valueColumn.setHidden(false);
        valueColumn.setLabel("Value");
        valueColumn.setFormat("0.000000");

        ColumnInfo featureIdColumn = addColumn(wrapColumn(getRealTable().getColumn("FeatureId")));
        featureIdColumn.setHidden(false);
        featureIdColumn.setLabel("Probe Id");
        featureIdColumn.setFk(new QueryForeignKey(MicroarrayUserSchema.SCHEMA_NAME, schema.getContainer(), null,
                schema.getUser(), MicroarrayUserSchema.TABLE_FEATURE_ANNOTATION, "RowId", "FeatureId"));

        ColumnInfo sampleIdColumn = addColumn(wrapColumn(getRealTable().getColumn("SampleId")));
        sampleIdColumn.setHidden(false);
        sampleIdColumn.setLabel("Sample Id");

        // Allow for multiple sample sets, but assume we're displaying data from whichever is currently active.
        String activeSampleSet = ExperimentService.get().ensureActiveSampleSet(schema.getContainer()).getName();
        sampleIdColumn.setFk(new QueryForeignKey(SamplesSchema.SCHEMA_NAME, schema.getContainer(), null, schema.getUser(),
                activeSampleSet,"RowId","Name"));

        SQLFragment runSQL = new SQLFragment("(SELECT d.RunId FROM ");
        runSQL.append(ExperimentService.get().getTinfoData(), "d");
        runSQL.append(" WHERE d.RowId = ");
        runSQL.append(ExprColumn.STR_TABLE_ALIAS);
        runSQL.append(".DataId)");
        ColumnInfo runIdColumn = new ExprColumn(this, "Run", runSQL, JdbcType.INTEGER);
        addColumn(runIdColumn);
        runIdColumn.setFk(new QueryForeignKey(getUserSchema(), null, AssayProtocolSchema.RUNS_TABLE_NAME, "RowId", "Name"));

        ColumnInfo dataIdColumn = addColumn(wrapColumn(getRealTable().getColumn("DataId")));
        dataIdColumn.setHidden(false);
        dataIdColumn.setLabel("Data Id");
        dataIdColumn.setFk(new QueryForeignKey(ExpSchema.SCHEMA_NAME, schema.getContainer(), null, schema.getUser(), ExpSchema.TableType.Data.toString(),
                "RowId", "Name"));

        List<FieldKey> columns = new ArrayList<>(getDefaultVisibleColumns());
        columns.remove(dataIdColumn.getFieldKey());
        setDefaultVisibleColumns(columns);

        // Issue 21134: filter by assay protocol
        SQLFragment filter = new SQLFragment("DataId");
        filter.append(" IN (SELECT d.RowId FROM ");
        filter.append(ExperimentService.get().getTinfoData(), "d");
        filter.append(", ");
        filter.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        filter.append(" WHERE d.RunId = r.RowId");
        if (schema.getProtocol() != null)
        {
            filter.append(" AND r.ProtocolLSID = ?");
            filter.add(schema.getProtocol().getLSID());
        }
        filter.append(") ");

        addCondition(filter, FieldKey.fromParts("DataId"));
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        // There isn't a container column directly on this table so do a special filter
        if (getContainer() != null)
        {
            FieldKey containerColumn = FieldKey.fromParts("Run", "Folder");
            clearConditions(containerColumn);
            addCondition(filter.getSQLFragment(getSchema(), new SQLFragment("(SELECT d.Container FROM exp.Data d WHERE d.RowId = DataId)"), getContainer()), containerColumn);
        }
    }

}
