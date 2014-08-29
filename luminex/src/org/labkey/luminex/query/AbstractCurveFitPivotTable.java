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
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.LookupForeignKey;

/**
 * User: jeckels
 * Date: Sep 22, 2011
 */
public abstract class AbstractCurveFitPivotTable extends AbstractLuminexTable
{
    private static final String CURVE_FIT_SUFFIX = "CurveFit";
    private final String _primaryCurveFitJoinColumn;

    public AbstractCurveFitPivotTable(TableInfo table, LuminexProtocolSchema schema, boolean filter, String primaryCurveFitJoinColumn)
    {
        super(table, schema, filter);
        _primaryCurveFitJoinColumn = primaryCurveFitJoinColumn;
    }

    @Override
    protected ColumnInfo resolveColumn(String name)
    {
        if (name.toLowerCase().endsWith(CURVE_FIT_SUFFIX.toLowerCase()) && name.length() > CURVE_FIT_SUFFIX.length())
        {
            String curveTypeName = name.substring(0, name.length() - CURVE_FIT_SUFFIX.length());
            return createCurveTypeColumn(curveTypeName);
        }
        return super.resolveColumn(name);
    }

    protected void addCurveTypeColumns()
    {
        for (final String curveType : _userSchema.getCurveTypes())
        {
            ColumnInfo curveTypeColumn = createCurveTypeColumn(curveType);
            addColumn(curveTypeColumn);
        }
    }

    private ColumnInfo createCurveTypeColumn(final String curveType)
    {
        ColumnInfo curveFitColumn = wrapColumn(curveType + "CurveFit", getRealTable().getColumn(_primaryCurveFitJoinColumn));

        LookupForeignKey fk = createCurveFitFK(curveType);
        // We need the prefix to distinguish between the different kinds of curve fits
        fk.setPrefixColumnCaption(true);
        curveFitColumn.setIsUnselectable(true);
        curveFitColumn.setShownInDetailsView(false);
        curveFitColumn.setReadOnly(true);
        curveFitColumn.setKeyField(false);
        curveFitColumn.setShownInInsertView(false);
        curveFitColumn.setShownInUpdateView(false);
        curveFitColumn.setFk(fk);
        return curveFitColumn;
    }
    
    protected abstract LookupForeignKey createCurveFitFK(final String curveType);
}
