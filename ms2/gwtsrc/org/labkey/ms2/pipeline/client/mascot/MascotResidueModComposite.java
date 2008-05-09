package org.labkey.ms2.pipeline.client.mascot;

import org.labkey.ms2.pipeline.client.ResidueModComposite;
import org.labkey.ms2.pipeline.client.Search;

import java.util.*;

import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.FlexTable;

/**
 * User: billnelson@uky.edu
 * Date: Apr 30, 2008
 */

/**
 * <code>MascotResidueModComposite</code>
 */
public class MascotResidueModComposite extends ResidueModComposite
{
    public MascotResidueModComposite(Search searchForm)
    {
        super();
        this.searchForm = searchForm;
        init();
    }

    public void init()
    {
        super.init();
        SimplePanel mod0WrapperPanel = new SimplePanel();
        mod0WrapperPanel.add(mod0ListBox);
        modsDeckPanel.add(mod0WrapperPanel);
        FlexTable.FlexCellFormatter formatter = modFlexTable.getFlexCellFormatter();
        formatter.setRowSpan(0, 0, 2);
        formatter.setRowSpan(0, 2, 2);
        modFlexTable.setWidget(0, 0, modsDeckPanel);
        modFlexTable.setWidget(0, 2, modTabPanel);
        modFlexTable.setWidget(0, 1, addButton);
        modsDeckPanel.showWidget(0);
        instance.setWidget(modFlexTable);
    }

        public String validate()
    {
        String error = validate(staticListBox);
        if(error.length()> 0) return error;
        error = validate(dynamicListBox);
        return error;
    }

    String validate(ListBox box)
    {
        Map modMap = getListBoxMap(box);
        Set keys = modMap.keySet();

        for(Iterator it = keys.iterator(); it.hasNext();)
        {
            String modName = (String)it.next();
            if(find(modName, mod0ListBox) == -1) return "modification mass contained an invalid value(" + modName + ").";
        }
        return "";
    }

    public Map getModMap(int modType)
    {
        return getListBoxMap(mod0ListBox);
    }

    public void update(Map mod0Map, Map mod1Map)
    {
        setListBoxMods(mod0Map, mod0ListBox);
    }

    protected String validate(ListBox box, int modType)
    {
        Map modMap = getListBoxMap(box);

        Set keys = modMap.keySet();

        for(Iterator it = keys.iterator(); it.hasNext();)
        {
            String modName = (String)it.next();
            if(find(modName, mod0ListBox) != -1) continue;
            if (modName.charAt(modName.length() - 2) != '@' && modName.length() > 3)
            {
                return "modification mass contained an invalid value(" + modName + ").";
            }
            char residue = modName.charAt(modName.length() - 1);
            if (!isValidResidue(residue))
            {
                return "modification mass contained an invalid residue(" + residue + ").";
            }
            String mass = modName.substring(0, modName.length() - 2);
            float massF;

            try
            {
                massF = Float.parseFloat(mass);
            }
            catch (NumberFormatException e)
            {
                return "modification mass contained an invalid mass value (" + mass + ")";
            }

        }
        return "";
    }
}
