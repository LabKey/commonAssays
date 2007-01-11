package Flow.Well;

import org.fhcrc.cpas.flow.data.FlowWell;
import org.labkey.api.view.ViewForm;

import java.util.Map;

public class EditWellForm extends ViewForm
{
    private FlowWell _well;
    public String ff_name;
    public String[] ff_keywordName;
    public String[] ff_keywordValue;
    public String ff_comment;

    public void setWell(FlowWell well) throws Exception
    {
        _well = well;
        if (ff_keywordName == null)
        {
            Map.Entry<String, String>[] entries = well.getKeywords().entrySet().toArray(new Map.Entry[0]);
            ff_keywordName = new String[entries.length];
            ff_keywordValue = new String[entries.length];
            for (int i = 0; i < entries.length; i ++)
            {
                ff_keywordName[i] = entries[i].getKey();
                ff_keywordValue[i] = entries[i].getValue();
            }
        }
        if (ff_comment == null)
        {
            ff_comment = well.getComment();
        }
        if (ff_name == null)
        {
            ff_name = well.getName();
        }
    }

    public FlowWell getWell()
    {
        return _well;
    }

    public void setFf_comment(String comment)
    {
        ff_comment = comment == null ? "" : comment;
    }
    public void setFf_name(String name)
    {
        ff_name = name == null ? "" : name;
    }

    public void setFf_keywordName(String[] names)
    {
        ff_keywordName = names;
    }

    public void setFf_keywordValue(String[] values)
    {
        ff_keywordValue = values;
    }
}
