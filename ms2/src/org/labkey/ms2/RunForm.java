package org.labkey.ms2;

import org.apache.beehive.netui.pageflow.FormData;
import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * FormData get and set methods may be overwritten by the Form Bean editor.
 */
public class RunForm extends FormData
{
    public int run;
    public int tryptic;
    boolean expanded;
    String grouping;
    public String columns;

    ArrayList<String> errors;

    // Set form default values; will be overwritten by any params included on the url
    public void reset(ActionMapping arg0, HttpServletRequest arg1)
    {
        run = 0;
        expanded = false;
        errors = new ArrayList<String>();
    }

    private int toInt(String s, String field)
    {
        try
        {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e)
        {
            errors.add("Error: " + s + " is not a valid value for " + field + ".");
            return 0;
        }
    }

    public List<String> getErrors()
    {
        return errors;
    }

    public void setExpanded(boolean expanded)
    {
        this.expanded = expanded;
    }

    public boolean getExpanded()
    {
        return this.expanded;
    }

    public void setRun(String run)
    {
        this.run = toInt(run, "Run");
    }

    public String getRun()
    {
        return String.valueOf(this.run);
    }

    public void setTryptic(String tryptic)
    {
        this.tryptic = toInt(tryptic, "Tryptic");
    }

    public String getTryptic()
    {
        return null;
    }

    public void setGrouping(String grouping)
    {
        this.grouping = grouping;
    }

    public String getGrouping()
    {
        return grouping;
    }

    public String getColumns()
    {
        return columns;
    }

    public void setColumns(String columns)
    {
        this.columns = columns;
    }
}
