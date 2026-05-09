package org.labkey.signaldata;

import org.apache.logging.log4j.Logger;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayService;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.security.User;
import org.labkey.api.util.logging.LogHelper;

public class SignalDataUpgradeCode implements UpgradeCode
{
    private static final Logger LOG = LogHelper.getLogger(SignalDataUpgradeCode.class, "SignalData upgrade code");

    /**
     * Called from signaldata-25.000-25.001.sql
     * Updates SignalData assay protocols to make the result domain DataFile field not required.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public static void updateDataFileField(ModuleContext ctx) throws Exception
    {
        Module module = ModuleLoader.getInstance().getModule(SignalDataModule.NAME);
        if (module != null)
        {
            for (Container c : ContainerManager.getAllChildrenWithModule(ContainerManager.getRoot(), module))
            {
                for (ExpProtocol protocol : ExperimentService.get().getExpProtocols(c))
                {
                    AssayProvider provider = AssayService.get().getProvider(protocol);
                    if (provider != null && provider.getName().equalsIgnoreCase("Signal Data"))
                    {
                        Domain domain = provider.getResultsDomain(protocol, true);
                        DomainProperty dataFile = domain.getPropertyByName("DataFile");
                        if (dataFile != null && dataFile.isRequired())
                        {
                            LOG.info("Updating Signal Data assay in folder '{}'", c.getPath());
                            dataFile.setRequired(false);
                            domain.save(User.getAdminServiceUser());
                        }
                    }
                }
            }
        }
    }
}
