package org.labkey.ms2.protein;

import org.labkey.ms2.protein.uniprot.uniprot;

import java.sql.Connection;
import java.util.Map;

/**
 * User: jeckels
 * Date: Oct 1, 2007
 */
public class CharactersParseActions extends ParseActions
{
    public void characters(Connection c, Map<String,ParseActions> tables, char ch[], int start, int len)
    {
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0)
        {
            return;
        }
        _accumulated += new String(ch, start, len);
    }
}
