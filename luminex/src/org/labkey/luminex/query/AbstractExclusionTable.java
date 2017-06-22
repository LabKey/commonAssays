/*
 * Copyright (c) 2011-2017 LabKey Corporation
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

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ForeignKey;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.UserIdQueryForeignKey;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.luminex.model.Analyte;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: jeckels
 * Date: Jun 29, 2011
 */
public abstract class AbstractExclusionTable extends AbstractLuminexTable
{
    protected AbstractExclusionTable(TableInfo realTable, LuminexProtocolSchema schema, boolean filter)
    {
        super(realTable, schema, filter);
        wrapAllColumns(true);

        assert getRealTable().getPkColumnNames().size() == 1;
        ColumnInfo analytesColumn = wrapColumn("Analytes", getRealTable().getColumn(getRealTable().getPkColumnNames().get(0)));
        analytesColumn.setKeyField(false);
        analytesColumn.setUserEditable(true);
        analytesColumn.setReadOnly(false);

        ForeignKey userIdForeignKey = new UserIdQueryForeignKey(schema.getUser(), schema.getContainer(), true);
        getColumn("ModifiedBy").setFk(userIdForeignKey);
        getColumn("CreatedBy").setFk(userIdForeignKey);

        addColumn(analytesColumn);

        getColumn("Created").setLabel("Excluded At");
        getColumn("CreatedBy").setLabel("Excluded By");
        getColumn("Comment").setLabel("Reason for Exclusion");

        //Needed to retain original creator of exclusions on reimport.
        getColumn("CreatedBy").setReadOnly(false);
    }

    @Override
    public abstract QueryUpdateService getUpdateService();

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        return _userSchema.getContainer().hasPermission(user, perm);
    }

    /**
     * These tables support a special field, "AnalyteId/RowId", which allows API operations based on the multi-value
     * foreign key representation. If it's included in an INSERT or UPDATE, the server will set the mapped set of
     * analyte accordingly.
     */
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

        /**
         * @return null if the analyte ids weren't included
         */
        protected Set<Integer> getAnalyteIds(Map<String, Object> rowMap)
        {
            Object ids = rowMap.get("AnalyteId/RowId");
            if (ids != null && StringUtils.trimToNull(ids.toString()) != null)
            {
                Set<Integer> result = new HashSet<>();
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
            resolveRun(oldRowMap);
            return super.deleteRow(user, container, oldRowMap);
        }

        /**
         * Clear out all of the existing analytes for this exclusion in the database
         */
        private void deleteAnalytes(Map<String, Object> oldRowMap) throws SQLException
        {
            Integer rowId = getPKValue(oldRowMap);
            if (rowId != null)
            {
                // Delete from the analyte mapping table first. If we don't have a rowId, we'll just let the call to
                // super.deleteRow() indicate the error to the caller
                new SqlExecutor(getDbTable().getSchema()).execute("DELETE FROM " + _analyteMappingTable + " WHERE " + _fkColumnName + " = ?", rowId);
            }
        }

        @Override
        protected Map<String, Object> updateRow(User user, Container container, Map<String, Object> row, @NotNull Map<String, Object> oldRow) throws InvalidKeyException, ValidationException, QueryUpdateServiceException, SQLException
        {
            checkPermissions(user, oldRow, UpdatePermission.class);
            checkPermissions(user, row, UpdatePermission.class);

            boolean analytesSpecified = getAnalyteIds(row) != null;
            // Use the lazy approach of deleting all of the mapped analytes and later re-inserting all of the new ones
            // We could diff them and only insert/delete the minimum set, but for the volume of expected usage this is
            // just fine
            if (analytesSpecified)
            {
                deleteAnalytes(oldRow);
            }

            Map<String, Object> result = super.updateRow(user, container, row, oldRow);

            if (analytesSpecified)
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

            String createdByKey = "CreatedBy";
            String createdKey = "Created";

            if(rowMap.get(createdByKey) != null)
            {
                //Inserts set the CreatedBy/Created fields, set them to original values
                Map<String, Object> copy = new HashMap<>();
                copy.putAll(result);
                copy.put(createdByKey, rowMap.get(createdByKey));
                copy.put(createdKey, rowMap.get(createdKey));

                try
                {
                    result = super.updateRow(user, container, copy, result, false, true);
                }
                catch (InvalidKeyException e)
                {
                    throw new QueryUpdateServiceException("Unable to retain created/created by values", e);
                }
            }
            // Be sure that the RowId is now included in the map
            rowMap.putAll(result);
            insertAnalytes(rowMap);

            return result;
        }

        /** Since we don't have a container column on these tables, check the permission on the data or run object as appropriate */
        protected abstract void checkPermissions(User user, Map<String, Object> rowMap, Class<? extends Permission> permission) throws QueryUpdateServiceException;

        protected void insertAnalytes(Map<String, Object> rowMap) throws SQLException, QueryUpdateServiceException
        {
            Integer rowId = getPKValue(rowMap);
            assert rowId != null;

            Set<Integer> analyteIds = getAnalyteIds(rowMap);
            if (analyteIds != null)
            {
                for (Integer analyteId : analyteIds)
                {
                    Analyte analyte = new TableSelector(LuminexProtocolSchema.getTableInfoAnalytes()).getObject(analyteId, Analyte.class);
                    if (analyte == null)
                    {
                        throw new QueryUpdateServiceException("No such analyte: " + analyteId);
                    }
                    validateAnalyte(rowMap, analyte);
                    Map<String, Object> fields = new HashMap<>();
                    fields.put("AnalyteId", analyteId);
                    fields.put(_fkColumnName, rowId);

                    Table.insert(null, _analyteMappingTable, fields);
                }
            }
        }

        /** @return the run associated with this exclusion */
        protected abstract @NotNull ExpRun resolveRun(Map<String, Object> rowMap) throws QueryUpdateServiceException, SQLException;

        /** Make sure that the analyte is part of the same data/run object that this exclusion is attached to */
        private void validateAnalyte(Map<String, Object> rowMap, Analyte analyte) throws QueryUpdateServiceException, SQLException
        {
            for (ExpData data : resolveRun(rowMap).getAllDataUsedByRun())
            {
                if (data.getRowId() == analyte.getDataId())
                {
                    return;
                }
            }
            throw new QueryUpdateServiceException("Attempting to reference analyte from another run");
        }
    }
}
