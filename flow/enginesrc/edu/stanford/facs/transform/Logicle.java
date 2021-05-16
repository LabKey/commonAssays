package edu.stanford.facs.transform;

/**
 * Logicle display transform.
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
public class Logicle
		extends Transform
{
	/**
	 * Formal parameter of the Logicle scale as defined in the Gating-ML standard.
	 */
	public final double T, W, M, A;

	/**
	 * Actual parameter of the Logicle scale as implemented
	 */
	public final double a, b, c, d, f, w, x0, x1, x2;

	/**
	 * Scale value below which Taylor series is used
	 */
	protected final double xTaylor;

	/**
	 * Coefficients of Taylor series expansion
	 */
	protected final double[] taylor;

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
	protected Logicle(double T, double W, double M, double A, int bins)
	{
		if (T <= 0)
			throw new TransformParameterException("T is not positive");
		if (W < 0)
			throw new TransformParameterException("W is negative");
		if (M <= 0)
			throw new TransformParameterException("M is not positive");
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
		// formulas from biexponential paper
		w = W / (M + A);
		x2 = A / (M + A);
		x1 = x2 + w;
		x0 = x2 + 2 * w;
		b = (M + A) * LN_10;
		d = solve(b, w);
		double c_a = Math.exp(x0 * (b + d));
		double mf_a = Math.exp(b * x1) - c_a / Math.exp(d * x1);
		a = T / ((Math.exp(b) - mf_a) - c_a / Math.exp(d));
		c = c_a * a;
		f = -mf_a * a;

		// use Taylor series near x1, i.e., data zero to
		// avoid round off problems of formal definition
		xTaylor = x1 + w / 4;
		// compute coefficients of the Taylor series
		double posCoef = a * Math.exp(b * x1);
		double negCoef = -c / Math.exp(d * x1);
		// 16 is enough for full precision of typical scales
		taylor = new double[16];
		for (int i = 0; i < taylor.length; ++i)
		{
			posCoef *= b / (i + 1);
			negCoef *= -d / (i + 1);
			taylor[i] = posCoef + negCoef;
		}
		taylor[1] = 0; // exact result of Logicle condition
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
	public Logicle(double T, double W, double M, double A)
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
	public Logicle(double T, double W, double M)
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
	public Logicle(double T, double W)
	{
		this(T, W, DEFAULT_DECADES, 0D);
	}

	/**
	 * Solve f(d;w,b) = 2 * (ln(d) - ln(b)) + w * (d + b) = 0 for d, given b and w
	 * 
	 * @param b
	 * @param w
	 * @return double root d
	 */
	protected static double solve (double b, double w)
	{
		// w == 0 means its really arcsinh
		if (w == 0)
			return b;

		// precision is the same as that of b
		double tolerance = 2 * Math.ulp(b);

		// based on RTSAFE from Numerical Recipes 1st Edition
		// bracket the root
		double d_lo = 0;
		double d_hi = b;

		// bisection first step
		double d = (d_lo + d_hi) / 2;
		double last_delta = d_hi - d_lo;
		double delta;

		// evaluate the f(d;w,b) = 2 * (ln(d) - ln(b)) + w * (b + d)
		// and its derivative
		double f_b = -2 * Math.log(b) + w * b;
		double f = 2 * Math.log(d) + w * d + f_b;
		double last_f = Double.NaN;

		for (int i = 1; i < 20; ++i)
		{
			// compute the derivative
			double df = 2 / d + w;

			// if Newton's method would step outside the bracket
			// or if it isn't converging quickly enough
			if (((d - d_hi) * df - f) * ((d - d_lo) * df - f) >= 0
					|| Math.abs(1.9 * f) > Math.abs(last_delta * df))
			{
				// take a bisection step
				delta = (d_hi - d_lo) / 2;
				d = d_lo + delta;
				if (d == d_lo)
					return d; // nothing changed, we're done
			}
			else
			{
				// otherwise take a Newton's method step
				delta = f / df;
				double t = d;
				d -= delta;
				if (d == t)
					return d; // nothing changed, we're done
			}
			// if we've reached the desired precision we're done
			if (Math.abs(delta) < tolerance)
				return d;
			last_delta = delta;

			// recompute the function
			f = 2 * Math.log(d) + w * d + f_b;
			if (f == 0 || f == last_f)
				return d; // found the root or are not going to get any closer
			last_f = f;

			// update the bracketing interval
			if (f < 0)
				d_lo = d;
			else
				d_hi = d;
		}

		throw new IllegalStateException("exceeded maximum iterations in solve()");
	}

	/**
	 * Computes the slope of the biexponential function at a scale value.
	 * 
	 * @param scale
	 * @return The slope of the biexponential at the scale point
	 */
	protected double slope (double scale)
	{
		// reflect negative scale regions
		if (scale < x1)
			scale = 2 * x1 - scale;

		// compute the slope of the biexponential
		return a * b * Math.exp(b * scale) + c * d / Math.exp(d * scale);
	}

	/**
	 * Computes the value of Taylor series at a point on the scale
	 * 
	 * @param scale
	 * @return value of the biexponential function
	 */
	protected double seriesBiexponential (double scale)
	{
		// Taylor series is around x1
		double x = scale - x1;
		// note that taylor[1] should be identically zero according
		// to the Logicle condition so skip it here
		double sum = taylor[taylor.length - 1] * x;
		for (int i = taylor.length - 2; i >= 2; --i)
			sum = (sum + taylor[i]) * x;
		return (sum * x + taylor[0]) * x;
	}

	/**
	 * Computes the Logicle scale value of the given data value
	 * 
	 * @param value
	 *          a data value
	 * @return the double Logicle scale value
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
		if (value < f)
			// use linear approximation in the quasi linear region
			x = x1 + value / taylor[0];
		else
		{
			// otherwise use ordinary logarithm
			x = Math.log(value / a) / b;
			// but that can fail if W and the value are very small
			if (x < x1)
				x = x1;
		}

		// try for double precision unless in extended range
		double tolerance = 3 * Math.ulp(1D);
		if (x > 1)
			tolerance = 3 * Math.ulp(x);

		for (int i = 0; i < 10; ++i)
		{
			// compute the function and its first two derivatives
			double ae2bx = a * Math.exp(b * x);
			double ce2mdx = c / Math.exp(d * x);
			double y;
			if (x < xTaylor)
				// near zero use the Taylor series
				y = seriesBiexponential(x) - value;
			else
				// this formulation has better roundoff behavior
				y = (ae2bx + f) - (ce2mdx + value);
			double abe2bx = b * ae2bx;
			double cde2mdx = d * ce2mdx;
			double dy = abe2bx + cde2mdx;
			double ddy = b * abe2bx - d * cde2mdx;

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
	 * Computes the data value corresponding to the given point of the Logicle
	 * scale. This is the inverse of the {@link Logicle#scale(double) scale}
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

		// compute the biexponential
		double inverse;
		if (scale < xTaylor)
			// near x1, i.e., data zero use the series expansion
			inverse = seriesBiexponential(scale);
		else
			// this formulation has better roundoff behavior
			inverse = (a * Math.exp(b * scale) + f) - c / Math.exp(d * scale);

		// handle scale for negative values
		if (negative)
			return -inverse;
		else
			return inverse;
	}

	/**
	 * Choose a suitable set of data coordinates for a Logicle scale
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
