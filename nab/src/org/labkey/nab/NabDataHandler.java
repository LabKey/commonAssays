/*
 * Copyright (c) 2009-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.nab;

import org.labkey.api.assay.dilution.DilutionAssayRun;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.api.assay.nab.NabSpecimen;
import org.labkey.api.data.Container;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.security.User;
import org.labkey.nab.query.NabProtocolSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: klum
 * Date: May 15, 2009
 */
public abstract class NabDataHandler extends DilutionDataHandler
{
    public static final DataType NAB_TRANSFORMED_DATA_TYPE = new DataType("AssayRunNabTransformedData"); // a marker data type
    public static final String NAB_DATA_ROW_LSID_PREFIX = "AssayRunNabDataRow";

    public NabDataHandler()
    {
        super(NAB_DATA_ROW_LSID_PREFIX);
    }

    public Map<DilutionSummary, DilutionAssayRun> getDilutionSummaries(User user, StatsService.CurveFitType fit, int... dataObjectIds) throws ExperimentException, SQLException
    {
        Map<DilutionSummary, DilutionAssayRun> summaries = new LinkedHashMap<>();
        if (dataObjectIds == null || dataObjectIds.length == 0)
            return summaries;

        Map<Integer, DilutionAssayRun> dataToAssay = new HashMap<>();
        List<Integer> nabSpecimenIds = new ArrayList<>(dataObjectIds.length);
        for (int nabSpecimenId : dataObjectIds)
            nabSpecimenIds.add(nabSpecimenId);
        List<NabSpecimen> nabSpecimens = NabManager.get().getNabSpecimens(nabSpecimenIds);
        for (NabSpecimen nabSpecimen : nabSpecimens)
        {
            String wellgroupName = nabSpecimen.getWellgroupName();
            if (null == wellgroupName)
                continue;

            int runId = nabSpecimen.getRunId();
            DilutionAssayRun assay = dataToAssay.get(runId);
            if (assay == null)
            {
                ExpRun run = ExperimentService.get().getExpRun(runId);
                if (null == run)
                    continue;
                assay = getAssayResults(run, user, fit);
                if (null == assay)
                    continue;
                dataToAssay.put(runId, assay);
            }

            for (DilutionSummary summary : assay.getSummaries())
            {
                if (wellgroupName.equals(summary.getFirstWellGroup().getName()))
                {
                    summaries.put(summary, assay);
                    break;
                }
            }
        }
        return summaries;
    }

