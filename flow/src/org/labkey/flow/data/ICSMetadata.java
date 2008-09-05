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
package org.labkey.flow.data;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.fhcrc.cpas.flow.script.xml.FilterDef;
import org.fhcrc.cpas.flow.script.xml.OpDef;
import org.labkey.api.query.FieldKey;
import org.labkey.flow.analysis.model.ScriptSettings;
import org.labkey.flow.icsmetadata.xml.ICSMetadataDocument;
import org.labkey.flow.icsmetadata.xml.ICSMetadataType;

import java.util.*;

/**
 * User: kevink
 * Date: Aug 13, 2008 9:33:31 AM
 */
public class ICSMetadata
{
    List<FieldKey> matchColumns; // columns shared between background and stimulated wells
    List<ScriptSettings.FilterInfo> background;

    public ICSMetadata()
    {

    }

    public boolean isEmpty()
    {
        if (matchColumns != null && matchColumns.size() > 0)
            return false;
        if (background != null && background.size() > 0)
            return false;
        return true;
    }

    public List<FieldKey> getMatchColumns()
    {
        return matchColumns;
    }

    public void setMatchColumns(List<FieldKey> matchColumns)
    {
        this.matchColumns = matchColumns;
    }

    public List<ScriptSettings.FilterInfo> getBackgroundFilter()
    {
        return background;
    }

    public void setBackgroundFilter(List<ScriptSettings.FilterInfo> filters)
    {
        background = filters;
    }

    public ScriptSettings.FilterInfo getBackgroundFilter(FieldKey fieldKey)
    {
        for (ScriptSettings.FilterInfo filter : background)
        {
            if (filter.getField().equals(fieldKey))
                return filter;
        }
        return null;
    }

    public String toXmlString()
    {
        ICSMetadataDocument doc = ICSMetadataDocument.Factory.newInstance();
        ICSMetadataType.Background background = doc.addNewICSMetadata().addNewBackground();

        if (getMatchColumns() != null && getMatchColumns().size() > 0)
        {
            List<String> matchColumns = new ArrayList<String>(getMatchColumns().size());
            for (FieldKey fieldKey : getMatchColumns())
            {
                if (fieldKey != null)
                    matchColumns.add(fieldKey.toString());
            }
            background.addNewMatchColumns().setFieldArray(matchColumns.toArray(new String[matchColumns.size()]));
        }

        if (this.getBackgroundFilter() != null && this.getBackgroundFilter().size() > 0)
        {
            FilterDef backgroundColumn = background.addNewBackgroundColumn();
            ScriptSettings.FilterInfo filter = this.getBackgroundFilter().get(0);
            if (filter != null)
            {
                backgroundColumn.setField(filter.getField().toString());
                backgroundColumn.setOp(OpDef.Enum.forString(filter.getOp().getUrlKey()));
                if (filter.getValue() != null)
                    backgroundColumn.setValue(filter.getValue());
            }
        }

        return doc.toString();
    }

    public static ICSMetadata fromXmlString(String value)
    {
        ICSMetadata result = new ICSMetadata();
        ICSMetadataDocument doc;
        try
        {
            doc = ICSMetadataDocument.Factory.parse(value);
        }
        catch (XmlException ex)
        {
            // failed to parse, just return an empty metadata
            return result;
        }

        ICSMetadataType metadata = doc.getICSMetadata();
        ICSMetadataType.Background background = metadata.getBackground();

        if (background.getMatchColumns() != null)
        {
            List<FieldKey> matchColumns = new LinkedList<FieldKey>();
            for (Object field : background.getMatchColumns().getFieldArray())
            {
                matchColumns.add(FieldKey.fromString((String)field));
            }
            result.setMatchColumns(matchColumns);
        }

        FilterDef backgroundColumn = background.getBackgroundColumn();
        if (backgroundColumn != null)
        {
            ScriptSettings.FilterInfo filter = ScriptSettings.FilterInfo.fromFilterDef(backgroundColumn);
            result.setBackgroundFilter(Collections.singletonList(filter));
        }
        return result;
    }
}
