/*
 * Copyright (c) 2008-2016 LabKey Corporation
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

package org.labkey.ms2.pipeline.client;

/**
 * User: billnelson@uky.edu
 * Date: Apr 24, 2008
 */

/**
 * <code>Enzyme</code>
 */
public class Enzyme
{
    private String displayName;
    private String[] names;
    private CutSite[] cutSite;

    public Enzyme(String displayName, String[] names, CutSite[] cutSite)
    {
        this.displayName = displayName;
        this.names = names;
        this.cutSite = cutSite;
    }

    public Enzyme(String signature)
    {
        String sigSites[] = signature.split(",");
        int numSites = sigSites.length;
        cutSite = new CutSite[numSites];
        for(int i = 0; i < numSites; i++)
        {
            cutSite[i] = new CutSite(sigSites[i]);
        }
     }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public String[] getNames()
    {
        return names;
    }

    public void setNames(String[] names)
    {
        this.names = names;
    }

    public CutSite[] getCutSite()
    {
        return cutSite;
    }

    public void setCutSite(CutSite[] cutSite)
    {
        this.cutSite = cutSite;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof Enzyme))
        {
            return false;
        }

        Enzyme enzyme = (Enzyme) o;

        if(cutSite.length != enzyme.cutSite.length) return false;
        for (CutSite aCutSite : cutSite)
        {
            boolean same = false;
            for (int y = 0; y < cutSite.length; y++)
            {
                if (aCutSite.equals(enzyme.cutSite[y]))
                {
                    same = true;
                    break;
                }
            }
            if (!same) return false;
        }
        if(displayName != null)
           if(displayName.equalsIgnoreCase(enzyme.displayName)) return true;

        if(names == enzyme.names)return true;
        if(names != null && enzyme.names != null)
        {
            for (String name : names)
            {
                for (int y = 0; y < enzyme.names.length; y++)
                {
                    if (name.equalsIgnoreCase(enzyme.names[y]))
                    {
                        return true;
                    }
                }
            }
            return false;
        }
        else
        {
            if(enzyme.names != null)
            {
                for(int i = 0; i < enzyme.names.length; i++)
                {
                    if(displayName.equalsIgnoreCase(enzyme.names[i]))
                    {
                        return true;
                    }
                }
                return false;

            }
        }
        return true;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < cutSite.length; i++)
        {
            if(i < 0) sb.append(", ");
            sb.append(cutSite[i].getSignature());
        }
        return sb.toString();
    }
}