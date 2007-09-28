package org.labkey.ms1.pipeline;

import org.labkey.api.pipeline.PipelineProvider;

import java.io.FileFilter;
import java.io.File;

/**
 * User: jeckels
* Date: Nov 3, 2006
 * This filters the files that the module knows how to import
 * as completed analysis files.
*/
public class MS1FileFilter extends PipelineProvider.FileEntryFilter
{
    public boolean accept(File f)
    {
        return f.getName().toLowerCase().endsWith(".rtfeatures.tsv");
    }
}
