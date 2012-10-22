package org.labkey.flow.data;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilterable;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;

/**
 * User: jeckels
 * Date: 10/19/12
 */
public class FlowProtocolSchema extends AssayProtocolSchema
{
    public FlowProtocolSchema(User user, Container container, ExpProtocol protocol, Container targetStudy)
    {
        super(user, container, protocol, targetStudy);
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
}
