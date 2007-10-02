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
import org.labkey.api.util.HashHelpers;

public class uniprot_entry_sequence extends CharactersParseActions
{

    public void beginElement(Connection c, Map<String,ParseActions> tables, Attributes attrs) throws SAXException
    {
        _accumulated = null;
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0)
        {
            return;
        }

        _accumulated = "";
        Map curSeq = tables.get("ProtSequences").getCurItem();
        if (curSeq == null)
        {
            throw new SAXException("ProtSequences was not set");
        }
        String curHash = attrs.getValue("checksum");
        if (curHash != null)
        {
            curSeq.put("hash", curHash);
        }
        String curSCD = attrs.getValue("modified");
        if (curSCD != null)
        {
            curSeq.put("source_change_date", curSCD);
        }
        String curMass = attrs.getValue("mass");
        if (curMass != null)
        {
            curSeq.put("mass", curMass);
        }
        String curLength = attrs.getValue("length");
        if (curLength != null)
        {
            curSeq.put("length", curLength);
        }
    }

    public void endElement(Connection c, Map<String,ParseActions> tables) throws SAXException
    {
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0)
        {
            return;
        }

        ParseActions p = tables.get("ProtSequences");
        Map curSeq = p.getCurItem();
        if (curSeq == null)
        {
            throw new SAXException("Unable to find a current sequence");
        }
        curSeq.put("ProtSequence", _accumulated.replaceAll("\\s", ""));
        // re-do hash of sequence
        String newHash = HashHelpers.hash((String) curSeq.get("ProtSequence"));
        curSeq.put("hash", newHash);
    }
}