    @Override
    protected void importRows(ExpData data, ExpRun run, ExpProtocol protocol, List<Map<String, Object>> rawData) throws ExperimentException
    {
        try
        {
            Container container = run.getContainer();
            OntologyManager.ensureObject(container, data.getLSID());
            Map<Integer, String> cutoffFormats = getCutoffFormats(protocol, run);

            Map<String, ExpMaterial> inputMaterialMap = new HashMap<>();

            for (ExpMaterial material : run.getMaterialInputs().keySet())
                inputMaterialMap.put(material.getLSID(), material);

            for (Map<String, Object> group : rawData)
            {
                if (!group.containsKey(WELLGROUP_NAME_PROPERTY))
                    throw new ExperimentException("The row must contain a value for the well group name : " + WELLGROUP_NAME_PROPERTY);

                if (group.get(WELLGROUP_NAME_PROPERTY) == null)
                    throw new ExperimentException("The row must contain a value for the well group name : " + WELLGROUP_NAME_PROPERTY);

                if (group.get(DILUTION_INPUT_MATERIAL_DATA_PROPERTY) == null)
                    throw new ExperimentException("The row must contain a value for the specimen lsid : " + DILUTION_INPUT_MATERIAL_DATA_PROPERTY);

                String groupName = group.get(WELLGROUP_NAME_PROPERTY).toString();
                String specimenLsid = group.get(DILUTION_INPUT_MATERIAL_DATA_PROPERTY).toString();

                ExpMaterial material = inputMaterialMap.get(specimenLsid);

                if (material == null)
                    throw new ExperimentException("The row must contain a value for the specimen lsid : " + DILUTION_INPUT_MATERIAL_DATA_PROPERTY);

                String dataRowLsid = getDataRowLSID(data, groupName, material.getPropertyValues()).toString();

                OntologyManager.ensureObject(container, dataRowLsid,  data.getLSID());
                int objectId = 0;

                // New code to insert into NAbSpecimen and CutoffValue tables instead of Ontology properties
                Map<String, Object> nabSpecimenEntries = new HashMap<>();
                nabSpecimenEntries.put(WELLGROUP_NAME_PROPERTY, groupName);
                nabSpecimenEntries.put("ObjectId", objectId);                       // TODO: this will go away  when nab table transfer is complete
                nabSpecimenEntries.put("ObjectUri", dataRowLsid);
                nabSpecimenEntries.put("ProtocolId", protocol.getRowId());
                nabSpecimenEntries.put("DataId", data.getRowId());
                nabSpecimenEntries.put("RunId", run.getRowId());
                nabSpecimenEntries.put("SpecimenLsid", group.get(DILUTION_INPUT_MATERIAL_DATA_PROPERTY));
                nabSpecimenEntries.put("FitError", group.get(FIT_ERROR_PROPERTY));
                nabSpecimenEntries.put("Auc_Poly", group.get(AUC_PREFIX + POLY_SUFFIX));
                nabSpecimenEntries.put("PositiveAuc_Poly", group.get(pAUC_PREFIX + POLY_SUFFIX));
                nabSpecimenEntries.put("Auc_4pl", group.get(AUC_PREFIX + PL4_SUFFIX));
                nabSpecimenEntries.put("PositiveAuc_4pl", group.get(pAUC_PREFIX + PL4_SUFFIX));
                nabSpecimenEntries.put("Auc_5pl", group.get(AUC_PREFIX + PL5_SUFFIX));
                nabSpecimenEntries.put("PositiveAuc_5pl", group.get(pAUC_PREFIX + PL5_SUFFIX));
                int nabRowid = NabManager.get().insertNabSpecimenRow(null, nabSpecimenEntries);

                for (Integer cutoffValue : cutoffFormats.keySet())
                {
                    Map<String, Object> cutoffEntries = new HashMap<>();
                    cutoffEntries.put("NabSpecimenId", nabRowid);
                    cutoffEntries.put("Cutoff", (double)cutoffValue);

                    String cutoffStr = cutoffValue.toString();
                    String icKey = POINT_IC_PREFIX + cutoffStr;
                    cutoffEntries.put("Point", group.get(icKey));
                    icKey = POINT_IC_PREFIX + cutoffStr + OOR_SUFFIX;
                    cutoffEntries.put("PointOORIndicator", group.get(icKey));
                    icKey = CURVE_IC_PREFIX + cutoffStr + POLY_SUFFIX;
                    cutoffEntries.put("IC_Poly", group.get(icKey));
                    icKey = CURVE_IC_PREFIX + cutoffStr + POLY_SUFFIX + OOR_SUFFIX;
                    cutoffEntries.put("IC_PolyOORIndicator", group.get(icKey));
                    icKey = CURVE_IC_PREFIX + cutoffStr + PL4_SUFFIX;
                    cutoffEntries.put("IC_4pl", group.get(icKey));
                    icKey = CURVE_IC_PREFIX + cutoffStr + PL4_SUFFIX + OOR_SUFFIX;
                    cutoffEntries.put("IC_4plOORIndicator", group.get(icKey));
                    icKey = CURVE_IC_PREFIX + cutoffStr + PL5_SUFFIX;
                    cutoffEntries.put("IC_5pl", group.get(icKey));
                    icKey = CURVE_IC_PREFIX + cutoffStr + PL5_SUFFIX + OOR_SUFFIX;
                    cutoffEntries.put("IC_5plOORIndicator", group.get(icKey));
                    NabManager.get().insertCutoffValueRow(null, cutoffEntries);
                }
                NabProtocolSchema.ensureCutoffValues(protocol, cutoffFormats.keySet());
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeDeleteData(List<ExpData> datas) throws ExperimentException
    {
        try
        {
            NabManager.get().deleteRunData(datas);
        }
        catch(SQLException e)
        {
            throw new ExperimentException(e);
        }
    }
}
