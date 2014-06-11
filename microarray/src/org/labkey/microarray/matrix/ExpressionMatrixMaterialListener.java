package org.labkey.microarray.matrix;

import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.exp.ExperimentMaterialListener;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.microarray.query.MicroarrayUserSchema;

import java.util.List;

/**
 * User: jeckels
 * Date: 6/11/14
 */
public class ExpressionMatrixMaterialListener implements ExperimentMaterialListener
{
    @Override
    public void beforeDelete(List<? extends ExpMaterial> materials)
    {
        SqlExecutor sqlExecutor = new SqlExecutor(MicroarrayUserSchema.getSchema());
        for (ExpMaterial material : materials)
        {
            SQLFragment sql = new SQLFragment("DELETE FROM ");
            sql.append(MicroarrayUserSchema.getSchema().getTable("FeatureData"));
            sql.append(" WHERE SampleId = ?");
            sql.add(material.getRowId());
            sqlExecutor.execute(sql);
        }
    }
}
