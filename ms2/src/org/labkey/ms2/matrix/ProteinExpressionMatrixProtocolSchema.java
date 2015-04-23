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

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ProteinExpressionMatrixProtocolSchema extends AbstractMatrixProtocolSchema
{
    public static final String PROTEIN_SEQ_DATA_TABLE_NAME = "ProteinSequenceData";
    public static final String PROTEIN_SEQ_DATA_BY_SAMPLE_TABLE_NAME = "ProteinSequenceDataBySample";
    private static final String SEQUENCE_ID = "SequenceId";
    private static final String SAMPLE_ID = "SampleId"; //TODO: change to ConditionId as per client data?
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
        return super.createTable(name, SEQUENCE_ID, SAMPLE_ID, VALUE_MEASURE_ID, TITLE); //TODO: Looks a bit funny, modify?
    }

    @Override
    public List<Map> getDistinctSampleIds()
    {
        List<Map> distinctSampleIds = null;
        try
        {
            distinctSampleIds = MS2Manager.getExpressionDataDistinctSamples(getProtocol());
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
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

