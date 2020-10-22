package org.labkey.elisa;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayService;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.security.LimitedUser;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.security.roles.SiteAdminRole;
import org.labkey.elisa.query.CurveFitDb;
import org.labkey.elisa.query.ElisaManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ElisaUpgradeCode implements UpgradeCode
{
    private static final Logger _log = Logger.getLogger(ElisaUpgradeCode.class);

    // Invoked by elisa-0.000-20.0000.sql
    @SuppressWarnings({"UnusedDeclaration"})
    @DeferredUpgrade
    public static void moveCurveFitData(final ModuleContext context)
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
                if (provider instanceof ElisaAssayProvider)
                {
                    _log.info("Attempting to move curve fit data for protocol: " + protocol.getName());

                    Domain runDomain = provider.getRunDomain(protocol);
                    DomainProperty curveFitProp = runDomain.getPropertyByName(ElisaAssayProvider.CURVE_FIT_PARAMETERS_PROPERTY);
                    DomainProperty rSquaredProp = runDomain.getPropertyByName(ElisaAssayProvider.CORRELATION_COEFFICIENT_PROPERTY);

                    if (curveFitProp != null && rSquaredProp != null)
                    {
                        try (DbScope.Transaction transaction = ExperimentService.get().getSchema().getScope().beginTransaction())
                        {
                            for (ExpRun run : protocol.getExpRuns())
                            {
                                Object curveFit = run.getProperty(curveFitProp);
                                Object rSquared = run.getProperty(rSquaredProp);
                                String[] params = curveFit.toString().split("&");

                                if (params.length == 2)
                                {
                                    JSONObject json = new JSONObject();
                                    json.put("slope", params[0]);
                                    json.put("intercept", params[1]);

                                    CurveFitDb curveFitDb = new CurveFitDb();
                                    curveFitDb.setRunId(run.getRowId());
                                    curveFitDb.setProtocolId(protocol.getRowId());
                                    curveFitDb.setPlateName(ManualImportHelper.PLACEHOLDER_PLATE_NAME);
                                    curveFitDb.setSpot(1);
                                    curveFitDb.setFitParameters(json.toString());
                                    if (rSquared instanceof Double)
                                        curveFitDb.setrSquared((Double) rSquared);

                                    ElisaManager.saveCurveFit(run.getContainer(), upgradeUser, curveFitDb);
                                }
                                else
                                    _log.warn("Curve fit parameters couldn't be parsed : " + curveFit);
                            }
                            // delete the old domain properties
                            curveFitProp.delete();
                            rSquaredProp.delete();
                            runDomain.save(upgradeUser);

                            transaction.commit();
                        }
                        catch (Exception e)
                        {
                            _log.warn("Failed moving curve fit data for protocol: " + protocol.getName());
                        }
                    }
                    else
                        _log.info("Unable to find fit and r-squared run properties for protocol : " + protocol.getName());
                }
            }
        }
    }
}
