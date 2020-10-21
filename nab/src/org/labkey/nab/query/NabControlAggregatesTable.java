package org.labkey.nab.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerFilterable;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.VirtualTable;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.QueryForeignKey;

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
        runCol.setFk(QueryForeignKey.from(schema, cf).to("Runs", "RowID", null));
        addColumn(runCol);

        addColumn(new BaseColumnInfo("ControlWellgroup", this, JdbcType.VARCHAR));
        addColumn(new BaseColumnInfo("PlateNumber", this, JdbcType.INTEGER));

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
            .append("w.PlateNumber,\n")
            .append("AVG(w.Value) AS AvgValue,\n")
            .append("MIN(w.Value) AS MinValue,\n")
            .append("MAX(w.Value) AS MaxValue,\n")
            .append(_userSchema.getDbSchema().getSqlDialect().getStdDevFunction()).append("(w.Value) AS StdDevValue\n")
            .append("FROM \n");

        NabWellDataTable wellDataTable = (NabWellDataTable)_userSchema.getTable(WELL_DATA_TABLE_NAME, getContainerFilter());
        result.append(wellDataTable, "w");

        // NOTE: currently filtering to just Plate 1, to not create Run table row duplication for the high-throughput assay case
        result.append(" WHERE w.ControlWellgroup = ? AND Excluded = ? AND PlateNumber = 1\n")
            .append("GROUP BY w.RunId, w.ControlWellgroup, w.PlateNumber\n");
        result.add(_controlName);
        result.add(false);

        return result;
    }
}
