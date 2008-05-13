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

import org.labkey.ms2.protein.ParseActions;
import org.labkey.ms2.protein.ParseContext;
import org.labkey.api.util.DateUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * User: tholzman
 * Date: Apr 4, 2005
 * Time: 11:58:19 AM
 */
public class uniprot_entry extends ParseActions
{
    public static final int BATCH_SIZE = 5000;

    public void beginElement(ParseContext context, Attributes attrs) throws SAXException
    {
        if (context.isIgnorable())
        {
            return;
        }
        
        UniprotSequence curSeq = new UniprotSequence();
        context.setCurrentSequence(curSeq);

        if (attrs.getValue("dataset") != null)
            curSeq.setSource("dataset");
        if (attrs.getValue("created") != null)
            curSeq.setSourceInsertDate(new Timestamp(DateUtil.parseDateTime(attrs.getValue("created"))));
        if (attrs.getValue("modified") != null)
            curSeq.setSourceChangeDate(new Timestamp(DateUtil.parseDateTime(attrs.getValue("modified"))));
    }

    public void endElement(ParseContext context) throws SAXException
    {
        if (!context.unBumpSkip())
        {
            return;
        }

        context.addCurrentSequence();
        if (context.getSequences().size() >= BATCH_SIZE)
        {
            try
            {
                context.insert();
            }
            catch (SQLException e)
            {
                throw new SAXException(e);
            }
            context.clear();
        }
    }
}

