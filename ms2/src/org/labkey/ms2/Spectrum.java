package org.labkey.ms2;

/**
 * User: adam
 * Date: May 10, 2006
 * Time: 11:09:10 AM
 */
public interface Spectrum
{
    public float[] getX();
    public float[] getY();
    public int getCharge();
    public double getPrecursorMass();
    public double getMZ();
}
