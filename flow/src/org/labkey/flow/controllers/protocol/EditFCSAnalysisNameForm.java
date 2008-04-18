package org.labkey.flow.controllers.protocol;

import org.apache.commons.lang.StringUtils;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.flow.data.FieldSubstitution;
import org.labkey.flow.data.FlowProtocol;

import java.util.ArrayList;
import java.util.List;

public class EditFCSAnalysisNameForm extends ProtocolForm
{
    static public final String SEPARATOR = "-";
    public String ff_rawString;
    public FieldKey[] ff_keyword;

    public void init() throws UnauthorizedException
    {
        FieldSubstitution fs = getProtocol().getFCSAnalysisNameExpr();
        if (fs != null)
        {
            setFieldSubstitution(fs);
        }
        else
        {
            setFieldSubstitution(FlowProtocol.getDefaultFCSAnalysisNameExpr());
        }
    }

    public void setFf_rawString(String s)
    {
        setFieldSubstitution(FieldSubstitution.fromString(s));
    }

    public void setFf_keyword(String[] keyword)
    {
        List<Object> parts = new ArrayList();
        for (int i = 0; i < keyword.length; i ++)
        {
            if (!StringUtils.isEmpty(keyword[i]))
            {
                if (parts.size() > 0)
                {
                    parts.add(SEPARATOR);
                }
                parts.add(FieldKey.fromString(keyword[i]));
            }
        }
        setFieldSubstitution(new FieldSubstitution(parts.toArray()));
    }

    public FieldSubstitution getFieldSubstitution()
    {
        return FieldSubstitution.fromString(ff_rawString);
    }

    public void setFieldSubstitution(FieldSubstitution fs)
    {
        ff_rawString = fs.toString();
        ff_keyword = fs.getFields(SEPARATOR);
    }
}
