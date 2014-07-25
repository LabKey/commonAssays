package org.labkey.nab.query;

import org.labkey.api.assay.dilution.DilutionManager;
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
        PlateSamplePropertyHelper helper = getProvider().getVirusPropertyHelper((PlateUploadForm) context);
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
                    insertVirusRecord(table, context.getUser(), virusLsid.toString(), entry.getValue());
                }
            }
        }
    }

    protected void insertVirusRecord(TableInfo table, User user, String virusLsid, Map<DomainProperty, String> values)
    {
        Map<String, Object> rowMap = new HashMap<>();
        for (Map.Entry<DomainProperty, String> entry : values.entrySet())
            rowMap.put(entry.getKey().getName(), entry.getValue());

        rowMap.put(NabVirusDomainKind.VIRUS_LSID_COLUMN_NAME, virusLsid);
        Table.insert(user, table, rowMap);
    }
}
