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

import org.apache.log4j.Logger;
import org.labkey.api.data.BaseSelector;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.api.study.SpecimenChangeListener;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: kevink
 * Date: 6/29/14
 */
public class ViabilitySpecimenChangeListener implements SpecimenChangeListener
{
    @Override
    public void specimensChanged(Container c, User user, Logger logger)
    {
        // Assuming the viability.results.targetstudy column is uptodate,
        // we can track backwards to determine which viability assays need updating.
        TableInfo resultsTable = ViabilitySchema.getTableInfoResults();
        SQLFragment frag = new SQLFragment();
        frag.append("SELECT DISTINCT protocolid FROM ").append(resultsTable, "r");
        frag.append(" WHERE targetStudy = ?").add(c.getId());
        SqlExecutor executor = new SqlExecutor(resultsTable.getSchema());

        final List<Integer> protocolIds = new ArrayList<>();
        executor.executeWithResults(frag, new BaseSelector.ResultSetHandler<Object>()
        {
            @Override
            public Object handle(ResultSet rs, Connection conn) throws SQLException
            {
                while (rs.next())
                {
                    int protocolId = rs.getInt("protocolid");
                    protocolIds.add(protocolId);
                }
                return null;
            }
        });

        // Update aggregates in each protocol
        for (Integer protocolId : protocolIds)
        {
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(protocolId);
            if (protocol == null)
            {
                logger.warn("No assay protocol found for id '" + protocolId + "'");
                continue;
            }

            AssayProvider provider = AssayService.get().getProvider(protocol);
            if (provider == null)
            {
                logger.warn("No assay provider found for protocol '" + protocol.getName() + "");
                continue;
            }

            if (!(provider instanceof ViabilityAssayProvider))
            {
                logger.warn("Expected viability assay provider for assay '" + provider.getName() + "'");
                continue;
            }

            Container protocolContainer = protocol.getContainer();
            logger.info("Updating specimens aggregates for viability assay '" + provider.getName() + "', container='" + protocolContainer + "'");
            ViabilityManager.updateSpecimenAggregates(user, protocolContainer, provider, protocol, null);
        }
    }
}
