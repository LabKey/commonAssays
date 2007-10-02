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

public class uniprot_entry_organism_name extends CharactersParseActions
{

    private String curType = null;

    public void beginElement(Connection c, Map<String,ParseActions> tables, Attributes attrs) throws SAXException
    {
        _accumulated = null;
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0)
        {
            return;
        }

        curType = attrs.getValue("type");
        clearCurItems();
        if (curType == null)
        {
            throw new SAXException("type is not set");
        }
        _accumulated = "";
    }

    public void endElement(Connection c, Map<String,ParseActions> tables) throws SAXException
    {
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0)
        {
            return;
        }

        ParseActions u = tables.get("Organism");
        if (u.getCurItem() == null)
        {
            throw new SAXException("No current organism");
        }
        if (curType.equalsIgnoreCase("common"))
        {
            u.getCurItem().put("common_name", _accumulated.trim());
            return;
        }
        if (curType.equalsIgnoreCase("scientific") || curType.equalsIgnoreCase("full"))
        {
            String together = _accumulated.trim();
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
            annots.add(getCurItem());

            getCurItem().put("annot_val", together);
            Map curSeq = tables.get("ProtSequences").getCurItem();
            getCurItem().put("sequence", curSeq);
            if (curType.equalsIgnoreCase("full"))
            {
                getCurItem().put("annotType", "FullOrganismName");
            }
            else
            {
                getCurItem().put("annotType", "ScientificOrganismName");
            }
        }
    }
}
