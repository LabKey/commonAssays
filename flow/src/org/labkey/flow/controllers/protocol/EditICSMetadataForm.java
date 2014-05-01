/*
 * Copyright (c) 2008-2014 LabKey Corporation
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

import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.query.FieldKey;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.FilterInfo;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.persist.AttributeCache;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.data.ICSMetadata;

import java.util.*;

/**
 * User: kevink
 * Date: Aug 14, 2008 5:26:14 PM
 */
public class EditICSMetadataForm extends ProtocolForm
{
    public static final int MATCH_COLUMNS_MAX = 4;
    public static final int BACKGROUND_COLUMNS_MAX = 5;

    // from form posted values
    public String ff_specimenIdColumn;
    public String ff_participantColumn;
    public String ff_visitColumn;
    public String ff_dateColumn;
    public String[] ff_matchColumn;
    public String[] ff_backgroundFilterField;
    public String[] ff_backgroundFilterOp;
    public String[] ff_backgroundFilterValue;

    // from FlowProtocol's ICSMetadata
    public FieldKey specimenIdColumn;
    public FieldKey participantColumn;
    public FieldKey visitColumn;
    public FieldKey dateColumn;
    public FieldKey[] matchColumn;
    public FilterInfo[] backgroundFilter;

    // populate the form fields from the saved ICSMetadata
    public void init(ICSMetadata icsmetadata)
    {
        matchColumn = new FieldKey[MATCH_COLUMNS_MAX];
        backgroundFilter = new FilterInfo[BACKGROUND_COLUMNS_MAX];

        if (icsmetadata != null)
        {
            if (icsmetadata.getSpecimenIdColumn() != null)
                specimenIdColumn = icsmetadata.getSpecimenIdColumn();

            if (icsmetadata.getParticipantColumn() != null)
                participantColumn = icsmetadata.getParticipantColumn();

            if (icsmetadata.getVisitColumn() != null)
                visitColumn = icsmetadata.getVisitColumn();

            if (icsmetadata.getDateColumn() != null)
                dateColumn = icsmetadata.getDateColumn();

            if (icsmetadata.getMatchColumns() != null)
            {
                for (int i = 0; i < icsmetadata.getMatchColumns().size(); i++)
                    matchColumn[i] = icsmetadata.getMatchColumns().get(i);
            }

            if (icsmetadata.getBackgroundFilter() != null && icsmetadata.getBackgroundFilter().size() > 0)
            {
                for (int i = 0; i < icsmetadata.getBackgroundFilter().size(); i++)
                {
                    FilterInfo filter = icsmetadata.getBackgroundFilter().get(i);
                    backgroundFilter[i] = new FilterInfo(filter.getField(), filter.getOp(), filter.getValue());
                }
            }
        }

        if (matchColumn[0] == null)
        {
            // default the form to include Run
            matchColumn[0] = new FieldKey(null, "Run");
        }
    }

    public void setFf_specimenIdColumn(String ff_specimenIdColumn)
    {
        this.ff_specimenIdColumn = ff_specimenIdColumn;
    }

    public void setFf_participantColumn(String ff_participantColumn)
    {
        this.ff_participantColumn = ff_participantColumn;
    }

    public void setFf_visitColumn(String ff_visitColumn)
    {
        this.ff_visitColumn = ff_visitColumn;
    }

    public void setFf_dateColumn(String ff_dateColumn)
    {
        this.ff_dateColumn = ff_dateColumn;
    }

    public void setFf_matchColumn(String[] ff_matchColumn)
    {
        this.ff_matchColumn = ff_matchColumn;
    }

    public void setFf_backgroundFilterField(String[] ff_backgroundFilterField)
    {
        this.ff_backgroundFilterField = ff_backgroundFilterField;
    }

    public void setFf_backgroundFilterOp(String[] ff_backgroundFilterOp)
    {
        this.ff_backgroundFilterOp = ff_backgroundFilterOp;
    }

    public void setFf_backgroundFilterValue(String[] ff_backgroundFilterValue)
    {
        this.ff_backgroundFilterValue = ff_backgroundFilterValue;
    }

    /** Get specimen ID FieldKey from form posted value. */
    public FieldKey getSpecimenIdColumn()
    {
        return ff_specimenIdColumn == null ? null : FieldKey.fromString(ff_specimenIdColumn);
    }

