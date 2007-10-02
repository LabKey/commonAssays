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

public class uniprot_entry_gene_name extends CharactersParseActions
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

        String nameType = attrs.getValue("type");
        if (nameType == null)
        {
            throw new SAXException("No type is currently set");
        }
        curType = nameType;
        _accumulated = "";
        clearCurItems();
    }

    public void endElement(Connection c, Map<String,ParseActions> tables) throws SAXException
    {
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0)
        {
            return;
        }

        Map<String, Object> curSeq = tables.get("ProtSequences").getCurItem();
        if (curSeq == null)
        {
            throw new SAXException("No current ProtSequences is available");
        }
        _accumulated = _accumulated.trim();
        if (curType.equalsIgnoreCase("primary") && _accumulated.length() > 0)
        {
            Vector idents = (Vector) tables.get("ProtIdentifiers").getCurItem().get("Identifiers");
            idents.add(getCurItem());
            getCurItem().put("identType", "GeneName");
            getCurItem().put("identifier", _accumulated);
            getCurItem().put("sequence", curSeq);
            curSeq.put("best_name", _accumulated);
            curSeq.put("best_gene_name", _accumulated);
        }
    }
}
