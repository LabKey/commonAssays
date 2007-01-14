package org.labkey.ms2.protein.organism;

/**
 * User: arauch
 * Date: Jan 11, 2006
 * Time: 9:38:57 PM
 */
abstract public class Timer
{
    long _elapsedTime = 0;
    long _startTime;

    void resetTimer()
    {
        _elapsedTime = 0;
    }

    public float getElapsedTime()
    {
        return _elapsedTime / 1000;
    }

    void startTimer()
    {
        _startTime = System.currentTimeMillis();
    }

    void stopTimer()
    {
        _elapsedTime += System.currentTimeMillis() - _startTime;
    }
}
