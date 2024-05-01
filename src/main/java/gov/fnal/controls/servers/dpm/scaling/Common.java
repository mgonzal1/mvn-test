// $Id: Common.java,v 1.1 2024/01/05 21:26:06 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

//import gov.fnal.controls.service.proto.Lookup_v2;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

class Common implements AcnetErrors
{
	private static final int PRIMARY_MAX = 84;
	private static final int COMMON_MAX = 90;
	private static final int MAX_ITERATIONS = 30;
	static final float FLT_MAX = ((float) 1.7014117e+38);

	// Binary search lower limits array dimension = (PI_MAX+2)/2

	private static final double lowlim[] = { -10.24, -10.0, -5.0, -2.5, 0.0, -4.295E9,
			0.0, 100.0, -FLT_MAX, 0.0, 0.0, -4.295E9, -FLT_MAX, -0.310269935,
			-4.295E9, -128.0, -128.0, 0.0, 0.0, -0.310269935, -8388608.0, 0.0,
			0.0, 0.0, -6.125E36, 0.0, -FLT_MAX, 4.0, -10.0, 0.0,
			-FLT_MAX / 500.0, 0.0, -1.0, 0.0, 0.0, -10.24, -10.24,
			0.0, 0.0, 0.0, 0.0 };

	// Binary search upper limits array dimension = (PI_MAX+2)/2

	private static final double uprlim[] = { 10.235, 9.995, 4.998, 2.499, 65536.0,
			4.295E9, 102.35, 10.0E9, FLT_MAX, 25.0, 65535.0, 4.295E9, FLT_MAX,
			3.10269935, 4.295E9, 127.0, 127.0, 255.0, 255.0, 3.10269935,
			8388607.996, 10.0, 9999999.0, 4294967295.0, 6.125E36, 10.0,
			FLT_MAX, 20.0, 10.0, 256.0, FLT_MAX / 500.0, 10.24, 1.0, 10.235,
			0.0, 10.235, 10.235, 21.0, 4294967295.0, 5.0, 10.0};

	final DeviceInfo.ReadSetScaling scaling;

	Common(DeviceInfo.ReadSetScaling scaling)
	{
		this.scaling = scaling;
	}

	double scale(double data) throws AcnetStatusException 
	{
		return scaling.common.index == 90 ? mfcTransform(data) : 
				_scale(data, scaling.common.index, scaling.common.constants, scaling.common.constants.length);
	}

	double unscale(double data) throws AcnetStatusException 
	{
		if (scaling.common.index == 90)
	       	return reverseMfcTransform(data);

		return _unscale(data, scaling.common.index, scaling.common.constants, 0, 0);
	}

	private int constCount(int index) 
	{
		switch (index) {
		// X' = X
		case 0:
			return 0;

		// X' = (C1*X/C2)+C3
		case 2:
			return 3;

		// X' = (X-C1)/C2
		case 4:
			return 2;

		// X' = C1*X/C2
		case 6:
			return 2;

		// X' = C4+(C1*X)/(C3+C2*X)
		case 8:
			return 4;

		// X' = C3+(C2/(C1*X))
		case 10:
			return 3;

		// X' = C5+(C4*X)+(C3*X**2)+(C2*X**3)+(C1*X**4)
		case 12:
			return 5;

		// X' = EXP(C5+(C4*X)+(C3*X**2)+(C2*X**3)+(C1*X**4))-C6
		case 14:
			return 6;

		// X' = C2*EXP(-X/C1)+C4*EXP(-X/C3)
		case 16:
			return 4;

		// X' = C3*EXP(C2*(X+C1))+C6*EXP(C5*(X+C4))
		case 18:
			return 6;

		// X' = LOG10(X)/(C1*LOG10(X)+C2)**2+C3
		case 20:
			return 3;

		// X' = C2*(10.0**(X/C1))
		case 22:
		return 2;

		// X' = C2*(C3*X+C4) on X<C1 or C2*EXP(C5*X+C6)
		case 24:
			return 6;

		// X' = C6+(C5*X)+(C4*X**2)+(C3*X**3)+(C2*X**4)+(C1*X**5)
		case 26:
			return 6;

		// X' = C3/(C2+C1*X)+C4
		case 28:
			return 4;

		// X' = C5+(C4*X)+(C3*X**2)+(C2*X**3) or C6 on X<C1
		case 30:
			return 6;

			// X' = C2*LOGe(C1*X+C4)+C3
		case 32:
			return 4;

			// X' = (C2+C1*X)/(C4+C3*X)
		case 34:
			return 4;

			// X' = C2*SQRT(X+C1)+C3
		case 36:
			return 3;

			// X' = 10**(C1+C2*X+C3*EXP(X)+C4/X+C5/X**2) on X>C6 or 760000.0
		case 38:
			return 6;

			// X' = (C1*X/C2)+C3 (C4 = min, C5 = max, C6 = knob inc)
		case 40:
			return 6;

			// X' = C2*X**2+C3*X+C4 on X<C1 or C2*EXP(C5*X+C6)
		case 42:
			return 6;

			// X' = C2*EXP(C3*X) on X<C1 or C4*EXP(C5*X)
		case 44:
			return 5;

			// X' = C2*EXP(C3*X**2+C4*X) on X<C1 or C5*EXP(C6*X)
		case 46:
			return 6;

			// X' = C1*C2**(1/X)*X**C3
		case 48:
			return 3;

			// X' = C1*ACOS(X/C2)
		case 50:
			return 2;

			// X' = EXP(C2*X+C3) on X<C1 or EXP(C4*X+C5)
		case 52:
			return 5;

			// X' = EXP(C2*X**2+C3*X+C4) on X<C1 or EXP(C5*X+C6)
		case 54:
			return 6;

			// X' = Table lookup (C1,X) where (C2<X<C3) (linear interpolation)
		case 56:
			return 3;

			// X' = Table lookup (C1,X) where (C2<X<C3) (exponential interpolation)
		case 58:
			return 3;

			// Special display types
		case 60:
			return 3;

			// X' = C2*(C3+10.0**(X/C1))
		case 62:
			return 3;

		case 64:
			return 1;

			// X' = C1*(2**(C2*(X+C3)))+C4
		case 66:
			return 4;

			// X' = C6*(C2*LOGe(C1*X+C4)+C3*X)^C5
		case 68:
			return 6;

			// X' = C1*EXP(-X/C2)+C3*EXP(-X/C4)+C5*EXP(-X/C6)+4
		case 70:
			return 6;

			// X' = C1*10**(C2+C3*LOG10(X)+C4*LOG10(X)**2+C5*LOG10(X)**3)+C6
		case 72:
			return 6;

			// X' = (C1 + C2*X + C3*X*X) / (C4 + C5*X + C6*X*X)
		case 74:
			return 6;

			// X' = C2*X**C3, X < C1 or X' = C4*exp(C5*X + C6)
		case 76:
			return 6;

			// X' = C1*10**(C2*X+C3) + C4
		case 78:
			return 4;

			// X' = X
		case 80:
			return 0;

			// X' = C2*LOG10(C1*X+C4)+C3
		case 82:
			return 4;

			// X' = (C1*X/C2) + C3 special display types including raw conversion
		case 84:
			return 6;

			// X' = C3*X + C4 on X < C1, X' = exp(C5*X + C6) on X > C2,
			// the logarithmic interpolation on C1 <= X <= C2 
		case 86:
			return 6;

			// X' = (C1+C2*X+C3*X^2)/(1+C4*X+C5*X^2+C6*X^3)
		case 88:
		        return 6;

			//  X' = Table lookup (C1,X) where (C2<X<C3) (multifunction transform)
		case 90:
			return 3;

		//		can be used in the case 90 only!
		//  X' = C8+C7*X+C6*X^2+C5*X^3+C4*X^4+C3*X^5+C2*X^6+C1*X^7
		case 201:
			return 8;

			// Invalid common_index passed, return error
		default:
			return -1;
		}
	}

