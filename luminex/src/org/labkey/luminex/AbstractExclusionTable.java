package org.labkey.luminex;

import org.apache.commons.beanutils.ConvertUtils;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.MultiValuedForeignKey;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.UpdatePermission;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Jun 29, 2011
 */
public abstract class AbstractExclusionTable extends FilteredTable
{
    private final LuminexSchema _schema;

    public AbstractExclusionTable(TableInfo realTable, LuminexSchema schema)
    {
        super(realTable);
        _schema = schema;
        wrapAllColumns(true);

        assert getRealTable().getPkColumnNames().size() == 1;
        ColumnInfo analytesColumn = wrapColumn("Analytes", getRealTable().getColumn(getRealTable().getPkColumnNames().get(0)));
        analytesColumn.setKeyField(false);
        analytesColumn.setFk(new MultiValuedForeignKey(new LookupForeignKey("WellExclusionId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _schema.createWellExclusionAnalyteTable();
            }
        }, "AnalyteId"));
        analytesColumn.setUserEditable(true);
        analytesColumn.setReadOnly(false);
        addColumn(analytesColumn);
    }

    @Override
    public abstract QueryUpdateService getUpdateService();

    @Override
    public boolean hasPermission(User user, Class<? extends Permission> perm)
    {
        return _schema.getContainer().hasPermission(user, perm);
    }

    static abstract class ExclusionUpdateService extends DefaultQueryUpdateService
    {
        private final TableInfo _analyteMappingTable;
        private final String _fkColumnName;

        public ExclusionUpdateService(TableInfo queryTable, TableInfo dbTable, TableInfo analyteMappingTable, String fkColumnName)
        {
            super(queryTable, dbTable);
            _analyteMappingTable = analyteMappingTable;
            _fkColumnName = fkColumnName;

            assert getQueryTable().getPkColumnNames().size() == 1;
        }

        protected Integer convertToInteger(Object value)
        {
            if (value == null)
            {
                return null;
            }
            if (value instanceof Number)
            {
                return ((Number)value).intValue();
            }
            else
            {
                return ((Integer)ConvertUtils.convert(value.toString(), Integer.class));
            }
        }

        protected List<Integer> getAnalyteIds(Map<String, Object> rowMap)
        {
            List<Integer> result = new ArrayList<Integer>();
            Object ids = rowMap.get("AnalyteId/RowId");
            if (ids != null)
            {
                String[] idStrings = ids.toString().split(",");
                for (String idString : idStrings)
                {
                    result.add(Integer.parseInt(idString.trim()));
                }
                return result;
            }
            return null;
        }

        protected Integer getPKValue(Map<String, Object> rowMap)
        {
            return convertToInteger(rowMap.get(getQueryTable().getPkColumnNames().get(0)));
        }

        @Override
        protected Map<String, Object> deleteRow(User user, Container container, Map<String, Object> oldRowMap) throws InvalidKeyException, QueryUpdateServiceException, SQLException
        {
            checkPermissions(user, oldRowMap, DeletePermission.class);
            deleteAnalytes(oldRowMap);
            return super.deleteRow(user, container, oldRowMap);
        }

        private void deleteAnalytes(Map<String, Object> oldRowMap) throws SQLException
        {
            Integer rowId = getPKValue(oldRowMap);
            if (rowId != null)
            {
                // Delete from the analyte mapping table first. If we don't have a rowId, we'll just let the call to
                // super.deleteRow() indicate the error to the caller
                Table.execute(getDbTable().getSchema(), "DELETE FROM " + _analyteMappingTable + " WHERE " + _fkColumnName + " = ?", rowId);
            }
        }

        @Override
        protected Map<String, Object> updateRow(User user, Container container, Map<String, Object> row, Map<String, Object> oldRow) throws InvalidKeyException, ValidationException, QueryUpdateServiceException, SQLException
        {
            checkPermissions(user, oldRow, UpdatePermission.class);
            checkPermissions(user, row, UpdatePermission.class);
            deleteAnalytes(oldRow);
            Map<String, Object> result = super.updateRow(user, container, row, oldRow);

            if (getAnalyteIds(row) != null)
            {
                insertAnalytes(row);
            }

            return result;
        }

        @Override
        protected Map<String, Object> insertRow(User user, Container container, Map<String, Object> rowMap) throws QueryUpdateServiceException, SQLException, ValidationException, DuplicateKeyException
        {
            checkPermissions(user, rowMap, InsertPermission.class);
            Map<String, Object> result = super.insertRow(user, container, rowMap);
            // Be sure that the RowId is set correctly
            rowMap.putAll(result);
            insertAnalytes(rowMap);

            return result;
        }

        protected abstract void checkPermissions(User user, Map<String, Object> rowMap, Class<? extends Permission> permission) throws QueryUpdateServiceException;

        protected void insertAnalytes(Map<String, Object> rowMap) throws SQLException, QueryUpdateServiceException
        {
            Integer rowId = getPKValue(rowMap);
            assert rowId != null;

            List<Integer> analyteIds = getAnalyteIds(rowMap);
            if (analyteIds != null)
            {
                for (Integer analyteId : analyteIds)
                {
                    Analyte analyte = Table.selectObject(LuminexSchema.getTableInfoAnalytes(), analyteId, Analyte.class);
                    if (analyte == null)
                    {
                        throw new QueryUpdateServiceException("No such analyte: " + analyteId);
                    }
                    validateAnalyte(rowMap, analyte);
                    Map<String, Object> fields = new HashMap<String, Object>();
                    fields.put("AnalyteId", analyteId);
                    fields.put(_fkColumnName, rowId);

                    Table.insert(null, _analyteMappingTable, fields);
                }
            }
        }

        protected abstract void validateAnalyte(Map<String, Object> rowMap, Analyte analyte) throws QueryUpdateServiceException;
    }
}
