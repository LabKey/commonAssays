package org.labkey.ms2.pipeline.client;

/**
 * User: billnelson@uky.edu
 * Date: Apr 26, 2008
 */

/**
 * <code>EnzymeParseException</code>
 */
public class EnzymeParseException extends RuntimeException
{

    public EnzymeParseException()
    {
        super();
    }

    public EnzymeParseException(String s)
    {
        super(s);
    }

    public EnzymeParseException(String s, Throwable throwable)
    {
        super(s, throwable);
    }

    public EnzymeParseException(Throwable throwable)
    {
        super(throwable);
    }
}
