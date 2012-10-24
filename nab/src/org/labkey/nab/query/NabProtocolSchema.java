/*
 * Copyright (c) 2012 LabKey Corporation
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
package org.labkey.nab.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayDataLinkDisplayColumn;
import org.labkey.api.study.assay.AssayProtocolSchema;

/**
 * User: kevink
 * Date: 10/13/12
 */
public class NabProtocolSchema extends AssayProtocolSchema
{
    /*package*/ static final String DATA_ROW_TABLE_NAME = "Data";

    public NabProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, protocol, targetStudy);
    }

    @Override
    public NabRunDataTable createDataTable(boolean includeCopiedToStudyColumns)
    {
        NabRunDataTable table = new NabRunDataTable(this, getProtocol());

        if (includeCopiedToStudyColumns)
        {
            addCopiedToStudyColumns(table, true);
        }
        return table;
    }

    @Override
    public ExpRunTable createRunsTable()
    {
        final ExpRunTable runTable = super.createRunsTable();
        ColumnInfo nameColumn = runTable.getColumn(ExpRunTable.Column.Name);
        // NAb has two detail type views of a run - the filtered results/data grid, and the run details page that
        // shows the graph. Set the run's name to be a link to the grid instead of the default details page.
        nameColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new AssayDataLinkDisplayColumn(colInfo, runTable.getContainerFilter());
            }
        });
        return runTable;
    }
}
