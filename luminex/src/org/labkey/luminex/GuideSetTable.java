/*
 * Copyright (c) 2011-2012 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ForeignKey;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.RowIdQueryUpdateService;
import org.labkey.api.query.UserIdQueryForeignKey;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;

import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Aug 26, 2011
 */
public class GuideSetTable extends AbstractCurveFitPivotTable
{
    public GuideSetTable(final LuminexProtocolSchema schema, boolean filter)
    {
        super(LuminexProtocolSchema.getTableInfoGuideSet(), schema, filter, "RowId");
        wrapAllColumns(true);
        setName(LuminexProtocolSchema.getProviderTableName(schema.getProtocol(), LuminexProtocolSchema.GUIDE_SET_TABLE_NAME));

        ColumnInfo protocolCol = getColumn("ProtocolId");
        protocolCol.setLabel("Assay Design");
        protocolCol.setHidden(true);
        protocolCol.setShownInDetailsView(false);
        protocolCol.setShownInUpdateView(false);
        protocolCol.setShownInInsertView(false);
        protocolCol.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return AssayService.get().createSchema(schema.getUser(), schema.getContainer(), null).getTable(AssaySchema.ASSAY_LIST_TABLE_NAME);
            }
        });

        SQLFragment maxFISQL = new SQLFragment(" FROM ");
        maxFISQL.append(LuminexProtocolSchema.getTableInfoAnalyteTitration(), "at");
        maxFISQL.append(" WHERE at.GuideSetId = ");
        maxFISQL.append(ExprColumn.STR_TABLE_ALIAS);
        maxFISQL.append(".RowId AND at.IncludeInGuideSetCalculation = ?");
        maxFISQL.add(Boolean.TRUE);
        maxFISQL.append(")");

        SQLFragment maxFIAverageSQL = new SQLFragment("(SELECT AVG(at.MaxFI)");
        maxFIAverageSQL.append(maxFISQL);
        ExprColumn maxFIAverageCol = new ExprColumn(this, "MaxFIAverage", maxFIAverageSQL, JdbcType.DOUBLE);
        maxFIAverageCol.setLabel("Max FI Average");
        maxFIAverageCol.setFormat("0.00");
        addColumn(maxFIAverageCol);

        SQLFragment maxFIStdDevSQL = new SQLFragment("(SELECT ");
        maxFIStdDevSQL.append(LuminexProtocolSchema.getSchema().getSqlDialect().getStdDevFunction());
        maxFIStdDevSQL.append("(at.MaxFI)");
        maxFIStdDevSQL.append(maxFISQL);
        ExprColumn maxFIStdDevCol = new ExprColumn(this, "MaxFIStdDev", maxFIStdDevSQL, JdbcType.DOUBLE);
        maxFIStdDevCol.setLabel("Max FI StdDev");
        maxFIStdDevCol.setFormat("0.00");
        addColumn(maxFIStdDevCol);

        ForeignKey userIdForeignKey = new UserIdQueryForeignKey(schema.getUser(), schema.getContainer());
        getColumn("ModifiedBy").setFk(userIdForeignKey);
        getColumn("CreatedBy").setFk(userIdForeignKey);

        addCurveTypeColumns();
    }

    protected LookupForeignKey createCurveFitFK(final String curveType)
    {
        return new LookupForeignKey("GuideSetId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _schema.createGuideSetCurveFitTable(curveType);
            }
        };
    }

    @Override
    protected SQLFragment createContainerFilterSQL(ContainerFilter filter, Container container)
    {
        // Guide sets are scoped to the protocol, not to folders
        SQLFragment sql = new SQLFragment("ProtocolId = ?");
        sql.add(_schema.getProtocol().getRowId());
        return sql;
    }

    @Override
    public boolean hasPermission(UserPrincipal user, Class<? extends Permission> perm)
    {
        // First check if the user has the permission in the folder where the assay is defined
        if (_schema.getProtocol().getContainer().hasPermission(user, perm))
        {
            return true;
        }

        // Then look if they have the permission in any of the folders where there are runs for the assay design
        for (Container container : _schema.getProtocol().getExpRunContainers())
        {
            if (container.hasPermission(user, perm))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new GuideSetTableUpdateService(this);
    }

    public static class GuideSetTableUpdateService extends RowIdQueryUpdateService<GuideSet>
    {
        private ExpProtocol _protocol;

        public GuideSetTableUpdateService(GuideSetTable guideSetTable)
        {
            super(guideSetTable);
            _protocol = guideSetTable._schema.getProtocol();
        }

        public static GuideSet getMatchingCurrentGuideSet(@NotNull ExpProtocol protocol, String analyteName, String titrationName, String conjugate, String isotype)
        {
            SQLFragment sql = new SQLFragment("SELECT * FROM ");
            sql.append(LuminexProtocolSchema.getTableInfoGuideSet(), "gs");
            sql.append(" WHERE ProtocolId = ?");
            sql.add(protocol.getRowId());
            sql.append(" AND AnalyteName");
            appendNullableString(sql, analyteName);
            sql.append(" AND TitrationName");
            appendNullableString(sql, titrationName);
            sql.append(" AND Conjugate");
            appendNullableString(sql, conjugate);
            sql.append(" AND Isotype");
            appendNullableString(sql, isotype);
            sql.append(" AND CurrentGuideSet = ?");
            sql.add(true);

            try
            {
                GuideSet[] matches = Table.executeQuery(LuminexProtocolSchema.getSchema(), sql, GuideSet.class);
                if (matches.length == 1)
                {
                    return matches[0];
                }
                if (matches.length == 0)
                {
                    return null;
                }

                throw new IllegalStateException("More than one guide set is current for assay design '" + protocol.getName() + "', analyte '" + analyteName + "', conjugate '" + conjugate + "', isotype '" + isotype + "'");
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        private static void appendNullableString(SQLFragment sql, String value)
        {
            if (value == null)
            {
                sql.append(" IS NULL ");
            }
            else
            {
                sql.append(" = ?");
                sql.add(value);
            }
        }

        @Override
        public GuideSet get(User user, Container container, int key) throws QueryUpdateServiceException, SQLException
        {
            return Table.selectObject(LuminexProtocolSchema.getTableInfoGuideSet(), key, GuideSet.class);
        }

        @Override
        public void delete(User user, Container container, int key) throws QueryUpdateServiceException, SQLException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected GuideSet createNewBean()
        {
            return new GuideSet();
        }

        @Override
        protected GuideSet insert(User user, Container container, GuideSet bean) throws ValidationException, DuplicateKeyException, QueryUpdateServiceException, SQLException
        {
            validateProtocol(bean);
            validateGuideSetValues(bean);
            boolean current = bean.isCurrentGuideSet();
            if (current && getMatchingCurrentGuideSet(_protocol, bean.getAnalyteName(), bean.getTitrationName(), bean.getConjugate(), bean.getIsotype()) != null)
            {
                throw new ValidationException("There is already a current guide set for that ProtocolId/AnalyteName/Conjugate/Isotype combination");
            }
            return Table.insert(user, LuminexProtocolSchema.getTableInfoGuideSet(), bean);
        }

        @Override
        protected GuideSet update(User user, Container container, GuideSet bean, Integer oldKey) throws ValidationException, QueryUpdateServiceException, SQLException
        {
            if (oldKey == null)
            {
                throw new ValidationException("RowId is required for updates");
            }
            validateGuideSetValues(bean);
            if (bean.isCurrentGuideSet())
            {
                GuideSet currentGuideSet = getMatchingCurrentGuideSet(_protocol, bean.getAnalyteName(), bean.getTitrationName(), bean.getConjugate(), bean.getIsotype());
                if (currentGuideSet != null && currentGuideSet.getRowId() != oldKey.intValue())
                {
                    throw new ValidationException("There is already a current guide set for that ProtocolId/AnalyteName/TitrationName/Conjugate/Isotype combination");
                }
            }
            return Table.update(user, LuminexProtocolSchema.getTableInfoGuideSet(), bean, oldKey);
        }

        private void validateProtocol(GuideSet bean) throws ValidationException
        {
            int protocolId = bean.getProtocolId();
            if (protocolId == 0)
            {
                bean.setProtocolId(_protocol.getRowId());
            }
            else
            {
                if (protocolId != _protocol.getRowId())
                {
                    throw new ValidationException("ProtocolId must be set to " + _protocol.getRowId());
                }
            }
        }

        private void validateGuideSetValues(GuideSet bean) throws ValidationException
        {
            int maxLength = LuminexProtocolSchema.getTableInfoGuideSet().getColumn("AnalyteName").getScale();
            if (bean.getAnalyteName() != null && bean.getAnalyteName().length() > maxLength)
            {
                throw new ValidationException("AnalyteName value '" + bean.getAnalyteName() + "' is too long, maximum length is " + maxLength + " characters");
            }
            maxLength = LuminexProtocolSchema.getTableInfoGuideSet().getColumn("Conjugate").getScale();
            if (bean.getConjugate() != null && bean.getConjugate().length() > maxLength)
            {
                throw new ValidationException("Conjugate value '" + bean.getConjugate() + "' is too long, maximum length is " + maxLength + " characters");
            }
            maxLength = LuminexProtocolSchema.getTableInfoGuideSet().getColumn("Isotype").getScale();
            if (bean.getIsotype() != null && bean.getIsotype().length() > maxLength)
            {
                throw new ValidationException("Isotype value '" + bean.getIsotype() + "' is too long, maximum length is " + maxLength + " characters");
            }
            maxLength = LuminexProtocolSchema.getTableInfoGuideSet().getColumn("TitrationName").getScale();
            if (bean.getTitrationName() != null && bean.getTitrationName().length() > maxLength)
            {
                throw new ValidationException("TitrationName value '" + bean.getTitrationName() + "' is too long, maximum length is " + maxLength + " characters");
            }
        }
    }
}
