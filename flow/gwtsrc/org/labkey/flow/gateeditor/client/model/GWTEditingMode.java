/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

abstract public class GWTEditingMode implements IsSerializable, Serializable
{
    abstract public String name();
    abstract public int getActionSequence();
    abstract public boolean isRunMode();
    abstract public boolean isCompensation();
    static public class Compensation extends GWTEditingMode
    {
        public String name()
        {
            return "compensation";
        }
        public int getActionSequence()
        {
            return 20;
        }

        public boolean isRunMode()
        {
            return false;
        }
        public boolean isCompensation()
        {
            return true;
        }
    }
    static public class Analysis extends GWTEditingMode
    {
        public String name()
        {
            return "analysis";
        }
        public int getActionSequence()
        {
            return 30;
        }
        public boolean isRunMode()
        {
            return false;
        }
        public boolean isCompensation()
        {
            return false;
        }
    }
    static public class Run extends GWTEditingMode
    {
        public String name()
        {
            return "run";
        }
        public int getActionSequence()
        {
            return 30;
        }

        public boolean isRunMode()
        {
            return true;
        }
        public boolean isCompensation()
        {
            return false;
        }
    }

    static public final GWTEditingMode run = new Run();
    static public final GWTEditingMode compensation = new Compensation();
    static public final GWTEditingMode analysis = new Analysis();
    static Map s_map = new HashMap();
    static
    {
        s_map.put(run.name(), run);
        s_map.put(compensation.name(), compensation);
        s_map.put(analysis.name(), analysis);

    }
    static public GWTEditingMode valueOf(String name)
    {
        return (GWTEditingMode) s_map.get(name);
    }

    public String toString()
    {
        return name();
    }

    public GWTScriptComponent getScriptComponent(GWTScript script)
    {
        if (getActionSequence() == 20)
        {
            return script.getCompensationCalculation();
        }
        return script.getAnalysis();
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof GWTEditingMode))
            return false;
        return ((GWTEditingMode) other).name().equals(name());
    }

    public int hashCode()
    {
        return name().hashCode();
    }
}
