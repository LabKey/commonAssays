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

import org.apache.commons.lang.StringUtils;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryService;

import java.util.Collection;

/**
 * User: jeckels
 * Date: 7/8/11
 */
public class TitrationTable extends AbstractLuminexTable
{
    private final LuminexSchema _schema;

    public TitrationTable(LuminexSchema schema, boolean filter)
    {
        super(LuminexSchema.getTableInfoTitration(), schema, filter);
        setName(LuminexSchema.getProviderTableName(schema.getProtocol(), LuminexSchema.TITRATION_TABLE_NAME));
        _schema = schema;
        addColumn(wrapColumn(getRealTable().getColumn("RowId"))).setHidden(true);
        ColumnInfo nameColumn = addColumn(wrapColumn(getRealTable().getColumn("Name")));
        // Set to be nullable so when a dataset backed by this assay type is exported, it's not considered required
        // to import correctly. This is important because not all Luminex rows will have a titration, and therefore
        // they won't all have a titration name.
        nameColumn.setNullable(true);
        addColumn(wrapColumn(getRealTable().getColumn("Standard")));
        addColumn(wrapColumn(getRealTable().getColumn("QCControl")));
        addColumn(wrapColumn(getRealTable().getColumn("Unknown")));
        ColumnInfo runColumn = addColumn(wrapColumn("Run", getRealTable().getColumn("RunId")));
        runColumn.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return QueryService.get().getUserSchema(_schema.getUser(), _schema.getContainer(), LuminexSchema.NAME).getTable(_schema.getRunsTableName(_schema.getProtocol()));
            }
        });
        setTitleColumn("Name");
    }

    @Override
    protected SQLFragment createContainerFilterSQL(Collection<String> ids)
    {
        SQLFragment sql = new SQLFragment("RunId IN (SELECT RowId FROM ");
        sql.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        sql.append(" WHERE Container IN (");
        sql.append(StringUtils.repeat("?", ", ", ids.size()));
        sql.append("))");
        sql.addAll(ids);
        return sql;
    }
}
