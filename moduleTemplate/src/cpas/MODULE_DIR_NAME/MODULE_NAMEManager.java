package cpas.@@MODULE_LOWERCASE_NAME@@;

public class @@MODULE_NAME@@Manager
{
    private static @@MODULE_NAME@@Manager _instance;

    private @@MODULE_NAME@@Manager()
    {
        // prevent external construction with a private default constructor
    }

    public static synchronized @@MODULE_NAME@@Manager get()
    {
        if (_instance == null)
            _instance = new @@MODULE_NAME@@Manager();
        return _instance;
    }
}