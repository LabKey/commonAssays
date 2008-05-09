package org.labkey.ms2.pipeline.client;

/**
 * User: billnelson@uky.edu
 * Date: Apr 21, 2008
 */

/**
 * <code>SearchFormException</code>
 */
public class SearchFormException extends Exception
{
    public SearchFormException()
    {
    }

    public SearchFormException(String s)
    {
        super(s);
    }

    public SearchFormException(String s, Throwable throwable)
    {
        super(s, throwable);
    }

    public SearchFormException(Throwable throwable)
    {
        super(throwable);
    }
}
