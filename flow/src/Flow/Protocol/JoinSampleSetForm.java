package Flow.Protocol;

import org.labkey.api.query.FieldKey;
import org.labkey.api.query.TableKey;
import org.labkey.api.exp.PropertyDescriptor;
import org.fhcrc.cpas.flow.data.FlowProtocol;
import org.fhcrc.cpas.flow.query.FlowSchema;
import org.fhcrc.cpas.flow.query.FlowTableType;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.util.UnexpectedException;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.LinkedHashMap;

public class JoinSampleSetForm extends ProtocolForm
{
    static public final int KEY_FIELDS_MAX = 5;

    public FieldKey[] ff_dataField = new FieldKey[KEY_FIELDS_MAX];
    public String[] ff_samplePropertyURI = new String[KEY_FIELDS_MAX];

    public void reset(ActionMapping actionMapping, HttpServletRequest request)
    {
        super.reset(actionMapping, request);
        try
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
        catch (ServletException e)
        {
            throw UnexpectedException.wrap(e);
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
        ret.put(new FieldKey(new TableKey(null, "Run"), "Name"), "Run Name");
        TableKey keyword = new TableKey(null, "Keyword");
        ColumnInfo colKeyword = tableFCSFiles.getColumn("Keyword");
        TableInfo tableKeywords = colKeyword.getFk().getLookupTableInfo();
        for (ColumnInfo column : tableKeywords.getColumns())
        {
            ret.put(new FieldKey(keyword, column.getName()), column.getCaption());
        }
        return ret;
    }

}
