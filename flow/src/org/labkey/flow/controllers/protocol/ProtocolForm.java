package org.labkey.flow.controllers.protocol;

import org.labkey.api.view.ViewForm;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.TableKey;
import org.labkey.api.exp.api.ExpDataTable;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.query.FlowPropertySet;
import org.labkey.flow.query.FlowSchema;
import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import java.util.Map;
import java.util.LinkedHashMap;

public class ProtocolForm extends ViewForm
{
    private FlowProtocol _protocol;

    public void reset(ActionMapping actionMapping, HttpServletRequest request)
    {
        super.reset(actionMapping, request);
    }

    public FlowProtocol getProtocol() throws ServletException
    {
        if (_protocol != null)
            return _protocol;
        _protocol = FlowProtocol.fromURL(getUser(), getContext().getViewURLHelper(), getRequest());
        return _protocol;
    }

    public Map<FieldKey, String> getKeywordFieldMap()
    {
        Map<FieldKey, String> options = new LinkedHashMap();
        options.put(new FieldKey(null, "Name"), "FCS file name");
        FlowSchema schema = new FlowSchema(getUser(), getContainer());
        ExpDataTable table = schema.createFCSFileTable(null);
        FlowPropertySet fps = new FlowPropertySet(table);
        TableKey keyKeyword = new TableKey(null, "Keyword");
        for (String keyword : fps.getKeywordProperties().keySet())
        {
            options.put(new FieldKey(keyKeyword, keyword), keyword);
        }
        return options;
    }
}
