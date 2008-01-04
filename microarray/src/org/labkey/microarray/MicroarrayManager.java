package org.labkey.microarray;

public class MicroarrayManager
{
    private static MicroarrayManager _instance;

    private MicroarrayManager()
    {
        // prevent external construction with a private default constructor
    }

    public static synchronized MicroarrayManager get()
    {
        if (_instance == null)
            _instance = new MicroarrayManager();
        return _instance;
    }
}