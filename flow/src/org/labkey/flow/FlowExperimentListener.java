package org.labkey.flow;

import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExperimentListener;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.flow.persist.FlowManager;

import java.util.List;

public class FlowExperimentListener implements ExperimentListener
{
    @Override
    public void beforeMaterialDelete(List<? extends ExpMaterial> materials, Container container, User user)
    {
        DbScope.Transaction tx = ExperimentService.get().getSchema().getScope().getCurrentTransaction();
        tx.addCommitTask(() -> FlowManager.get().flowObjectModified(), DbScope.CommitTaskOption.POSTCOMMIT);
    }

}
