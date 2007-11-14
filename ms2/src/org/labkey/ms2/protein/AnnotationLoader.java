package org.labkey.ms2.protein;

import java.io.IOException;

/**
 * User: brittp
 * Date: Dec 21, 2005
 * Time: 2:20:30 PM
 */
public interface AnnotationLoader
{
    public class KillRequestedException extends RuntimeException
    {
    }

    public static enum Status
    {
        UNKNOWN,RUNNING,PAUSED,INCOMPLETE,COMPLETE,DYING,KILLED
    }

    void parseFile() throws Exception;

    void setRecoveryId(int id);

    void requestThreadState(Status state);

    Status getRequestedThreadState();

    void cleanUp();

    String getParseFName();

    int getId();

    boolean isPaused();

    void validate() throws IOException;
}
