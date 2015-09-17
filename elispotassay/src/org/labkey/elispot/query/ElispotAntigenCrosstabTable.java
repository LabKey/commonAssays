/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.elispot.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.AggregateColumnInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.CrosstabDimension;
import org.labkey.api.data.CrosstabMeasure;
import org.labkey.api.data.CrosstabMember;
import org.labkey.api.data.CrosstabSettings;
import org.labkey.api.data.CrosstabTable;
import org.labkey.api.data.Sort;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.elispot.ElispotAssayProvider;
import org.labkey.elispot.ElispotDataHandler;
import org.labkey.elispot.ElispotManager;
import org.labkey.elispot.ElispotProtocolSchema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by davebradlee on 3/23/15.
 *
 */
public class ElispotAntigenCrosstabTable extends CrosstabTable
{
    private final Set<String> _nonBasePropertyNames;

    public static ElispotAntigenCrosstabTable create(ElispotRunAntigenTable elispotRunAntigenTable, ExpProtocol protocol, ElispotProtocolSchema protocolSchema)
    {
        CrosstabSettings crosstabSettings = new CrosstabSettings(elispotRunAntigenTable);
        crosstabSettings.getRowAxis().addDimension(FieldKey.fromParts("Run", "Container"));
        crosstabSettings.getRowAxis().addDimension(FieldKey.fromString("Run"));

        crosstabSettings.getRowAxis().addDimension(FieldKey.fromString("RunId"));
        crosstabSettings.getRowAxis().addDimension(FieldKey.fromString("WellgroupName"));
        crosstabSettings.getRowAxis().addDimension(FieldKey.fromString("SpecimenLsid"));

        FieldKey ptidFieldKey = FieldKey.fromParts("SpecimenLsid", "Property", "ParticipantId");
        if (!QueryService.get().getColumns(elispotRunAntigenTable, Arrays.asList(ptidFieldKey)).isEmpty())
            crosstabSettings.getRowAxis().addDimension(ptidFieldKey);

        FieldKey analyteFieldKey = FieldKey.fromParts(ElispotDataHandler.ANALYTE_PROPERTY_NAME);
        if (null != elispotRunAntigenTable.getColumn(analyteFieldKey))
            crosstabSettings.getRowAxis().addDimension(analyteFieldKey);
        FieldKey cytokineFieldKey = FieldKey.fromParts(ElispotDataHandler.CYTOKINE_PROPERTY_NAME);
        if (null != elispotRunAntigenTable.getColumn(cytokineFieldKey))
            crosstabSettings.getRowAxis().addDimension(cytokineFieldKey);

        CrosstabDimension colDim = crosstabSettings.getColumnAxis().addDimension(FieldKey.fromString("AntigenHeading"));

        crosstabSettings.addMeasure(FieldKey.fromParts("Mean"), CrosstabMeasure.AggregateFunction.AVG, "Mean");
        crosstabSettings.addMeasure(FieldKey.fromParts("Median"), CrosstabMeasure.AggregateFunction.AVG, "Median");

        ArrayList<CrosstabMember> members = new ArrayList<>();
        for (Map.Entry<String, Set<Integer>> antigenHeadingEntry :
                ElispotManager.get().getAntigenHeadings(elispotRunAntigenTable.getContainer(), elispotRunAntigenTable).entrySet())
        {
            String antigenHeading = antigenHeadingEntry.getKey();
            if (null != antigenHeading)
            {
                members.add(new ElispotAntigenCrosstabMember(antigenHeading, colDim, antigenHeading, antigenHeadingEntry.getValue()));
            }
            else
                throw new IllegalStateException("Expected non-null AntigenHeading");
        }

        Set<String> nonBasePropertyNames = new HashSet<>();
        ElispotAssayProvider provider = (ElispotAssayProvider)AssayService.get().getProvider(protocol);
        assert null != provider;
        assert null != provider.getAntigenWellGroupDomain(protocol);

        // add caption over the antigen well groups
        crosstabSettings.getColumnAxis().setCaption("Spot count per million cells");

        for (DomainProperty property : provider.getAntigenWellGroupDomain(protocol).getNonBaseProperties())
        {
            nonBasePropertyNames.add(property.getName());
            crosstabSettings.addMeasure(FieldKey.fromString(property.getName()), CrosstabMeasure.AggregateFunction.GROUP_CONCAT, property.getName());
        }

        return new ElispotAntigenCrosstabTable(crosstabSettings, members, nonBasePropertyNames);
    }
    public ElispotAntigenCrosstabTable(CrosstabSettings crosstabSettings, ArrayList<CrosstabMember> members, Set<String> nonBasePropertyNames)
    {
        super(crosstabSettings, members);
        _nonBasePropertyNames = nonBasePropertyNames;
        getColumn("InstanceCount").setHidden(true);
        getColumn("Run").setHidden(true);
        getColumn("SpecimenLsid").setHidden(true);
        setTitle("AntigenStats");
    }

    @Override
    public ContainerFilter getContainerFilter()
    {
        return getGroupTable().getSourceTable().getContainerFilter();
    }

    @Override
    public List<FieldKey> getDefaultVisibleColumns()
    {
        List<FieldKey> fieldKeys = new ArrayList<>();
        for (FieldKey fieldKey : super.getDefaultVisibleColumns())
            if (0 != fieldKey.compareTo(FieldKey.fromString("InstanceCount")) &&
                0 != fieldKey.compareTo(FieldKey.fromString("Run")) &&
                0 != fieldKey.compareTo(FieldKey.fromString("SpecimenLsid")) &&
                0 != fieldKey.compareTo(FieldKey.fromString("Run_fs_folder")))

            {
                boolean addName = true;
                for (String name : _nonBasePropertyNames)
                    if (fieldKey.getName().toLowerCase().endsWith(name.toLowerCase()))
                    {
                        addName = false;
                        break;
                    }

                if (addName)
                    fieldKeys.add(fieldKey);
            }
        return fieldKeys;
    }

    public Sort getDefaultSort()
    {
        return new Sort("+" + "RunId" + ",+" + "WellgroupName");
    }

    @Override
    protected ColumnInfo createMemberMeasureCol(@Nullable CrosstabMember member, CrosstabMeasure measure)
    {
        AggregateColumnInfo column = (AggregateColumnInfo)super.createMemberMeasureCol(member, measure);
        column.setCrosstabColumnDimension(measure.getSourceColumn().getFieldKey());
        return column;
    }

    public static class ElispotAntigenCrosstabMember extends CrosstabMember
    {
        private final Set<Integer> _runIds;
        public ElispotAntigenCrosstabMember(@Nullable Object value, @NotNull CrosstabDimension dimension,
                                            @Nullable String caption, Set<Integer> runIds)
        {
            super(value, dimension, caption);
            _runIds = runIds;
        }

        public Set<Integer> getRunIds()
        {
            return _runIds;
        }
    }
}
