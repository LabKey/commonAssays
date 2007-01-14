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

public class uniprot_entry_keyword extends ParseActions
{

    private String kwid;

    public boolean beginElement(Connection c, Map<String,ParseActions> tables, Attributes attrs)
    {
        accumulated = null;
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0) return true;
        try
        {
            kwid = attrs.getValue("id");
            if (kwid == null) return false;
            accumulated = new String("");
            this.clearCurItems();
            Vector idents = (Vector) (((ParseActions) tables.get("ProtIdentifiers")).getCurItem().get("Identifiers"));
            idents.add(this.getCurItem());
            this.getCurItem().put("identType", "Uniprot_keyword");
            this.getCurItem().put("identifier", kwid);
            this.getCurItem().put("sequence", ((ParseActions) tables.get("ProtSequences")).getCurItem());
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
            Vector annots = (Vector) (((ParseActions) tables.get("ProtAnnotations")).getCurItem().get("Annotations"));
            annots.add(this.getCurItem());
            this.getCurItem().put("annot_val", accumulated);
            this.getCurItem().put("annotType", "keyword");
        }
        catch (Exception e)
        {
            return false;
        }
        return true;
    }

    public boolean characters(Connection c, Map<String,ParseActions> tables, char ch[], int start, int len)
    {
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0) return true;
        accumulated += new String(ch, start, len);
        return true;
    }

}
