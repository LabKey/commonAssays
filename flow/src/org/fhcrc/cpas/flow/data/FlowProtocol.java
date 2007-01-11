package org.fhcrc.cpas.flow.data;

import org.labkey.api.security.User;
import org.labkey.api.security.ACL;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.ExperimentProperty;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.HttpView;
import org.labkey.api.data.*;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.query.api.FieldKey;
import org.labkey.api.query.api.TableKey;
import org.labkey.api.query.api.QueryService;
import org.fhcrc.cpas.flow.query.FlowSchema;
import org.fhcrc.cpas.flow.persist.AttributeSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.ObjectUtils;
import Flow.FlowParam;
import Flow.Protocol.ProtocolController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import java.util.*;
import java.sql.SQLException;
import java.sql.ResultSet;

public class FlowProtocol extends FlowObject<ExpProtocol>
{
    

    static private final String DEFAULT_PROTOCOL_NAME = "Flow";
    static final private String SAMPLESET_NAME = "Samples";
    static public FlowProtocol ensureForContainer(User user, Container container) throws Exception
    {
        FlowProtocol ret = getForContainer(container);
        if (ret != null)
        {
            if (ret.getProtocol().getImplementation() == null)
            {
                ret.setProperty(user, ExperimentProperty.PROTOCOLIMPLEMENTATION.getPropertyDescriptor(), FlowProtocolImplementation.NAME);
            }
            return ret;
        }
        ExpProtocol protocol = ExperimentService.get().createProtocol(container, DEFAULT_PROTOCOL_NAME, ExpProtocol.ApplicationType.ExperimentRun);
        protocol.save(user);
        ret = new FlowProtocol(protocol);
        FlowProtocolStep.initProtocol(user, ret);
        return ret;
    }

    static public FlowProtocol getForContainer(Container container)
    {
        return getForContainer(container, DEFAULT_PROTOCOL_NAME);
    }

    static public FlowProtocol fromURL(User user, ViewURLHelper url, HttpServletRequest request) throws ServletException
    {
        FlowProtocol ret = fromProtocolId(getIntParam(url, request, FlowParam.experimentId));
        if (ret == null)
        {
            ret = FlowProtocol.getForContainer(ContainerManager.getForPath(url.getExtraPath()));
        }
        if (ret == null)
            return null;
        if (!ret.getContainer().hasPermission(user, ACL.PERM_READ))
        {
            HttpView.throwUnauthorized();
        }
        return ret;
    }

    static public FlowProtocol fromProtocolId(int id)
    {
        ExpProtocol protocol = ExperimentService.get().getProtocol(id);
        if (protocol == null)
            return null;
        return new FlowProtocol(protocol);
    }

    static public FlowProtocol getForContainer(Container container, String name)
    {
        ExpProtocol protocol = ExperimentService.get().getProtocol(container, name);
        if (protocol != null)
            return new FlowProtocol(protocol);
        return null;
    }

    public FlowProtocol(ExpProtocol protocol)
    {
        super(protocol);
    }

    public ExpProtocol getProtocol()
    {
        return getExpObject();
    }

    public void addParams(Map<FlowParam, Object> map)
    {
        switch (getProtocol().getApplicationType())
        {
            case ExperimentRun:
                map.put(FlowParam.protocolId, getProtocol().getRowId());
                break;
            case ProtocolApplication:
                FlowProtocolStep step = getStep();
                if (step != null)
                    map.put(FlowParam.actionSequence, step.getDefaultActionSequence());
                break;
        }
    }

    public FlowObject getParent()
    {
        return null;
    }

    public ViewURLHelper urlShow()
    {
        return urlFor(ProtocolController.Action.showProtocol);
    }

    public FlowProtocolStep getStep()
    {
        return FlowProtocolStep.fromLSID(getContainer(), getLSID());
    }

    public ExpSampleSet getSampleSet()
    {
        return ExperimentService.get().getSampleSet(getContainer(), SAMPLESET_NAME);
    }

    public Map<String, FieldKey> getSampleSetJoinFields()
    {
        String prop;
        try
        {
            prop = (String) getProperty(FlowProperty.SampleSetJoin.getPropertyDescriptor());
        }
        catch (SQLException e)
        {
            return Collections.EMPTY_MAP;
        }
        if (prop == null)
            return Collections.EMPTY_MAP;
        String[] values = StringUtils.split(prop, "&");
        Map<String, FieldKey> ret = new LinkedHashMap();
        for (String value : values)
        {
            int ichEquals = value.indexOf("=");
            String left = PageFlowUtil.decode(value.substring(0, ichEquals));
            String right = PageFlowUtil.decode(value.substring(ichEquals + 1));
            ret.put(left, FieldKey.fromString(right));
        }
        return ret;
    }

    public void setSampleSetJoinFields(User user, Map<String, FieldKey> values) throws Exception
    {
        List<String> strings = new ArrayList();
        for (Map.Entry<String, FieldKey> entry : values.entrySet())
        {
            strings.add(PageFlowUtil.encode(entry.getKey()) + "=" + PageFlowUtil.encode(entry.getValue().toString()));
        }
        String value = StringUtils.join(strings.iterator(), "&");
        setProperty(user, FlowProperty.SampleSetJoin.getPropertyDescriptor(), value);
    }

    public ViewURLHelper urlUploadSamples()
    {
        ViewURLHelper ret = new ViewURLHelper("Experiment", "showUploadMaterials", getContainerPath());
        ret.addParameter("name", SAMPLESET_NAME);
        return ret;
    }

