/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.elispot;

import com.drew.lang.annotations.Nullable;
import org.apache.log4j.Logger;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.PropertyStorageSpec;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.ChangePropertyDescriptorException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.StorageProvisioner;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.FieldKey;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.security.User;
import org.labkey.api.util.Pair;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by davebradlee on 3/17/15.
 */
public class ElispotUpgradeCode implements UpgradeCode
{
    private static final Logger _log = Logger.getLogger(ElispotUpgradeCode.class);

    private static class AntigenPropMap extends CaseInsensitiveHashMap<Object> {}

    // Map AntigenLsid to AntigenProps; holds map of ObjectId to AntigenProps temporarily
    private static class AntigenLsidMap extends HashMap<String, AntigenPropMap>
    {
        // build up groups of props; after figuring out antigenLsid they get moved
        // Map objectId to AntigenPropMap
        private final Map<Integer, AntigenPropMap> _antigenPropMaps = new HashMap<>();

        // Within the run, map ElispotProperty names to their propertyIds
        private final Map<String, Integer> _propertyNameToIdMap = new HashMap<>();

        public Map<Integer, AntigenPropMap> getAntigenPropMaps()
        {
            return _antigenPropMaps;
        }

        public Map<String, Integer> getPropertyNameToIdMap()
        {
            return _propertyNameToIdMap;
        }
    }

    // Map Run to AntigenLsids
    private static class RunMap extends HashMap<Integer, AntigenLsidMap> {}

