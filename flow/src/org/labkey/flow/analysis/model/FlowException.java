package org.labkey.flow.analysis.model;

public class FlowException extends RuntimeException
{
    public FlowException(String message)
    {
        super(message);
    }
    public FlowException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
