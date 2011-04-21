/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

package org.labkey.ms2.protein.query;

import org.labkey.api.data.*;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QuerySchema;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.CustomAnnotationSet;

import java.sql.Types;
import java.util.Map;

/**
 * User: jeckels
* Date: Apr 3, 2007
*/
public class CustomAnnotationSetsTable extends VirtualTable
{
    private final Container _container;
    private final QuerySchema _schema;

    public CustomAnnotationSetsTable(TableInfo parentTable, QuerySchema schema, Container container)
    {
        super(ProteinManager.getSchema());
        _container = container;
        _schema = schema;
        Map<String, CustomAnnotationSet> annotationSets = ProteinManager.getCustomAnnotationSets(_container, true);
        SQLFragment sqlFragment = new SQLFragment("SELECT 1");

        if (annotationSets.isEmpty())
        {
            ExprColumn noneAvailableColumn = new ExprColumn(this, "NoneAvailable", sqlFragment, JdbcType.INTEGER);
            noneAvailableColumn.setIsUnselectable(true);
            addColumn(noneAvailableColumn);
        }
        else
        {
            for (final CustomAnnotationSet annotationSet : annotationSets.values())
            {
                ExprColumn setColumn = new ExprColumn(parentTable, annotationSet.getName(), sqlFragment, JdbcType.INTEGER);
                addColumn(setColumn);
                setColumn.setFk(new LookupForeignKey("CustomAnnotationSetId", "CustomAnnotationSetId")
                {
                    public TableInfo getLookupTableInfo()
                    {
                        return new CustomAnnotationTable(annotationSet, _schema);
                    }
                });
            }
        }
    }
}