    // invoked by elispotlk-15.10-15.12.sql
    @SuppressWarnings({"UnusedDeclaration"})
    public static void migrateToElispotTables(ModuleContext moduleContext)
    {
        if (moduleContext.isNewInstall())
            return;

        User user = moduleContext.getUpgradeUser();

        updateAntigenLsidsInRunData(user);
        updateAntigenDomains(user);
        // Populate elispotantigen provisioned tables

        // Build map ContainerId --> RunId --> AntigenLsid --> field map
        final Map<String, RunMap> containerToRunMap = new HashMap<>();

        // Extract Antigen properties from AssayData, such as AntigenId, AntigenName and CellWell
        SQLFragment dataRowSql = new SQLFragment("SELECT o.ObjectId, o.ObjectUri, spd.PropertyId, spd.Name, RunId, o.Container, op.TypeTag, op.FloatValue, op.StringValue, op.DateTimeValue FROM exp.Object o\n" +
                " JOIN (SELECT parent.ObjectId, d.RunId FROM exp.Data d, exp.Object parent WHERE d.LSID = parent.ObjectURI) sd\n" +
                "       ON o.OwnerObjectId = sd.ObjectId\n" +
                " JOIN exp.ObjectProperty op ON op.ObjectId = o.ObjectId\n" +
                " JOIN (SELECT * FROM exp.PropertyDescriptor pd WHERE pd.PropertyUri LIKE '%ElispotProperty%') spd\n" +
                "       ON op.PropertyId = spd.PropertyId \n" +
                " WHERE o.ObjectURI LIKE '%ElispotAssayDataRow%'");

        new SqlSelector(ExperimentService.get().getSchema(), dataRowSql).forEachMap(new Selector.ForEachBlock<Map<String, Object>>()
        {
            @Override
            public void exec(Map<String, Object> map) throws SQLException
            {
                String containerId = (String) map.get("Container");
                if (null == containerId)
                {
                    _log.error("Null containerId");
                    return;
                }
                Integer runId = (Integer) map.get("RunId");
                if (null == runId)
                {
                    _log.error("Null antigen runId");
                    return;
                }
                Integer objectId = (Integer) map.get("ObjectId");
                if (null == objectId)
                {
                    _log.error("Null antigen objectId");
                    return;
                }
                String fullName = (String) map.get("Name");
                if (null == fullName)
                {
                    _log.error("Null antigen property name");
                    return;
                }
                Integer propertyId = (Integer) map.get("PropertyId");
                if (null == propertyId)
                {
                    _log.error("Null antigen property id");
                    return;
                }

                if (!containerToRunMap.containsKey(containerId))
                {
                    containerToRunMap.put(containerId, new RunMap());
                }
                RunMap runMap = containerToRunMap.get(containerId);
                if (!runMap.containsKey(runId))
                {
                    runMap.put(runId, new AntigenLsidMap());
                }
                AntigenLsidMap antigenLsidMap = runMap.get(runId);
                if (!antigenLsidMap.getAntigenPropMaps().containsKey(objectId))
                {
                    antigenLsidMap.getAntigenPropMaps().put(objectId, new AntigenPropMap());
                }
                AntigenPropMap antigenPropMap = antigenLsidMap.getAntigenPropMaps().get(objectId);

                if (fullName.equals(ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY))
                {
                    String specimenLsid = (String) map.get("StringValue");
                    Pair<String, String> pair = getSpecimenAndProtocolNames(runId, specimenLsid);
                    if (null == pair)
                        return;     // error already logged

                    antigenPropMap.put(ElispotDataHandler.WELLGROUP_PROPERTY_NAME, pair.first);
                    antigenPropMap.put("ProtocolName", pair.second);
                    antigenPropMap.put(fullName, specimenLsid);
                }
                else if (!fullName.equals(ElispotDataHandler.SFU_PROPERTY_NAME) && !fullName.equals(ElispotDataHandler.NORMALIZED_SFU_PROPERTY_NAME) &&
                        !fullName.equals(ElispotDataHandler.WELLGROUP_PROPERTY_NAME) && !fullName.equals(ElispotDataHandler.WELLGROUP_LOCATION_PROPERTY))
                {
                    String typeTag = (String) map.get("TypeTag");
                    if (null == typeTag)
                    {
                        _log.error("Property type tag not found");
                        return;
                    }
                    antigenPropMap.put(fullName, "f".equals(typeTag) ? map.get("FloatValue") :
                                                 "s".equals(typeTag) ? map.get("StringValue") :
                                                                       map.get("DateTimeValue"));
                    antigenLsidMap.getPropertyNameToIdMap().put(fullName, propertyId);
                }
            }
        });

        // We know enough to calculate AntigenLsids now, so do that and move AntigenProperties into AntigenPropMap
        for (RunMap runMap : containerToRunMap.values())
        {
            for (Map.Entry<Integer, AntigenLsidMap> antigenLsidMapEntry : runMap.entrySet())
            {
                Integer runId = antigenLsidMapEntry.getKey();
                AntigenLsidMap antigenLsidMap = antigenLsidMapEntry.getValue();
                for (AntigenPropMap antigenPropMap : antigenLsidMap.getAntigenPropMaps().values())
                {
                    if (antigenPropMap.containsKey(ElispotDataHandler.WELLGROUP_PROPERTY_NAME) && antigenPropMap.containsKey("ProtocolName") &&
                            antigenPropMap.containsKey(ElispotDataHandler.ANTIGEN_WELLGROUP_PROPERTY_NAME))
                    {
                        Lsid antigenLsid = ElispotDataHandler.getAntigenLsid((String) antigenPropMap.get(ElispotDataHandler.ANTIGEN_WELLGROUP_PROPERTY_NAME),
                                (String) antigenPropMap.get(ElispotDataHandler.WELLGROUP_PROPERTY_NAME), runId, (String) antigenPropMap.get("ProtocolName"), null);
                        antigenLsidMap.put(antigenLsid.toString(), getStreamlinedAntigenPropMap(antigenPropMap));
                    }
                    else
                    {
                        _log.error("Could not find components for antigenLsid");
                        return;
                    }
                }
                antigenLsidMap.getAntigenPropMaps().clear();
            }
        }

        // Add antigen properties to map we've already built; we'll have to add based on objectId first and then move to antigenLsid, as before
        // maps containerId to map of runId to ObjectId (ElispotAssayAntigenRow) to propertyGroup for that run
        SQLFragment antigenRowSql = new SQLFragment("SELECT o.ObjectId, o.ObjectUri, spd.PropertyId, spd.Name, RunId, o.Container, op.TypeTag, op.FloatValue, op.StringValue FROM exp.Object o\n" +
                " JOIN (SELECT parent.ObjectId, d.RunId FROM exp.Data d, exp.Object parent WHERE d.LSID = parent.ObjectURI) sd\n" +
                "       ON o.OwnerObjectId = sd.ObjectId\n" +
                " JOIN exp.ObjectProperty op ON op.ObjectId = o.ObjectId\n" +
                " JOIN (SELECT * FROM exp.PropertyDescriptor pd WHERE pd.PropertyUri LIKE '%ElispotAntigenProperty%') spd\n" +
                "       ON op.PropertyId = spd.PropertyId \n" +
                " WHERE o.ObjectURI LIKE '%ElispotAssayAntigenRow%'");

        new SqlSelector(ExperimentService.get().getSchema(), antigenRowSql).forEachMap(new Selector.ForEachBlock<Map<String, Object>>()
        {
            @Override
            public void exec(Map<String, Object> map) throws SQLException
            {
                String containerId = (String) map.get("Container");
                if (null == containerId)
                {
                    _log.error("Null containerId");
                    return;
                }
                Integer objectId = (Integer) map.get("ObjectId");
                if (null == objectId)
                {
                    _log.error("Null antigen objectId");
                    return;
                }
                Integer runId = (Integer) map.get("RunId");
                if (null == runId)
                {
                    _log.error("Null antigen runId");
                    return;
                }
                String fullName = (String) map.get("Name");
                if (null == fullName)
                {
                    _log.error("Null antigen property name");
                    return;
                }

                if (!containerToRunMap.containsKey(containerId))
                {
                    _log.error("Should have seen data props with this containerId before");
                    return;
                }
                RunMap runMap = containerToRunMap.get(containerId);
                if (!runMap.containsKey(runId))
                {
                    _log.error("Should have seen data props with this runId before");
                    return;
                }
                AntigenLsidMap antigenLsidMap = runMap.get(runId);
                if (!antigenLsidMap.getAntigenPropMaps().containsKey(objectId))
                {
                    antigenLsidMap.getAntigenPropMaps().put(objectId, new AntigenPropMap());
                }
                AntigenPropMap antigenPropMap = antigenLsidMap.getAntigenPropMaps().get(objectId);

                if (fullName.equals(ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY))
                {
                    String specimenLsid = (String) map.get("StringValue");
                    Pair<String, String> pair = getSpecimenAndProtocolNames(runId, specimenLsid);
                    if (null == pair)
                        return;     // error already logged

                    antigenPropMap.put(ElispotDataHandler.WELLGROUP_PROPERTY_NAME, pair.first);
                    antigenPropMap.put("ProtocolName", pair.second);
                    antigenPropMap.put(fullName, specimenLsid);
                }
                else
                {
                    assert null != map.get("TypeTag") && "f".equals(map.get("TypeTag"));
                    antigenPropMap.put(fullName, map.get("FloatValue"));
                }
            }
        });

        for (RunMap runMap : containerToRunMap.values())
        {
            for (Map.Entry<Integer, AntigenLsidMap> antigenLsidMapEntry : runMap.entrySet())
            {
                Integer runId = antigenLsidMapEntry.getKey();
                AntigenLsidMap antigenLsidMap = antigenLsidMapEntry.getValue();

                for (AntigenPropMap antigenPropMap : antigenLsidMap.getAntigenPropMaps().values())
                {
                    Map<String, AntigenPropMap> awgNameToProps = getProcessedAntigenProperties(antigenPropMap, antigenLsidMap);
                    for (Map.Entry<String, AntigenPropMap> awgNameToProp : awgNameToProps.entrySet())
                    {
                        String antigenWellGroupName = awgNameToProp.getKey();
                        AntigenPropMap processedAntigenPropMap = awgNameToProp.getValue();
                        if (processedAntigenPropMap.containsKey(ElispotDataHandler.WELLGROUP_PROPERTY_NAME) && processedAntigenPropMap.containsKey("ProtocolName"))
                        {
                            Lsid antigenLsid = ElispotDataHandler.getAntigenLsid(antigenWellGroupName,
                                    (String)processedAntigenPropMap.get(ElispotDataHandler.WELLGROUP_PROPERTY_NAME), runId, (String)processedAntigenPropMap.get("ProtocolName"), null);
                            if (!antigenLsidMap.containsKey(antigenLsid.toString()))
                            {
                                if (!"Background".equals(antigenWellGroupName))
                                {
                                    _log.error("We expected entry for antigenLsid to already exist");
                                    return;
                                }
                                antigenLsidMap.put(antigenLsid.toString(), new AntigenPropMap());
                                antigenLsidMap.get(antigenLsid.toString()).put(ElispotDataHandler.ANTIGEN_WELLGROUP_PROPERTY_NAME, antigenWellGroupName);
                                antigenLsidMap.get(antigenLsid.toString()).put(ElispotDataHandler.WELLGROUP_PROPERTY_NAME, processedAntigenPropMap.get(ElispotDataHandler.WELLGROUP_PROPERTY_NAME));
                                antigenLsidMap.get(antigenLsid.toString()).put(ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY, processedAntigenPropMap.get("SpecimenLsid"));
                            }
                            antigenLsidMap.get(antigenLsid.toString()).put("Mean", processedAntigenPropMap.get("Mean"));
                            antigenLsidMap.get(antigenLsid.toString()).put("Median", processedAntigenPropMap.get("Median"));
                        }
                        else
                        {
                            _log.error("Could not find components for antigenLsid");
                            return;
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, RunMap> runMapEntry : containerToRunMap.entrySet())
        {
            Container container = ContainerManager.getForId(runMapEntry.getKey());

            for (Map.Entry<Integer, AntigenLsidMap> antigenLsidMapEntry : runMapEntry.getValue().entrySet())
            {
                int runId = antigenLsidMapEntry.getKey();
                final TableInfo antigenTable = getAntigenTableAndEnsureProperties(runId);
                if (null == antigenTable)
                    return;     // error already logged

                for (Map.Entry<String, AntigenPropMap> antigenPropMapEntry : antigenLsidMapEntry.getValue().entrySet())
                {
                    AntigenPropMap antigenPropMap = antigenPropMapEntry.getValue();
                    antigenPropMap.put("AntigenLsid", antigenPropMapEntry.getKey());
                    antigenPropMap.put("RunId", runId);
                    Table.insert(user, antigenTable, antigenPropMap);
                }
            }
            removeOldOntologyObjects(container);
        }
    }

    // Return <SampleName, ProtocolName>
    @Nullable
    private static Pair<String, String> getSpecimenAndProtocolNames(int runId, String specimenLsid)
    {
        // This is a pain, but we need the name of the material for the specimenLsid
        ExpRun run = ExperimentService.get().getExpRun(runId);
        if (null == run)
        {
            _log.error("Run not found: " + runId);
            return null;
        }
        String wellgroupName = null;
        for (ExpMaterial material : run.getMaterialInputs().keySet())
            if (material.getLSID().equals(specimenLsid))
            {
                wellgroupName = material.getName();
                break;
            }

        if (null == wellgroupName)
        {
            _log.error("Specimen name not found for Lsid: " + specimenLsid);
            return null;
        }
        if (null == run.getProtocol())
        {
            _log.error("Protocol not found for run :" + runId);
            return null;
        }
        String protocolName = run.getProtocol().getName();
        return new Pair<>(wellgroupName, protocolName);
    }

    private static AntigenPropMap getStreamlinedAntigenPropMap(AntigenPropMap antigenPropMapIn)
    {
        AntigenPropMap antigenPropMap = new AntigenPropMap();
        for (Map.Entry<String, Object> entry : antigenPropMapIn.entrySet())
        {
            if (!entry.getKey().equals("ProtocolName"))
            {
                antigenPropMap.put(entry.getKey(), entry.getValue());
            }
        }
        return antigenPropMap;
    }

    private static Map<String, AntigenPropMap> getProcessedAntigenProperties(AntigenPropMap antigenPropMap, AntigenLsidMap antigenLsidMap)
    {
        // Properties we collected from antigen rows need to be processed;
        // they are named in the form <antigenname>[_antigenwellgoupname]_(Mean|Median)
        // -- To determine antigenLsid, if name has antigenwellgroupname, use it, otherwise find antigen (processed in first step) that
        //      has antigenname that matches and take the associated antigenwellgroupname (that hasn't already been used)
        // -- map property into Mean or Median property for that antigenLsid
        Map<String, AntigenPropMap> awgNameToProps = new HashMap<>();
        Set<String> usedAnitigenWellgroupNames = new HashSet<>();

        String wellgroupName = null;
        String protocolName = null;
        String specimenLsid = null;
        for (Map.Entry<String, Object> antigenPropEntry : antigenPropMap.entrySet())
        {
            String key = antigenPropEntry.getKey();
            if (key.equals(ElispotDataHandler.WELLGROUP_PROPERTY_NAME))
                wellgroupName = (String)antigenPropEntry.getValue();
            else if (key.equals("ProtocolName"))
                protocolName = (String)antigenPropEntry.getValue();
            else if (key.equals(ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY))
            {
                specimenLsid = (String)antigenPropEntry.getValue();
            }
            else
            {
                String[] splitKey = key.split("_");
                assert splitKey.length == 2 || splitKey.length == 3;
                if (splitKey.length > 2)
                {
                    String antigenWellgroupName = splitKey[1];
                    addPropToAntigenPropMap(awgNameToProps, antigenWellgroupName, antigenPropEntry.getValue(), splitKey[2].equals("Mean"));
                    usedAnitigenWellgroupNames.add(antigenWellgroupName);
                }
                else if (splitKey[0].equals("Background"))
                {
                    addPropToAntigenPropMap(awgNameToProps, "Background", antigenPropEntry.getValue(), splitKey[1].equals("Mean"));
                }
            }
        }
        assert null != wellgroupName && null != protocolName && null != specimenLsid;

        Map<String, Set<AntigenPropMap>> mapKeyToAntigenWellgroupPossibilities = new HashMap<>();
        for (Map.Entry<String, Object> antigenPropEntry : antigenPropMap.entrySet())
        {
            String key = antigenPropEntry.getKey();
            if (!key.equals(ElispotDataHandler.WELLGROUP_PROPERTY_NAME) && !key.equals("ProtocolName") &&
                !key.equals(ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY))
            {
                String[] splitKey = key.split("_");
                assert splitKey.length == 2 || splitKey.length == 3;
                if (splitKey.length == 2 && !splitKey[0].equals("Background"))
                {
                    // Need to lookup antigen name
                    String antigenNameIfAny = splitKey[0];
                    Set<AntigenPropMap> antigenPropMapsMatchingName = getAntigenPropMapsMatchingName(antigenNameIfAny, antigenLsidMap);
                    if (!mapKeyToAntigenWellgroupPossibilities.containsKey(antigenNameIfAny))
                    {
                        mapKeyToAntigenWellgroupPossibilities.put(antigenNameIfAny, antigenPropMapsMatchingName);
                    }
                }
            }
        }

        Map<String, String> mapKeyToWellgroupName = new HashMap<>();
        for (Map.Entry<String, Set<AntigenPropMap>> antigenPropMapsMatchingNameEntry : mapKeyToAntigenWellgroupPossibilities.entrySet())
        {
            String key = antigenPropMapsMatchingNameEntry.getKey();
            if (antigenPropMapsMatchingNameEntry.getValue().isEmpty())
            {
                // Nothing matched the possible antigen name, so it must have been null;
                // Therefore this name must be the antigenWellgroupName
                if (!usedAnitigenWellgroupNames.contains(key))
                {
                    mapKeyToWellgroupName.put(key, key);
                    usedAnitigenWellgroupNames.add(key);
                }
                else
                {
                    _log.error("Somehow the wellgroupName was already used.");
                    return Collections.emptyMap();
                }
            }
            else
            {
                for (AntigenPropMap antigenPropMap1 : antigenPropMapsMatchingNameEntry.getValue())
                {
                    String antigenWellgroupName = (String) antigenPropMap1.get(ElispotDataHandler.ANTIGEN_WELLGROUP_PROPERTY_NAME);
                    if (!usedAnitigenWellgroupNames.contains(antigenWellgroupName))
                    {
                        mapKeyToWellgroupName.put(key, antigenWellgroupName);
                        usedAnitigenWellgroupNames.add(antigenWellgroupName);
                        break;
                    }
                }
            }
        }

        for (Map.Entry<String, Object> antigenPropEntry : antigenPropMap.entrySet())
        {
            String key = antigenPropEntry.getKey();
            if (!key.equals(ElispotDataHandler.WELLGROUP_PROPERTY_NAME) && !key.equals("ProtocolName") &&
                    !key.equals(ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY))
            {
                String[] splitKey = key.split("_");
                assert splitKey.length == 2 || splitKey.length == 3;
                if (splitKey.length == 2 && !splitKey[0].equals("Background"))
                {
                    String antigenNameIfAny = splitKey[0];
                    String antigenWellgroupName = mapKeyToWellgroupName.get(antigenNameIfAny);
                    addPropToAntigenPropMap(awgNameToProps, antigenWellgroupName, antigenPropEntry.getValue(), splitKey[1].equals("Mean"));
                }
            }
        }

        for (AntigenPropMap antigenPropMap1 : awgNameToProps.values())
        {
            antigenPropMap1.put(ElispotDataHandler.WELLGROUP_PROPERTY_NAME, wellgroupName);
            antigenPropMap1.put("ProtocolName", protocolName);
            antigenPropMap1.put(ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY, specimenLsid);
        }
        return awgNameToProps;
    }

    private static void addPropToAntigenPropMap(Map<String, AntigenPropMap> awgNameToProps, String antigenWellgroupName, Object value, boolean isMean)
    {
        if (!awgNameToProps.containsKey(antigenWellgroupName))
            awgNameToProps.put(antigenWellgroupName, new AntigenPropMap());
        awgNameToProps.get(antigenWellgroupName).put(isMean ? "Mean" : "Median", value);
    }

    private static Set<AntigenPropMap> getAntigenPropMapsMatchingName(String antigenName, AntigenLsidMap antigenLsidMap)
    {
        Set<AntigenPropMap> antigenPropMaps = new HashSet<>();
        for (AntigenPropMap antigenPropMap : antigenLsidMap.values())
        {
            if (antigenName.equals(antigenPropMap.get(ElispotAssayProvider.ANTIGENNAME_PROPERTY_NAME)))
                antigenPropMaps.add(antigenPropMap);
        }
        return antigenPropMaps;
    }

    private static TableInfo getAntigenTableAndEnsureProperties(int runId)
    {
        ExpRun run = ExperimentService.get().getExpRun(runId);
        if (null == run)
        {
            _log.error("Run not found: " + runId);
            return null;
        }

        Domain domain = AbstractAssayProvider.getDomainByPrefix(run.getProtocol(), ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
        return StorageProvisioner.createTableInfo(domain);
    }

    private static void updateAntigenLsidsInRunData(final User user)
    {
        final TableInfo runDataTable = ElispotManager.getTableInfoElispotRunData();
        new TableSelector(runDataTable).forEach(new Selector.ForEachBlock<RunDataRow>()
        {
            @Override
            public void exec(RunDataRow row) throws SQLException
            {
                ExpRun run = ExperimentService.get().getExpRun(row.getRunId());
                Lsid antigenLsid = ElispotDataHandler.getAntigenLsid(row.getAntigenWellgroupName(), row.getWellgroupName(),
                                                                     row.getRunId(), run.getProtocol().getName(), null);
                Map<String, Object> fields = new CaseInsensitiveHashMap<>();
                fields.put("AntigenLsid", antigenLsid.toString());
                Table.update(user, runDataTable, fields, row.getRowId());
            }
        }, RunDataRow.class);
    }

    private static void removeOldOntologyObjects(Container container)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Container"), container);
        filter.addClause(new SimpleFilter.SQLClause("ObjectURI LIKE '%ElispotAssayData.%'", null, FieldKey.fromString("ObjectUri")));
        String[] objectUris = new TableSelector(OntologyManager.getTinfoObject(), Collections.singleton("ObjectUri"), filter, null).getArray(String.class);
        OntologyManager.deleteOntologyObjects(container, objectUris);

        SimpleFilter pdFilter = new SimpleFilter(FieldKey.fromString("Container"), container);
        pdFilter.addClause(new SimpleFilter.SQLClause("PropertyUri LIKE '%ElispotProperty%' OR PropertyUri LIKE '%ElispotAntigenProperty%'", null, FieldKey.fromString("ObjectUri")));
        new TableSelector(OntologyManager.getTinfoPropertyDescriptor(), pdFilter, null).forEach(new Selector.ForEachBlock<PropertyDescriptor>()
        {
            @Override
            public void exec(PropertyDescriptor pd) throws SQLException
            {
                OntologyManager.deletePropertyDescriptor(pd);
            }
        }, PropertyDescriptor.class);
    }

    private static void updateAntigenDomains(User user)
    {
        for (Container container : ContainerManager.getAllChildren(ContainerManager.getRoot()))
        {
            for (ExpProtocol protocol : ExperimentService.get().getExpProtocols(container))
            {
                Domain domain = AbstractAssayProvider.getDomainByPrefixIfExists(protocol, ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);

                if (null != domain)
                {
                    // Ensure base properties are there
                    boolean addedProperty = false;
                    for (PropertyStorageSpec propertyStorageSpec : domain.getDomainKind().getBaseProperties())
                        if (null == domain.getPropertyByName(propertyStorageSpec.getName()))
                        {
                            DomainProperty domainProperty = domain.addProperty(propertyStorageSpec);
                            domainProperty.setHidden(true);
                            domainProperty.setShownInInsertView(false);
                            domainProperty.setShownInUpdateView(false);
                            domainProperty.setShownInDetailsView(false);
                            addedProperty = true;
                        }

                    if (addedProperty)
                    {
                        try
                        {
                            domain.save(user);
                        }
                        catch (ChangePropertyDescriptorException e)
                        {
                            _log.error(e.getMessage());
                        }
                    }
                }
            }
        }

    }
}
