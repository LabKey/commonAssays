/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

package org.labkey.api.protein.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.protein.annotation.CustomAnnotationSet;
import org.labkey.api.protein.ProteinSchema;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.PropertyForeignKey;

import java.util.ArrayList;
import java.util.List;

public class CustomAnnotationTable extends FilteredTable<CustomAnnotationSchema>
{
    private final CustomAnnotationSet _annotationSet;
    private final boolean _includeSeqId;

    private final Domain _domain;

    public CustomAnnotationTable(CustomAnnotationSet annotationSet, CustomAnnotationSchema schema)
    {
        this(annotationSet, schema, null, false);
    }

    public CustomAnnotationTable(CustomAnnotationSet annotationSet, CustomAnnotationSchema schema, ContainerFilter cf, boolean includeSeqId)
    {
        super(ProteinSchema.getTableInfoCustomAnnotation(), schema, cf);
        _includeSeqId = includeSeqId;
        wrapAllColumns(true);
        _annotationSet = annotationSet;

        var propertyCol = addColumn(createPropertyColumn("Property"));
        _domain = PropertyService.get().getDomain(_annotationSet.lookupContainer(), _annotationSet.getLsid());
        if (_domain != null)
        {
            // TODO ContainerFilter
            propertyCol.setFk(new PropertyForeignKey(schema, null, _domain));
        }

        List<FieldKey> defaultCols = new ArrayList<>();
        defaultCols.add(FieldKey.fromParts("LookupString"));
        List<PropertyDescriptor> props = OntologyManager.getPropertiesForType(annotationSet.getLsid(), annotationSet.lookupContainer());
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
        var col = new ExprColumn(this, "Protein", sql, JdbcType.INTEGER);
        col.setFk(new LookupForeignKey("SeqId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                ProteinUserSchema schema = new ProteinUserSchema(_userSchema.getUser(), _userSchema.getContainer());
                return schema.createSequences();
            }
        });
        addColumn(col);
    }

    public BaseColumnInfo createPropertyColumn(String name)
    {
        String sql = ExprColumn.STR_TABLE_ALIAS + ".objecturi";
        var ret = new ExprColumn(this, name, new SQLFragment(sql), JdbcType.VARCHAR);
        ret.setIsUnselectable(true);
        return ret;
    }

    @Override @NotNull
    public SQLFragment getFromSQL(String alias)
    {
        checkReadBeforeExecute();
        if (!_includeSeqId)
            return super.getFromSQL(alias);

        SQLFragment result = new SQLFragment("(SELECT CustomAnnotationWithoutSeqId.*, i.seqId FROM ");
        result.append(super.getFromSQL("CustomAnnotationWithoutSeqId"));
        result.append(" LEFT OUTER JOIN (");
        result.append(_annotationSet.lookupCustomAnnotationType().getSeqIdSelect());
        result.append(") i ON (CustomAnnotationWithoutSeqId.LookupString = i.ident)");
        result.append(") ").append(alias);
        return result;
    }
}
