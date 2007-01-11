package cpas.ms2.compare;

/**
 * User: jeckels
 * Date: Oct 6, 2006
 */
public class RunColumn
{
    private final String _label;
    private final String _name;
    private final String _aggregate;

    public RunColumn(String label, String name, String aggregate)
    {
        _label = label;
        _name = name;
        _aggregate = aggregate;
    }
    
    public String getLabel()
    {
        return _label;
    }

    public String getName()
    {
        return _name;
    }

    public String getAggregate()
    {
        return _aggregate;
    }
}
