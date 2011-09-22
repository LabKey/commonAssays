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

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.AbstractBeanQueryUpdateService;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.util.Pair;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * User: jeckels
 * Date: 7/8/11
 */
public class AnalyteTitrationTable extends AbstractLuminexTable
{
    public AnalyteTitrationTable(LuminexSchema schema, boolean filter)
    {
        super(LuminexSchema.getTableInfoAnalyteTitration(), schema, filter);
        setName(LuminexSchema.getProviderTableName(schema.getProtocol(), LuminexSchema.ANALYTE_TITRATION_TABLE_NAME));

        ColumnInfo analyteCol = addColumn(wrapColumn("Analyte", getRealTable().getColumn("AnalyteId")));
        analyteCol.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _schema.createAnalyteTable(false);
            }
        });
        ColumnInfo titrationCol = addColumn(wrapColumn("Titration", getRealTable().getColumn("TitrationId")));
        titrationCol.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _schema.createTitrationTable(false);
            }
        });
        addColumn(wrapColumn(getRealTable().getColumn("MaxFI")));

        ColumnInfo guideSetCol = addColumn(wrapColumn("GuideSet", getRealTable().getColumn("GuideSetId")));
        guideSetCol.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _schema.createGuideSetTable(false);
            }
        });

        addColumn(wrapColumn(getRealTable().getColumn("IncludeInGuideSetCalculation")));

        for (final String curveType : _schema.getCurveTypes())
        {
            ColumnInfo curveFitColumn = wrapColumn(curveType + "CurveFit", getRealTable().getColumn("AnalyteId"));

            LookupForeignKey fk = new LookupForeignKey("Analyte")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    CurveFitTable result = _schema.createCurveFitTable(false);
                    result.addCondition(result.getRealTable().getColumn("CurveType"), curveType);
                    return result;
                }

                @Override
                protected ColumnInfo getPkColumn(TableInfo table)
                {
                    return table.getColumn("AnalyteId");
                }
            };

            fk.addJoin(getColumn("Titration"), "TitrationId");
            curveFitColumn.setIsUnselectable(true);
            curveFitColumn.setShownInDetailsView(false);
            curveFitColumn.setReadOnly(true);
            curveFitColumn.setKeyField(false);
            curveFitColumn.setShownInInsertView(false);
            curveFitColumn.setShownInUpdateView(false);
            curveFitColumn.setFk(fk);

            addColumn(curveFitColumn);
        }
    }

    @Override
    protected SQLFragment createContainerFilterSQL(Collection<String> ids)
    {
        SQLFragment sql = new SQLFragment("AnalyteId IN (SELECT RowId FROM ");
        sql.append(LuminexSchema.getTableInfoAnalytes(), "a");
        sql.append(" WHERE DataId IN (SELECT RowId FROM ");
        sql.append(ExperimentService.get().getTinfoData(), "d");
        sql.append(" WHERE Container IN (");
        sql.append(StringUtils.repeat("?", ", ", ids.size()));
        sql.append(")))");
        sql.addAll(ids);
        return sql;
    }

    @Override
    public boolean hasPermission(User user, Class<? extends Permission> perm)
    {
        return (perm.equals(UpdatePermission.class) || perm.equals(ReadPermission.class))
                && _schema.getContainer().hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        // Pair<Integer, Integer> is analyteid/titrationid combo
        return new AbstractBeanQueryUpdateService<AnalyteTitration, Pair<Integer, Integer>>(this)
        {
            @Override
            protected Pair<Integer, Integer> keyFromMap(Map<String, Object> map) throws InvalidKeyException
            {
                Integer analyteId = getInteger(map, map.containsKey("analyte") ? "analyte" : "analyteid");
                Integer titrationId = getInteger(map, map.containsKey("titration") ? "titration" : "titrationid");
                return new Pair<Integer, Integer>(analyteId, titrationId);
            }

            @Override
            protected AnalyteTitration get(User user, Container container, Pair<Integer, Integer> key) throws QueryUpdateServiceException, SQLException
            {
                SimpleFilter filter = new SimpleFilter("AnalyteId", key.getKey());
                filter.addCondition("TitrationId", key.getValue());
                return Table.selectObject(LuminexSchema.getTableInfoAnalyteTitration(), filter, null, AnalyteTitration.class);
            }

            @Override
            protected AnalyteTitration update(User user, Container container, AnalyteTitration bean, Pair<Integer, Integer> oldKey) throws ValidationException, QueryUpdateServiceException, SQLException
            {
                Integer newGuideSetId = bean.getGuideSetId();

                if (newGuideSetId != null)
                {
                    GuideSet guideSet = Table.selectObject(LuminexSchema.getTableInfoGuideSet(), newGuideSetId.intValue(), GuideSet.class);
                    if (guideSet == null)
                    {
                        throw new ValidationException("No such guideSetId: " + newGuideSetId);
                    }
                    if (guideSet.getProtocolId() != _schema.getProtocol().getRowId())
                    {
                        throw new ValidationException("Can't set guideSetId to point to a guide set from another assay definition: " + newGuideSetId);
                    }

                    Analyte analyte = Table.selectObject(LuminexSchema.getTableInfoAnalytes(), bean.getAnalyteId(), Analyte.class);
                    Titration titration = Table.selectObject(LuminexSchema.getTableInfoTitration(), bean.getTitrationId(), Titration.class);

                    if (analyte == null)
                    {
                        throw new IllegalStateException("Unable to find referenced analyte: " + bean.getAnalyteId());
                    }
                    if (titration == null)
                    {
                        throw new IllegalStateException("Unable to find referenced titration: " + bean.getTitrationId());
                    }

                    if (!ObjectUtils.equals(analyte.getName(), guideSet.getAnalyteName()))
                    {
                        throw new ValidationException("GuideSet is for analyte " + guideSet.getAnalyteName(), " but this row is mapped to analyte " + analyte.getName());
                    }
                    if (!ObjectUtils.equals(titration.getName(), guideSet.getTitrationName()))
                    {
                        throw new ValidationException("GuideSet is for titration " + guideSet.getTitrationName(), " but this row is mapped to titration " + titration.getName());
                    }
                }

                Object[] keys = new Object[2];

                boolean analyteFirst = LuminexSchema.getTableInfoAnalyteTitration().getPkColumnNames().get(0).equalsIgnoreCase("AnalyteId");
                keys[0] = analyteFirst ? oldKey.getKey() : oldKey.getValue();
                keys[1] = analyteFirst ? oldKey.getValue() : oldKey.getKey();

                return Table.update(user, LuminexSchema.getTableInfoAnalyteTitration(), bean, keys);
            }

            @Override
            protected void delete(User user, Container container, Pair<Integer, Integer> key) throws QueryUpdateServiceException, SQLException
            {
                throw new UnsupportedOperationException();
            }

            @Override
            protected AnalyteTitration createNewBean()
            {
                return new AnalyteTitration();
            }

            @Override
            protected AnalyteTitration insert(User user, Container container, AnalyteTitration bean) throws ValidationException, DuplicateKeyException, QueryUpdateServiceException, SQLException
            {
                throw new UnsupportedOperationException();
            }
        };
    }
}
