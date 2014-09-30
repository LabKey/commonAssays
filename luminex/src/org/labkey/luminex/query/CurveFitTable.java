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
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.luminex.LuminexDataHandler;

/**
 * User: jeckels
 * Date: Aug 18, 2011
 */
public class CurveFitTable extends AbstractLuminexTable
{
    public CurveFitTable(LuminexProtocolSchema schema, boolean filterTable)
    {
        super(LuminexProtocolSchema.getTableInfoCurveFit(), schema, filterTable);
        setName(LuminexProtocolSchema.CURVE_FIT_TABLE_NAME);
        wrapAllColumns(true);
        ColumnInfo titrationCol = getColumn("TitrationId");
        titrationCol.setLabel("Titration");
        titrationCol.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.createTitrationTable(false);
            }
        });

        ColumnInfo analyteCol = getColumn("AnalyteId");
        analyteCol.setLabel("Analyte");
        analyteCol.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.createAnalyteTable(false);
            }
        });

        ColumnInfo ec50Col = getColumn("EC50");
        ec50Col.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new QCFlagHighlightDisplayColumn(colInfo, "EC50QCFlagsEnabled");
            }
        });
        SQLFragment ec50FlagSQL = createQCFlagEnabledSQLFragment(this.getSqlDialect(), "EC50", null, LuminexDataHandler.QC_FLAG_TITRATION_ID);
        ExprColumn ec50FlagEnabledColumn = new ExprColumn(this, "EC50QCFlagsEnabled", ec50FlagSQL, JdbcType.VARCHAR);
        ec50FlagEnabledColumn.setLabel("EC50 QC Flags Enabled State");
        ec50FlagEnabledColumn.setHidden(true);
        addColumn(ec50FlagEnabledColumn);

        ColumnInfo aucCol = getColumn("AUC");
        aucCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new QCFlagHighlightDisplayColumn(colInfo, "AUCQCFlagsEnabled");
            }
        });
        SQLFragment aucFlagSQL = createQCFlagEnabledSQLFragment(this.getSqlDialect(), LuminexDataHandler.QC_FLAG_AUC_FLAG_TYPE, "Trapezoidal", LuminexDataHandler.QC_FLAG_TITRATION_ID);
        ExprColumn aucFlagEnabledColumn = new ExprColumn(this, "AUCQCFlagsEnabled", aucFlagSQL, JdbcType.VARCHAR);
        aucFlagEnabledColumn.setLabel("AUC QC Flags Enabled State");
        aucFlagEnabledColumn.setHidden(true);
        addColumn(aucFlagEnabledColumn);
    }

    @Override
    protected SQLFragment createContainerFilterSQL(ContainerFilter filter, Container container)
    {
        SQLFragment sql = new SQLFragment("TitrationId IN (SELECT t.RowId FROM ");
        sql.append(LuminexProtocolSchema.getTableInfoTitration(), "t");
        sql.append(" WHERE t.RunId IN (SELECT r.RowId FROM ");
        sql.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        sql.append(" WHERE ");
        sql.append(filter.getSQLFragment(getSchema(), new SQLFragment("r.Container"), container));
        if (getUserSchema().getProtocol() != null)
        {
            sql.append(" AND r.ProtocolLSID = ?");
            sql.add(getUserSchema().getProtocol().getLSID());
        }
        sql.append("))");
        return sql;

    }
}
