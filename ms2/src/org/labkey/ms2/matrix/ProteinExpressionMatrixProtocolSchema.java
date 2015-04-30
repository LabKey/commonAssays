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
package org.labkey.ms2.matrix;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.matrix.AbstractMatrixProtocolSchema;
import org.labkey.ms2.MS2Manager;

import java.util.List;
import java.util.Map;

public class ProteinExpressionMatrixProtocolSchema extends AbstractMatrixProtocolSchema
{
    public static final String PROTEIN_SEQ_DATA_TABLE_NAME = "ExpressionData";
    public static final String PROTEIN_SEQ_DATA_BY_SAMPLE_TABLE_NAME = "ProteinSequenceDataBySample";
    private static final String SEQUENCE_ID = "SeqId";
    private static final String SAMPLE_ID = "SampleId";
    private static final String VALUE_MEASURE_ID = "Value";
    private static final String TITLE = "Protein Sequence Data By Sample";

    public ProteinExpressionMatrixProtocolSchema(User user, Container container, @NotNull ProteinExpressionMatrixAssayProvider provider, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, provider, protocol, targetStudy, PROTEIN_SEQ_DATA_BY_SAMPLE_TABLE_NAME, PROTEIN_SEQ_DATA_TABLE_NAME);
    }

    @Override
    public FilteredTable createDataTable(boolean includeCopiedToStudyColumns)
    {
        ProteinSequenceDataTable result = new ProteinSequenceDataTable(this);
        result.setName(AssayProtocolSchema.DATA_TABLE_NAME);
        return result;
    }

    @Override
    public TableInfo createTable(String name)
    {
        return super.createTable(name, SEQUENCE_ID, SAMPLE_ID, VALUE_MEASURE_ID, TITLE);
    }

    @Override
    public List<Map> getDistinctSampleIds()
    {
        List<Map> distinctSampleIds = null;
        distinctSampleIds = MS2Manager.getExpressionDataDistinctSamples(getProtocol());

        return distinctSampleIds;
    }

    @Override
    public TableInfo getDataTableInfo()
    {
        return new ProteinSequenceDataTable(this);
    }

    public static TableInfo getTableInfoSequenceData()
    {
        return MS2Manager.getSchema().getTable(PROTEIN_SEQ_DATA_TABLE_NAME);
    }

}

