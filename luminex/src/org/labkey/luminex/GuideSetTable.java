package org.labkey.luminex;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * User: jeckels
 * Date: Aug 26, 2011
 */
public class GuideSetTable extends AbstractLuminexTable
{
    public GuideSetTable(final LuminexSchema schema, boolean filter)
    {
        super(LuminexSchema.getTableInfoGuideSet(), schema, filter);
        wrapAllColumns(true);

        ColumnInfo protocolCol = getColumn("ProtocolId");
        protocolCol.setLabel("Assay Design");
        protocolCol.setHidden(true);
        protocolCol.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return AssayService.get().createSchema(schema.getUser(), schema.getContainer()).getTable(AssaySchema.ASSAY_LIST_TABLE_NAME);
            }
        });

        getColumn("MaxFIAverage").setShownInInsertView(false);
        getColumn("MaxFIAverage").setShownInUpdateView(false);
        getColumn("MaxFIStdDev").setShownInInsertView(false);
        getColumn("MaxFIStdDev").setShownInUpdateView(false);
    }

    @Override
    protected SQLFragment createContainerFilterSQL(Collection<String> ids)
    {
        // Guide sets are scoped to the protocol, not to folders
        SQLFragment sql = new SQLFragment("ProtocolId = ?");
        sql.add(_schema.getProtocol().getRowId());
        return sql;
    }

    @Override
    public boolean hasPermission(User user, Class<? extends Permission> perm)
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

    public static class GuideSetTableUpdateService extends DefaultQueryUpdateService
    {
        public GuideSetTableUpdateService(GuideSetTable guideSetTable)
        {
            super(guideSetTable, guideSetTable.getRealTable());
        }

        public static GuideSet[] getMatchingCurrentGuideSet(int protocolId, String analyteName, String conjugate, String isotype)
        {
            SQLFragment sql = new SQLFragment("SELECT * FROM ");
            sql.append(LuminexSchema.getTableInfoGuideSet(), "gs");
            sql.append(" WHERE ProtocolId = ? AND AnalyteName");
            sql.add(protocolId);
            appendNullableString(sql, analyteName);
            sql.append(" AND Conjugate");
            appendNullableString(sql, conjugate);
            sql.append(" AND Isotype");
            appendNullableString(sql, isotype);
            sql.append(" AND CurrentGuideSet = ?");
            sql.add(true);

            try
            {
                return Table.executeQuery(LuminexSchema.getSchema(), sql, GuideSet.class);
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
        protected Map<String, Object> insertRow(User user, Container container, Map<String, Object> row) throws DuplicateKeyException, ValidationException, QueryUpdateServiceException, SQLException
        {
            ExpProtocol protocol = validateProtocol(row);
            if (protocol == null)
            {
                throw new ValidationException("No ProtocolId specified");
            }
            Boolean current = (Boolean)row.get("CurrentGuideSet");
            if (current != null && current.booleanValue() && getMatchingCurrentGuideSet(protocol.getRowId(), (String)row.get("AnalyteName"), (String)row.get("Conjugate"), (String)row.get("Isotype")).length > 0)
            {
                throw new ValidationException("There is already a current guide set for that ProtocolId/AnalyteName/Conjugate/Isotype combination");
            }
            return super.insertRow(user, container, row);
        }

        @Override
        protected Map<String, Object> updateRow(User user, Container container, Map<String, Object> row, Map<String, Object> oldRow) throws InvalidKeyException, ValidationException, QueryUpdateServiceException, SQLException
        {
            ExpProtocol protocol = validateProtocol(row);
            Boolean current = (Boolean)row.get("CurrentGuideSet");
            Number rowId = (Number)oldRow.get("RowId");
            if (rowId == null)
            {
                throw new InvalidKeyException("RowId is required for updates");
            }
            if (current != null && current.booleanValue())
            {
                GuideSet[] currents = getMatchingCurrentGuideSet(protocol.getRowId(), (String)row.get("AnalyteName"), (String)row.get("Conjugate"), (String)row.get("Isotype"));
                for (GuideSet guideSet : currents)
                {
                    if (guideSet.getRowId() != rowId.intValue())
                    {
                        throw new ValidationException("There is already a current guide set for that ProtocolId/AnalyteName/Conjugate/Isotype combination");
                    }
                }
            }
            return super.updateRow(user, container, row, oldRow);
        }

        private ExpProtocol validateProtocol(Map<String, Object> row) throws ValidationException
        {
            Number protocolId = (Number)row.get("ProtocolId");
            if (protocolId != null)
            {
                ExpProtocol protocol = ExperimentService.get().getExpProtocol(protocolId.intValue());
                if (protocol == null)
                {
                    throw new ValidationException("No such ProtocolId:" + protocolId);
                }
                AssayProvider provider = AssayService.get().getProvider(protocol);
                if (!(provider instanceof LuminexAssayProvider))
                {
                    throw new ValidationException("The ProtocolId, " + protocolId + ", must refer to a Luminex assay design");
                }
                return protocol;
            }
            return null;
        }
    }
}
