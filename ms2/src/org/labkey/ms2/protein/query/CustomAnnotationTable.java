/*
 * Copyright (c) 2007-2010 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.ms2.protein.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.query.*;
import org.labkey.ms2.protein.CustomAnnotationSet;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.SequencesTableInfo;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * User: jeckels
 * Date: Apr 3, 2007
 */
public class CustomAnnotationTable extends FilteredTable
{
    private final CustomAnnotationSet _annotationSet;
    private final boolean _includeSeqId;

    private final QuerySchema _schema;
    private Domain _domain;

    public CustomAnnotationTable(CustomAnnotationSet annotationSet, QuerySchema schema)
    {
        this(annotationSet, schema, false);
    }

    public CustomAnnotationTable(CustomAnnotationSet annotationSet, QuerySchema schema, boolean includeSeqId)
    {
        super(ProteinManager.getTableInfoCustomAnnotation());
        _schema = schema;
        _includeSeqId = includeSeqId;
        wrapAllColumns(true);
        _annotationSet = annotationSet;

        ColumnInfo propertyCol = addColumn(createPropertyColumn("Property"));
        _domain = PropertyService.get().getDomain(_annotationSet.lookupContainer(), _annotationSet.getLsid());
        if (_domain != null)
        {
            propertyCol.setFk(new PropertyForeignKey(_domain, schema));
        }

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

    @Override
    public Domain getDomain()
    {
        return _domain;
    }

    private void addProteinDetailsColumn()
    {
        SQLFragment sql = new SQLFragment(getName() + ".SeqId");
        ColumnInfo col = new ExprColumn(this, "Protein", sql, Types.INTEGER);
        col.setFk(new LookupForeignKey("SeqId")
        {
            public TableInfo getLookupTableInfo()
            {
                return new SequencesTableInfo(_schema);
            }
        });
        addColumn(col);
    }

    public ColumnInfo createPropertyColumn(String name)
    {
        String sql = ExprColumn.STR_TABLE_ALIAS + ".objecturi";
        ColumnInfo ret = new ExprColumn(this, name, new SQLFragment(sql), Types.VARCHAR);
        ret.setIsUnselectable(true);
        return ret;
    }


    @Override @NotNull
    public SQLFragment getFromSQL()
    {
        SQLFragment sql = super.getFromSQL();
        if (!_includeSeqId)
            return sql;
        
        SQLFragment result = new SQLFragment("SELECT CustomAnnotationWithoutSeqId.*, i.seqId FROM (");
        result.append(sql);
        result.append(") CustomAnnotationWithoutSeqId LEFT OUTER JOIN (");
        result.append(_annotationSet.lookupCustomAnnotationType().getSeqIdSelect());
        result.append(") i ON (CustomAnnotationWithoutSeqId.LookupString = i.ident)");
        return result;
    }
}
