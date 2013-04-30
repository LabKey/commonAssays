package org.labkey.microarray.assay;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilterable;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayResultTable;
import org.labkey.microarray.view.SampleDisplayColumn;

public class AffymetrixProtocolSchema extends AssayProtocolSchema
{
    AffymetrixProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, protocol, targetStudy);
    }
    @Nullable
    @Override
    public ContainerFilterable createDataTable(boolean includeCopiedToStudyColumns)
    {
        return new AssayResultTable(this, includeCopiedToStudyColumns);
    }

    @Nullable
    @Override
    public final ContainerFilterable createDataTable()
    {
        ContainerFilterable table = super.createDataTable();

        if (null != table)
        {
            ColumnInfo columnInfo = table.getColumn("SampleName");
            columnInfo.setDisplayColumnFactory(new DisplayColumnFactory()
            {
                @Override
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    return new SampleDisplayColumn(colInfo);
                }
            });
        }

        return table;
    }

}
