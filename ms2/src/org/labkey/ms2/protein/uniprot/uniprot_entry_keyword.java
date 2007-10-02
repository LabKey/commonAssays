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

public class uniprot_entry_keyword extends CharactersParseActions
{

    public void beginElement(Connection c, Map<String,ParseActions> tables, Attributes attrs) throws SAXException
    {
        _accumulated = null;
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0)
        {
            return;
        }

        String kwid = attrs.getValue("id");
        if (kwid == null)
        {
            throw new SAXException("id is not set");
        }
        _accumulated = "";
        clearCurItems();
        Vector idents = (Vector) (((ParseActions) tables.get("ProtIdentifiers")).getCurItem().get("Identifiers"));
        idents.add(getCurItem());
        getCurItem().put("identType", "Uniprot_keyword");
        getCurItem().put("identifier", kwid);
        getCurItem().put("sequence", tables.get("ProtSequences").getCurItem());
    }

    public void endElement(Connection c, Map<String,ParseActions> tables)
    {
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0)
        {
            return;
        }

        Vector annots = (Vector) (((ParseActions) tables.get("ProtAnnotations")).getCurItem().get("Annotations"));
        annots.add(getCurItem());
        getCurItem().put("annot_val", _accumulated);
        getCurItem().put("annotType", "keyword");
    }
}