	/**
	 * Scales the value of a device from primary units to common units.
	 * 	not for the case #90 (see primaryToCommon() below )
	 * @param data
	 *            double primary value to scale to common units
	 * @return a scaled value (in common units)
	 * @exception AcnetStatusException
	 *                on scaling error
	 *	data - primary value
	 *      index - common transform index
	 *	c        - scaling constants
	 *	num_constants - number of scaling constants
	 */
	private double _scale(double data, int index, double[] constants, int constCount) throws AcnetStatusException 
	{
		int il;
		double x = 0.0;
		double flt_data = 0.0;
		double flt_temp = 0.0;
		double log_e_10 = 2.302585092994045684;
		
		final double[] c = constants;
		final int num_constants = constCount;

		/*
		 * * fetch input data to be scaled
		 */
		x = data;

//		num_constants = constants.length;

		/*
		 * * Scale input data to COMMON units based upon the common transform
		 * index * in PDB
		 */

		switch (index) {
		case 0:
			return data;

			/*
			 * * Process to common units where * X' = (C1*X/C2)+C3
			 */
		case 2:
			if (num_constants < 3)
				throw new AcnetStatusException(DIO_SCALEFAIL, "Insufficient number of constants provided in PDB.");
			
			if (c[1] != 0.0)
				flt_data = (c[0] * x / c[1]) + c[2];
			else
				throw new AcnetStatusException(DIO_SCALEFAIL, "Invalid constant C2 in PDB.");
			
			return flt_data;

			/*
			 * * Process to common units where * X' = (X-C1)/C2
			 */

		case 4:
			if (num_constants < 2) { /*
			 * insufficient number of constants
			 * provided
			 */
				throw new AcnetStatusException(DIO_SCALEFAIL, "Insufficient number of constants provided in PDB.");
			}
			if (c[1] != 0.0) {
				flt_data = (x - c[0]) / c[1];
			} else {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Invalid constant C2 in PDB.");
			}
			return flt_data;
			/*
			 * * Process to common units where * X' = C1*X/C2
			 */

		case 6:
			if (num_constants < 2) { /*
			 * insufficient number of constants
			 * provided
			 */
				throw new AcnetStatusException(DIO_SCALEFAIL, "Insufficient number of constants provided in PDB.");
			}
			if (c[1] != 0.0) {
				flt_data = c[0] * x / c[1];
			} else {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Invalid constant C2 in PDB.");
			}
			return flt_data;

			/*
			 * * Process to common units where * X' = C4+(C1*X)/(C3+C2*X)
			 */

		case 8:
			if (num_constants < 4) { /*
			 * insufficient number of constants
			 * provided
			 */
				throw new AcnetStatusException(DIO_SCALEFAIL,  "Insufficient number of constants provided in PDB.");
			}
			flt_temp = c[2] + c[1] * x;
			if (flt_temp != 0.0) {
				flt_data = c[3] + (c[0] * x) / flt_temp;
			} else {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Scaling failure due to divide by zero operation.");
			}
			return flt_data;

			/*
			 * * Process to common units where * X' = C3+(C2/(C1*X))
			 */

		case 10:
			if (num_constants < 3) { /*
			 * insufficient number of constants
			 * provided
			 */
				throw new AcnetStatusException(DIO_SCALEFAIL, "Insufficient number of constants provided in PDB.");
			}
			flt_temp = c[0] * x;
			if (flt_temp != 0.0) {
				flt_data = c[2] + (c[1] / flt_temp);
			} else {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Scaling failure due to divide by zero.");
			}
			return flt_data;

			/*
			 * * Process to common units where * X' =
			 * C5+(C4*X)+(C3*X**2)+(C2*X**3)+(C1*X**4)
			 */

		case 12:
			if (num_constants < 5) { /*
			 * insufficient number of constants
			 * provided
			 */
				throw new AcnetStatusException(DIO_SCALEFAIL, "Insufficient number of constants provided in PDB.");
			}
			flt_data = 0;
			for (il = 0; il < 5; il++) flt_data = c[il] + flt_data * x;
//			flt_data = c[4] + (c[3] * x) + (c[2] * Math.pow(x, 2.0))
//					+ (c[1] * Math.pow(x, 3.0)) + (c[0] * Math.pow(x, 4.0));
			return flt_data;

			/*
			 * * Process to common units where * X' =
			 * MATH.EXP(C5+(C4*X)+(C3*X**2)+(C2*X**3)+(C1*X**4))-C6
			 */

		case 14:// *NOTE* *NOTE* *NOTE* - math exception handler important here
			if (num_constants < 6) { /*
			 * insufficient number of constants
			 * provided
			 */
				throw new AcnetStatusException(DIO_SCALEFAIL, "Insufficient number of constants provided in PDB.");
			}
			flt_temp = 0;
			for (il = 0; il < 5; il++) flt_temp = c[il] + flt_temp * x;
//			flt_temp = c[4] + (c[3] * x) + (c[2] * Math.pow(x, 2.0))
//					+ (c[1] * Math.pow(x, 3.0)) + (c[0] * Math.pow(x, 4.0));
			flt_data = Math.exp(flt_temp) - c[5];
			return flt_data;

			/*
			 * * Process to common units where * X' = C2*EXP(-X/C1)+C4*EXP(-X/C3)
			 */
		case 16:
			if (num_constants < 4) { /*
			 * insufficient number of constants
			 * provided
			 */
				throw new AcnetStatusException(DIO_SCALEFAIL, "Insufficient number of constants provided in PDB.");
			}
			if ((c[0] != 0.0) && (c[2] != 0.0)) {
				flt_data = c[1] * Math.exp(-x / c[0]) + c[3]
						* Math.exp(-x / c[2]);
			} else {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Zero C1, C3 constants in PDB are invalid.");
			}
			return flt_data;

			/*
			 * * Process to common units where * X' =
			 * C3*EXP(C2*(X+C1))+C6*EXP(C5*(X+C4))
			 */

		case 18:
			if (num_constants < 6) { /*
			 * insufficient number of constants
			 * provided
			 */
				throw new AcnetStatusException(DIO_SCALEFAIL, "Insufficient number of constants provided in PDB.");
			}
			flt_data = c[2] * Math.exp(c[1] * (x + c[0])) + c[5]
					* Math.exp(c[4] * (x + c[3]));
			return flt_data;

			/*
			 * * Process to common units where * X' =
			 * MATH.LOG10((X)/(C1*MATH.LOG10((X)+C2)**2+C3
			 */

		case 20:
			if (num_constants < 3) { /*
			 * insufficient number of constants
			 * provided
			 */
				throw new AcnetStatusException(DIO_SCALEFAIL, "Insufficient number of constants provided in PDB."); }
			// To get base 10 logarithm, use Math.log(value)/Math.log(10)
			flt_temp = Math
					.pow(c[0] * (Math.log(x) / Math.log(10)) + c[1], 2.0);

			if ((flt_temp != 0.0) /* && (errno == 0) */) {
				flt_data = (Math.log(x) / Math.log(10)) / flt_temp + c[2];
			} else {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Divide by zero scaling failure.");
			}
			return flt_data;

			/*
			 * * Process to common units where * X' = C2*(10.0**(X/C1))
			 */

		case 22:
			if (num_constants < 2) { /*
			 * insufficient number of constants
			 * provided
			 */
				throw new AcnetStatusException(DIO_SCALEFAIL, "Insufficient number of constants provided in PDB.");
			}
			if (c[0] != 0.0) {
				flt_data = c[1] * Math.pow(10.0, (x / c[0]));
			} else {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Zero C1 constant in PDB is invalid.");
			}
			return flt_data;

			/*
			 * * Process to common units where * X' = C2*(C3*X+C4) --> if X less
			 * than C1 -OR- * X' = C2*EXP(C5*X+C6)
			 */

		case 24:
			if (num_constants < 6) /* insufficient number of constants provided */
			{
				throw new AcnetStatusException(DIO_SCALEFAIL,  "Insufficient number of constants provided in PDB.");
			}
			if (x < c[0]) {
				flt_data = c[1] * (c[2] * x + c[3]);
			} else {
				flt_data = c[1] * Math.exp(c[4] * x + c[5]);
			}
			return flt_data;

			/*
			 * * Process to common units where * X' =
			 * C6+(C5*X)+(C4*X**2)+(C3*X**3)+(C2*X**4)+(C1*X**5)
			 */

		case 26:
			if (num_constants < 6) /* insufficient number of constants provided */
			{
				throw new AcnetStatusException(DIO_SCALEFAIL, "Insufficient number of constants provided in PDB.");
			}
			flt_data = 0;
			for (il = 0; il < 6; il++) flt_data = c[il] + flt_data * x;
//			flt_data = c[5] + (c[4] * x) + (c[3] * Math.pow(x, 2.0))
//					+ (c[2] * Math.pow(x, 3.0)) + (c[1] * Math.pow(x, 4.0))
//					+ (c[0] * Math.pow(x, 5.0));
			return flt_data;

			/*
			 * * Process to common units where * X' = C3/(C2+C1*X)+C4
			 */

		case 28:
			if (num_constants < 4) /* insufficient number of constants provided */
			{
				throw new AcnetStatusException(DIO_SCALEFAIL, "Insufficient number of constants provided in PDB.");
			}
			flt_temp = c[1] + c[0] * x;
			if (flt_temp != 0.0) {
				flt_data = c[2] / flt_temp + c[3];
			} else {
				throw new AcnetStatusException(DIO_SCALEFAIL,"Divide by zero. Scaling failed.");
			}
			return flt_data;

			/*
			 * * Process to common units where * X' = C5+(C4*X)+(C3*X**2)+(C2*X**3)
			 * -OR- * X' = C6 --> if X less than C1
			 */

		case 30:
			if (num_constants < 6) /* insufficient number of constants provided */
			{
				throw new AcnetStatusException(DIO_SCALEFAIL, "Insufficient number of constants provided in PDB.");
			}
			if (x < c[0]) {
				flt_data = c[5];
			} else {
				flt_data = c[4] + (c[3] * x) + (c[2] * Math.pow(x, 2.0))
						+ (c[1] * Math.pow(x, 3.0));
			}
			return flt_data;

			/*
			 * * Process to common units where * X' = C2*LOGe(C1*X+C4)+C3
			 */

		case 32:
			if (num_constants < 4) /* insufficient number of constants provided */
			{
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			}
			flt_data = c[1] * Math.log(c[0] * x + c[3]) + c[2];
			return flt_data;

			/*
			 * * Process to common units where * X' = (C2+C1*X)/(C4+C3*X)
			 */

		case 34:
			if (num_constants < 4) /* insufficient number of constants provided */
			{
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			}
			flt_temp = c[3] + c[2] * x;
			if (flt_temp != 0.0) {
				flt_data = (c[1] + c[0] * x) / flt_temp;
			} else {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Divide by zero. Scaling failed.");
			}
			return flt_data;

			/*
			 * * Process to common units where * X' = C2*MATH.SQRT(X+C1)+C3
			 */

		case 36:
			if (num_constants < 3) /* insufficient number of constants provided */
			{
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			}
			flt_temp = x + c[0];
			if (flt_temp >= 0.0) {
				flt_data = c[1] * Math.sqrt(flt_temp) + c[2];
			} else {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Divide by zero. Scaling failed.");
			}
			return flt_data;

			/*
			 * * Process to common units where * * X' =
			 * 10**(C1+C2*X+C3*EXP(X)+C4/X+C5/X**2) --> if X greater than C6 -OR- *
			 * X' = 760000.0
			 */

		case 38:
			if (num_constants < 6) /* insufficient number of constants provided */
			{
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			}
			if (x > c[5]) {
				if (x != 0.0) {
                                        if (c[2] != 0 )
					        flt_temp = c[0] + (c[1] * x) + (c[2] * Math.exp(x))
							        + (c[3] / x) + (c[4] / Math.pow(x, 2.0));
                                         else
					        flt_temp = c[0] + (c[1] * x) 
							        + (c[3] / x) + (c[4] / Math.pow(x, 2.0));

					flt_data = Math.pow(10.0, flt_temp);
				} else {
					throw new AcnetStatusException(DIO_SCALEFAIL, "Scaling failed.");
				}
			} else {
				flt_data = 760000.0;
			}

			return flt_data;

		case 40:
			if (num_constants < 6) /* insufficient number of constants provided */
			{
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			}
			if (c[1] != 0.0) {
				flt_data = (c[0] * x / c[1]) + c[2];
			} else {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Zero C1 constant in PDB is invalid.");
			}
			return flt_data;

			/*
			 * * Process to common units where * X' = C2*X**2+C3*X+C4 --> if X less
			 * than C1 -OR- * X' = C2*EXP(C5*X+C6)
			 */

		case 42:
			if (num_constants < 6) /* insufficient number of constants provided */
			{
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			}
			if (x < c[0]) {
				flt_data = c[1] * Math.pow(x, 2.0) + c[2] * x + c[3];
			} else {
				flt_data = c[1] * Math.exp(c[4] * x + c[5]);
			}
			return flt_data;

			/*
			 * * Process to common units where * X' = C2*EXP(C3*X) --> if X less
			 * than C1 -OR- * X' = C4*EXP(C5*X)
			 */
		case 44:
			if (num_constants < 5) { /*
			 * insufficient number of constants
			 * provided
			 */
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			}
			if (x < c[0]) {
				flt_data = c[1] * Math.exp(c[2] * x);
			} else {
				flt_data = c[3] * Math.exp(c[4] * x);
			}
			return flt_data;

			/*
			 * * Process to common units where * X' = C2*EXP(C3*X**2+C4*X) --> if X
			 * less than C1 -OR- * X' = C5*EXP(C6*X)
			 */
		case 46:
			if (num_constants < 6) {
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			}
			if (x < c[0]) {
				flt_data = c[1] * Math.exp(c[2] * Math.pow(x, 2.0) + c[3] * x);
			} else {
				flt_data = c[4] * Math.exp(c[5] * x);
			}
			return flt_data;

			/*
			 * * Process to common units where * X' = C1*C2**(1/X)*X**C3
			 */

		case 48:
			if (num_constants < 3) {
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			}
			flt_data = c[0] * Math.pow(c[1], (1.0 / x)) * Math.pow(x, c[2]);
			return flt_data;

			/*
			 * * Process to common units where * X' = C1*ACOS(X/C2)
			 */
		case 50:
			if (num_constants < 2)
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			flt_data = c[0] * Math.acos(x / c[1]);
			return flt_data;

			/*
			 * * Process to common units where * X' = EXP(C2*X+C3) --> if X less
			 * than C1 -OR- * X' = EXP(C4*X+C5)
			 */
		case 52:
			if (num_constants < 5)
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			if (x < c[0])
				flt_data = Math.exp(c[1] * x + c[2]);
			else
				flt_data = Math.exp(c[3] * x + c[4]);
			return flt_data;

			/*
			 * * Process to common units where * X' = EXP(C2*X**2+C3*X+c4) --> if X
			 * less than C1 -OR- * X' = EXP(C5*X+C6)
			 */
		case 54:
			if (num_constants < 6)
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			if (x < c[0])
				flt_data = Math.exp(c[1] * Math.pow(x, 2.0) + c[2] * x + c[3]);
			else
				flt_data = Math.exp(c[4] * x + c[5]);
			return flt_data;

			/*
			 * * X' = Table lookup (C1,X) where (C2<X<C3) (linear interpolation)
			 */
		case 56:
			if (num_constants < 3)
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			flt_data = interpolate((int) c[0],
					new double[] { c[1], c[2] },
					ScalingInternal.INTERPOLATE_LINEAR, x);
			return flt_data;

			/*
			 * * X' = Table lookup (C1,X) where (C2<X<C3) (exponential
			 * interpolation)
			 */
		case 58:
			if (num_constants < 3)
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			flt_data = interpolate((int) c[0],
					new double[] { c[1], c[2] },
					ScalingInternal.INTERPOLATE_EXP, x);
			return flt_data;

			/*
			 * * Special display types
			 */
		case 60:
			throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Required");

			/*
			 * * Process to common units where * X' = C2*(C3+10.0**(X/C1))
			 */
		case 62:
			if (num_constants < 3)
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			if (c[0] == 0.0)
				throw new AcnetStatusException(DIO_SCALEFAIL, "Zero C1 constant in PDB is invalid.");
			flt_data = c[1] * (c[2] + Math.pow(10.0, (x / c[0])));
			return flt_data;

		case 64:
			if (num_constants < 1)
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			switch ((int) c[0]) {
			case 0: /* N2 VPT */
				if (x <= 4.825) {
					flt_data = 77.4 + 13.463 * x - 8.2465 * Math.pow(x, 2.0)
							+ 3.6896 * Math.pow(x, 3.0) - 0.7824
							* Math.pow(x, 4.0) + 0.0608 * Math.pow(x, 5.0);
				} else {
					flt_data = -0.5451 - 428.2927 * x - 210274.8806
							* Math.pow(x, 2.0) + 129913.0267 * Math.pow(x, 3.0)
							- 26741.8467 * Math.pow(x, 4.0) + 1834.8285
							* Math.pow(x, 5.0);
				}
				break;
			case 1: /* He VPT */
				if (x <= 0.9148) {
					flt_data = 4.2 + 1.443 * x - 0.40755 * Math.pow(x, 2.0)
							+ 0.3005 * Math.pow(x, 3.0) - 1.5166
							* Math.pow(x, 4.0) + 1.3716 * Math.pow(x, 5.0);
				} else {
					flt_data = 4.271 + 2.89 * x - 4.11 * Math.pow(x, 2.0)
							+ 3.27 * Math.pow(x, 3.0) - 1.26 * Math.pow(x, 4.0)
							+ 0.202 * Math.pow(x, 5.0);
				}
				break;
			default:
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Invalid value for constant C1 in PDB.");
			}
			return flt_data;

			/*
			 * * Process to common units where * X' = C1*(2**(C2*(X+C3)))+C4
			 */
		case 66:
			if (num_constants < 4)
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			flt_data = c[0] * Math.pow(2.0, c[1]*(x + c[2])) + c[3];

			return flt_data;

			/*
			 * * X' = C6*(C2*LOGe(C1*X+C4)+C3*X)^C5
			 */
		case 68:
			if (num_constants < 6)
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			if ( (c[0] != 0.0)&&((c[0]*x+c[3]) <= 0.0 ) )	/* clamp at 0 */
			    flt_data = 0.0;
			 else
			    flt_data = c[5]
					* Math.pow(c[1] * Math.log(c[0] * x + c[3]) + c[2] * x,
							c[4]);

			return flt_data;

			/*
			 * * X' = C1*EXP(-X/C2)+C3*EXP(-X/C4)+C5*EXP(-X/C6)+4
			 */
		case 70:
			if (num_constants < 6)
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			flt_data = c[0] * Math.exp(-x / c[1]) + c[2] * Math.exp(-x / c[3])
					+ c[4] * Math.exp(-x / c[5]) + 4;

			return flt_data;

			/*
			 **  X' = C1*10**(C2+C3*LOG10(X)+C4*LOG10(X)**2+C5*LOG10(X)**3)+C6
			 */
		case 72:
			if (num_constants < 6)
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			flt_data = c[0]
					* Math.pow(10.0, c[1] + c[2] * Math.log(x) / log_e_10
							+ c[3] * Math.pow(Math.log(x) / log_e_10, 2.0)
							+ c[4] * Math.pow(Math.log(x) / log_e_10, 3.0))
					+ c[5];

			return flt_data;

			/*
			 **  X' = (C1 + C2*X + C3*X*X) / (C4 + C5*X + C6*X*X)
			 */
		case 74:
			if (num_constants < 6)
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			flt_data = (c[0]+c[1]*x+c[2]*x*x)/(c[3]+c[4]*x+c[5]*x*x);

			return flt_data;

			/*
			** X' = C2*X**C3, X < C1 or X' = C4*exp(C5*X + C6)
			*/
		case 76:
			if (num_constants < 6)
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			if (x < c[0])
			    flt_data = c[1] * Math.pow (x, c[2]);
			 else
			    flt_data = c[3] * Math.exp (c[4]*x + c[5]);

			return flt_data;
			
			/*
			** X' = C1*10**(C2*X+C3) + C4
			*/
		case 78:
			if (num_constants < 4)
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			flt_data = c[0] * Math.pow (10, c[1]*x+c[2]) + c[3];

			return flt_data;

			/*
			** X' = X
			*/
		case 80:
			flt_data = x;

			return flt_data;

			/*
			 * * Process to common units where * X' = C2*LOG10(C1*X+C4)+C3
                         *   log10(X) = log(X)/log(10)
			 */
		case 82:
			if (num_constants < 4) /* insufficient number of constants provided */
			{
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			}
			flt_data = c[1] * Math.log(c[0] * x + c[3]) / log_e_10 + c[2];
			return flt_data;

			
			/*
			 * * X' = (C1*X/C2) + C3 special display types including raw conversion
			 */
		case 84:
			throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Required");

			/*
			 * * NOTE!!! If a new transform is added, the constant CI_MAX *must* be *
			 * ^^^^ changed in order for the transform to be properly recognized *
			 * insert in common_transform_num_const number of parameters also
			 */

			/*
			** X' = C3*X + C4 on X < C1, X' = exp(C5*X + C6) on X > C2,
			** the logarithmic interpolation on C1 <= X <= C2 
			*/
		case 86:
			if (num_constants < 6) /* insufficient number of constants provided */
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Insufficient number of constants provided in PDB.");
			if (x < c[0]) {
			    flt_data = c[2] * x + c[3];
			    }
			else
			if (x > c[1]) {
			    flt_data =  Math.exp( c[4] * x + c[5]);
			    }
			else {         /* c[0] <= x <= c[1] */
			    double x0, x1, y0, y1, log_result;
			    x0 = c[0]; y0 = c[2] * c[0] +  c[3];
			    x1 = c[1]; y1 = Math.exp( c[4] * c[1] + c[5]);
			    log_result =  Math.log(y1/y0) * (x - x0) / (x1 - x0) + Math.log(y0);
			    flt_data = Math.exp(log_result);
      			    }
			return flt_data;	

			/*
			**  X' = (C1+C2*X+C3*X^2)/(1+C4*X+C5*X^2+C6*X^3)
			*/

		case 88:
			if (num_constants < 6)
				throw new AcnetStatusException(DIO_SCALEFAIL,
					"Insufficient number of constants provided in PDB.");

			flt_data = (c[0] + x * (c[1] + x * c[2])) /
				   (1.0 + x * (c[3] + x * (c[4] + x * c[5])));
			return flt_data;

			/*
			**  X' = Table lookup (C1,X) where (C2<X<C3) (multifunction transform)
			*/

		case 90:	/* we can not be here! - recursive call will happen */
			throw new AcnetStatusException(DIO_SCALEFAIL, "Recursive transform #90 found.");


		//		can be used in the case 90 only!
		//  X' = C8+C7*X+C6*X^2+C5*X^3+C4*X^4+C3*X^5+C2*X^6+C1*X^7
		case 201:
			if (num_constants < 8)
				throw new AcnetStatusException(DIO_SCALEFAIL,
					"Insufficient number of constants provided in PDB.");
			flt_data = 0;
			for (il = 0; il < num_constants; il++) flt_data = c[il] + flt_data * x;
			return flt_data;

		default:
			throw new AcnetStatusException(DIO_SCALEFAIL, "Common transform " + index + " not found.");
		}

	}

