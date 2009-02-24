/*
 * Copyright (c) 2009 LabKey Corporation
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
package org.labkey.luminex;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Table;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.UnexpectedException;
import org.labkey.common.util.Pair;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * User: jgarms
 * Date: Feb 20, 2009
 */
public class LuminexProtocolUpgrader
{
    private LuminexProtocolUpgrader() {}

    public static void upgrade()
    {
        Table.TableResultSet rs = null;
        DbScope scope = LuminexSchema.getSchema().getScope();
        boolean transactionOwner = !scope.isTransactionActive();
        try
        {
            if (transactionOwner)
                scope.beginTransaction();

            String lsidAuthority = AppProps.getInstance().getDefaultLsidAuthority();
            String lsidPrefix = "urn:lsid:" + lsidAuthority + ":LuminexDataRow:%"; // used for a LIKE

            String datasetSql = "SELECT sd.datasetid, sd.container, max(CAST (sd._key AS INT)) AS lumkey\n" +
                    "FROM study.studydata sd\n" +
                    "WHERE\n" +
                    "sd._key IS NOT NULL\n" +
                    "AND sd._key <> ''\n" +
                    "AND sd.sourcelsid LIKE ?\n" +
                    "GROUP BY datasetid, container";

            rs = Table.executeQuery(LuminexSchema.getSchema(), datasetSql, new Object[] {lsidPrefix});
            // container id to dataset id and luminex key
            MultiMap<String, Pair<Integer,Integer>> container2datasets = new MultiHashMap<String,Pair<Integer,Integer>>();
            while (rs.next())
            {
                Map<String,Object> rowMap = rs.getRowMap();
                Pair<Integer,Integer> datasetAndLumKey =
                        new Pair<Integer,Integer>((Integer)rowMap.get("datasetid"), (Integer)rowMap.get("lumkey"));

                container2datasets.put((String)rowMap.get("container"), datasetAndLumKey);
            }
            rs.close();

            // Need to loop over all the containers, as we have a constraint that the same protocol can
            // only have one entry per container.
            for (Map.Entry<String, Collection<Pair<Integer,Integer>>> entry : container2datasets.entrySet())
            {
                String container = entry.getKey();
                Map<Integer,Integer> protocol2dataset = new HashMap<Integer,Integer>(); // only one per container
                for (Pair<Integer,Integer> datasetAndLumKey : entry.getValue())
                {
                    Integer lumKey = datasetAndLumKey.getValue();
                    String protocolSql = "SELECT p.rowid\n" +
                            "FROM exp.experimentrun er, exp.protocol p, luminex.datarow lum, exp.data ed\n" +
                            "WHERE\n" +
                            "lum.rowid = ?\n" +
                            "AND lum.dataid = ed.rowid\n" +
                            "AND ed.runid = er.rowid\n" +
                            "AND er.protocollsid = p.lsid";
                    rs = Table.executeQuery(LuminexSchema.getSchema(), protocolSql, new Object[] {lumKey});
                    boolean foundRowId = rs.next();
                    Integer protocolId = foundRowId ? (Integer)rs.getRowMap().get("rowid") : null;
                    rs.close();
                    if (protocolId == null)
                        continue;

                    Integer datasetId = datasetAndLumKey.getKey();
                    Integer previousDatasetId = protocol2dataset.get(protocolId);
                    if (previousDatasetId == null || previousDatasetId < datasetId)
                        protocol2dataset.put(protocolId, datasetId);
                }
                // Update the datasets
                for (Map.Entry<Integer,Integer> protocolAndDatasetId : protocol2dataset.entrySet())
                {
                    Integer protocolId = protocolAndDatasetId.getKey();
                    Integer datasetId = protocolAndDatasetId.getValue();

                    // first check that there isn't already a dataset with that protocol in there
                    SQLFragment checkSql = new SQLFragment(
                            "SELECT d.datasetid\n" +
                            "FROM study.dataset d\n" +
                            "WHERE protocolid = ?\n" +
                            "and container = ?", protocolId, container);
                    rs = Table.executeQuery(LuminexSchema.getSchema(), checkSql);
                    boolean needsUpgrade = !rs.next();
                    rs.close();
                    if (needsUpgrade)
                    {
                        SQLFragment updateSql = new SQLFragment("UPDATE study.dataset SET protocolId = ?\n" +
                                "WHERE\n" +
                                "protocolId IS NULL\n" +
                                "AND\n" +
                                "container = ?\n" +
                                "AND\n" +
                                "datasetid = ?",
                                protocolId, container, datasetId);

                        Table.execute(LuminexSchema.getSchema(), updateSql);
                    }
                }
            }
            if (transactionOwner)
                scope.commitTransaction();
        }
        catch (SQLException se)
        {
            throw UnexpectedException.wrap(se);
        }
        finally
        {
            if (rs != null) try {rs.close();} catch (SQLException se) {}
            if (transactionOwner)
                scope.closeConnection();
        }
    }
}
