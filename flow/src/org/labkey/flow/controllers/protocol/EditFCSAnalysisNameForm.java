package org.labkey.flow.controllers.protocol;

import org.labkey.flow.data.FieldSubstitution;
import org.labkey.api.query.FieldKey;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.ArrayList;

public class EditFCSAnalysisNameForm extends ProtocolForm
{
    static public final String SEPARATOR = "-";
    public String ff_rawString;
    public FieldKey[] ff_keyword;

    public void reset(ActionMapping actionMapping, HttpServletRequest request)
    {
        super.reset(actionMapping, request);
        try
        {
            setFieldSubstitution(getProtocol().getFCSAnalysisNameExpr());
        }
        catch (ServletException e)
        {
            ff_rawString = "#Error#";
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
