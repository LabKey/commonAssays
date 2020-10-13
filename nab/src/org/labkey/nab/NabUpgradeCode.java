package org.labkey.nab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayService;
import org.labkey.api.assay.dilution.DilutionAssayRun;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.api.assay.nab.NabSpecimen;
import org.labkey.api.assay.plate.WellGroup;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.security.LimitedUser;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.security.roles.SiteAdminRole;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NabUpgradeCode implements UpgradeCode
{
    private static final Logger _log = LogManager.getLogger(NabUpgradeCode.class);

    // Invoked by nab-20.000-20.001.sql
    @SuppressWarnings({"UnusedDeclaration"})
    @DeferredUpgrade
    public static void populateFitParameters(final ModuleContext context)
    {
        if (!context.isNewInstall())
        {
            Set<ExpProtocol> protocols = new HashSet<>();
            for (Container container : ContainerManager.getAllChildren(ContainerManager.getRoot()))
            {
                if (container != null)
                    protocols.addAll(AssayService.get().getAssayProtocols(container));
            }

            User upgradeUser = new LimitedUser(UserManager.getGuestUser(), new int[0], Collections.singleton(RoleManager.getRole(SiteAdminRole.class)), false);
            for (ExpProtocol protocol : protocols)
            {
                AssayProvider provider = AssayService.get().getProvider(protocol);
                if (provider instanceof NabAssayProvider)
                {
                    _log.info("Attempting to populate fit parameters for protocol: " + protocol.getName());
                    DilutionDataHandler dataHandler = ((NabAssayProvider) provider).getDataHandler();
                    int counter = 0;

                    try (DbScope.Transaction transaction = ExperimentService.get().getSchema().getScope().beginTransaction())
                    {
                        for (ExpRun run : protocol.getExpRuns())
                        {
                            DilutionAssayRun assayRun = dataHandler.getAssayResults(run, upgradeUser);
                            for (DilutionAssayRun.SampleResult result : assayRun.getSampleResults())
                            {
                                DilutionSummary dilutionSummary = result.getDilutionSummary();
                                ExpMaterial sampleInput = assayRun.getMaterial(dilutionSummary.getFirstWellGroup());
                                if (sampleInput != null)
                                {
                                    NabSpecimen specimenRow = NabManager.get().getNabSpecimen(sampleInput.getLSID());
                                    if (specimenRow != null && specimenRow.getFitParameters() == null)
                                    {
                                        NabManager.get().ensureFitParameters(upgradeUser, specimenRow, assayRun, dilutionSummary);
                                        counter++;
                                    }
                                }
                                else
                                    _log.warn("Unable to find sample input for run: run " + run.getRowId() + ", wellgroup " + dilutionSummary.getFirstWellGroup().getName());
                            }
                        }

                        transaction.commit();

                        _log.info("Set NAb fit parameters for " + counter + " NabSpecimen rows.");
                    }
                    catch (Exception e)
                    {
                        _log.warn("Failed while populating fit parameters for protocol: " + protocol.getName());
                    }
                }
            }
        }
    }
}
