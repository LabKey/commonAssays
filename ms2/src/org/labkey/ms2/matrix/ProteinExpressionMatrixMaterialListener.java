package org.labkey.ms2.matrix;

import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.exp.ExperimentMaterialListener;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.ms2.MS2Manager;

import java.util.List;

public class ProteinExpressionMatrixMaterialListener implements ExperimentMaterialListener
{
    @Override
    public void beforeDelete(List<? extends ExpMaterial> materials)
    {
        SqlExecutor sqlExecutor = new SqlExecutor(MS2Manager.getSchema());
        for (ExpMaterial material : materials)
        {
            SQLFragment sql = new SQLFragment("DELETE FROM ");
            sql.append(MS2Manager.getSchema().getTable(ProteinExpressionMatrixProtocolSchema.PROTEIN_SEQ_DATA_TABLE_NAME));
            sql.append(" WHERE SampleId = ?");
            sql.add(material.getRowId());
            sqlExecutor.execute(sql);
        }
    }
}
