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
 * Date: Mar 3, 2005
 * Time: 9:41:21 AM
 * To change this template use File | Settings | File Templates.
 */

import java.util.*;
import java.sql.*;

import org.xml.sax.*;
import org.labkey.ms2.protein.*;

public class uniprot_entry_feature_location_end extends ParseActions
{

    public boolean beginElement(Connection c, Map<String,ParseActions> tables, Attributes attrs)
    {
        accumulated = null;
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0) return true;
        try
        {
            Map surroundingFeature = (Map) ((Vector) (tables.get("ProtAnnotations")).getCurItem().get("Annotations")).lastElement();
            if (surroundingFeature == null) return true;
            String position = attrs.getValue("position");
            if (position != null)
                surroundingFeature.put("end_pos", position);
        }
        catch (Exception e)
        {
            return false;
        }
        return true;
    }

    public boolean characters(Connection c, Map<String,ParseActions> tables, char ch[], int start, int len)
    {
        return true;
    }
}
