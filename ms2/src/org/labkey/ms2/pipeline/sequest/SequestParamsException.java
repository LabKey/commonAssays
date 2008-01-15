package org.labkey.ms2.pipeline.sequest;

/**
 * User: billnelson@uky.edu
 * Date: Sep 8, 2006
 * Time: 12:21:56 PM
 */
public class SequestParamsException extends Exception
{

    public SequestParamsException(String message)
    {
        super(message);
    }

    public SequestParamsException(Exception e)
    {
        super(e);
    }

    public SequestParamsException(String message, Exception e)
    {
        super(message, e);
    }
}
