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
package org.labkey.flow.data;

import org.apache.xmlbeans.XmlException;
import org.fhcrc.cpas.flow.script.xml.FilterDef;
import org.fhcrc.cpas.flow.script.xml.OpDef;
import org.fhcrc.cpas.flow.script.xml.FiltersDef;
import org.labkey.api.query.FieldKey;
import org.labkey.api.data.FilterInfo;
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
    List<FilterInfo> background;

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

    public List<FilterInfo> getBackgroundFilter()
    {
        return background;
    }

    public void setBackgroundFilter(List<FilterInfo> filters)
    {
        background = filters;
    }

    public FilterInfo getBackgroundFilter(FieldKey fieldKey)
    {
        for (FilterInfo filter : background)
        {
            if (filter.getField().equals(fieldKey))
                return filter;
        }
        return null;
    }

    public String toXmlString()
    {
        ICSMetadataDocument xDoc = ICSMetadataDocument.Factory.newInstance();
        ICSMetadataType.Background xBackground = xDoc.addNewICSMetadata().addNewBackground();

        if (getMatchColumns() != null && getMatchColumns().size() > 0)
        {
            List<String> matchColumns = new ArrayList<String>(getMatchColumns().size());
            for (FieldKey fieldKey : getMatchColumns())
            {
                if (fieldKey != null)
                    matchColumns.add(fieldKey.toString());
            }
            xBackground.addNewMatchColumns().setFieldArray(matchColumns.toArray(new String[matchColumns.size()]));
        }

        if (getBackgroundFilter() != null && getBackgroundFilter().size() > 0)
        {
            FiltersDef xBackgroundFilter = null;
            for (FilterInfo filterInfo : getBackgroundFilter())
            {
                if (filterInfo != null && filterInfo.getField() != null && filterInfo.getOp() != null)
                {
                    if (xBackgroundFilter == null)
                        xBackgroundFilter = xBackground.addNewBackgroundFilter();
                    FilterDef xFilterDef = xBackgroundFilter.addNewFilter();

                    xFilterDef.setField(filterInfo.getField().toString());
                    xFilterDef.setOp(OpDef.Enum.forString(filterInfo.getOp().getPreferredUrlKey()));
                    if (filterInfo.getValue() != null)
                        xFilterDef.setValue(filterInfo.getValue());
                }
            }
        }

        return xDoc.toString();
    }

    public static ICSMetadata fromXmlString(String value)
    {
        ICSMetadata result = new ICSMetadata();
        ICSMetadataDocument xDoc;
        try
        {
            xDoc = ICSMetadataDocument.Factory.parse(value);
        }
        catch (XmlException ex)
        {
            // failed to parse, just return an empty metadata
            return result;
        }

        ICSMetadataType xMetadata = xDoc.getICSMetadata();
        ICSMetadataType.Background xBackground = xMetadata.getBackground();

        if (xBackground.getMatchColumns() != null)
        {
            List<FieldKey> matchColumns = new LinkedList<FieldKey>();
            for (Object field : xBackground.getMatchColumns().getFieldArray())
            {
                matchColumns.add(FieldKey.fromString((String)field));
            }
            result.setMatchColumns(matchColumns);
        }

        List<FilterInfo> backgroundFilters = new ArrayList<FilterInfo>();

        // 'backgroundColumn' element is deprecated
        FilterDef xBackgroundColumn = xBackground.getBackgroundColumn();
        if (xBackgroundColumn != null)
        {
            FilterInfo filter = ScriptSettings.fromFilterDef(xBackgroundColumn);
            backgroundFilters.add(filter);
        }

        FiltersDef xBackgroundFilter = xBackground.getBackgroundFilter();
        if (xBackgroundFilter != null)
        {
            FilterDef[] xFilters = xBackgroundFilter.getFilterArray();
            if (xFilters != null && xFilters.length > 0)
            {
                for (FilterDef xFilter : xFilters)
                {
                    if (xFilter == null)
                        continue;
                    FilterInfo filter = ScriptSettings.fromFilterDef(xFilter);
                    backgroundFilters.add(filter);
                }
            }
        }

        result.setBackgroundFilter(backgroundFilters);

        return result;
    }
}