    public Map<SampleKey, ExpMaterial> getSampleMap() throws SQLException
    {
        ExpSampleSet ss = getSampleSet();
        if (ss == null)
            return Collections.EMPTY_MAP;
        Set<String> propertyNames = getSampleSetJoinFields().keySet();
        if (propertyNames.size() == 0)
            return Collections.EMPTY_MAP;
        SamplesSchema schema = new SamplesSchema(null, getContainer());

        ExpMaterialTable sampleTable = schema.getSampleTable("samples", ss);
        List<ColumnInfo> selectedColumns = new ArrayList();
        ColumnInfo colProperty = sampleTable.getColumn(ExpMaterialTable.Column.Property.toString());
        ColumnInfo colRowId = sampleTable.getColumn(ExpMaterialTable.Column.RowId.toString());
        selectedColumns.add(colRowId);
        for (String propertyName : propertyNames)
        {
            selectedColumns.add(colProperty.getFk().createLookupColumn(colProperty, propertyName));
        }
        ResultSet rsSamples = Table.select(sampleTable, selectedColumns.toArray(new ColumnInfo[0]), null, null);
        Map<SampleKey, ExpMaterial> ret = new HashMap();
        while (rsSamples.next())
        {
            int rowId = ((Number) colRowId.getValue(rsSamples)).intValue();
            ExpMaterial sample = ExperimentService.get().getMaterial(rowId);
            if (sample == null)
                continue;
            SampleKey key = new SampleKey();
            for (int i = 1; i < selectedColumns.size(); i ++)
            {
                ColumnInfo column = selectedColumns.get(i);
                key.addValue(column.getValue(rsSamples));
            }
            ret.put(key, sample);
        }
        return ret;
    }

    public int updateSampleIds(User user) throws Exception
    {
        ExperimentService.Interface svc = ExperimentService.get();
        Map<String, FieldKey> joinFields = getSampleSetJoinFields();
        Map<SampleKey, ExpMaterial> sampleMap = getSampleMap();
        ExpSampleSet ss = getSampleSet();

        FlowSchema schema = new FlowSchema(user, getContainer());
        TableInfo fcsFilesTable = schema.getTable("FCSFiles", "FCSFiles");
        List<FieldKey> fields = new ArrayList();
        FieldKey fieldRowId = new FieldKey(null, "RowId");
        FieldKey fieldSampleRowId = new FieldKey(null, "Sample");
        fields.add(fieldRowId);
        fields.add(fieldSampleRowId);
        fields.addAll(joinFields.values());
        Map<FieldKey, ColumnInfo> columns = QueryService.get().getColumns(fcsFilesTable, fields);
        ColumnInfo colRowId = columns.get(fieldRowId);
        ColumnInfo colSampleId = columns.get(fieldSampleRowId);
        ResultSet rs = Table.select(fcsFilesTable, columns.values().toArray(new ColumnInfo[0]), null, null);
        int ret = 0;
        boolean fTransaction = false;
        try
        {
            PropertyDescriptor pdInputRole = null;

            if (!svc.isTransactionActive())
            {
                svc.beginTransaction();
                fTransaction = true;
            }
            while (rs.next())
            {
                int fcsFileId = ((Number) colRowId.getValue(rs)).intValue();
                ExpData fcsFile = svc.getData(fcsFileId);
                if (fcsFile == null)
                    continue;
                SampleKey key = new SampleKey();
                for (FieldKey fieldKey : joinFields.values())
                {
                    ColumnInfo column = columns.get(fieldKey);
                    Object value = null;
                    if (column != null)
                    {
                        value = column.getValue(rs);
                    }
                    key.addValue(value);
                }
                ExpMaterial sample = sampleMap.get(key);
                Integer newSampleId = sample == null ? null : sample.getRowId();
                Object oldSampleId = colSampleId.getValue(rs);
                if (ObjectUtils.equals(newSampleId, oldSampleId))
                    continue;
                ExpProtocolApplication app = fcsFile.getSourceApplication();
                if (app == null)
                {
                    // This will happen for orphaned FCSFiles (where the ExperimentRun has been deleted).
                    continue;
                }

                boolean found = false;
                for (ExpMaterial material : app.getInputMaterials())
                {
                    if (material.getSampleSet() == null || material.getSampleSet().getRowId() != ss.getRowId())
                        continue;
                    if (sample != null)
                    {
                        if (material.equals(sample))
                        {
                            found = true;
                            ret ++;
                            break;
                        }
                    }
                    app.removeMaterialInput(user, material);
                }
                if (!found && sample != null)
                {
                    pdInputRole = app.addMaterialInput(user, sample, null, pdInputRole);
                    ret ++;
                }
            }
        }
        catch (Exception e)
        {
            if (fTransaction)
            {
                svc.rollbackTransaction();
                fTransaction = false;
            }
            throw e;
        }
        finally
        {
            if (fTransaction)
                svc.commitTransaction();
        }
        return ret;
    }

    public SampleKey makeSampleKey(String runName, String fileName, AttributeSet attrs)
    {
        Collection<FieldKey> fields = getSampleSetJoinFields().values();
        if (fields.size() == 0)
            return null;
        TableKey tableRun = new TableKey(null, "Run");
        TableKey tableKeyword = new TableKey(null, "Keyword");
        SampleKey ret = new SampleKey();
        for (FieldKey field : fields)
        {
            if (field.getTable() == null)
            {
                if ("Name".equals(field.getName()))
                {
                    ret.addValue(fileName);
                }
                else
                {
                    return null;
                }
            }
            else if (tableRun.equals(field.getTable()))
            {
                if ("Name".equals(field.getName()))
                {
                    ret.addValue(runName);
                }
                else
                {
                    return null;
                }
            }
            else if (tableKeyword.equals(field.getTable()))
            {
                ret.addValue(attrs.getKeywords().get(field.getName()));
            }
        }
        return ret;
    }
}
