/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.microarray.matrix;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.CrosstabDimension;
import org.labkey.api.data.CrosstabMeasure;
import org.labkey.api.data.CrosstabMember;
import org.labkey.api.data.CrosstabSettings;
import org.labkey.api.data.CrosstabTable;
import org.labkey.api.data.CrosstabTableInfo;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.microarray.MicroarrayManager;
import org.labkey.microarray.query.MicroarrayUserSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExpressionMatrixProtocolSchema extends AssayProtocolSchema
{
    public static final String FEATURE_DATA_TABLE_NAME = "FeatureData";
    public static final String FEATURE_DATA_BY_SAMPLE_TABLE_NAME = "FeatureDataBySample";

    public ExpressionMatrixProtocolSchema(User user, Container container, @NotNull ExpressionMatrixAssayProvider provider, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, provider, protocol, targetStudy);
    }
    
    @Override
    public FilteredTable createDataTable(boolean includeCopiedToStudyColumns)
    {
        FeatureDataTable result = new FeatureDataTable(this);
        result.setName(AssayProtocolSchema.DATA_TABLE_NAME);
        return result;
    }

    @Override
    public TableInfo createTable(String name)
    {
        if (FEATURE_DATA_BY_SAMPLE_TABLE_NAME.equals(name))
        {
            return getFeatureDataBySampleTable();
        }
        
        return super.createTable(name);
    }

    public static TableInfo getTableInfoFeatureData()
    {
        return MicroarrayUserSchema.getSchema().getTable(FEATURE_DATA_TABLE_NAME);
    }

    public CrosstabTableInfo getFeatureDataBySampleTable()
    {
        TableInfo featureDataTableInfo = new FeatureDataTable(this);
        CrosstabSettings settings = new CrosstabSettings(featureDataTableInfo);
        CrosstabTable cti;
        CrosstabDimension rowDim = settings.getRowAxis().addDimension(FieldKey.fromParts("FeatureId"));
        CrosstabDimension colDim = settings.getColumnAxis().addDimension(FieldKey.fromParts("SampleId"));
        CrosstabMeasure valueMeasure = settings.addMeasure(FieldKey.fromParts("Value"), CrosstabMeasure.AggregateFunction.MIN, "Value");
        List<Map> distinctSampleIds;
        try
        {
            List<FieldKey> defaultCols = new ArrayList<>();
            ArrayList<CrosstabMember> members = new ArrayList<>();
            distinctSampleIds = MicroarrayManager.get().getDistinctSamples(getProtocol());

            for (Map sample : distinctSampleIds)
            {
                members.add(new CrosstabMember(sample.get("SampleId"), colDim, (String) sample.get("Name")));
            }

            defaultCols.add(FieldKey.fromParts(valueMeasure.getName()));
            defaultCols.add(rowDim.getFieldKey());
            defaultCols.add(colDim.getFieldKey());

            cti = new CrosstabTable(settings, members);
            cti.setDefaultVisibleColumns(defaultCols);
            cti.setTitle("Feature Data By Sample");
            
            return cti;
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public Set<String> getTableNames()
    {
        Set<String> result = super.getTableNames();
        result.add(FEATURE_DATA_BY_SAMPLE_TABLE_NAME);
        return result;
    }
}
