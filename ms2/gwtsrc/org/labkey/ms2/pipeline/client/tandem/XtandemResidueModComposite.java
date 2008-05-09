package org.labkey.ms2.pipeline.client.tandem;

import org.labkey.ms2.pipeline.client.ResidueModComposite;
import org.labkey.ms2.pipeline.client.Search;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;

/**
 * User: billnelson@uky.edu
 * Date: Apr 30, 2008
 */

/**
 * <code>XtandemResidueModComposite</code>
 */
public class XtandemResidueModComposite extends ResidueModComposite
{
    public XtandemResidueModComposite(Search searchForm)
    {
        super();
        this.searchForm = searchForm;
        init();
    }

    public void init()
    {
        super.init();
        SimplePanel mod0WrapperPanel = new SimplePanel();
        SimplePanel mod1WrapperPanel = new SimplePanel();
        mod0WrapperPanel.add(mod0ListBox);
        mod1WrapperPanel.add(mod1ListBox);
        FlexTable.FlexCellFormatter formatter = modFlexTable.getFlexCellFormatter();
        formatter.setRowSpan(0, 0, 2);
        formatter.setRowSpan(0, 2, 2);
        modsDeckPanel.add(mod0WrapperPanel);
        modsDeckPanel.add(mod1WrapperPanel);
        modFlexTable.setWidget(0, 0, modsDeckPanel);
        modFlexTable.setWidget(0, 2, modTabPanel);
        modFlexTable.setWidget(0, 1, addButton);
        modFlexTable.setWidget(1, 0, newButton);
        modsDeckPanel.showWidget(0);
        instance.setWidget(modFlexTable);
    }

    public void update(Map mod0Map, Map mod1Map)
    {
        setListBoxMods(mod0Map, mod0ListBox);
        setListBoxMods(mod1Map, mod1ListBox);
    }

    public Map getModMap(int modType)
    {
        if(modType == STATIC)
            return getListBoxMap(mod0ListBox);
        else if(modType == DYNAMIC)
            return getListBoxMap(mod1ListBox);
        return null;
    }

    protected String validate(ListBox box, int modType)
    {
        Map modMap = getListBoxMap(box);
        ListBox defaultModListBox;
        if(modType == STATIC) defaultModListBox = mod0ListBox;
        else defaultModListBox = mod1ListBox;
        Set keys = modMap.keySet();

        for(Iterator it = keys.iterator(); it.hasNext();)
        {
            String modName = (String)it.next();
            if(find(modName, defaultModListBox) != -1) continue;
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