    /******************************************************************************
    * mfc_transform: multifunction common transform
    *	This function is called to do the actual scaling of 
    *       data from primary to common units for the case 90
    *
    ******************************************************************************/
    private double mfcTransform(double data) throws AcnetStatusException
	{
		final double[] c = scaling.common.constants;
		final int num_constants = c.length;

		double x;
		double flt_data = 0;;
		int il, jl, sts;
		int table_number;
		int table_pointer;
		int n_of_diapasons;		// the table size: number of primary diapasons

		//num_constants = constants.length;
		if (num_constants < 3)
			throw new AcnetStatusException(DIO_SCALEFAIL,
				"Insufficient number of constants provided in PDB.");

		ScalingInternal.read_multifunction_transforms();

		table_number = (int)c[0];
		if ((table_number > ScalingInternal.n_mfc_transform_tables) ||
				(table_number <= 0) ) {
			ScalingInternal.n_mfc_transform_tables = 0;
			ScalingInternal.read_multifunction_transforms();
			if ((table_number > ScalingInternal.n_mfc_transform_tables) ||
						(table_number <= 0) ) {
				flt_data = 0.0;
				throw new AcnetStatusException(DIO_SCALEFAIL,
					"multifunction transform: bad argument (table_number)");
			}
		}
		
		x = data;
			/* c[1] = lower limit, c[2] = upper limit */
		if ((x < c[1]) || (x > c[2]))
			throw new AcnetStatusException(DIO_SCALEFAIL, "multifunction_table: primary_value out of range (" + x + ")");



			// copy the interpolation tables
		table_pointer = ScalingInternal.mfct_table_offset(table_number);
		n_of_diapasons = ScalingInternal.mfct_table_size[table_number - 1];


				/* looking for x's diapason and conversion */   
		for (il = table_pointer; il < table_pointer+n_of_diapasons; il++) {
			if ( (x >= ScalingInternal.all_transform_values[il].primary_initial)&&
				 (x <= ScalingInternal.all_transform_values[il].primary_final) ) {
			/* the diapason has been found, proceed to the conversion */
			jl = ScalingInternal.all_transform_values[il].common_transform;
			flt_data  = _scale(x, jl, ScalingInternal.all_transform_values[il].cx, constCount(jl));
			break;
			}
			}
		
		if (il == n_of_diapasons)
			throw new AcnetStatusException(DIO_SCALEFAIL, "multifunction_table: primary_value out of diapasons (" + x + ")");

		return flt_data;
    }

