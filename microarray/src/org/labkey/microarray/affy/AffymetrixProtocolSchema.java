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
package org.labkey.microarray.affy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilterable;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayResultTable;
import org.labkey.microarray.view.SampleDisplayColumn;

public class AffymetrixProtocolSchema extends AssayProtocolSchema
{
    AffymetrixProtocolSchema(User user, Container container, @NotNull AffymetrixAssayProvider provider, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, provider, protocol, targetStudy);
    }

    @Nullable
    @Override
    public ContainerFilterable createDataTable(boolean includeCopiedToStudyColumns)
    {
        return new AssayResultTable(this, includeCopiedToStudyColumns);
    }

    @Nullable
    @Override
    public final ContainerFilterable createDataTable()
    {
        ContainerFilterable table = super.createDataTable();

        if (null != table)
        {
            ColumnInfo columnInfo = table.getColumn(AffymetrixAssayProvider.SAMPLE_NAME_COLUMN);
            if (columnInfo != null)
            {
                columnInfo.setDisplayColumnFactory(new DisplayColumnFactory()
                {
                    @Override
                    public DisplayColumn createRenderer(ColumnInfo colInfo)
                    {
                        return new SampleDisplayColumn(colInfo);
                    }
                });
            }
        }

        return table;
    }

}
