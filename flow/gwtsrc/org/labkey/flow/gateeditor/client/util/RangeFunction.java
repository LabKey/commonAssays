package org.labkey.flow.gateeditor.client.util;

/**
 * Created by IntelliJ IDEA.
 * User: matthewb
 * Date: Sep 13, 2007
 * Time: 2:35:11 PM
 */
public interface RangeFunction
{
    double compute(double range);
    double invert(double domain);
}
