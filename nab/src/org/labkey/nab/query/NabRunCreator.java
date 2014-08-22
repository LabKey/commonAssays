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
package org.labkey.nab.query;

import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.security.User;
import org.labkey.api.study.actions.PlateUploadForm;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.ParticipantVisitResolver;
import org.labkey.api.study.assay.PlateBasedRunCreator;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabDataHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by klum on 7/18/2014.
 */
public class NabRunCreator extends PlateBasedRunCreator<NabAssayProvider>
{
    public NabRunCreator(NabAssayProvider provider)
    {
        super(provider);
    }

    @Override
    protected void resolveExtraRunData(ParticipantVisitResolver resolver, AssayRunUploadContext context, Map<ExpMaterial, String> inputMaterials, Map<ExpData, String> inputDatas, Map<ExpMaterial, String> outputMaterials, Map<ExpData, String> outputDatas) throws ExperimentException
    {
        super.resolveExtraRunData(resolver, context, inputMaterials, inputDatas, outputMaterials, outputDatas);

        // insert virus properties
        PlateSamplePropertyHelper helper = getProvider().getVirusPropertyHelper((PlateUploadForm) context, false);
        Map<String, Map<DomainProperty, String>> virusProperties = null != helper ?
                helper.getSampleProperties(context.getRequest())
                : null;

        //context.getRunProperties();    // TODO: for old assays?

        Set<ExpData> datas = outputDatas.keySet();
        assert datas.size() == 1 : "Expecting only a single output material";

        if (datas.size() == 1 && virusProperties != null)
        {
            ExpData outputData = datas.iterator().next();
            AssayProtocolSchema protocolSchema = context.getProvider().createProtocolSchema(context.getUser(), context.getContainer(), context.getProtocol(), null);
            TableInfo virusTable = protocolSchema.createTable(DilutionManager.VIRUS_TABLE_NAME);

            if (virusTable instanceof FilteredTable)
            {
                TableInfo table = ((FilteredTable)virusTable).getRealTable();
                for (Map.Entry<String, Map<DomainProperty, String>> entry : virusProperties.entrySet())
                {
                    // create the virus lsid based on the virus well name
                    Lsid virusLsid = NabDataHandler.createVirusWellGroupLsid(outputData, entry.getKey());
                    insertVirusRecord(table, context.getContainer(), context.getUser(), outputData.getLSID(), virusLsid.toString(), entry.getValue());
                }
            }
        }
    }

    /**
     * Adds a record to the nab virus table for each virus group in the run
     *
     * @param dataLsid the lsid for the (ExpData) run associated with this record
     * @param virusLsid the lsid identifying the virus and associated run
     * @param values a map of values for the virus domain table
     */
    protected void insertVirusRecord(TableInfo table, Container container, User user, String dataLsid, String virusLsid, Map<DomainProperty, String> values)
    {
        Map<String, Object> rowMap = new HashMap<>();
        for (Map.Entry<DomainProperty, String> entry : values.entrySet())
            rowMap.put(entry.getKey().getName(), entry.getValue());

        rowMap.put(NabVirusDomainKind.VIRUS_LSID_COLUMN_NAME, virusLsid);
        rowMap.put("Container", container);
        rowMap.put(NabVirusDomainKind.DATLSID_COLUMN_NAME, dataLsid);

        Table.insert(user, table, rowMap);
    }
}
