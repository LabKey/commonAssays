/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
package org.labkey.flow.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilterable;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.query.RunListQueryView;
import org.labkey.api.view.ViewContext;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;
import org.springframework.validation.BindException;

/**
 * User: jeckels
 * Date: 10/19/12
 */
public class FlowProtocolSchema extends AssayProtocolSchema
{
    public FlowProtocolSchema(User user, Container container, @NotNull FlowAssayProvider provider, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, provider, protocol, targetStudy);
    }

    @NotNull
    @Override
    public FlowAssayProvider getProvider()
    {
        return (FlowAssayProvider)super.getProvider();
    }

    @Override
    public ExpRunTable createRunsTable()
    {
        FlowSchema flowSchema = new FlowSchema(getUser(), getContainer());
        //assert protocol == flowSchema.getProtocol();
        return (ExpRunTable)flowSchema.getTable(FlowTableType.Runs);
    }

    @Override
    public ContainerFilterable createDataTable(boolean includeCopiedToStudyColumns)
    {
        FlowSchema flowSchema = new FlowSchema(getUser(), getContainer());
        //assert protocol == flowSchema.getProtocol();
        return flowSchema.createFCSAnalysisTable(FlowTableType.FCSAnalyses.name(), FlowDataType.FCSAnalysis, includeCopiedToStudyColumns);
    }

    @Nullable
    @Override
    protected RunListQueryView createRunsQueryView(ViewContext context, QuerySettings settings, BindException errors)
    {
        // UNDONE: Create query view over flow.Runs
        return null;
    }
}
