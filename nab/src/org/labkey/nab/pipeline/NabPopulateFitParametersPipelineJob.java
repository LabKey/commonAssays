package org.labkey.nab.pipeline;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayService;
import org.labkey.api.assay.dilution.DilutionAssayRun;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.api.assay.nab.NabSpecimen;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbScope;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.security.LimitedUser;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.security.roles.SiteAdminRole;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabManager;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NabPopulateFitParametersPipelineJob extends PipelineJob
{
    /** For JSON serialization/deserialzation round-tripping
     * @noinspection unused*/
    protected NabPopulateFitParametersPipelineJob()
    {}

    public NabPopulateFitParametersPipelineJob(ViewBackgroundInfo info, @NotNull PipeRoot root)
    {
        super(null, info, root);
        setLogFile(new File(root.getRootPath(), FileUtil.makeFileNameWithTimestamp("NAbPopulateFitParameters", "log")));
    }

    @Override
    public String getDescription()
    {
        return "Iterate through all NAb protocols to set the NAbSpecimen FitParameters for any run's where it has not already been set.";
    }

    @Override
    public URLHelper getStatusHref()
    {
        return null;
    }

    @Override
    public void run()
    {
        User upgradeUser = this.getUpgradeUser();
        setStatus(TaskStatus.running);

        for (ExpProtocol protocol : getAllNAbProtocols())
        {
            info("Populating fit parameters for protocol: " + protocol.getName());
            AssayProvider provider = AssayService.get().getProvider(protocol);
            DilutionDataHandler dataHandler = ((NabAssayProvider) provider).getDataHandler();
            int updateCounter = 0, completeCounter = 0, missingCounter = 0;

            try (DbScope.Transaction transaction = ExperimentService.get().getSchema().getScope().beginTransaction())
            {
                for (ExpRun run : protocol.getExpRuns())
                {
                    try
                    {
                        DilutionAssayRun assayRun = dataHandler.getAssayResults(run, upgradeUser);
                        if (assayRun != null)
                        {
                            for (DilutionAssayRun.SampleResult result : assayRun.getSampleResults())
                            {
                                DilutionSummary dilutionSummary = result.getDilutionSummary();
                                ExpMaterial sampleInput = assayRun.getMaterial(dilutionSummary.getFirstWellGroup());
                                if (sampleInput != null)
                                {
                                    NabSpecimen specimenRow = NabManager.get().getNabSpecimen(sampleInput.getLSID());
                                    if (specimenRow != null)
                                    {
                                        if (specimenRow.getFitParameters() == null)
                                        {
                                            NabManager.get().ensureFitParameters(upgradeUser, specimenRow, assayRun, dilutionSummary);
                                            updateCounter++;
                                        }
                                        else
                                            completeCounter++;
                                    }
                                    else
                                        missingCounter++;
                                }
                                else
                                {
                                    warn("Unable to find sample input for run: run " + run.getRowId() + ", wellgroup " + dilutionSummary.getFirstWellGroup().getName());
                                    missingCounter++;
                                }
                            }
                        }
                        else
                            warn("Unable to find dilution results for run: " + run.getRowId());
                    }
                    catch (ExperimentException e)
                    {
                        warn("Unable to find dilution results for run: " + run.getRowId());
                        warn(e.getMessage());
                    }
                }

                transaction.commit();

                info("NAbSpecimen row fit parameters updated: " + updateCounter);
                if (completeCounter > 0)
                    info("NAbSpecimen row fit parameters already set: " + completeCounter);
                if (missingCounter > 0)
                    info("NAbSpecimen rows missing: " + missingCounter);
            }
            catch (Exception e)
            {
                error("Failed while populating fit parameters for protocol: " + protocol.getName(), e);
            }
        }

        setStatus(TaskStatus.complete);
    }

    private User getUpgradeUser()
    {
        return new LimitedUser(UserManager.getGuestUser(), new int[0], Collections.singleton(RoleManager.getRole(SiteAdminRole.class)), false);
    }

    private Set<ExpProtocol> getAllNAbProtocols()
    {
        Set<ExpProtocol> protocols = new HashSet<>();
        for (Container container : ContainerManager.getAllChildren(ContainerManager.getRoot()))
        {
            if (container != null)
            {
                for (ExpProtocol protocol : AssayService.get().getAssayProtocols(container))
                {
                    AssayProvider provider = AssayService.get().getProvider(protocol);
                    if (provider instanceof NabAssayProvider)
                        protocols.add(protocol);
                }
            }
        }
        return protocols;
    }
}
