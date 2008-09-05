/*
 * Copyright (c) 2008 LabKey Corporation
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

import org.labkey.api.query.FieldKey;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.data.ICSMetadata;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.analysis.model.ScriptSettings;

import javax.servlet.ServletException;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * User: kevink
 * Date: Aug 14, 2008 5:26:14 PM
 */
public class EditICSMetadataForm extends ProtocolForm
{
    public static final int MATCH_COLUMNS_MAX = 3;
    
    public FieldKey[] matchColumn;
    public FieldKey backgroundField;
    public CompareType backgroundOp;
    public String backgroundValue;

    public void init(FlowProtocol protocol)
    {
        matchColumn = new FieldKey[MATCH_COLUMNS_MAX];

        ICSMetadata icsmetadata = protocol.getICSMetadata();
        if (icsmetadata != null)
        {
            if (icsmetadata.getMatchColumns() != null)
            {
                for (int i = 0; i < icsmetadata.getMatchColumns().size(); i++)
                    matchColumn[i] = icsmetadata.getMatchColumns().get(i);
            }

            if (icsmetadata.getBackgroundFilter() != null && icsmetadata.getBackgroundFilter().size() > 0)
            {
                ScriptSettings.FilterInfo backgroundFilter = icsmetadata.getBackgroundFilter().get(0);
                backgroundField = backgroundFilter.getField();
                backgroundOp = backgroundFilter.getOp();
                backgroundValue = backgroundFilter.getValue();
            }
        }
    }

    public void setMatchColumn(String[] fields)
    {
        matchColumn = new FieldKey[fields.length];
        for (int i = 0; i < fields.length; i ++)
        {
            matchColumn[i] = fields[i] == null ? null : FieldKey.fromString(fields[i]);
        }
    }

    public void setBackgroundField(String field)
    {
        backgroundField = field == null ? null : FieldKey.fromString(field);
    }

    public void setBackgroundOp(String op)
    {
        backgroundOp = op == null ? null : CompareType.getByURLKey(op);
    }

    public void setBackgroundValue(String value)
    {
        backgroundValue = value;
    }

    public Map<FieldKey, String> getKeywordAndSampleFieldMap() throws ServletException
    {
        LinkedHashMap<FieldKey, String> ret = new LinkedHashMap<FieldKey, String>();
        FlowSchema schema = new FlowSchema(getUser(), getContainer());
        TableInfo tableFCSFiles = schema.getTable(FlowTableType.FCSFiles.toString(), "Foo");

        ret.put(null, "");
        ret.put(new FieldKey(null, "Run"), "FCSAnalysis Run");

        FieldKey keyword = FieldKey.fromParts("FCSFile", "Keyword");
        ColumnInfo colKeyword = tableFCSFiles.getColumn("Keyword");
        TableInfo tableKeywords = colKeyword.getFk().getLookupTableInfo();
        for (ColumnInfo column : tableKeywords.getColumns())
        {
            ret.put(new FieldKey(keyword, column.getName()), "Keyword " + column.getCaption());
        }

        FieldKey sampleProperty = FieldKey.fromParts("FCSFile", "Sample", "Property");
        if (getProtocol().getSampleSet() != null)
        {
            for (PropertyDescriptor pd : getProtocol().getSampleSet().getPropertiesForType())
            {
                ret.put(new FieldKey(sampleProperty, pd.getName()), "Sample " + pd.getName());
            }
        }
        
        return ret;
    }

}
