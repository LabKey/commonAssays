package org.labkey.viability;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.viability.ViabilityService;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProvider;

/**
 * User: kevink
 * Date: 6/23/14
 */
public class ViabilityServiceImpl implements ViabilityService
{
    private static final ViabilityServiceImpl INSTANCE = new ViabilityServiceImpl();

    public static final ViabilityService get()
    {
        return INSTANCE;
    }

    private ViabilityServiceImpl() { }

    @Override
    public void updateSpecimenAggregates(User user, Container c, AssayProvider provider, ExpProtocol protocol, @Nullable ExpRun run)
    {
        ViabilityManager.updateSpecimenAggregates(user, c, provider, protocol, run);
    }
}
