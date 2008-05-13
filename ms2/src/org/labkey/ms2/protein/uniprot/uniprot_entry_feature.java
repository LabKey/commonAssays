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

import org.xml.sax.*;
import org.labkey.ms2.protein.*;

public class uniprot_entry_feature extends ParseActions
{

    public void beginElement(ParseContext context, Attributes attrs) throws SAXException
    {
        if (context.isIgnorable())
        {
            return;
        }
        String annotVal = "";
        String attrType = attrs.getValue("type");
        String attrDesc = attrs.getValue("description");
        String attrStatus = attrs.getValue("status");

        if (attrType != null) annotVal += attrType + " ";
        if (attrDesc != null) annotVal += attrDesc + " ";
        if (attrStatus != null) annotVal += attrStatus + " ";

        UniprotSequence curSeq = context.getCurrentSequence();
        if (curSeq == null)
        {
            throw new SAXException("ProtSequences has no current item");
        }
        UniprotAnnotation annot = new UniprotAnnotation(annotVal.trim(), "feature", curSeq);
        context.getAnnotations().add(annot);
    }
}
