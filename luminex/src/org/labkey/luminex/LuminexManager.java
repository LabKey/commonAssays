package org.labkey.luminex;

public class LuminexManager
{
    private static LuminexManager _instance;

    private LuminexManager()
    {
        // prevent external construction with a private default constructor
    }

    public static synchronized LuminexManager get()
    {
        if (_instance == null)
            _instance = new LuminexManager();
        return _instance;
    }
}