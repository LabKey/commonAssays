package edu.stanford.facs.transform;

/**
 * Hyperlog display transform.
 * 
 * Maps a data value onto the interval [0,1] such that:
 * <ul>
 * <li>data value T is mapped to 1</li>
 * <li>large data values are mapped to locations similar to an M + A decade
 * logarithmic scale</li>
 * <li>A decades of negative data are brought on scale.</li>
 * </ul>
 * This is a reference implementation, accurate to <code>double</code>
 * precision. It's reasonably efficient but highly tuned less accurate
 * implementations can be several times faster.
 * 
 * @author Wayne A. Moore
 * @version 1.0
 */
public class Hyperlog
		extends Transform
{
	/**
	 * Formal parameter of the Hyperlog scale as defined in the Gating-ML
	 * standard.
	 */
	public final double T, W, M, A;

	/**
	 * Actual parameter of the Hyperlog scale as implemented
	 */
	public final double a, b, c, f, w, x0, x1, x2;

	/**
	 * Scale value below which Taylor series is used
	 */
	protected final double xTaylor;

	/**
	 * Coefficients of Taylor series expansion
	 */
	protected final double[] taylor;

	private final double inverse_x0;

	/**
	 * Real constructor that does all the work. Called only from implementing
	 * classes.
	 * 
	 * @param T
	 *          maximum data value or "top of scale"
	 * @param W
	 *          number of decades to linearize
	 * @param M
	 *          number of decades that a pure log scale would cover
	 * @param A
	 *          additional number of negative decades to include on scale
	 * @param bins
	 *          number of bins in the lookup table
	 */
	protected Hyperlog(double T, double W, double M, double A, int bins)
	{
		if (T <= 0)
			throw new TransformParameterException("T is not positive");
		if (W < 0)
			throw new TransformParameterException("W is negative");
		if (M <= 0)
			throw new TransformParameterException("M is not positive");
		if (W <= 0)
			throw new TransformParameterException("W is not positive");
		if (2 * W > M)
			throw new TransformParameterException("W is too large");
		if (-A > W || A + W > M - W)
			throw new TransformParameterException("A is too large");

		// if we're going to bin the data make sure that
		// zero is on a bin boundary by adjusting A
		if (bins > 0)
		{
			double zero = (W + A) / (M + A);
			zero = Math.rint(zero * bins) / bins;
			A = (M * zero - W) / (1 - zero);
		}

		// standard parameters
		this.T = T;
		this.M = M;
		this.W = W;
		this.A = A;

		// actual parameters
		// choose the data zero location and the width of the linearization region
		// to match the corresponding logicle scale
		w = W / (M + A);
		x2 = A / (M + A);
		x1 = x2 + w;
		x0 = x2 + 2 * w;
		// make the logarithmic portion match corresponding log/logicle scales
		b = (M + A) * LN_10;
		// for a logicle scale the positive and negative exponential terms are equal
		// at x0
		// for the hyperlog we choose the slope so that the exponential and linear
		// terms are equal there
		// and thus the linear to log transition occurs at roughly the same point
		double e2bx0 = Math.exp(b * x0);
		// this is roughly the B parameter of the original method adjusted to map
		// the
		// appropriate range of log values onto the unit display interval
		double c_a = e2bx0 / w;
		// data value zero occurs at x1
		double f_a = Math.exp(b * x1) + c_a * x1;
		// adjust for top of scale
		a = T / ((Math.exp(b) + c_a) - f_a);
		c = c_a * a;
		f = f_a * a;

		// use Taylor series near x1, i.e., data zero to
		// avoid round off problems of formal definition
		xTaylor = x1 + w / 4;
		// compute coefficients of the Taylor series
		double coef = a * Math.exp(b * x1);
		// 16 is enough for full precision of typical scales
		taylor = new double[16];
		for (int i = 0; i < taylor.length; ++i)
		{
			coef *= b / (i + 1);
			taylor[i] = coef;
		}
		taylor[0] += c; // hyperlog condition

		inverse_x0 = inverse(x0);
	}

	/**
	 * Constructor taking all possible parameters
	 * 
	 * @param T
	 *          the double maximum data value or "top of scale"
	 * @param W
	 *          the double number of decades to linearize
	 * @param M
	 *          the double number of decades that a pure log scale would cover
	 * @param A
	 *          the double additional number of negative decades to include on
	 *          scale
	 */
	public Hyperlog(double T, double W, double M, double A)
	{
		this(T, W, M, A, 0);
	}

	/**
	 * Constructor with no additional negative decades
	 * 
	 * @param T
	 *          the double maximum data value or "top of scale"
	 * @param W
	 *          the double number of decades to linearize
	 * @param M
	 *          the double number of decades that a pure log scale would cover
	 */
	public Hyperlog(double T, double W, double M)
	{
		this(T, W, M, 0D);
	}

	/**
	 * Constructor with default number of decades and no additional negative
	 * decades
	 * 
	 * @param T
	 *          the double maximum data value or "top of scale"
	 * @param W
	 *          the double number of decades to linearize
	 */
	public Hyperlog(double T, double W)
	{
		this(T, W, DEFAULT_DECADES, 0D);
	}

	/**
	 * Computes the value of Taylor series at a point on the scale
	 * 
	 * @param scale
	 * @return value of the inverse hyperlog function
	 */
	protected double taylorSeries (double scale)
	{
		// Taylor series is around x1
		double x = scale - x1;
		double sum = taylor[taylor.length - 1] * x;
		for (int i = taylor.length - 2; i >= 0; --i)
			sum = (sum + taylor[i]) * x;
		return sum;
	}

	/**
	 * Computes the Hyperlog scale value of the given data value
	 * 
	 * @param value
	 *          a data value
	 * @return the double Hyperlog scale value
	 */
	public double scale (double value)
	{
		// handle true zero separately
		if (value == 0)
			return x1;

		// reflect negative values
		boolean negative = value < 0;
		if (negative)
			value = -value;

		// initial guess at solution
		double x;
		if (value < inverse_x0)
			// use linear approximation in the quasi linear region
			x = x1 + value * w / inverse_x0;
		else
			// otherwise use ordinary logarithm
			x = Math.log(value / a) / b;

		// try for double precision unless in extended range
		double tolerance = 3 * Math.ulp(1D);
		if (x > 1)
			tolerance = 3 * Math.ulp(x);

		for (int i = 0; i < 10; ++i)
		{
			// compute the function and its first two derivatives
			double ae2bx = a * Math.exp(b * x);
			double y;
			if (x < xTaylor)
				// near zero use the Taylor series
				y = taylorSeries(x) - value;
			else
				// this formulation has better roundoff behavior
				y = (ae2bx + c * x) - (f + value);
			double abe2bx = b * ae2bx;
			double dy = abe2bx + c;
			double ddy = b * abe2bx;

			// this is Halley's method with cubic convergence
			double delta = y / (dy * (1 - y * ddy / (2 * dy * dy)));
			x -= delta;

			// if we've reached the desired precision we're done
			if (Math.abs(delta) < tolerance)
				// handle negative arguments
				if (negative)
					return 2 * x1 - x;
				else
					return x;
		}

		throw new IllegalStateException("scale() didn't converge");
	}

	/**
	 * Computes the data value corresponding to the given point of the Hyperlog
	 * scale. This is the inverse of the {@link Hyperlog#scale(double) scale}
	 * function.
	 * 
	 * @param scale
	 *          a double scale value
	 * @return the double data value
	 */
	public double inverse (double scale)
	{
		// reflect negative scale regions
		boolean negative = scale < x1;
		if (negative)
			scale = 2 * x1 - scale;

		double inverse;
		if (scale < xTaylor)
			// near x1, i.e., data zero use the series expansion
			inverse = taylorSeries(scale);
		else
			// this formulation has better roundoff behavior
			inverse = (a * Math.exp(b * scale) + c * scale) - f;

		// handle scale for negative values
		if (negative)
			return -inverse;
		else
			return inverse;
	}

	@Override
	protected double slope (double scale)
	{
		// reflect negative scale regions
		if (scale < x1)
			scale = 2 * x1 - scale;

		return a * b * Math.exp(b * scale) + c;
	}

	/**
	 * Choose a suitable set of data coordinates for a Hyperlog scale
	 * 
	 * @return a double array of data values
	 */
	public double[] axisLabels ()
	{
		// number of decades in the positive logarithmic region
		double p = M - 2 * W;
		// smallest power of 10 in the region
		double log10x = Math.ceil(Math.log(T) / LN_10 - p);
		// data value at that point
		double x = Math.exp(LN_10 * log10x);
		// number of positive labels
		int np;
		if (x > T)
		{
			x = T;
			np = 1;
		}
		else
			np = (int) (Math.floor(Math.log(T) / LN_10 - log10x)) + 1;
		// bottom of scale
		double B = this.inverse(0);
		// number of negative labels
		int nn;
		if (x > -B)
			nn = 0;
		else if (x == T)
			nn = 1;
		else
			nn = (int) Math.floor(Math.log(-B) / LN_10 - log10x) + 1;

		// fill in the axis labels
		double[] label = new double[nn + np + 1];
		label[nn] = 0;
		for (int i = 1; i <= nn; ++i)
		{
			label[nn - i] = -x;
			label[nn + i] = x;
			x *= 10;
		}
		for (int i = nn + 1; i <= np; ++i)
		{
			label[nn + i] = x;
			x *= 10;
		}

		return label;
	}
}
