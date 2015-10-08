/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.nab.query;

import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.AbstractForeignKey;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.VirtualTable;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.WellGroupTemplate;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.StringExpression;
import org.labkey.nab.NabAssayProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by davebradlee on 7/10/15.
 *
 */
public class NabWellDataTable extends NabBaseTable
{
    public static final String CONTROL_WELLGROUP_NAME = "ControlWellgroup";
    public static final String VIRUS_WELLGROUP_NAME = "VirusWellgroup";
    public static final String SPECIMEN_WELLGROUP_NAME = "SpecimenWellgroup";
    public static final String REPLICATE_WELLGROUP_NAME = "ReplicateWellgroup";
    public NabWellDataTable(final NabProtocolSchema schema, ExpProtocol protocol)
    {
        super(schema, DilutionManager.getTableInfoWellData(), protocol);

        addRunColumn();
        addSpecimenColumn();

        // Wrap all columns except a couple we'll handle specially
        for (ColumnInfo col : getRealTable().getColumns())
        {
            if (getInputMaterialPropertyName().equalsIgnoreCase(col.getName()) || "RunId".equalsIgnoreCase(col.getName()))
                continue;

            String name = col.getName();
            if ("DilutionDataId".equalsIgnoreCase(name))
                name = "DilutionData";
            else if ("RunDataId".equalsIgnoreCase(name))
                name = "RunData";

            ColumnInfo newCol;
            if ("Row".equalsIgnoreCase(name) || "Column".equalsIgnoreCase(name))
                newCol = addOneBasedColumn(name, col);
            else
                newCol = addWrapColumn(name, col);
            if (col.isHidden())
            {
                newCol.setHidden(col.isHidden());
            }
        }

        AssayProvider provider = AssayService.get().getProvider(_protocol);
        if (provider instanceof NabAssayProvider)
        {
            NabAssayProvider nabAssayProvider = (NabAssayProvider)provider;
            PlateTemplate template = nabAssayProvider.getPlateTemplate(getContainer(), _protocol);
            if (null != template)
            {
                addWellNameColumn(template.getRows());
                addWellgroupPropertyColumns(template);
            }
        }
        addCondition(getRealTable().getColumn("ProtocolId"), protocol.getRowId());
    }

    private ColumnInfo addOneBasedColumn(String name, ColumnInfo column)
    {
        if (!JdbcType.INTEGER.equals(column.getJdbcType()))
            throw new IllegalStateException("Can only add 1 to value of integer type.");
        SQLFragment sql = new SQLFragment("(");
        sql.append(column.getValueSql(ExprColumn.STR_TABLE_ALIAS)).append(" + 1)");
        return addColumn(new ExprColumn(this, name, sql, JdbcType.INTEGER, column));
    }

    private void addWellNameColumn(int rowCount)
    {
        ColumnInfo row = getColumn("Row");
        ColumnInfo column = getColumn("Column");
        SQLFragment rowSql = new SQLFragment("(CASE ");
        for (int i = 0; i < rowCount; i++)
        {
            char chr = (char)('A' + i);
            rowSql.append("\nWHEN ").append(row.getValueSql(ExprColumn.STR_TABLE_ALIAS)).append("=").append(i + 1).append(" THEN '").append(new Character(chr)).append("'");
        }
        rowSql.append("\nELSE '' END) ");
        SQLFragment colSql = new SQLFragment("CAST(");
        colSql.append(column.getValueSql(ExprColumn.STR_TABLE_ALIAS)).append(" AS VARCHAR(10))");
        SQLFragment sql = getSqlDialect().concatenate(rowSql, colSql);
        addColumn(new ExprColumn(this, "WellName", sql, JdbcType.VARCHAR, row, column));
    }

