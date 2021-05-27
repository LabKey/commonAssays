package org.labkey.elisa.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AssayProtocolSchema;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.UserIdForeignKey;
import org.labkey.elisa.ElisaProtocolSchema;

public class CurveFitTable extends FilteredTable<AssayProtocolSchema>
{
    public CurveFitTable(@NotNull AssayProtocolSchema userSchema, @Nullable ContainerFilter cf)
    {
        super(ElisaProtocolSchema.getTableInfoCurveFit(), userSchema, cf);

        ExpProtocol protocol = userSchema.getProtocol();

        setDescription("Contains standard curve fit information for the " + protocol.getName() + " assay definition");
        setName(ElisaProtocolSchema.CURVE_FIT_TABLE_NAME);

        for (ColumnInfo col : getRealTable().getColumns())
        {
            var newCol = addWrapColumn(col);

            if (newCol.getName().equalsIgnoreCase("RowId") || newCol.getName().equalsIgnoreCase("ProtocolId"))
            {
                newCol.setHidden(true);
            }
        }
        addCondition(getRealTable().getColumn("ProtocolId"), protocol.getRowId());
    }
}
