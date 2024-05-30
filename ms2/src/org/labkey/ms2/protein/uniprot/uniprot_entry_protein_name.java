/*
 * Copyright (c) 2005-2018 Fred Hutchinson Cancer Research Center
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

import org.labkey.ms2.protein.CharactersParseActions;
import org.labkey.ms2.protein.ParseContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class uniprot_entry_protein_name extends CharactersParseActions
{
    @Override
    public void beginElement(ParseContext context, Attributes attrs)
    {
        if (context.isIgnorable())
        {
            return;
        }

        _accumulated = "";
    }


    @Override
    public void endElement(ParseContext context) throws SAXException
    {
        if (context.isIgnorable())
        {
            return;
        }

        UniprotSequence curSeq = context.getCurrentSequence();
        if (curSeq == null)
        {
            throw new SAXException("Unable to find a current ProtSequences");
        }

        if (curSeq.getDescription() == null)
        {
            curSeq.setDescription(_accumulated);
        }
    }
}
