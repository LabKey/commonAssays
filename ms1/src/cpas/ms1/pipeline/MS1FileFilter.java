package cpas.ms1.pipeline;

import java.io.FileFilter;
import java.io.File;

/**
 * User: jeckels
* Date: Nov 3, 2006
 * This filters the files that the module knows how to import
 * as completed analysis files.
*/
public class MS1FileFilter implements FileFilter
{
    public boolean accept(File f)
    {
        return f.getName().toLowerCase().endsWith(".tsv");
    }
}
