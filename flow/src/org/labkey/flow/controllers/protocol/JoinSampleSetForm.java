package org.labkey.flow.controllers.protocol;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;

import javax.servlet.ServletException;
import java.util.LinkedHashMap;
import java.util.Map;

public class JoinSampleSetForm extends ProtocolForm
{
    static public final int KEY_FIELDS_MAX = 5;

    public FieldKey[] ff_dataField = new FieldKey[KEY_FIELDS_MAX];
    public String[] ff_samplePropertyURI = new String[KEY_FIELDS_MAX];

    public void init() throws UnauthorizedException
    {
        FlowProtocol protocol = getProtocol();
        Map.Entry<String, FieldKey>[] entries = protocol.getSampleSetJoinFields().entrySet().toArray(new Map.Entry[KEY_FIELDS_MAX]);
        for (int i = 0; i < KEY_FIELDS_MAX; i ++)
        {
            Map.Entry<String, FieldKey> entry = entries[i];
            if (entry == null)
                break;
            ff_samplePropertyURI[i] = entry.getKey();
            ff_dataField[i] = entry.getValue();
        }
    }


    public void setFf_dataField(String[] fields)
    {
        ff_dataField = new FieldKey[fields.length];
        for (int i = 0; i < fields.length; i ++)
        {
            ff_dataField[i] = fields[i] == null ? null : FieldKey.fromString(fields[i]);
        }
    }

    public void setFf_samplePropertyURI(String[] propertyURIs)
    {
        ff_samplePropertyURI = propertyURIs;
    }

    public Map<String, String> getAvailableSampleKeyFields() throws ServletException
    {
        LinkedHashMap<String,String> ret = new LinkedHashMap();
        ret.put("", "");
        for (PropertyDescriptor pd : getProtocol().getSampleSet().getPropertiesForType())
        {
            ret.put(pd.getName(), pd.getName());
        }
        return ret;
    }

    public Map<FieldKey, String> getAvailableDataKeyFields()
    {
        LinkedHashMap<FieldKey, String> ret = new LinkedHashMap();
        FlowSchema schema = new FlowSchema(getUser(), getContainer());
        TableInfo tableFCSFiles = schema.getTable(FlowTableType.FCSFiles.toString(), "Foo");

        ret.put(null, "");
        ret.put(new FieldKey(null, "Name"), "Name");
        ret.put(FieldKey.fromParts("Run", "Name"), "Run Name");
        FieldKey keyword = new FieldKey(null, "Keyword");
        ColumnInfo colKeyword = tableFCSFiles.getColumn("Keyword");
        TableInfo tableKeywords = colKeyword.getFk().getLookupTableInfo();
        for (ColumnInfo column : tableKeywords.getColumns())
        {
            ret.put(new FieldKey(keyword, column.getName()), column.getCaption());
        }
        return ret;
    }

}
