package org.labkey.ms2.protein.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.*;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.ms2.protein.CustomAnnotationSet;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.SequencesTableInfo;

import java.sql.Types;
import java.util.List;
import java.util.ArrayList;

/**
 * User: jeckels
 * Date: Apr 3, 2007
 */
public class CustomAnnotationTable extends FilteredTable
{
    private final CustomAnnotationSet _annotationSet;
    private final boolean _includeSeqId;

    public CustomAnnotationTable(CustomAnnotationSet annotationSet)
    {
        this(annotationSet, false);
    }

    public CustomAnnotationTable(CustomAnnotationSet annotationSet, boolean includeSeqId)
    {
        super(ProteinManager.getTableInfoCustomAnnotation());
        _includeSeqId = includeSeqId;
        wrapAllColumns(true);
        _annotationSet = annotationSet;

        ColumnInfo propertyCol = addColumn(createPropertyColumn("Property"));
        propertyCol.setFk(new DomainForeignKey(_annotationSet.lookupContainer(), _annotationSet.getLsid()));

        List<FieldKey> defaultCols = new ArrayList<FieldKey>();
        defaultCols.add(FieldKey.fromParts("LookupString"));
        PropertyDescriptor[] props = OntologyManager.getPropertiesForType(annotationSet.getLsid(), annotationSet.lookupContainer());
        for (PropertyDescriptor prop : props)
        {
            defaultCols.add(FieldKey.fromParts("Property", prop.getName()));
        }

        if (includeSeqId)
        {
            defaultCols.add(FieldKey.fromParts("Protein", "BestName"));
            addProteinDetailsColumn();
        }

        setDefaultVisibleColumns(defaultCols);
        SQLFragment sql = new SQLFragment();
        sql.append("CustomAnnotationSetId = ?");
        sql.add(annotationSet.getCustomAnnotationSetId());
        addCondition(sql);

    }

    private void addProteinDetailsColumn()
    {
        SQLFragment sql = new SQLFragment(getAliasName() + ".SeqId");
        ColumnInfo col = new ExprColumn(this, "Protein", sql, Types.INTEGER);
        col.setFk(new LookupForeignKey("SeqId")
        {
            public TableInfo getLookupTableInfo()
            {
                return new SequencesTableInfo(null, _annotationSet.lookupContainer());
            }
        });
        addColumn(col);
    }

    public ColumnInfo createPropertyColumn(String name)
    {
        String sql = "( SELECT objectid FROM exp.object WHERE exp.object.objecturi = " + ExprColumn.STR_TABLE_ALIAS + ".objecturi)";
        ColumnInfo ret = new ExprColumn(this, name, new SQLFragment(sql), Types.INTEGER);
        ret.setIsUnselectable(true);
        return ret;
    }


    public SQLFragment getFromSQL(String alias)
    {
        if (!_includeSeqId)
        {
            return super.getFromSQL(alias);
        }
        SQLFragment sql = super.getFromSQL("CustomAnnotationWithoutSeqId");

        SQLFragment result = new SQLFragment("(SELECT CustomAnnotationWithoutSeqId.*, i.seqId FROM ");
        result.append(sql);
        result.append(" LEFT OUTER JOIN (");
        result.append(_annotationSet.lookupCustomAnnotationType().getSeqIdSelect());
        result.append(") i ON (CustomAnnotationWithoutSeqId.LookupString = i.ident)) ");
        result.append(alias);
        return result;
    }
}
