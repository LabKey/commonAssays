package org.labkey.luminex;

import org.apache.commons.lang.StringUtils;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.LookupForeignKey;

import java.util.Collection;

/**
 * User: jeckels
 * Date: Aug 18, 2011
 */
public class CurveFitTable extends AbstractLuminexTable
{
    public CurveFitTable(LuminexSchema schema, boolean filterTable)
    {
        super(LuminexSchema.getTableInfoCurveFit(), schema, filterTable);
        wrapAllColumns(true);
        ColumnInfo titrationCol = getColumn("TitrationId");
        titrationCol.setLabel("Titration");
        titrationCol.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _schema.createTitrationTable(false);
            }
        });

        ColumnInfo analyteCol = getColumn("AnalyteId");
        analyteCol.setLabel("Analyte");
        analyteCol.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _schema.createAnalyteTable(false);
            }
        });

    }

    @Override
    protected SQLFragment createContainerFilterSQL(Collection<String> ids)
    {
        SQLFragment sql = new SQLFragment("TitrationId IN (SELECT t.RowId FROM ");
        sql.append(LuminexSchema.getTableInfoTitration(), "t");
        sql.append(" WHERE t.RunId IN (SELECT r.RowId FROM ");
        sql.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        sql.append(" WHERE r.Container IN (");
        sql.append(StringUtils.repeat("?", ", ", ids.size()));
        sql.addAll(ids);
        sql.append(")))");
        return sql;

    }
}
