/*
 * Copyright (c) 2014 LabKey Corporation
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
package org.labkey.viability;

import org.labkey.api.data.Container;
import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleUpgrader;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssaySaveHandler;
import org.labkey.api.study.assay.AssayService;

/**
 * User: kevink
 * Date: 5/18/14
 */
public class ViabilityUpgradeCode implements UpgradeCode
{
    /**
     * Called from viability-14.10-14.11 upgrade script
     */
    @DeferredUpgrade
    public void updateViabilitySpecimenAggregates(final ModuleContext context)
    {
        if (context.isNewInstall())
            return;

        ModuleUpgrader.getLogger().info("Upgrading viability specimen aggregates");
        _updateViabilitySpecimenAggregates(context);
        ModuleUpgrader.getLogger().info("Finished viability specimen aggregates");
    }

    private void _updateViabilitySpecimenAggregates(ModuleContext context)
    {
        // get all the viability assay instances
        for (ExpProtocol protocol : ExperimentService.get().getAllExpProtocols())
        {
            AssayProvider provider = AssayService.get().getProvider(protocol);
            if (provider instanceof ViabilityAssayProvider)
            {
                Container c = protocol.getContainer();

                // Change 'SampleNum' property from varchar to int type -- viability.results.samplenum is already an int so no conversion errors should occur.
                Domain resultsDomain = provider.getResultsDomain(protocol);
                DomainProperty dp = resultsDomain.getPropertyByName(ViabilityAssayProvider.SAMPLE_NUM_PROPERTY_NAME);
                if (dp != null && dp.getPropertyDescriptor() != null)
                {
                    PropertyDescriptor pd = dp.getPropertyDescriptor();
                    if (pd.getJdbcType() != null && pd.getJdbcType().isText())
                    {
                        pd.setJdbcType(JdbcType.INTEGER, 0);
                        OntologyManager.updatePropertyDescriptor(pd);
                    }
                }

                // Update all specimen aggregates for this assay
                ViabilityManager.updateSpecimenAggregates(context.getUpgradeUser(), c, protocol, null);
            }
        }
    }

}
