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

import org.apache.log4j.Logger;
import org.labkey.ms2.protein.ParseActions;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.Vector;

/**
 * User: tholzman
 * Date: Apr 4, 2005
 * Time: 11:58:19 AM
 */
public class uniprot_entry extends ParseActions
{

    private static Logger _log = Logger.getLogger(uniprot_entry.class);

    public static final int REPORT_MOD = 50;

    public int _mouthful = 5000;

    public int getMouthful()
    {
        return _mouthful;
    }

    public void setMouthful(int i)
    {
        _mouthful = i;
    }

    public boolean _verbose = false;

    public boolean isVerbose()
    {
        return _verbose;
    }

    public void setVerbose(boolean verbose)
    {
        _verbose = verbose;
    }

    private int _skipTracer = 0;

    public void beginElement(Connection c, Map<String,ParseActions> tables, Attributes attrs) throws SAXException
    {
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0)
        {
            return;
        }
        
        clearCurItems();
        tables.put("ProtSequences", this);

        if (_verbose)
        {
            if (getItemCount() % REPORT_MOD == 0 && getItemCount() > 0)
            {
                System.out.println(getItemCount());
            }
            else
            {
                System.out.print(".");
            }
        }
        if (attrs.getValue("dataset") != null)
            getCurItem().put("source", attrs.getValue("dataset"));
        if (attrs.getValue("created") != null)
            getCurItem().put("source_insert_date", attrs.getValue("created"));
        if (attrs.getValue("modified") != null)
            getCurItem().put("source_change_date", attrs.getValue("modified"));
    }

    public void endElement(Connection c, Map<String,ParseActions> tables) throws SAXException
    {
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (!root.unBumpSkip())
        {
            _skipTracer++;
            if (_verbose)
            {
                if ((_skipTracer % 1000) == 0)
                    _log.info((new Date()) + " 1000 <entry> elements skipped, " + root.getSkipEntries() + " more to go");
            }
            return;
        }

        String uniqKey =
                ((String) getCurItem().get("genus")).toUpperCase() +
                        " " +
                        ((String) getCurItem().get("species")).toUpperCase() +
                        " " +
                        ((String) getCurItem().get("hash"));
        getAllItems().put(uniqKey, getCurItem());
        setItemCount(getItemCount() + 1);
        if (getAllItems().size() >= _mouthful)
        {
            try
            {
                root.insertTables(tables, c);
            }
            catch (SQLException e)
            {
                throw new SAXException(e);
            }
            getAllItems().clear();
            tables.get("Organism").getAllItems().clear();
            ((Vector)tables.get("ProtIdentifiers").getCurItem().get("Identifiers")).clear();
            ((Vector)tables.get("ProtAnnotations").getCurItem().get("Annotations")).clear();
        }
    }
}