    private void addWellgroupPropertyColumns(PlateTemplate plateTemplate)
    {
        Map<WellGroup.Type, Map<String, WellGroupTemplate>> wellGroupTemplateMap = plateTemplate.getWellGroupTemplateMap();
        Map<String, WellGroupTemplate> controlTemplates = wellGroupTemplateMap.get(WellGroup.Type.CONTROL);
        if (null != controlTemplates && !controlTemplates.isEmpty())
        {
            addWellgroupPropertyColumns(controlTemplates, CONTROL_WELLGROUP_NAME);
        }
        Map<String, WellGroupTemplate> virusTemplates = wellGroupTemplateMap.get(WellGroup.Type.VIRUS);
        if (null != virusTemplates && !virusTemplates.isEmpty())
        {
            addWellgroupPropertyColumns(virusTemplates, VIRUS_WELLGROUP_NAME);
        }
        Map<String, WellGroupTemplate> specimenTemplates = wellGroupTemplateMap.get(WellGroup.Type.SPECIMEN);
        if (null != specimenTemplates && !specimenTemplates.isEmpty())
        {
            addWellgroupPropertyColumns(specimenTemplates, SPECIMEN_WELLGROUP_NAME);
        }
        Map<String, WellGroupTemplate> replicateTemplates = wellGroupTemplateMap.get(WellGroup.Type.REPLICATE);
        if (null != replicateTemplates && !replicateTemplates.isEmpty())
        {
            addWellgroupPropertyColumns(replicateTemplates, REPLICATE_WELLGROUP_NAME);
        }
    }

    private void addWellgroupPropertyColumns(Map<String, WellGroupTemplate> wellgroupTemplates, String wellgroupNameColumnName)
    {
        Map<String, Map<String, Object>> propertyMap = new CaseInsensitiveHashMap<>();        // Map propertyName -> (Map wellGroupName -> propertyValue)
        for (WellGroupTemplate template : wellgroupTemplates.values())
        {
            for (String propertyName : template.getPropertyNames())
            {
                if (!propertyMap.containsKey(propertyName))
                    propertyMap.put(propertyName, new HashMap<>());
                propertyMap.get(propertyName).put(template.getName(), template.getProperty(propertyName));
            }
        }

        final ColumnInfo wellgroupNameColumn = getColumn(wellgroupNameColumnName);

        final TableInfo parentTable = this;
        wellgroupNameColumn.setFk(new AbstractForeignKey()
        {
            // This is a little interesting. The virtual table has an ExprColumn for each property, but since the properties
            // are all from the plate template, their values are fixed, from the point of view of this protocol
            // So, when we view the query details, we want it to look like another table (the little plus that opens with the properties),
            // but when generating the query, the columns live in this, the WellData table, since their expressions use other WellData columns
            @Override
            public ColumnInfo createLookupColumn(ColumnInfo parent, String displayField)
            {
                if (null == displayField)
                {
                    return parent;
                }
                else
                {
                    TableInfo tableInfo = getLookupTableInfo();
                    ColumnInfo column = tableInfo.getColumn(displayField);
                    return new ExprColumn(null != parent ? parent.getParentTable() : parentTable, column.getName(),
                            column.getValueSql(ExprColumn.STR_TABLE_ALIAS), column.getJdbcType(), wellgroupNameColumn);
                }
            }

            @Override
            public TableInfo getLookupTableInfo()
            {
                VirtualTable ret = new VirtualTable(ExperimentService.get().getSchema(), null);
                for (Map.Entry<String, Map<String, Object>> propertyEntry : propertyMap.entrySet())
                {
                    SQLFragment sql = new SQLFragment("(CASE ");
                    for (Map.Entry<String, Object> wellGroupEntry : propertyEntry.getValue().entrySet())
                    {
                        String value = null != wellGroupEntry.getValue() ? wellGroupEntry.getValue().toString() : "";
                        sql.append("\nWHEN ").append(wellgroupNameColumn.getValueSql(ExprColumn.STR_TABLE_ALIAS)).append("='")
                                .append(wellGroupEntry.getKey()).append("' THEN '").append(value).append("'");
                    }
                    sql.append("\nELSE '' END) ");
                    String columnName = propertyEntry.getKey();
                    ret.addColumn(new ExprColumn(ret, columnName, sql, JdbcType.VARCHAR, wellgroupNameColumn));
                }
                return ret;
            }

            @Override
            public StringExpression getURL(ColumnInfo parent)
            {
                return null;
            }
        });
    }
}
