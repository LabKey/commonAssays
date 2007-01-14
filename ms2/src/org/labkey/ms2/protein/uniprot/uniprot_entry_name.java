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

/**
 * Created by IntelliJ IDEA.
 * User: tholzman
 * Date: Feb 28, 2005
 * Time: 9:34:42 AM
 * To change this template use File | Settings | File Templates.
 */

import java.util.*;
import java.sql.*;

import org.labkey.ms2.protein.*;

public class uniprot_entry_name extends ParseActions
{

    public boolean endElement(Connection c, Map<String,ParseActions> tables)
    {
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0) return true;
        try
        {
            this.clearCurItems();
            Map curSeq = (Map) ((ParseActions) tables.get("ProtSequences")).getCurItem();
            if (curSeq == null) return false;
            Vector idents = (Vector) (((ParseActions) tables.get("ProtIdentifiers")).getCurItem().get("Identifiers"));
            idents.add(this.getCurItem());
            this.getCurItem().put("identType", "SwissProt");
            this.getCurItem().put("identifier", accumulated);
            this.getCurItem().put("sequence", curSeq);
            curSeq.put("best_name", accumulated);
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
