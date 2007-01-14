/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.ms2.protein.uniprot;

import java.util.*;
import java.sql.*;

import org.xml.sax.*;
import org.labkey.ms2.protein.*;

public class uniprot_entry_organism_name extends ParseActions
{

    private String curType = null;

    public boolean beginElement(Connection c, Map<String,ParseActions> tables, Attributes attrs)
    {
        accumulated = null;
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0) return true;
        try
        {
            curType = attrs.getValue("type");
            this.clearCurItems();
            if (curType == null) return false;
            accumulated = "";
        }
        catch (Exception e)
        {
            return false;
        }
        return true;
    }

    public boolean endElement(Connection c, Map<String,ParseActions> tables)
    {
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0) return true;
        try
        {
            ParseActions u = tables.get("Organism");
            if (u.getCurItem() == null) return false;
            if (curType.equalsIgnoreCase("common"))
            {
                u.getCurItem().put("common_name", accumulated.trim());
                return true;
            }
            if (curType.equalsIgnoreCase("scientific") || curType.equalsIgnoreCase("full"))
            {
                String together = accumulated.trim();
                String separate[] = together.split(" ");
                if (separate == null || separate.length < 2)
                {
                    XMLProteinHandler.parseWarning("Found organism with this name: '" + together + "'");
                }
                if (separate != null && separate.length >= 1)
                {
                    u.getCurItem().put("genus", separate[0].replaceAll("'", ""));
                }
                if (separate != null && separate.length >= 2)
                {
                    u.getCurItem().put("species", separate[1].replaceAll("'", ""));
                }
                if (separate != null && (curType.equalsIgnoreCase("full") || separate.length > 2))
                {
                    u.getCurItem().put("comments", together);
                }
                Vector annots = (Vector) tables.get("ProtAnnotations").getCurItem().get("Annotations");
                annots.add(this.getCurItem());

                this.getCurItem().put("annot_val", together);
                Map curSeq = tables.get("ProtSequences").getCurItem();
                this.getCurItem().put("sequence", curSeq);
                if (curType.equalsIgnoreCase("full"))
                {
                    this.getCurItem().put("annotType", "FullOrganismName");
                }
                else
                {
                    this.getCurItem().put("annotType", "ScientificOrganismName");
                }

                return true;
            }
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public boolean characters(Connection c, Map<String,ParseActions> tables, char ch[], int start, int len)
    {
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0) return true;
        accumulated += new String(ch, start, len);
        return true;
    }

}
