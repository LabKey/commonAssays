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
 * User: tholzman
 * Date: Feb 28, 2005
 */
import java.util.*;
import java.sql.*;

import org.labkey.ms2.protein.*;
import org.xml.sax.SAXException;

public class uniprot_entry_protein_name extends CharactersParseActions
{

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
            throw new SAXException("Unable to find a current ProtSequences");
        }

        if (!curSeq.containsKey("description"))
        {
            curSeq.put("description", _accumulated);
        }
    }
}