    /** Get participant FieldKey from form posted value. */
    public FieldKey getParticipantColumn()
    {
        return ff_participantColumn == null ? null : FieldKey.fromString(ff_participantColumn);
    }

    /** Get timepoint FieldKey from form posted value. */
    public FieldKey getVisitColumn()
    {
        return ff_visitColumn == null ? null : FieldKey.fromString(ff_visitColumn);
    }

    /** Get TimepointType from form posted value. */
    public FieldKey getDateColumn()
    {
        return ff_dateColumn == null ? null : FieldKey.fromString(ff_dateColumn);
    }

    /** Get match columns from form posted values. */
    public List<FieldKey> getMatchColumns()
    {
        List<FieldKey> matchColumns = new ArrayList<>(ff_matchColumn.length);
        for (String field : ff_matchColumn)
        {
            if (field != null)
                matchColumns.add(FieldKey.fromString(field));
        }
        return matchColumns;
    }

    /** Get background filters from form posted values. */
    public List<FilterInfo> getBackgroundFilters()
    {
        List<FilterInfo> filters = new ArrayList<>(ff_backgroundFilterField.length);
        if (ff_backgroundFilterField != null && ff_backgroundFilterOp != null)
        {
            for (int i = 0; i < ff_backgroundFilterField.length; i++)
            {
                String field = ff_backgroundFilterField[i];
                if (field == null)
                    continue;
                
                String op;
                if (ff_backgroundFilterOp.length < i || ff_backgroundFilterOp[i] == null)
                    op = CompareType.NONBLANK.getPreferredUrlKey();
                else
                    op = ff_backgroundFilterOp[i];

                String value;
                if (ff_backgroundFilterValue.length < i || ff_backgroundFilterValue[i] == null)
                    value = null;
                else
                    value = ff_backgroundFilterValue[i];

                FilterInfo filter = new FilterInfo(field, op, value);
                filters.add(filter);
            }
        }
        return filters;
    }

    public Map<FieldKey, String> getKeywordAndSampleFieldMap(boolean includeStatistics)
    {
        LinkedHashMap<FieldKey, String> ret = new LinkedHashMap<>();
        FlowSchema schema = new FlowSchema(getUser(), getContainer());
        TableInfo tableFCSFiles = schema.getTable(FlowTableType.FCSFiles.toString());

        ret.put(null, "");
        ret.put(new FieldKey(null, "Run"), "FCSAnalysis Run");

        FieldKey keyword = FieldKey.fromParts("FCSFile", "Keyword");
        ColumnInfo colKeyword = tableFCSFiles.getColumn("Keyword");
        TableInfo tableKeywords = colKeyword.getFk().getLookupTableInfo();

        for (ColumnInfo column : tableKeywords.getColumns())
        {
            if (column.isHidden())
                continue;
            ret.put(new FieldKey(keyword, column.getName()), "Keyword " + column.getLabel());
        }

        FieldKey sampleProperty = FieldKey.fromParts("FCSFile", "Sample");
        ExpSampleSet sampleSet = getProtocol().getSampleSet();
        if (sampleSet != null)
        {
            if (sampleSet.hasNameAsIdCol())
                ret.put(new FieldKey(sampleProperty, "Name"), "Sample Name");
            for (DomainProperty pd : sampleSet.getType().getProperties())
            {
                ret.put(new FieldKey(sampleProperty, pd.getName()), "Sample " + pd.getName());
            }
        }

        if (includeStatistics)
        {
            // ADD statistics too.
            // this is to filter for minimum count in background control
            // e.g. Statistic."S/Lv/L/3+/4+:Count" > 5000
            Collection<AttributeCache.StatisticEntry> stats = AttributeCache.STATS.byContainer(getContainer());
            FieldKey statisticProperty = FieldKey.fromParts("Statistic");
            for (AttributeCache.StatisticEntry stat : stats)
            {
                StatisticSpec spec = stat.getAttribute();
                if (spec.getStatistic() != StatisticSpec.STAT.Count)
                    continue;
                ret.put(new FieldKey(statisticProperty, spec.toString()), "Statistic " + spec.toString());
            }
        }

        return ret;
    }
}
