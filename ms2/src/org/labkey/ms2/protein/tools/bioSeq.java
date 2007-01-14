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

package org.labkey.ms2.protein.tools;

/**
 * Created by IntelliJ IDEA.
 * User: tholzman
 * Date: Mar 14, 2005
 * Time: 12:07:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class bioSeq
{

    public static final int DEFAULT_FORMAT_WIDTH = 60;

    public bioSeq()
    {
        setName(null);
        setDescription(null);
        setSequence(null);
        setFormatWidth(DEFAULT_FORMAT_WIDTH);
    }

    public bioSeq(String name, String desc, String seq)
    {
        setName(name);
        setDescription(desc);
        setSequence(seq);
        setFormatWidth(DEFAULT_FORMAT_WIDTH);
    }

    protected String name;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    protected String description;

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    protected String sequence;

    public String getSequence()
    {
        return sequence;
    }

    public void setSequence(String sequence)
    {
        this.sequence = sequence;
    }

    protected int formatWidth;

    public int getFormatWidth()
    {
        return formatWidth;
    }

    public void setFormatWidth(int formatWidth)
    {
        this.formatWidth = formatWidth;
    }

    protected String hash;

    public String getHash()
    {
        return hash;
    }

    public void setHash(String hash)
    {
        this.hash = hash;
    }

    public String toFasta(int fw)
    {
        String retVal = ">" + getName() + " " + getDescription() + "\n";
        String tmp = this.getSequence();
        while (tmp.length() != 0)
        {
            retVal += tmp.substring(0, fw) + "\n";
            tmp = tmp.substring(fw);
        }
        return retVal;
    }

    public String toFasta()
    {
        return this.toFasta(getFormatWidth());
    }

    public int length()
    {
        return getSequence().length();
    }
}
