/**
 * 
 */
package edu.stanford.facs.transform;

/**
 * Abstract display transform. A display transform maps a range of scaled and
 * compensated data values monotonically onto the interval [0,1]. They are
 * primarily for use in constructing data visualizations but also useful for
 * clustering and other applications.
 * 
 * @author Wayne A. Moore
 * @version 1.0
 */
public abstract class Transform
{
	/**
	 * Number of decades in default log or log like scale
	 */
	public static final double DEFAULT_DECADES = 4.5;

	/**
	 * Constant needed by log and log like scales
	 */
	protected static final double LN_10 = Math.log(10);

	/**
	 * Computes the scale position for a given data value
	 * 
	 * @param value
	 *          a scaled and compensated data value
	 * @return the double scale position
	 */
	abstract public double scale (final double value);

	/**
	 * Computes the data value mapped to a given position on the scale
	 * 
	 * @param scale
	 *          the position on the display scale
	 * @return the double data value mapped to the scale position
	 */
	abstract public double inverse (final double scale);

	/**
	 * A suggested list of data values suitable for use as labels on an axis for
	 * this transform. Each data value may be turned into a string to be located
	 * at the scale position of the data.
	 * 
	 * @return the array of data values
	 */
	abstract public double[] axisLabels ();

	/**
	 * Computes the slope of the inverse function at a scale value. Needed by the
	 * unit tests.
	 * 
	 * @param scale
	 * @return The slope of the inverse transform at the scale point
	 */
	abstract protected double slope (double scale);
}
