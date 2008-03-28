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

    public String getMessage()
    {
        StringBuffer sb = new StringBuffer();
        String concat = "";
        for (String msg : _messages)
        {
            sb.append(msg);
            sb.append(concat);
            concat = "\n";
        }
        return sb.toString();
    }
}
