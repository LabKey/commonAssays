package org.labkey.nab.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayService;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerFilterable;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.VirtualTable;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.util.StringExpression;

import static org.labkey.api.assay.dilution.DilutionManager.WELL_DATA_TABLE_NAME;

public class NabControlAggregatesTable extends VirtualTable<NabProtocolSchema> implements ContainerFilterable
{
    private final String _controlName;

    public NabControlAggregatesTable(String tableName, String controlName, NabProtocolSchema schema, ContainerFilter cf, ExpProtocol protocol, AssayProvider provider)
    {
        super(schema.getDbSchema(), tableName, schema, cf);
        _controlName = controlName;
        setDescription("Contains one row per run for the aggregate values of the control well data values.");

        var runCol = new BaseColumnInfo("RunId", this, JdbcType.INTEGER);
        runCol.setFk(new LookupForeignKey(getContainerFilter(), "RowID", null)
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return AssayService.get().createRunTable(protocol, provider, schema.getUser(), schema.getContainer(), getLookupContainerFilter());
            }

            @Override
            public StringExpression getURL(ColumnInfo parent)
            {
                return getURL(parent, true);
            }

        });
        addColumn(runCol);

        var controlWellgroupCol = new BaseColumnInfo("ControlWellgroup", this, JdbcType.VARCHAR);
        addColumn(controlWellgroupCol);

        var averageCol = new BaseColumnInfo("AvgValue", this, JdbcType.REAL);
        averageCol.setFormat("0.000");
        averageCol.setLabel("Avg Value");
        averageCol.setDescription("Average of control well data values");
        addColumn(averageCol);

        var minCol = new BaseColumnInfo("MinValue", this, JdbcType.REAL);
        minCol.setLabel("Min Value");
        minCol.setDescription("Minimum of control well data values");
        addColumn(minCol);

        var maxCol = new BaseColumnInfo("MaxValue", this, JdbcType.REAL);
        maxCol.setLabel("Max Value");
        maxCol.setDescription("Maximum of control well data values");
        addColumn(maxCol);

        var stdDevCol = new BaseColumnInfo("StdDevValue", this, JdbcType.REAL);
        stdDevCol.setFormat("0.000");
        stdDevCol.setLabel("Std Dev Value");
        stdDevCol.setDescription("Standard deviation of control well data values");
        addColumn(stdDevCol);
    }

    @NotNull
    @Override
    public SQLFragment getFromSQL()
    {
        SQLFragment result = new SQLFragment("SELECT\n")
            .append("w.RunId,\n")
            .append("w.ControlWellgroup,\n")
            .append("AVG(w.Value) AS AvgValue,\n")
            .append("MIN(w.Value) AS MinValue,\n")
            .append("MAX(w.Value) AS MaxValue,\n")
            .append(_userSchema.getDbSchema().getSqlDialect().getStdDevFunction()).append("(w.Value) AS StdDevValue\n")
            .append("FROM \n");

        NabWellDataTable wellDataTable = (NabWellDataTable)_userSchema.getTable(WELL_DATA_TABLE_NAME, getContainerFilter());
        result.append(wellDataTable, "w");

        result.append(" WHERE w.ControlWellgroup = ? AND Excluded = ?\n")
            .append("GROUP BY w.RunId, w.ControlWellgroup\n");
        result.add(_controlName);
        result.add(false);

        return result;
    }
}
