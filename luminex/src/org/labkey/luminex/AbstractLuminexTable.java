/*
 * Copyright (c) 2011 LabKey Corporation
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
package org.labkey.luminex;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.study.assay.AssaySchema;

import java.util.Collection;

/**
 * User: jeckels
 * Date: 7/8/11
 */
public abstract class AbstractLuminexTable extends FilteredTable
{
    protected final LuminexSchema _schema;
    private final boolean _needsFilter;

    private static final String CONTAINER_FAKE_COLUMN_NAME = "Container";

    public AbstractLuminexTable(TableInfo table, LuminexSchema schema, boolean filter)
    {
        super(table, schema.getContainer());
        _schema = schema;
        _needsFilter = filter;

        applyContainerFilter(getContainerFilter());
    }

    @Override
    protected final void applyContainerFilter(ContainerFilter filter)
    {
        if (_needsFilter)
        {
            clearConditions(CONTAINER_FAKE_COLUMN_NAME);
            addCondition(createContainerFilterSQL(filter, _schema.getContainer()), CONTAINER_FAKE_COLUMN_NAME);
        }
    }

    protected abstract SQLFragment createContainerFilterSQL(ContainerFilter filter, Container container);

    @Override
    public String getPublicSchemaName()
    {
        return AssaySchema.NAME;
    }

    public static SQLFragment createQCFlagEnabledSQLFragment(SqlDialect sqlDialect, String flagType, String curveType)
    {
        SQLFragment sql = new SQLFragment(" ");
        sql.append("SELECT qf.Enabled FROM ");
        sql.append(ExperimentService.get().getTinfoAssayQCFlag(), "qf");
        sql.append(" WHERE " + ExprColumn.STR_TABLE_ALIAS + ".AnalyteId = qf.IntKey1");
        sql.append("   AND " + ExprColumn.STR_TABLE_ALIAS + ".TitrationId = qf.IntKey2");
        sql.append("   AND qf.FlagType = '" + flagType + "'");
        if (null != curveType)
        {
            sql.append("    AND " + ExprColumn.STR_TABLE_ALIAS + ".CurveType = '" + curveType + "'");
        }
        sql.append(" ORDER BY qf.RowId");

        return sqlDialect.getSelectConcat(sql);
    }
}
