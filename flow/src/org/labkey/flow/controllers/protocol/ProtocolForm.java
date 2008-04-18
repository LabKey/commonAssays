package org.labkey.flow.controllers.protocol;

import org.labkey.api.exp.api.ExpDataTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.ViewForm;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.query.FlowPropertySet;
import org.labkey.flow.query.FlowSchema;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProtocolForm extends ViewForm
{
    private FlowProtocol _protocol;

    public FlowProtocol getProtocol() throws UnauthorizedException
    {
        if (_protocol != null)
            return _protocol;
        _protocol = FlowProtocol.fromURL(getUser(), getContext().getActionURL(), getRequest());
        return _protocol;
    }

    public Map<FieldKey, String> getKeywordFieldMap()
    {
        Map<FieldKey, String> options = new LinkedHashMap();
        options.put(FieldKey.fromParts("Name"), "FCS file name");
        options.put(FieldKey.fromParts("Run", "Name"), "Run name");
        FlowSchema schema = new FlowSchema(getUser(), getContainer());
        ExpDataTable table = schema.createFCSFileTable(null);
        FlowPropertySet fps = new FlowPropertySet(table);
        FieldKey keyKeyword = FieldKey.fromParts("Keyword");
        for (String keyword : fps.getKeywordProperties().keySet())
        {
            options.put(new FieldKey(keyKeyword, keyword), keyword);
        }
        return options;
    }
}
