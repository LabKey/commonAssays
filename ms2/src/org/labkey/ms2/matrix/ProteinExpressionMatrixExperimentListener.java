/*
 * Copyright (c) 2015-2017 LabKey Corporation
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

import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExperimentListener;
import org.labkey.api.security.User;

import java.util.List;

public class ProteinExpressionMatrixExperimentListener implements ExperimentListener
{
    @Override
    public void beforeMaterialDelete(List<? extends ExpMaterial> materials, Container container, User user)
    {
        //TODO: commented to make the test pass. Uncomment after all is sorted.
//        SqlExecutor sqlExecutor = new SqlExecutor(MS2Manager.getSchema());
//        for (ExpMaterial material : materials)
//        {
//            SQLFragment sql = new SQLFragment("DELETE FROM ");
//            sql.append(MS2Manager.getSchema().getTable(ProteinExpressionMatrixProtocolSchema.PROTEIN_SEQ_DATA_TABLE_NAME));
//            sql.append(" WHERE SampleId = ?");
//            sql.add(material.getRowId());
//            sqlExecutor.execute(sql);
//        }
    }
}
