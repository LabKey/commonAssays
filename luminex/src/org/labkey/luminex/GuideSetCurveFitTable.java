package org.labkey.luminex;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerFilterable;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.VirtualTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.study.assay.AssaySchema;

/**
 * User: jeckels
 * Date: Sep 2, 2011
 */
public class GuideSetCurveFitTable extends VirtualTable implements ContainerFilterable
{
    private final LuminexSchema _schema;
    private @NotNull ContainerFilter _containerFilter = ContainerFilter.CURRENT;

    public GuideSetCurveFitTable(LuminexSchema schema, boolean filterTable)
    {
        super(schema.getDbSchema());
        _schema = schema;

        ColumnInfo guideSetIdColumn = new ColumnInfo("GuideSetId", this);
        guideSetIdColumn.setLabel("Guide Set");
        guideSetIdColumn.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _schema.createGuideSetTable(false);
            }
        });
        guideSetIdColumn.setJdbcType(JdbcType.INTEGER);
        addColumn(guideSetIdColumn);

        ColumnInfo runCountColumn = new ColumnInfo("RunCount", this);
        runCountColumn.setJdbcType(JdbcType.INTEGER);
        addColumn(runCountColumn);

        ColumnInfo aucAverageColumn = new ColumnInfo("AUCAverage", this);
        aucAverageColumn.setJdbcType(JdbcType.REAL);
        aucAverageColumn.setFormat("0.00");
        addColumn(aucAverageColumn);

        ColumnInfo aucStdDevColumn = new ColumnInfo("AUCStdDev", this);
        aucStdDevColumn.setJdbcType(JdbcType.REAL);
        aucStdDevColumn.setFormat("0.00");
        addColumn(aucStdDevColumn);

        ColumnInfo ec50AverageColumn = new ColumnInfo("EC50Average", this);
        ec50AverageColumn.setJdbcType(JdbcType.REAL);
        ec50AverageColumn.setFormat("0.00");
        addColumn(ec50AverageColumn);

        ColumnInfo ec50StdDevColumn = new ColumnInfo("EC50StdDev", this);
        ec50StdDevColumn.setJdbcType(JdbcType.REAL);
        ec50StdDevColumn.setFormat("0.00");
        addColumn(ec50StdDevColumn);

        ColumnInfo curveTypeColumn = new ColumnInfo("CurveType", this);
        curveTypeColumn.setJdbcType(JdbcType.VARCHAR);
        addColumn(curveTypeColumn);

        setName(AssaySchema.getProviderTableName(schema.getProtocol(), LuminexSchema.GUIDE_SET_CURVE_FIT_TABLE_NAME));
    }

    @NotNull
    @Override
    public SQLFragment getFromSQL()
    {
        SQLFragment result = new SQLFragment("SELECT AVG(cf.EC50) AS EC50Average,\n");
        result.append(_schema.getDbSchema().getSqlDialect().getStdDevFunction() + "(cf.EC50) AS EC50StdDev, \n");
        result.append("AVG(cf.AUC) AS AUCAverage, \n");
        result.append("COUNT(DISTINCT at.AnalyteId) AS RunCount, \n");
        result.append(_schema.getDbSchema().getSqlDialect().getStdDevFunction() + "(cf.AUC) AS AUCStdDev, \n");
        result.append("a.GuideSetId,\n");
        result.append("cf.CurveType FROM \n");

        AnalyteTable analyteTable = _schema.createAnalyteTable(true);
        analyteTable.setContainerFilter(getContainerFilter());
        result.append(analyteTable, "a");
        result.append(", ");

        TableInfo analyteTitrationTable = LuminexSchema.getTableInfoAnalyteTitration();
        result.append(analyteTitrationTable, "at");
        result.append(", ");

        CurveFitTable curveFitTable = _schema.createCurveFitTable(true);
        curveFitTable.setContainerFilter(getContainerFilter());
        result.append(curveFitTable, "cf");

        result.append(" WHERE a.RowId = at.AnalyteId AND at.AnalyteId = cf.AnalyteId AND at.TitrationId = cf.TitrationId AND a.GuideSetId IS NOT NULL AND a.IncludeInGuideSetCalculation = ?\n");
        result.add(true);
        result.append(" GROUP BY a.GuideSetId, cf.CurveType");

        return result;
    }

    @Override
    public void setContainerFilter(@NotNull ContainerFilter containerFilter)
    {
        _containerFilter = containerFilter;
    }

    @NotNull
    @Override
    public ContainerFilter getContainerFilter()
    {
        return _containerFilter;
    }

    @Override
    public boolean hasDefaultContainerFilter()
    {
        return false;
    }

    @Override
    public String getPublicSchemaName()
    {
        return AssaySchema.NAME;
    }
}
