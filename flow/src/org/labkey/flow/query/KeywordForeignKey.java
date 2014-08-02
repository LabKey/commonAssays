/*
 * Copyright (c) 2006-2014 LabKey Corporation
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

package org.labkey.flow.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.SpecimenForeignKey;
import org.labkey.flow.data.AttributeType;
import org.labkey.flow.data.ICSMetadata;
import org.labkey.flow.persist.AttributeCache;
import org.labkey.flow.util.KeywordUtil;

import java.util.Collection;
import java.util.Map;

public class KeywordForeignKey extends AttributeForeignKey<String>
{
    private final FlowSchema _schema;
    FlowPropertySet _fps;
    private SpecimenForeignKey _specimenFK;

    public KeywordForeignKey(FlowSchema schema, FlowPropertySet fps)
    {
        super(schema.getContainer());
        _schema = schema;
        _fps = fps;
    }

    @Override
    protected AttributeType type()
    {
        return AttributeType.keyword;
    }

    protected String attributeFromString(String field)
    {
        return field;
    }

    protected Collection<AttributeCache.KeywordEntry> getAttributes()
    {
        return _fps.getKeywordProperties();
    }

    protected void initColumn(String attrName, String preferredName, ColumnInfo column)
    {
        column.setSqlTypeName("VARCHAR");
        column.setLabel(attrName);
        if (KeywordUtil.isHidden(attrName))
        {
            column.setHidden(true);
        }

        column.setDimension(KeywordUtil.isDimension(attrName));
        column.setMeasure(false);
    }

    protected SQLFragment sqlValue(ColumnInfo objectIdColumn, String attrName, int attrId)
    {
        // SQL server 2000 does not allow a TEXT column (i.e. flow.keyword.value) to appear in this subquery.
        // For this reason, we cast it to VARCHAR(4000).
        SQLFragment ret = new SQLFragment("(SELECT CAST(flow.Keyword.Value AS VARCHAR(4000)) FROM flow.Keyword WHERE flow.Keyword.ObjectId = ");
        ret.append(objectIdColumn.getValueSql(ExprColumn.STR_TABLE_ALIAS));
        ret.append(" AND flow.Keyword.KeywordId = ");
        ret.append(attrId);
        ret.append(")");

        //SQLFragment ret = new SQLFragment("(SELECT CAST(flow.Keyword.Value AS VARCHAR(4000)) FROM flow.Keyword, flow.KeywordAttr WHERE flow.Keyword.ObjectId = ");
        //ret.append(objectIdColumn.getValueSql(ExprColumn.STR_TABLE_ALIAS));
        //ret.append(" AND flow.Keyword.KeywordId = flow.KeywordAttr.Id AND flow.KeywordAttr.Name = ?");
        //ret.add(attrName);
        //ret.append(")");
        return ret;
    }

}
