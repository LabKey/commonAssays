package org.labkey.ms2.query;

import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.data.SQLFragment;
import org.labkey.ms2.protein.ProteinManager;

import java.sql.Types;

/**
 * User: jeckels
 * Date: Feb 22, 2007
 */
public class OrganismTableInfo extends FilteredTable
{
    public OrganismTableInfo()
    {
        super(ProteinManager.getTableInfoOrganisms());

        wrapAllColumns(true);

        SQLFragment sql = new SQLFragment();
        sql.append("CASE WHEN CommonName IS NULL THEN Genus ");
        sql.append(getSqlDialect().getConcatenationOperator());
        sql.append(" ' ' ");
        sql.append(getSqlDialect().getConcatenationOperator());
        sql.append(" Species ELSE CommonName END");
        ExprColumn descriptionColumn = new ExprColumn(this, "Description", sql, Types.VARCHAR);
        addColumn(descriptionColumn);

        removeColumn(getColumn("IdentId"));
    }
}
