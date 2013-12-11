/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.util.Pair;
import org.labkey.flow.data.ICSMetadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: kevink
 * Date: 3/31/13
 *
 * Select the metadata special columns from both FCSFile and OriginalFCSFile.
 *
 * See Issue 16945: flow specimen FK doesn't work for 'fake' FCS file wells created during FlowJo import
 */
public class FCSFileCoalescingColumn extends ExprColumn
{
    private boolean _relativeFromFCSFile;

    private Pair<FieldKey, FieldKey> _specimenIdFieldKeys;
    private Pair<FieldKey, FieldKey> _participantIdFieldKeys;
    private Pair<FieldKey, FieldKey> _visitFieldKeys;
    private Pair<FieldKey, FieldKey> _dateFieldKeys;
    private Pair<FieldKey, FieldKey> _targetStudyFieldKeys;

    public FCSFileCoalescingColumn(TableInfo parent, FieldKey key, JdbcType type, @Nullable ICSMetadata metadata, boolean relativeFromFCSFile)
    {
        super(parent, key, new SQLFragment(ExprColumn.STR_TABLE_ALIAS + (relativeFromFCSFile ? "$FCSFile" : "$FCSAnalysis") + "$z." + key.getName()), type);
        _relativeFromFCSFile = relativeFromFCSFile;

        if (metadata != null)
        {
            FieldKey specimenIdFieldKey = relativeFromFCSFile ? FlowSchema.removeParent(metadata.getSpecimenIdColumn(), FlowSchema.FCSFILE_NAME) : metadata.getSpecimenIdColumn();
            if (specimenIdFieldKey != null)
                _specimenIdFieldKeys = Pair.of(specimenIdFieldKey, FlowSchema.rewriteAsOriginalFCSFile(specimenIdFieldKey));

            FieldKey participantIdFieldKey = relativeFromFCSFile ? FlowSchema.removeParent(metadata.getParticipantColumn(), FlowSchema.FCSFILE_NAME) : metadata.getParticipantColumn();
            if (participantIdFieldKey != null)
                _participantIdFieldKeys = Pair.of(participantIdFieldKey, FlowSchema.rewriteAsOriginalFCSFile(participantIdFieldKey));

            FieldKey visitFieldKey = relativeFromFCSFile ? FlowSchema.removeParent(metadata.getVisitColumn(), FlowSchema.FCSFILE_NAME) : metadata.getVisitColumn();
            if (visitFieldKey != null)
                _visitFieldKeys = Pair.of(visitFieldKey, FlowSchema.rewriteAsOriginalFCSFile(visitFieldKey));

            FieldKey dateFieldKey = relativeFromFCSFile ? FlowSchema.removeParent(metadata.getDateColumn(), FlowSchema.FCSFILE_NAME) : metadata.getDateColumn();
            if (dateFieldKey != null)
                _dateFieldKeys = Pair.of(dateFieldKey, FlowSchema.rewriteAsOriginalFCSFile(dateFieldKey));
        }

        FieldKey targetStudyFieldKey = relativeFromFCSFile ? FieldKey.fromParts("Run", "TargetStudy") : FieldKey.fromParts("FCSFile", "Run", "TargetStudy");
        _targetStudyFieldKeys = Pair.of(targetStudyFieldKey, FlowSchema.rewriteAsOriginalFCSFile(targetStudyFieldKey));
    }

    @Override
    public void declareJoins(String parentAlias, Map<String, SQLFragment> map)
    {
        TableInfo parentTable = getParentTable();

        List<FieldKey> pkCols = new ArrayList<>();
        for (String pkCol : parentTable.getPkColumnNames())
            pkCols.add(FieldKey.fromParts(pkCol));

        List<FieldKey> coalesceFields = new ArrayList<>();
        Map<String, Pair<FieldKey, FieldKey>> pairs = new LinkedHashMap<>();
        if (_specimenIdFieldKeys != null)
        {
            coalesceFields.add(_specimenIdFieldKeys.first);
            coalesceFields.add(_specimenIdFieldKeys.second);
            pairs.put(FlowSchema.SPECIMENID_FIELDKEY.getName(), _specimenIdFieldKeys);
        }

        if (_participantIdFieldKeys != null)
        {
            coalesceFields.add(_participantIdFieldKeys.first);
            coalesceFields.add(_participantIdFieldKeys.second);
            pairs.put(FlowSchema.PARTICIPANTID_FIELDKEY.getName(), _participantIdFieldKeys);
        }

        if (_visitFieldKeys != null)
        {
            coalesceFields.add(_visitFieldKeys.first);
            coalesceFields.add(_visitFieldKeys.second);
            pairs.put(FlowSchema.VISITID_FIELDKEY.getName(), _visitFieldKeys);
        }

        if (_dateFieldKeys != null)
        {
            coalesceFields.add(_dateFieldKeys.first);
            coalesceFields.add(_dateFieldKeys.second);
            pairs.put(FlowSchema.DATE_FIELDKEY.getName(), _dateFieldKeys);
        }

        if (_targetStudyFieldKeys != null)
        {
            coalesceFields.add(_targetStudyFieldKeys.first);
            coalesceFields.add(_targetStudyFieldKeys.second);
            pairs.put(FlowSchema.TARGET_STUDY_FIELDKEY.getName(), _targetStudyFieldKeys);
        }

        List<FieldKey> fields = new ArrayList<>(pkCols.size() + coalesceFields.size());
        fields.addAll(pkCols);
        fields.addAll(coalesceFields);

        Map<FieldKey, ColumnInfo> columnMap = QueryService.get().getColumns(parentTable, fields);
        SQLFragment sub = QueryService.get().getSelectSQL(parentTable, columnMap.values(), null, null, Table.ALL_ROWS, Table.NO_OFFSET, false);

        SQLFragment coalesceFrag = new SQLFragment();
        coalesceFrag.append("SELECT\n");
        for (FieldKey pkCol : pkCols)
            coalesceFrag.append("  ").append(columnMap.get(pkCol).getAlias()).append("\n");

        for (Map.Entry<String, Pair<FieldKey, FieldKey>> entry : pairs.entrySet())
        {
            String name = entry.getKey();
            Pair<FieldKey, FieldKey> pair = entry.getValue();

            coalesceFrag.append(",\n");
            coalesceFrag.append("  COALESCE(\n");
            coalesceFrag.append(columnMap.get(pair.first).getAlias());
            coalesceFrag.append(", ");
            coalesceFrag.append(columnMap.get(pair.second).getAlias());
            coalesceFrag.append(") AS ").append(name);
        }
        coalesceFrag.append("\n");
        coalesceFrag.append("FROM (\n").append(sub).append(") y\n");

        SQLFragment frag = new SQLFragment();
        frag.append("\nLEFT OUTER JOIN (\n");
        frag.append(coalesceFrag);
        String name = parentAlias + (_relativeFromFCSFile ? "$FCSFile" : "$FCSAnalysis") + "$z";
        frag.append(") AS ").append(name).append(" ON ");
        String and = "";
        for (FieldKey pkCol : pkCols)
        {
            frag.append(and);
            frag.append(parentAlias).append(".").append(pkCol);
            frag.append(" = ");
            frag.append(name).append(".").append(columnMap.get(pkCol).getAlias());
            and = " AND ";
        }

        map.put(name, frag);
    }

}
