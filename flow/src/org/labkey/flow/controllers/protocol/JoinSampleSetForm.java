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

package org.labkey.flow.controllers.protocol;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;

import java.util.LinkedHashMap;
import java.util.Map;

public class JoinSampleSetForm extends ProtocolForm
{
    static public final int KEY_FIELDS_MAX = 5;

    public FieldKey[] ff_dataField = new FieldKey[KEY_FIELDS_MAX];
    public String[] ff_samplePropertyURI = new String[KEY_FIELDS_MAX];

    public void init() throws UnauthorizedException
    {
        FlowProtocol protocol = getProtocol();
        Map.Entry<String, FieldKey>[] entries = protocol.getSampleSetJoinFields().entrySet().toArray(new Map.Entry[KEY_FIELDS_MAX]);
        for (int i = 0; i < KEY_FIELDS_MAX; i ++)
        {
            Map.Entry<String, FieldKey> entry = entries[i];
            if (entry == null)
                break;
            ff_samplePropertyURI[i] = entry.getKey();
            ff_dataField[i] = entry.getValue();
        }
    }


    public void setFf_dataField(String[] fields)
    {
        ff_dataField = new FieldKey[fields.length];
        for (int i = 0; i < fields.length; i ++)
        {
            ff_dataField[i] = fields[i] == null ? null : FieldKey.fromString(fields[i]);
        }
    }

    public void setFf_samplePropertyURI(String[] propertyURIs)
    {
        ff_samplePropertyURI = propertyURIs;
    }

    public Map<String, String> getAvailableSampleKeyFields()
    {
        LinkedHashMap<String,String> ret = new LinkedHashMap<>();
        ret.put("", "");
        ExpSampleSet sampleSet = getProtocol().getSampleSet();
        if (sampleSet != null)
        {
            if (sampleSet.hasNameAsIdCol())
                ret.put("Name", "Name");
            for (DomainProperty property : sampleSet.getType().getProperties())
            {
                ret.put(property.getName(), property.getName());
            }
        }
        return ret;
    }

    public Map<FieldKey, String> getAvailableDataKeyFields()
    {
        LinkedHashMap<FieldKey, String> ret = new LinkedHashMap<>();
        FlowSchema schema = new FlowSchema(getUser(), getContainer());
        TableInfo tableFCSFiles = schema.getTable(FlowTableType.FCSFiles.toString());

        ret.put(null, "");
        ret.put(new FieldKey(null, "Name"), "Name");
        ret.put(FieldKey.fromParts("Run", "Name"), "Run Name");
        FieldKey keyword = new FieldKey(null, "Keyword");
        ColumnInfo colKeyword = tableFCSFiles.getColumn("Keyword");
        TableInfo tableKeywords = colKeyword.getFk().getLookupTableInfo();
        for (ColumnInfo column : tableKeywords.getColumns())
        {
            if (column.isHidden())
                continue;
            ret.put(new FieldKey(keyword, column.getName()), column.getLabel());
        }
        return ret;
    }

}
