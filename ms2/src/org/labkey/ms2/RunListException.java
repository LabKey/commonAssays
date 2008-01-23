package org.labkey.ms2;

import java.util.List;
import java.util.Collections;

/**
 * User: jeckels
* Date: Jan 22, 2008
*/
public class RunListException extends Exception
{
    private List<String> _messages;

    public RunListException(String message)
    {
        this(Collections.singletonList(message));
    }

    public RunListException(List<String> messages)
    {
        _messages = messages;
    }

    public List<String> getMessages()
    {
        return _messages;
    }
}
