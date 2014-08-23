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

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.luminex.LuminexDataHandler;
import org.labkey.luminex.query.LuminexProtocolSchema;

/**
 * User: jeckels
 * Date: 7/8/11
 */
public abstract class AbstractLuminexTable extends FilteredTable<LuminexProtocolSchema>
{
    private final boolean _needsFilter;

    private static final FieldKey CONTAINER_FAKE_COLUMN_NAME = FieldKey.fromParts("Container");

    public AbstractLuminexTable(TableInfo table, LuminexProtocolSchema schema, boolean filter)
    {
        super(table, schema);
        _needsFilter = filter;

        applyContainerFilter(getContainerFilter());
    }

    @Override
    protected final void applyContainerFilter(ContainerFilter filter)
    {
        if (_needsFilter)
        {
            clearConditions(CONTAINER_FAKE_COLUMN_NAME);
            addCondition(createContainerFilterSQL(filter, _userSchema.getContainer()), CONTAINER_FAKE_COLUMN_NAME);
        }
    }

    protected abstract SQLFragment createContainerFilterSQL(ContainerFilter filter, Container container);

    @Override
    public String getPublicSchemaName()
    {
        return _userSchema.getSchemaName();
    }

    // param: titrationSinglePointControlSwitch - TitrationId for AnalyteTitration, SinglePointControlId for AnalyteSinglePointControl
    public static SQLFragment createQCFlagEnabledSQLFragment(SqlDialect sqlDialect, String flagType, String curveType, String titrationSinglePointControlSwitch)
    {
        SQLFragment sql = new SQLFragment(" ");
        sql.append("SELECT qf.Enabled FROM ");
        sql.append(ExperimentService.get().getTinfoAssayQCFlag(), "qf");
        sql.append(" WHERE " + ExprColumn.STR_TABLE_ALIAS + ".AnalyteId = qf.IntKey1");
        sql.append("   AND " + ExprColumn.STR_TABLE_ALIAS + "." + titrationSinglePointControlSwitch + " = qf.IntKey2");
        if (null != curveType)
        {
            sql.append("    AND " + ExprColumn.STR_TABLE_ALIAS + ".CurveType = '" + curveType + "'");
        }

        // special case for EC50 to join flags based on 4PL or 5PL curve fits
        if (!flagType.equals("EC50"))
        {
            sql.append("   AND qf.FlagType = '" + flagType + "'");
        }
        else
        {
            sql.append("   AND (" + ExprColumn.STR_TABLE_ALIAS + ".CurveType = '" + StatsService.CurveFitType.FOUR_PARAMETER.getLabel() + "' AND qf.FlagType = '" + LuminexDataHandler.QC_FLAG_EC50_4PL_FLAG_TYPE + "' ");
            sql.append("        OR " + ExprColumn.STR_TABLE_ALIAS + ".CurveType = '" + StatsService.CurveFitType.FIVE_PARAMETER.getLabel() + "' AND qf.FlagType = '" + LuminexDataHandler.QC_FLAG_EC50_5PL_FLAG_TYPE + "') ");
        }
        sql.append(" ORDER BY qf.RowId");

        return sqlDialect.getSelectConcat(sql, ",");
    }
}
