package org.labkey.ms2.protein.tools;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.protein.ProteinSchema;

public class GoHelpers
{
    private static String getGODefinitionFromId(int id)
    {
        return new SqlSelector(ProteinSchema.getSchema(), "SELECT TermDefinition FROM " + ProteinSchema.getTableInfoGoTermDefinition() + " WHERE TermId=?", id).getObject(String.class);
    }

    public static String getGODefinitionFromAcc(String acc)
    {
        return getGODefinitionFromId(getGOIdFromAcc(acc));
    }

    public static int getGOIdFromAcc(String acc)
    {
        Integer goId = new SqlSelector(ProteinSchema.getSchema(), "SELECT Id FROM " + ProteinSchema.getTableInfoGoTerm() + " WHERE Acc = ?", acc).getObject(Integer.class);

        return (null == goId ? 0 : goId);
    }

    public enum GoTypes
    {
        CELL_LOCATION {
            public String toString()
            {
                return "Cellular Location";
            }
        },
        FUNCTION {
            public String toString()
            {
                return "Molecular Function";
            }
        },
        PROCESS {
            public String toString()
            {
                return "Metabolic Process";
            }
        },
        ALL {
            public String toString()
            {
                return "All GO Ontologies";
            }
        }
    }

    @Nullable
    public static GoTypes GTypeStringToEnum(String label)
    {
        for (GoTypes g : GoTypes.values())
        {
            if (g.toString().equals(label)) return g;
        }
        return null;
    }

    public static int gTypeC = 0;
    public static int gTypeF = 0;
    public static int gTypeP = 0;

    public static int getgTypeC()
    {
        if (gTypeC == 0) getGOTypes();
        return gTypeC;
    }

    public static int getgTypeF()
    {
        if (gTypeF == 0) getGOTypes();
        return gTypeF;
    }

    public static int getgTypeP()
    {
        if (gTypeP == 0) getGOTypes();
        return gTypeP;
    }

    private static final Object _lock = new Object();

    protected static void getGOTypes()
    {
        synchronized (_lock)
        {
            if (gTypeC == 0 || gTypeF == 0 || gTypeP == 0)
            {
                new SqlSelector(ProteinSchema.getSchema(), "SELECT annottypeid,name FROM " + ProteinSchema.getTableInfoAnnotationTypes() + " WHERE name in ('GO_C','GO_F','GO_P')").forEach(rs -> {
                    int antypeid = rs.getInt(1);
                    String gt = rs.getString(2);
                    if (gt.equals("GO_C"))
                    {
                        gTypeC = antypeid;
                    }
                    if (gt.equals("GO_F"))
                    {
                        gTypeF = antypeid;
                    }
                    if (gt.equals("GO_P"))
                    {
                        gTypeP = antypeid;
                    }
                });
            }
        }
    }

    public static String getAnnotTypeWhereClause(GoTypes kind)
    {
        if (kind == null)
        {
            kind = GoTypes.ALL;
        }
        switch (kind)
        {
            case CELL_LOCATION:
                return "AnnotTypeId=" + getgTypeC();
            case FUNCTION:
                return "AnnotTypeId=" + getgTypeF();
            case PROCESS:
                return "AnnotTypeId=" + getgTypeP();
            case ALL:
                return "AnnotTypeId IN (" + getgTypeC() + "," + getgTypeF() + "," + getgTypeC() + ")";
        }

        return null;
    }
}
