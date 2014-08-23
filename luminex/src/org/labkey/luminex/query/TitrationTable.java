/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
package org.labkey.luminex.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ActionURL;
import org.labkey.luminex.LuminexController;

/**
 * User: jeckels
 * Date: 7/8/11
 */
public class TitrationTable extends AbstractLuminexTable
{
    public TitrationTable(LuminexProtocolSchema schema, boolean filter)
    {
        super(LuminexProtocolSchema.getTableInfoTitration(), schema, filter);
        setName(LuminexProtocolSchema.TITRATION_TABLE_NAME);
        addColumn(wrapColumn(getRealTable().getColumn("RowId"))).setHidden(true);

        ColumnInfo nameColumn = addColumn(wrapColumn(getRealTable().getColumn("Name")));
        ActionURL url = new ActionURL(LuminexController.LeveyJenningsReportAction.class, schema.getContainer());
        nameColumn.setURL(StringExpressionFactory.createURL(url + "rowId=${Run/Protocol/RowId}" + "&titration=${Name}"));

        // Set to be nullable so when a dataset backed by this assay type is exported, it's not considered required
        // to import correctly. This is important because not all Luminex rows will have a titration, and therefore
        // they won't all have a titration name.
        nameColumn.setNullable(true);

        addColumn(wrapColumn(getRealTable().getColumn("Standard")));
        addColumn(wrapColumn(getRealTable().getColumn("QCControl")));
        addColumn(wrapColumn(getRealTable().getColumn("Unknown")));
        addColumn(wrapColumn(getRealTable().getColumn("OtherControl")));

        // issue 21138
        String bTRUE = getSchema().getSqlDialect().getBooleanTRUE();
        String bFALSE = getSchema().getSqlDialect().getBooleanFALSE();
        SQLFragment qcReportSQL = new SQLFragment();
        qcReportSQL.append("(CASE WHEN Standard=").append(bTRUE).append(" OR QCControl=").append(bTRUE);
        qcReportSQL.append(" THEN ").append(bTRUE).append(" ELSE ").append(bFALSE).append(" END)");
        addColumn(new ExprColumn(this, "IncludeInQcReport", qcReportSQL, JdbcType.BOOLEAN));

        ColumnInfo runColumn = addColumn(wrapColumn("Run", getRealTable().getColumn("RunId")));
        QueryForeignKey runFk = new QueryForeignKey(schema, null, AssayProtocolSchema.RUNS_TABLE_NAME, "RowId", "Name");
        runColumn.setFk(runFk);
        setTitleColumn("Name");
    }

    @Override
    protected SQLFragment createContainerFilterSQL(ContainerFilter filter, Container container)
    {
        SQLFragment sql = new SQLFragment("RunId IN (SELECT RowId FROM ");
        sql.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        sql.append(" WHERE ");
        sql.append(filter.getSQLFragment(getSchema(), new SQLFragment("Container"), container));
        sql.append(")");
        return sql;
    }
}
