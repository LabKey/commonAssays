/*
 * Copyright (c) 2005-2008 Fred Hutchinson Cancer Research Center
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

import java.sql.*;

import org.xml.sax.*;
import org.labkey.ms2.protein.*;
import org.labkey.api.util.HashHelpers;
import org.labkey.api.util.DateUtil;

public class uniprot_entry_sequence extends CharactersParseActions
{

    public void beginElement(ParseContext context, Attributes attrs) throws SAXException
    {
        if (context.isIgnorable())
        {
            return;
        }

        _accumulated = "";
        UniprotSequence curSeq = context.getCurrentSequence();
        if (curSeq == null)
        {
            throw new SAXException("ProtSequences was not set");
        }
        String curSCD = attrs.getValue("modified");
        if (curSCD != null)
        {
            curSeq.setSourceChangeDate(new Timestamp(DateUtil.parseDateTime(curSCD)));
        }
        String curMass = attrs.getValue("mass");
        if (curMass != null)
        {
            curSeq.setMass(new Float(curMass));
        }
        String curLength = attrs.getValue("length");
        if (curLength != null)
        {
            curSeq.setLength(new Integer(curLength));
        }
    }

    public void endElement(ParseContext context) throws SAXException
    {
        if (context.isIgnorable())
        {
            return;
        }

        UniprotSequence curSeq = context.getCurrentSequence();
        if (curSeq == null)
        {
            throw new SAXException("Unable to find a current sequence");
        }
        String sequence = _accumulated.replaceAll("\\s", "");
        curSeq.setProtSequence(sequence);
        String newHash = HashHelpers.hash(sequence);
        curSeq.setHash(newHash);
    }
}