/*
 * Copyright (c) 2008-2009 LabKey Corporation
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
import java.util.*;
import java.util.regex.Pattern;

/**
 * User: kevink
 * Date: Aug 14, 2008 5:26:14 PM
 */
public class EditICSMetadataForm extends ProtocolForm
{
    public static final int MATCH_COLUMNS_MAX = 4;
    public static final int BACKGROUND_COLUMNS_MAX = 5;

    // from form posted values
    public String[] matchColumnFields;
    public String[] backgroundFilterFields;
    public String[] backgroundFilterOps;
    public String[] backgroundFilterValues;

    // from FlowProtocol's ICSMetadata
    public FieldKey[] matchColumn;
    public ScriptSettings.FilterInfo[] backgroundFilter;

    public void init(FlowProtocol protocol)
    {
        matchColumn = new FieldKey[MATCH_COLUMNS_MAX];
        backgroundFilter = new ScriptSettings.FilterInfo[BACKGROUND_COLUMNS_MAX];

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
                for (int i = 0; i < icsmetadata.getBackgroundFilter().size(); i++)
                {
                    ScriptSettings.FilterInfo filter = icsmetadata.getBackgroundFilter().get(i);
                    backgroundFilter[i] = new ScriptSettings.FilterInfo(filter.getField(), filter.getOp(), filter.getValue());
                }
            }
        }

        if (matchColumn[0] == null)
        {
            // default the form to include Run
            matchColumn[0] = new FieldKey(null, "Run");
        }
    }

    public void setMatchColumn(String[] fields)
    {
        matchColumnFields = fields;
    }

    public void setBackgroundField(String[] fields)
    {
        backgroundFilterFields = fields;
    }

    public void setBackgroundOp(String[] ops)
    {
        backgroundFilterOps = ops;
    }

    public void setBackgroundValue(String[] values)
    {
        backgroundFilterValues = values;
    }

    /** Get match columns from form posted values. */
    public List<FieldKey> getMatchColumns()
    {
        List<FieldKey> matchColumns = new ArrayList<FieldKey>(matchColumnFields.length);
        for (String field : matchColumnFields)
        {
            if (field != null)
                matchColumns.add(FieldKey.fromString(field));
        }
        return matchColumns;
    }

    /** Get background filters from form posted values. */
    public List<ScriptSettings.FilterInfo> getBackgroundFilters()
    {
        List<ScriptSettings.FilterInfo> filters = new ArrayList<ScriptSettings.FilterInfo>(backgroundFilterFields.length);
        if (backgroundFilterFields != null && backgroundFilterOps != null)
        {
            for (int i = 0; i < backgroundFilterFields.length; i++)
            {
                String field = backgroundFilterFields[i];
                if (field == null)
                    continue;
                
                String op;
                if (backgroundFilterOps.length < i || backgroundFilterOps[i] == null)
                    op = CompareType.NONBLANK.getUrlKey();
                else
                    op = backgroundFilterOps[i];

                String value;
                if (backgroundFilterValues.length < i || backgroundFilterValues[i] == null)
                    value = null;
                else
                    value = backgroundFilterValues[i];

                ScriptSettings.FilterInfo filter = new ScriptSettings.FilterInfo(field, op, value);
                filters.add(filter);
            }
        }
        return filters;
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

        Pattern skipKeyword = Pattern.compile("^(" +
                "\\$BEGINANALYSIS|\\$BEGINDATA|\\$BEGINSTEXT|" +
                "\\$ENDANALYSIS|\\$ENDDATA|\\$ENDSTEXT|" +
                "\\$BYTEORD|\\$DATATYPE|\\$MODE|\\$NEXTDATA|" +
                "\\$P\\d+.|\\$PAR|\\$TOT|" +
                "\\$ABRT|\\$BTIM|\\$ETIM|" +
                "\\$CSMODE|\\$CSVBITS|" +
                "\\$CSV\\d+FLAG|" +
                "\\$GATING|\\$LOST|" +
                "\\$PK\\d+.|" +
                "\\$G\\d+.|\\$R\\d.|" +
                "\\$TIMESTEP|" +
                "WINDOW EXTENSION)$", Pattern.CASE_INSENSITIVE);
        for (ColumnInfo column : tableKeywords.getColumns())
        {
            String name = column.getName();
            if (skipKeyword.matcher(name).matches())
                continue;
            ret.put(new FieldKey(keyword, name), "Keyword " + column.getCaption());
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
