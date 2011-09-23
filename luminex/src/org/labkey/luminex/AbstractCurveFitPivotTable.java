package org.labkey.luminex;

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

    public AbstractCurveFitPivotTable(TableInfo table, LuminexSchema schema, boolean filter, String primaryCurveFitJoinColumn)
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
        return null;
    }

    protected void addCurveTypeColumns()
    {
        for (final String curveType : _schema.getCurveTypes())
        {
            ColumnInfo curveTypeColumn = createCurveTypeColumn(curveType);
            addColumn(curveTypeColumn);
        }
    }

    private ColumnInfo createCurveTypeColumn(final String curveType)
    {
        ColumnInfo curveFitColumn = wrapColumn(curveType + "CurveFit", getRealTable().getColumn(_primaryCurveFitJoinColumn));

        LookupForeignKey fk = createCurveFitFK(curveType);
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