	/**
	 * Converts the value of a device from common units to primary units.
	 * 
	 * @param x
	 *            double common value to unscale to primary units
	 * @return a device value in primary units
	 * @exception AcnetStatusException
	 *                on scaling error
	 */
	private double _unscale(double xx, int index, double[] constants, double prim_low, double prim_upp) throws AcnetStatusException
	{
		double state;
		double tmpx;
		double pulow, pumid, puupr;
		double tol;
		double a, b, g, d;
		double toldiv = 131072.0; /* tolerance divisor */

		final double[] c = constants;

		if ((prim_low == 0)&&(prim_upp == 0))
		    {		/* default values */
		    pulow = lowlim[scaling.primary.index / 2];
		    puupr = uprlim[scaling.primary.index /2];
		    }
		else
		    {		/* the common transform #90 values */
		    pulow = prim_low;
		    puupr = prim_upp;
		    }    

		if (index != 201) {	//for the case 90 - no index control
			if ((scaling.primary.index > PRIMARY_MAX) || (index > COMMON_MAX))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Invalid primary or common transform index.");
			}

		switch (index) {
		case 0:
			return xx;

		case 2:
		case 40:
			state = (xx - c[2]) * c[1] / c[0];
			return (state);

		case 4:
			state = xx * c[1] + c[0];
			return (state);

		case 6:
			state = xx * c[1] / c[0];
			return (state);

			/* Uncommon transform 8: X = (X'- C4)C3 / (C1 - C2(X' -C4)) */
		case 8:
			if ((c[0] + c[1] * (c[3] - xx)) == 0) {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Division by zero detected!");
			}
			state = c[2] * (xx - c[3]) / (c[0] - c[1] * (xx - c[3]));
			return (state);

		case 10:
			if (((xx - c[2]) * c[0]) == 0) {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Division by zero detected!");
			}
			state = c[1] / ((xx - c[2]) * c[0]);
			return (state);

		case 12:
			if ((c[0] == 0.) && (c[1] == 0.) && (c[2] != 0.)) {
				/* we have quadratic equation c[4]+c[3]y+c[2]*y^2 = x */
				double aa, bb, cc, discr, result;
				aa = c[2];
				bb = c[3];
				cc = c[4] - xx;
				discr = bb * bb - 4. * aa * cc;
				if (discr < 0)
					throw new AcnetStatusException(DIO_SCALEFAIL, "Scaling failed: sqrt from number < 0");
				result = (-bb + Math.sqrt(discr)) / (2. * aa);
				return (result);
			} else { /* just default - binary search */
				tol = puupr / toldiv; /* calculate tolerance */
				pumid = binarySearch(tol, pulow, puupr, xx);
				return pumid;
			}

		case 20:
			if (c[2] == 0.0)
				tmpx = xx; /* old version */
			else
				tmpx = xx - c[2]; /* new version */
			a = -4 * tmpx * c[0] * c[1] + 1;
			if (a < 0 || tmpx == 0) {
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"Division by zero or sqrt of a negative detected!");
			}
			b = (float) (((1 - 2 * tmpx * c[0] * c[1] + Math.sqrt(a)) / (2
					* tmpx * c[0] * c[0])) / 0.43429);
			state = (float) (Math.exp(b));
			return (state);

			/*
			 * case 22: state = (float)(c[0]*log10(xx/c[1])); return (state); break;
			 */

		case 28:
			if ((c[0] * (xx - c[3])) == 0) {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Division by zero detected!");
			}
			state = c[2] / (c[0] * (xx - c[3])) - (c[1] / c[0]);
			return (state);

		case 32:
			a = (xx - c[2]) / c[1];
			state = (float) (Math.exp(a) / c[0] - c[3]);
			return (state);

		case 34:
			if ((c[0] - c[2] * xx) == 0) {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Division by zero detected!");
			}
			state = (c[3] * xx - c[1]) / (c[0] - c[2] * xx);
			return (state);

		case 36:
			state = ((xx - c[2]) / c[1]) * ((xx - c[2]) / c[1]) - c[0];
			return (state);

		case 38:
			if (xx == 7.6E5)
			    return c[5];
			if ( (xx > 7.6E5 )||(xx <= 0) )
				throw new AcnetStatusException(DIO_SCALEFAIL, "Value is out of range");
		    	
			if (((c[2] == 0)&&(c[3] == 0)&&(c[4] == 0))&&(c[1] != 0) )
		    	    return  (log10(xx) - c[0]) / c[1];
			if (scaling.primary.index == 16) {		/* raw floating point number */
		    	    pulow = 1.00001*c[5];	/* empirically determined 1.00001 */
		    	    puupr = 15.0;		/* empirically determined */
		    	}
			else {
		    	    if (c[5] == 0)
		                pulow = 0.00001;	/* empirically determined 0.00001 */
		            else
		                pulow = 1.00001*c[5];	/* empirically determined 1.00001 */
		        }
			tol = (float) (puupr / toldiv); /* calculate tolerance */
			return rootBisection(xx, pulow, puupr, tol);

		case 50:
			state = (float) (c[1] * Math.cos(xx / c[0]));
			return (state);

		case 56:
			 // determine the value via an  interpolation  table  (linear)
			state = (float) reverseInterpolate((int) c[0], new double[] {
					c[1], c[2] }, ScalingInternal.INTERPOLATE_LINEAR, xx);
			return (state);

		case 58:
			// determine the value via an interpolation table (exponential)
			state = (float) reverseInterpolate((int) c[0], new double[] {
					c[1], c[2] }, ScalingInternal.INTERPOLATE_EXP, xx);
			return (state);

			/*
			 * * Special display types
			 */
		case 60:
			return xx;
			//throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Required");

		case 62:
			if ((c[0] == 0.0) || (c[1] == 0.0))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Zero constant is invalid");
			tmpx = xx / c[1] - c[2];
			if (tmpx < 0)
				throw new AcnetStatusException(DIO_SCALEFAIL, "Logarithm of negative value");

			state = c[0] * Math.log(tmpx) / Math.log(10);
			return (state);

		case 64:
			pulow = -1.75489; /* -1.75489 corresponds 0 DegK */
			puupr = 5.05; /* -1.75489 to 5.05 instead of -FLT_MAX ... +FLT_MAX */
			tol = (double) ((puupr - pulow) / toldiv); /* calculate tolerance */
			if ((xx < 0) || (xx > scale(puupr)))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Value is out of range");
			pumid = binarySearch(tol, pulow, puupr, xx);
			return (pumid);

		case 66:
			state = ( (Math.log((xx - c[3]) / c[0]) / 0.6931471805599453) - c[2])/ c[1];
								 /* /Math.log(2.0) */
			return (state);

		case 68:
			puupr = 0.0; // volts must be <= 0 because of LOGe
			tol = (float) ((puupr - pulow) / toldiv); /* calculate tolerance */
			if ((xx < 0) || (xx > scale(pulow)))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Value is out of range");
			pumid = binarySearch(tol, pulow, puupr, xx);
			return pumid;

		case 74:
			a = xx*c[5] - c[2];
			b = xx*c[4] - c[1];
			g = xx*c[3] - c[0];
			d = b*b - 4*a*g;
			if ( d < 0 ) 
				throw new AcnetStatusException(DIO_SCALEFAIL, "sqrt of a negative detected!");
			d = Math.sqrt(d);
			tmpx = (-b + d) / (2*a);
			if ( (tmpx >= pulow)&&(tmpx <= puupr))
			    state = tmpx;
			else
			    state = (-b - d) / (2*a);
			if ((state < pulow) || (state > puupr))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Value is out of range");
			return (state);

			// X' = C2*X**C3, X < C1 or X' = C4*exp(C5*X + C6)
		case 76:
			a = c[1]*Math.pow(c[0], c[2]);	// should be = c[3]*Math.exp(c[4]*c[1]+c[5])
			b = c[3]*c[4];	// it's used for the increasing test

			if ((c[1] > 0)&&(b > 0) ) {
				/* both functions are increasing */
			    if (xx < a)
				state = Math.pow(xx/c[1], 1.0/c[2]);
			    else
				state = (Math.log(xx/c[3]) - c[5])/c[4]; 
			} else
			if ((c[1] < 0)&&(b < 0) ) {
				/* both functions are decreasing */
			    if (xx >= a)
				state = Math.pow(xx/c[1], 1.0/c[2]);
			    else
				state = (Math.log(xx/c[3]) - c[5])/c[4]; 
			} else
			if ((c[1] > 0)&&(b < 0) ) {
				/* left function is increasing, right one is decreasing  */
			    if (xx < a)
				state = Math.pow(xx/c[1], 1.0/c[2]);
			    else
				throw new AcnetStatusException(DIO_SCALEFAIL, "Value is out of range"); 
			} else {
				/* left function is decreasing, right one is increasing  */
			    if (xx >= a)
				state = Math.pow(xx/c[1], 1.0/c[2]);
			    else
				throw new AcnetStatusException(DIO_SCALEFAIL, "Value is out of range"); 
			}
			return (state);

			// X' = C1*10**(C2*X+C3) + C4, X = (log10((xx-C4)/C1)-C3)/C2;
		case 78:
			state = (Math.log ((xx - c[3])/c[0]) / Math.log(10) - c[2]) / c[1];
 			return (state);


			// X' = C3*X + C4 on X < C1, X' = exp(C5*X + C6) on X > C2,
			// the logarithmic interpolation on C1 <= X <= C2 
		case 86:
	        	double x_0, x_1, y_0, y_1;
		        x_0 = c[0]; y_0 =  c[2] * c[0] + c[3];
			x_1 = c[1]; y_1 = Math.exp( c[4] * c[1] + c[5]);
			if ( xx > y_0) {
			    state = (xx - c[3])/c[2];
			    }
			else {
			    if ( xx < y_1)
			        state = (Math.log(xx)-c[5])/c[4];
			    else 	/* y_1 <= xx <= y_0 */
			        state = (x_1-x_0)*Math.log(xx/y_0)/Math.log(y_1/y_0) + x_0;
	    		    }  	    
			return(state);


		default: // do the binary search for the result 
			tol = puupr / toldiv; /* calculate tolerance */
			pumid = binarySearch(tol, pulow, puupr, xx);
			return pumid;
		}
	}

    private double reverseMfcTransform(double data) throws AcnetStatusException
	{
		final double[] c = scaling.common.constants;
		int num_constants;

		double x;
		double flt_data = 0;;
		int il, jl=0, sts;
		int table_number;
		int table_pointer;
		int n_of_diapasons;		// the table size: number of primary diapasons

		num_constants = scaling.common.constants.length;

		if (num_constants < 3)
			throw new AcnetStatusException(DIO_SCALEFAIL,
				"Insufficient number of constants provided in PDB.");

		ScalingInternal.read_multifunction_transforms();

		table_number = (int)c[0];
		if ((table_number > ScalingInternal.n_mfc_transform_tables) ||
				(table_number <= 0) ) {
			ScalingInternal.n_mfc_transform_tables = 0;
			ScalingInternal.read_multifunction_transforms();
			if ((table_number > ScalingInternal.n_mfc_transform_tables) ||
						(table_number <= 0) ) {
				flt_data = 0.0;
				throw new AcnetStatusException(DIO_SCALEFAIL,
					"multifunction transform: bad argument (table_number)");
			}
		}
		
		x = data;

			// copy the interpolation tables
		table_pointer = ScalingInternal.mfct_table_offset(table_number);
		n_of_diapasons = ScalingInternal.mfct_table_size[table_number - 1];


				/* looking for x's diapason and conversion */   
		for (il = table_pointer; il < table_pointer+n_of_diapasons; il++) {
			if ( ((x >= ScalingInternal.all_transform_values[il].common_initial)&&
				 (x <= ScalingInternal.all_transform_values[il].common_final)) || 
			 ((x <= ScalingInternal.all_transform_values[il].common_initial)&&
				 (x >= ScalingInternal.all_transform_values[il].common_final))  )  {
			/* the diapason has been found, proceed to the conversion */
			jl = ScalingInternal.all_transform_values[il].common_transform;
			flt_data  = _unscale( x, jl, ScalingInternal.all_transform_values[il].cx,
								  ScalingInternal.all_transform_values[il].primary_initial,
								  ScalingInternal.all_transform_values[il].primary_final);
			break;
			}
		}
		
		if (il == n_of_diapasons)
			throw new AcnetStatusException(DIO_SCALEFAIL, "multifunction_table: primary_value out of diapasons (" + x + ")");

			/* c[1] = lower limit, c[2] = upper limit for primary units */
		if ((flt_data < c[1]) || (flt_data > c[2]))
			throw new AcnetStatusException(DIO_SCALEFAIL, "multifunction_table: primary_value out of range (" + flt_data + ")");

		return(flt_data);

    }

	private double binarySearch(double tol, double pul, double puu, double xx) throws AcnetStatusException 
	{
		double diff, toler; //tolerance
		double culow, cuupr, cumid; //transform function values
		double pulow, puupr, pumid; //interval limits and the middle
		double d1, d2, d3;
		int max_search_iterations = 1000;
		int iter = 0;

		toler = tol;
		if (toler > 0.0001)
			toler = 0.0001; // the smaller tolerance
		pulow = pul;
		puupr = puu;

		/* scale upper limit to common units */
		cuupr = scale(puupr) - xx;
		/* scale lower  limit to common units */
		culow = scale(pulow) - xx;

		while (true) {

			/* check if the same sign at the ends */
			if (((cuupr > 0) && (culow > 0)) || ((cuupr < 0) && (culow < 0))) {
				pumid = (pulow + puupr) / 2.;
				cumid = scale(pumid) - xx;
				if ((Math.abs(puupr) < FLT_MAX / 2.0)
						&& (Math.abs(pulow) < FLT_MAX / 2.0)) {
					diff = puupr - pulow;
					if (Math.abs(diff) <= toler) /* our search is ended - no interval */
						throw new AcnetStatusException(DIO_SCALEFAIL,
								"Scaling failed: interval not found");
					/* small interval but the same signs at the ends */
				}

				/* computer accuracy is not enough to divide by 2 ?*/
				if ((pumid == pulow) || (pumid == puupr))
					throw new AcnetStatusException(DIO_SCALEFAIL,
							"Scaling failed: Interval Not Found");

				/* looking for different signs (of the function at the ends) */
				if (((cuupr > 0) && (cumid < 0))
						|| ((cuupr < 0) && (cumid > 0))) {
					puupr = pumid; /* rearrange the right end of the interval */
					cuupr = cumid; /* the transform function value at */
					break;
				}

				/* looking for a better approximation to 0 (indirectly to xx)*/
				d1 = Math.abs(cumid);
				d2 = Math.abs(culow);
				d3 = Math.abs(cuupr);
				if ((d3 > d1) && (d3 >= d2)) {
					/* we see: d3 is the worst one - get rid of it 
					we take the left half of the preceeded interval */
					puupr = pumid; /* rearrange the right end of the interval */
					cuupr = cumid; /* the transform function value at */
				} else {
					if ((d2 > d1) && (d2 >= d3)) {
						/* we see: d2 is the worst one - get rid of it 
						we take the right half of the preceeded interval */
						pulow = pumid; /* rearrange the left end of the interval */
						culow = cumid; /* the transform function value at */
					} else {
						/* the middle appoximation is not better - so stop */
						throw new AcnetStatusException(DIO_SCALEFAIL,
								"Scaling failed: Interval not found");
					}
				}
				iter++;
				if (iter >= max_search_iterations)
					/* we're not converging - give up, too many iterations */
					throw new AcnetStatusException(DIO_SCALEFAIL,
							"Scaling failed: Interval not found");
			} else
				break; /* if different signs interval has been found or is */

		} /* go back for next iteration */

		/* */
		iter = 0;
		while (true) {
			pumid = (pulow + puupr) / 2.0; /* calculate midpoint */

			if ((Math.abs(puupr) < FLT_MAX / 2.0)
					|| (Math.abs(pulow) < FLT_MAX / 2.0)) {
				diff = puupr - pulow;
				if (Math.abs(diff) <= toler) /* our search is ended */
					return pumid;
			}
			/* computer accuracy is not enough to divide by 2 ?*/
			if ((pumid == pulow) || (pumid == puupr))
				return pumid;

			/* scale upper limit to common units */
			cuupr = scale(puupr);

			/* scale midpoint to common units */
			cumid = scale(pumid);

			/* we've hit it exactly */
			if (cumid == xx) /* value returned - all done */
			{
				return pumid;
			} else if (cumid > xx && cuupr >= xx) /*
			 * value is in lower half of
			 * range (for positive
			 * slope)
			 */
			{
				puupr = pumid; /* move upper limit down */
			} else if (cumid < xx && cuupr <= xx) /*
			 * value is in upper half of
			 * range (for negative
			 * slope)
			 */
			{
				puupr = pumid; /* move upper limit up */
			} else if (cumid < xx && cuupr >= xx) /*
			 * value is in upper half of
			 * range (for positive
			 * slope)
			 */
			{
				pulow = pumid; /* move lower limit up */
			} else if (cumid > xx && cuupr <= xx) /*
			 * value is in lower half of
			 * range (for negative
			 * slope)
			 */
			{
				pulow = pumid; /* move lower limit down */
			}

			iter++;
			if (iter >= max_search_iterations)
				/* we're not converging - give up */
				throw new AcnetStatusException(DIO_SCALEFAIL, "Scaling failed: too many iterations");

		} /* go back for next iteration */

	}

	private double rootBisection(double desired, double x1, double x2, double xacc) throws AcnetStatusException 
	{
		int j;
		double dx, f, fmid, xmid, rtb;

		f = scale(x1) - desired;

		fmid = scale(x2) - desired;

		if ((f * fmid) >= 0.0)
			return -4; // desired value must be bracketed

		// rtb = (f < 0.0) ? (dx = x2 - x1, x1) : (dx = x1 - x2, x2);
		if (f < 0.0) {
			rtb = x1;
			dx = x2 - x1;
		} else {
			dx = x1 - x2;
			rtb = x2;
		}

		for (j = 1; j <= MAX_ITERATIONS; j++) { // iterate until the result is in tolerance
			xmid = rtb + (dx *= 0.5);
			fmid = scale(xmid) - desired;
			if (fmid <= 0.0)
				rtb = xmid;
			if ((Math.abs(dx) < xacc) || (fmid == 0.0))
				return (rtb);
		}

		return 0.0;
	}

	private double interpolate(int table_number, double[] passed_absolute_limits, int type, double primary_value) throws AcnetStatusException 
	{
		int tbl;
		int table_pointer, table_size;
		boolean check_low_high;
		boolean limits_reversed;
		double absolute_limits[] = new double[2];
		double dx, gx, gy;
		double log_result;
		double [] primary_values = new double[ScalingInternal.MAX_TABLE_SIZE];
		double [] common_values  = new double[ScalingInternal.MAX_TABLE_SIZE];
		double common_value;

		ScalingInternal.read_interpolation_tables();

		if ((table_number > ScalingInternal.n_interpolation_tables)
				|| (table_number <= 0)) {
		// try to read again all tables - may be some ones were added recently
			ScalingInternal.n_interpolation_tables = 0;
			ScalingInternal.read_interpolation_tables( );
			if ((table_number > ScalingInternal.n_interpolation_tables)
				|| (table_number <= 0)) {
				common_value = 0.0;
				throw new AcnetStatusException(DIO_SCALEFAIL,
					"interpolate_table: bad argument (table_number)");
			}
			// return (DIO_BADARG);
		}
			// copy the interpolation tables
		table_pointer = ScalingInternal.interpolation_table_offset(table_number);
		table_size = ScalingInternal.table_size[table_number - 1];
		System.arraycopy(ScalingInternal.table_primary_values, table_pointer,
				 primary_values, 0, table_size);
		System.arraycopy(ScalingInternal.table_common_values, table_pointer,
				 common_values, 0, table_size);

		if ((passed_absolute_limits[0] != 0.0)
				|| (passed_absolute_limits[1] != 0.0)) {
			// use passed limits
			absolute_limits[0] = passed_absolute_limits[0];
			absolute_limits[1] = passed_absolute_limits[1];
			check_low_high = true;
		} else {
			// use limits from table
			absolute_limits[0] = primary_values[0];
			absolute_limits[1] = primary_values[table_size - 1];
			check_low_high = false;
		}

		if ((primary_value < absolute_limits[0]) || (primary_value > absolute_limits[1]))
			throw new AcnetStatusException(DIO_SCALEFAIL, "interpolate_table: primary_value out of range (" + primary_value + ")");

		if (check_low_high) {
			// absolute limits may be wider than the table limits
			limits_reversed = (common_values[0] > common_values[table_size - 1]);
			if (false && limits_reversed)
				System.out.println("ReadSetScaling, limits reversed");
			if ((primary_value < primary_values[0])
					&& (primary_value >= absolute_limits[0])) {
				// value is low, but acceptable
				common_value = common_values[0];
				return common_value; // ?
				/*
				 * if (limits_reversed) throw new AcnetStatusException(DIO_SCALEFAIL,
				 * "interpolate_table: primary_value is too high" ); else throw
				 * new AcnetStatusException(DIO_SCALEFAIL "interpolate_table: primary_value is too
				 * low" );
				 */
			}
			if ((primary_value > primary_values[table_size - 1])
					&& (primary_value <= absolute_limits[1])) {
				// value is high, but acceptable
				common_value = common_values[table_size - 1];
				return common_value; // ?
				/*
				 * if (limits_reversed) throw new AcnetStatusException(DIO_SCALEFAIL,
				 * "interpolate_table: primary_value is too low" ); else throw
				 * new AcnetStatusException(DIO_SCALEFAIL "interpolate_table: primary_value is too
				 * high" );
				 */
			}
		}

		common_value = common_values[0]; // x value is too small (default)

		switch (type) {
		case ScalingInternal.INTERPOLATE_LINEAR: // linear interpolation
			for (tbl = table_size - 1; tbl >= 0; tbl--) {
				if (primary_value >= primary_values[tbl]) { // found slot in
					// table
					if (tbl == (table_size - 1)) {
						common_value = common_values[tbl]; // x value is too
						// big (out of
						// domain)
						break;
					}
					dx = primary_value - primary_values[tbl]; // primary_values[tbl]
					// <= x <
					// primary_values[tbl+1]
					gx = primary_values[tbl + 1] - primary_values[tbl];
					gy = common_values[tbl + 1] - common_values[tbl];
					common_value = common_values[tbl] + (dx / gx) * gy;
					break;
				}
			}
			break;
		case ScalingInternal.INTERPOLATE_EXP: // exponential interpolation
			for (tbl = table_size - 1; tbl >= 0; tbl--) {
				if (primary_value >= primary_values[tbl]) { // found slot in
					// table
					if (tbl == (table_size - 1)) {
						common_value = common_values[tbl]; // x value is too
						// big (out of
						// domain)
						break;
					}
					dx = primary_value - primary_values[tbl]; // primary_values[tbl]
					// <= x <
					// primary_values[tbl+1]
					gx = primary_values[tbl + 1] - primary_values[tbl];
					gy = log10(common_values[tbl + 1])
							- log10(common_values[tbl]);
					log_result = log10(common_values[tbl]) + (dx / gx) * gy;
					common_value = Math.pow(10.0, log_result);
					break;
				}
			}
			break;
		default:
			throw new AcnetStatusException(DIO_SCALEFAIL, "interpolate_table: bad argument (type)");
		}

		return common_value;
	}

	private double reverseInterpolate(int table_number, double[] passed_absolute_limits, int type, double common_value) throws AcnetStatusException 
	{

		boolean inverted_table;
		int tbl;
		int table_pointer, table_size;
		double absolute_limits[] = new double[2];
		double dx, gx, gy;
		double [] primary_values = new double[ScalingInternal.MAX_TABLE_SIZE];
		double [] common_values  = new double[ScalingInternal.MAX_TABLE_SIZE];
		double primary_value = 0.0;

		ScalingInternal.read_interpolation_tables( );
		if ((table_number > ScalingInternal.n_interpolation_tables)
				|| (table_number <= 0)) {
		// try to read again all tables - may be some ones were added recently
			ScalingInternal.n_interpolation_tables = 0;
			ScalingInternal.read_interpolation_tables( );
			if ((table_number > ScalingInternal.n_interpolation_tables)
				|| (table_number <= 0)) {
				common_value = 0.0;
				throw new AcnetStatusException(DIO_SCALEFAIL,
					"reverse_interpolate_table: bad argument (table_number)");
			}
		}
			// copy the interpolation tables
		table_pointer = ScalingInternal.interpolation_table_offset(table_number);
		table_size = ScalingInternal.table_size[table_number - 1];
		System.arraycopy(ScalingInternal.table_primary_values, table_pointer,
				 primary_values, 0, table_size);
		System.arraycopy(ScalingInternal.table_common_values, table_pointer,
				 common_values, 0, table_size);

		// what type of the table? (increasing(inverted_table = true) or
		// decreasing)
		if (common_values[0] > common_values[table_size - 1]) {
			inverted_table = true; // decreasing table
			if ((common_value < common_values[table_size - 1])
					|| (common_value > common_values[0])) // the value in
				// range or out ?
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"reverse_interpolate_table: common value out of range");
		} else {
			inverted_table = false; // increasing table
			if ((common_value < common_values[0])
					|| (common_value > common_values[table_size - 1])) // the
				// value
				// in
				// range
				// or
				// out ?
				throw new AcnetStatusException(DIO_SCALEFAIL,
						"reverse_interpolate_table: common value out of range");
		}

		if ((passed_absolute_limits[0] != 0.0)
				|| (passed_absolute_limits[1] != 0.0)) {
			// use passed limits
			absolute_limits[0] = passed_absolute_limits[0];
			absolute_limits[1] = passed_absolute_limits[1];
		} else {
			// use limits from table
			absolute_limits[0] = primary_values[0];
			absolute_limits[1] = primary_values[table_size - 1];
		}

		primary_value = primary_values[0]; // x value is too small (default)

		switch (type) {
		case ScalingInternal.INTERPOLATE_LINEAR: // linear interpolation
			if (inverted_table) { // decreasing table
				for (tbl = 0; tbl < table_size; tbl++) {
					if (common_value >= common_values[tbl]) {/*
					 * found slot in
					 * table
					 */
						if (tbl == 0) {
							primary_value = primary_values[tbl]; /*
							 * x value
							 * is too
							 * big (out
							 * of
							 * domain)
							 */
							break;
						}
						dx = common_values[tbl - 1] - common_value; /*
						 * common_values[tbl] <=
						 * x <
						 * common_values[tbl-1]
						 */
						gx = common_values[tbl - 1] - common_values[tbl];
						gy = primary_values[tbl] - primary_values[tbl - 1];
						primary_value = primary_values[tbl - 1] + (dx / gx)
								* gy;
						break;
					}
				}
			} else { // increasing table
				for (tbl = table_size - 1; tbl >= 0; tbl--) {
					if (common_value >= common_values[tbl]) {/*
					 * found slot in
					 * table
					 */
						if (tbl == (table_size - 1)) {
							primary_value = primary_values[tbl]; /*
							 * x value
							 * is too
							 * big (out
							 * of
							 * domain)
							 */
							break;
						}
						dx = common_value - common_values[tbl]; /*
						 * common_values[tbl] <=
						 * x <
						 * common_values[tbl+1]
						 */
						gx = common_values[tbl + 1] - common_values[tbl];
						gy = primary_values[tbl + 1] - primary_values[tbl];
						primary_value = primary_values[tbl] + (dx / gx) * gy;
						break;
					}
				}
			}
			break;

		case ScalingInternal.INTERPOLATE_EXP: // exponential interpolation
			if (inverted_table) { // decreasing table
				for (tbl = 0; tbl < table_size; tbl++) {
					if (common_value >= common_values[tbl]) { /*
					 * found slot in
					 * table
					 */
						if (tbl == 0) {
							primary_value = primary_values[tbl]; /*
							 * x value
							 * is too
							 * big (out
							 * of
							 * domain)
							 */
							break;
						}
						dx = log10(common_values[tbl - 1])
								- log10(common_value); /*
						 * common_values[tbl] <=
						 * x <
						 * common_values[tbl-1]
						 */
						gx = log10(common_values[tbl - 1])
								- log10(common_values[tbl]);
						gy = primary_values[tbl] - primary_values[tbl - 1];
						primary_value = primary_values[tbl - 1] + (dx / gx)
								* gy;
						break;
					}
				}
			} else { // increasing table
				for (tbl = table_size - 1; tbl >= 0; tbl--) {
					if (common_value >= common_values[tbl]) { /*
					 * found slot in
					 * table
					 */
						if (tbl == (table_size - 1)) {
							primary_value = primary_values[tbl]; /*
							 * x value
							 * is too
							 * big (out
							 * of
							 * domain)
							 */
							break;
						}
						dx = log10(common_value) - log10(common_values[tbl]); /*
						 * common_values[tbl] <=
						 * x <
						 * common_values[tbl+1]
						 */
						gx = log10(common_values[tbl + 1])
								- log10(common_values[tbl]);
						gy = primary_values[tbl + 1] - primary_values[tbl];
						primary_value = primary_values[tbl] + (dx / gx) * gy;
						break;
					}
				}
			}
			break;
		}

		return primary_value;
	}

	private double log10(double val) 
	{
		return Math.log(val) / Math.log(10.0);
	}
}
