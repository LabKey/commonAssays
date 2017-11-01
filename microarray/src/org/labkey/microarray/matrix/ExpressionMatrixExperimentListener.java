/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExperimentListener;
import org.labkey.api.security.User;
import org.labkey.microarray.query.MicroarrayUserSchema;

import java.util.List;

/**
 * User: jeckels
 * Date: 6/11/14
 */
public class ExpressionMatrixExperimentListener implements ExperimentListener
{
    @Override
    public void beforeMaterialDelete(List<? extends ExpMaterial> materials, Container container, User user)
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
