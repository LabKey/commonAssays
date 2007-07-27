package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashMap;
import java.util.Map;

abstract public class GWTEditingMode implements IsSerializable
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
