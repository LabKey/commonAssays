/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.query.PlateBasedAssayRunDataTable;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.data.*;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.ExprColumn;
import org.labkey.nab.NabDataHandler;

import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

/**
 * User: brittp
 * Date: Jul 6, 2007
 * Time: 5:35:15 PM
 */
public class NabRunDataTable extends PlateBasedAssayRunDataTable
{
    public NabRunDataTable(final AssaySchema schema, final ExpProtocol protocol)
    {
        super(schema, protocol);
    }

    public PropertyDescriptor[] getExistingDataProperties(ExpProtocol protocol) throws SQLException
    {
        return NabSchema.getExistingDataProperties(protocol);
    }

    public String getInputMaterialPropertyName()
    {
        return NabDataHandler.NAB_INPUT_MATERIAL_DATA_PROPERTY;
    }

    public String getDataRowLsidPrefix()
    {
        return NabDataHandler.NAB_DATA_ROW_LSID_PREFIX;
    }

    static final int IC_INDEX = 0;
    static final int OOR_INDEX = 1;

    private ColumnInfo[] getCurveICColumns(AssaySchema schema, ExpProtocol protocol)
    {
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
        AssayProvider provider = AssayService.get().getProvider(protocol);

        try {
            Map<Integer, Map<DilutionCurve.FitType, int[]>> cutoffMap = new HashMap<Integer, Map<DilutionCurve.FitType, int[]>>();

            // get the cutoff values and property id informatin for each curve fit type
            for (PropertyDescriptor prop : getExistingDataProperties(protocol))
            {
                final String propertyName = prop.getName();
                if (propertyName.startsWith(NabDataHandler.CURVE_IC_PREFIX))
                {
                    int idx = propertyName.indexOf('_');
                    if (idx != -1)
                    {
                        // this is one of the newer columns with the curve fit method in the column name (we should use a regex for this)
                        String num = propertyName.substring(NabDataHandler.CURVE_IC_PREFIX.length(), propertyName.indexOf('_'));
                        Integer cutoff = Integer.valueOf(num);

                        if (!cutoffMap.containsKey(cutoff))
                            cutoffMap.put(cutoff, new HashMap<DilutionCurve.FitType, int[]>());

                        Map<DilutionCurve.FitType, int[]> fitMap = cutoffMap.get(cutoff);
                        String suffix;
                        int propIdIdx;

                        if (propertyName.endsWith(NabDataHandler.OORINDICATOR_SUFFIX))
                        {
                            suffix = propertyName.substring(idx+1, propertyName.indexOf(NabDataHandler.OORINDICATOR_SUFFIX));
                            propIdIdx = OOR_INDEX;
                        }
                        else
                        {
                            suffix = propertyName.substring(idx+1);
                            propIdIdx = IC_INDEX;
                        }

                        DilutionCurve.FitType type = DilutionCurve.FitType.fromColSuffix(suffix);
                        if (!fitMap.containsKey(type))
                            fitMap.put(type, new int[2]);

                        int[] propId = fitMap.get(type);
                        propId[propIdIdx] = prop.getPropertyId();
                    }
                }
            }
            FieldKey fitMethodFK = FieldKey.fromParts(AssayService.RUN_PROPERTIES_COLUMN_NAME, "CurveFitMethod");
            TableInfo runTable = AssayService.get().createRunTable(protocol, provider, schema.getUser(), schema.getContainer());
            Map<FieldKey, ColumnInfo> cols = QueryService.get().getColumns(runTable, Arrays.asList(fitMethodFK));
            if (cols.containsKey(fitMethodFK))
            {
                SQLFragment sqlFitMethod = QueryService.get().getSelectSQL(runTable, cols.values(), null, null, Table.ALL_ROWS, 0);

                for (Map.Entry<Integer, Map<DilutionCurve.FitType, int[]>> entry : cutoffMap.entrySet())
                {
                    SQLFragment sql = getCurveICSql(sqlFitMethod, entry.getValue(), IC_INDEX);
                    SQLFragment sqlOOR = getCurveICSql(sqlFitMethod, entry.getValue(), OOR_INDEX);

                    ExprColumn col = new ExprColumn(this, NabDataHandler.CURVE_IC_PREFIX + entry.getKey(), sql, Types.DOUBLE);
                    col.setFormatString("0.000");

                    columns.add(col);

                    ExprColumn colOOR = new ExprColumn(this, NabDataHandler.CURVE_IC_PREFIX + entry.getKey() + NabDataHandler.OORINDICATOR_SUFFIX, sqlOOR, Types.VARCHAR);
                    columns.add(colOOR);
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        return columns.toArray(new ColumnInfo[columns.size()]);
    }

    private SQLFragment getCurveICSql(SQLFragment sqlFitMethod, Map<DilutionCurve.FitType, int[]> cutoff, int propIdIdx)
    {
        SQLFragment sql = new SQLFragment("( CASE (");
        sql.append(sqlFitMethod);
        sql.append(" WHERE (RowID = " + ExprColumn.STR_TABLE_ALIAS + ".RunID)");

        // polynomial
        sql.append(") WHEN '" + DilutionCurve.FitType.POLYNOMIAL.getLabel() + "' THEN ");
        sql.append("(SELECT exp.ObjectProperty.FloatValue FROM exp.ObjectProperty WHERE PropertyId = ");
        sql.append(cutoff.get(DilutionCurve.FitType.POLYNOMIAL)[propIdIdx]);
        sql.append(" AND ObjectId = " + ExprColumn.STR_TABLE_ALIAS + ".ObjectId)");

        // 5 parameter
        sql.append(" WHEN '" + DilutionCurve.FitType.FIVE_PARAMETER.getLabel() + "' THEN ");
        sql.append("(SELECT exp.ObjectProperty.FloatValue FROM exp.ObjectProperty WHERE PropertyId = ");
        sql.append(cutoff.get(DilutionCurve.FitType.FIVE_PARAMETER)[propIdIdx]);
        sql.append(" AND ObjectId = " + ExprColumn.STR_TABLE_ALIAS + ".ObjectId)");

        // 4 parameter
        sql.append(" WHEN '" + DilutionCurve.FitType.FOUR_PARAMETER.getLabel() + "' THEN ");
        sql.append("(SELECT exp.ObjectProperty.FloatValue FROM exp.ObjectProperty WHERE PropertyId = ");
        sql.append(cutoff.get(DilutionCurve.FitType.FOUR_PARAMETER)[propIdIdx]);
        sql.append(" AND ObjectId = " + ExprColumn.STR_TABLE_ALIAS + ".ObjectId)");
        sql.append(" END");

        sql.append(")");
        return sql;
    }

    @Override
    protected Set<String> getHiddenColumns(ExpProtocol protocol)
    {
        Set<String> hiddenCols = super.getHiddenColumns(protocol);

        try {
            // hide the fit method specific values for curve IC and AUC
            for (PropertyDescriptor prop : getExistingDataProperties(protocol))
            {
                String propName = prop.getName();
                if (propName.startsWith(NabDataHandler.CURVE_IC_PREFIX) || propName.startsWith(NabDataHandler.AUC_PREFIX))
                {
                    if (propName.indexOf('_') != -1)
                        hiddenCols.add(propName);
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        return hiddenCols;
    }
}
