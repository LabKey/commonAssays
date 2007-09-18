package org.labkey.flow.analysis.model;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: matthewb
 * Date: Sep 18, 2007
 * Time: 2:03:40 PM
 *
 * Defines a 1 or 2 region
 */
public abstract class RegionGate extends Gate
{
    public abstract String getXAxis();
    public abstract String getYAxis();
}
