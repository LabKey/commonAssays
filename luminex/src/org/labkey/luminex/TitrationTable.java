package org.labkey.luminex;

import org.apache.commons.lang.StringUtils;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryService;

import java.util.Collection;

/**
 * User: jeckels
 * Date: 7/8/11
 */
public class TitrationTable extends AbstractLuminexTable
{
    private final LuminexSchema _schema;

    public TitrationTable(LuminexSchema schema, boolean filter)
    {
        super(LuminexSchema.getTableInfoTitration(), schema, filter);
        _schema = schema;
        addColumn(wrapColumn(getRealTable().getColumn("RowId"))).setHidden(true);
        addColumn(wrapColumn(getRealTable().getColumn("Name")));
        addColumn(wrapColumn(getRealTable().getColumn("Standard")));
        addColumn(wrapColumn(getRealTable().getColumn("QCControl")));
        addColumn(wrapColumn(getRealTable().getColumn("Unknown")));
        ColumnInfo runColumn = addColumn(wrapColumn("Run", getRealTable().getColumn("RunId")));
        runColumn.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return QueryService.get().getUserSchema(_schema.getUser(), _schema.getContainer(), LuminexSchema.NAME).getTable(_schema.getRunsTableName(_schema.getProtocol()));
            }
        });
        setTitleColumn("Name");
    }

    @Override
    protected SQLFragment createContainerFilterSQL(Collection<String> ids)
    {
        SQLFragment sql = new SQLFragment("RunId IN (SELECT RowId FROM ");
        sql.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        sql.append(" WHERE Container IN (");
        sql.append(StringUtils.repeat("?", ", ", ids.size()));
        sql.append("))");
        sql.addAll(ids);
        return sql;
    }
}
