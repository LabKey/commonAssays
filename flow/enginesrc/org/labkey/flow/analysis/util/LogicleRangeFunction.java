package org.labkey.flow.analysis.util;

import edu.stanford.facs.transform.Logicle;
import org.junit.Test;
import org.labkey.flow.analysis.model.FlowException;

import static org.junit.Assert.assertEquals;

public class LogicleRangeFunction extends AbstractRangeFunction
{
    private static final double DEFAULT_T = 262144;
    private static final double DEFAULT_W = 0.5;
    private static final double DEFAULT_M = 4.5;
    private static final double DEFAULT_A = 0;

    private final Logicle _logicle;

    public LogicleRangeFunction(double min, double max)
    {
        this(min, max, new Logicle(DEFAULT_T, DEFAULT_W, DEFAULT_M, DEFAULT_A));
    }

    public LogicleRangeFunction(double min, double max, Logicle logicle)
    {
        super(min, max);
        _logicle = logicle;
    }

    @Override
    public boolean isLogarithmic()
    {
        return true;
    }

    @Override
    public double compute(double range)
    {
        try
        {
            // We need to prevent placeholder max values from causing IllegalStateException
            if (Math.abs(range) >= Float.MAX_VALUE)
                return range;
            return _logicle.scale(range);
        }
        catch (IllegalStateException e)
        {
            throw new FlowException(String.format("Error scaling '%f' for Logicle(T=%f, W=%f, M=%f, A=%f): " + e.getMessage(),
                    range, _logicle.T, _logicle.W, _logicle.M, _logicle.A), e);
        }
    }

    @Override
    public double invert(double domain)
    {
        return _logicle.inverse(domain);
    }

    public static class TestCase
    {
        @Test
        public void testLogicle()
        {
            double T = 262144;
            double A = 0;
            double W = 0.5;
            double M = 4.418539922;

            Logicle logicle = new Logicle(T, W, M, A);
            int length = 4096;

            // compute
            assertEquals(-1849.33, logicle.scale(-10000) * length, 0.1d);
            assertEquals(10.55, logicle.scale(-130) * length, 0.1d);
            assertEquals(463.50, logicle.scale(0) * length, 0.1d);
            assertEquals(1021.67, logicle.scale(170) * length, 0.1d);
            assertEquals(3522.215, logicle.scale(63130) * length, 0.1d);
            assertEquals(4096.00, logicle.scale(logicle.T) * length, 0.1d);

            // invert
            assertEquals(-10000, logicle.inverse(-1849.33 / length), 0.1d);
            assertEquals(-130, logicle.inverse(10.55 / length), 0.1d);
            assertEquals(0, logicle.inverse(463.50 / length), 0.1d);
            assertEquals(170, logicle.inverse(1021.67 / length), 0.1d);
            assertEquals(63130, logicle.inverse(3522.215 / length), 0.1d);
            assertEquals(logicle.T, logicle.inverse(4096.00 / length), 0.1d);
        }
    }
}
