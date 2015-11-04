/*
 * Copyright (c) 2013-2015 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.matrix.AbstractMatrixProtocolSchema;
import org.labkey.microarray.MicroarrayManager;
import org.labkey.microarray.query.MicroarrayUserSchema;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ExpressionMatrixProtocolSchema extends AbstractMatrixProtocolSchema
{
    public static final String FEATURE_DATA_TABLE_NAME = "FeatureData";
    public static final String FEATURE_DATA_BY_SAMPLE_TABLE_NAME = "FeatureDataBySample";
    private static final String FEATURE_ID = "FeatureId";
    private static final String SAMPLE_ID = "SampleId";
    private static final String VALUE_MEASURE_ID = "Value";
    private static final String TITLE = "Feature Data By Sample";

    public ExpressionMatrixProtocolSchema(User user, Container container, @NotNull ExpressionMatrixAssayProvider provider, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, provider, protocol, targetStudy, FEATURE_DATA_BY_SAMPLE_TABLE_NAME, FEATURE_DATA_TABLE_NAME);
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
        return super.createTable(name, FEATURE_ID, SAMPLE_ID, VALUE_MEASURE_ID, TITLE); //TODO: Looks a bit funny, modify?
    }

    @Override
    public List<Map> getDistinctSampleIds()
    {
        List<Map> distinctSampleIds = null;
        try
        {
            distinctSampleIds = MicroarrayManager.get().getDistinctSamples(getProtocol());
        }
        catch (SQLException e)
        {
            // NEED COMMENT - why doesn't this rethrow?
            Logger.getLogger(ExpressionMatrixProtocolSchema.class).warn(e);
        }
        return distinctSampleIds;
    }

    @Override
    public TableInfo getDataTableInfo()
    {
        return new FeatureDataTable(this);
    }

    public static TableInfo getTableInfoFeatureData()
    {
        return MicroarrayUserSchema.getSchema().getTable(FEATURE_DATA_TABLE_NAME);
    }

}
