// $Id: Primary.java,v 1.1 2024/01/05 21:26:06 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

class Primary implements AcnetErrors
{
	//static final int PRIMARY_MAX = 84;

	final DeviceInfo.ReadSetScaling scaling;

	Primary(DeviceInfo.ReadSetScaling scaling)
	{
		this.scaling = scaling;
	}

	double scale(int data) throws AcnetStatusException 
	{
		int int_temp = 0;
		int i = 0;
		int x = 0;
		int y = 0;
		int x1 = 0, x2 = 0;
		long unsigned_x = 0;
		long unsigned_y = 0;
		double flt_data = 0;

		final int length = scaling.primary.inputLen;

		switch (length) {
		 case 1: // Input data length = 8 bits
			x = (byte) (data & 0xff);
			break;
		 case 2: // Input data length = 16 bits
			x = (short) (data & 0xffff);
			break;
		 case 4: // Input data length = 32 bits
			x = (int) (data & 0xffffffff);
			break;
		 default: // Input data length undefined
			flt_data = 0;
			throw new AcnetStatusException(DIO_SCALEFAIL, "Invalid scaling length field");
		}

		// Scale input data to PRIMARY units based upon the primary transform index
		
		switch (scaling.primary.index) {

			/*
			 * * Process to primary units where * X = FLOAT(input)/3200.0
			 */
		case 0:
			flt_data = x / 3200.0;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input)/3276.8
			 */
		case 2:
			flt_data = x / 3276.8;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input)/6553.6
			 */
		case 4:
			flt_data = x / 6553.6;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input)/13107.2
			 */
		case 6:
			flt_data = x / 13107.2;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input)+32768.0
			 */
		case 8:
			flt_data = x + 32768.0;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input) --> input in DEC
			 * word ordering
			 */

		case 10:
			flt_data = x;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input)/320.0
			 */
		case 12:
			flt_data = x / 320.0;
			return flt_data;

			/*
			 * * Process to primary units where * UD = ddlcccmmmmmmmmmm * X =
			 * FLOAT(m)*10**c
			 */
		case 14:
			flt_data = x & 0x03FF;
			int_temp = (int) (x & 0x1C00);
			int_temp >>= 10;
			for (i = 0; i < int_temp; i++)
				flt_data *= 10.0;
			return flt_data;

			/*
			 * * Process to primary units where * X = IEEE to DEC floating --> input
			 * in DEC word ordering
			 */
		case 16:
			/* NOTE - word swapping not needed */
			flt_data = Float.intBitsToFloat(x);
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input)*0.0010406
			 */
		case 18:
			flt_data = x * 0.0010406;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input) + (65536.0 if
			 * input negative and 16-bits) -OR- * X = FLOAT(input) + (256.0 if input
			 * negative and 8-bits) -OR- * X = FLOAT(input)
			 */
		case 20:
			flt_data = x;
			if (flt_data >= 0.0)
				return flt_data;
			if (length != 1)
				flt_data += 65536.0;
			else
				flt_data += 256.0;
			return flt_data;

			/*
			 * * Process to primary units where * X = input --> input in DEC FLOAT
			 * format
			 */
		case 22:
			/*
			 * Note: the precise format of these statements are VERY important
			 * due to Java's promototion of intermediate results to long ints.
			 * While the additional bitmasks appear unnecessary, they insure
			 * that Java treats negative intermediate results correctly.
			 */
			x1 = (((x & 0xffff0000) >> 16) & 0x0000ffff);
			x2 = (((x & 0x0000ffff) << 16) & 0xffff0000);
			y = x1 | x2; // Word swap
			flt_data = Float.intBitsToFloat(y) / (float) 4.0; // DEC
			// float/4=IEEE
			// float
			return flt_data;

			/*
			 * * Process to primary units where * X = IEEE to DEC floating --> input
			 * in 68000 word ordering
			 */
		case 24:
			// Again with the extra bitmasks to fix results
			x1 = (((x & 0xffff0000) >> 16) & 0x0000ffff);
			x2 = (((x & 0x0000ffff) << 16) & 0xffff0000);
			y = x1 | x2;
			flt_data = Float.intBitsToFloat(y);

			//System.out.println("scaled: " + flt_data + "   " + Float.intBitsToFloat(y));
			return flt_data;

			/*
			 * * Process to primary units where * X =
			 * (FLOAT(UNS(input)/256)/82.1865)-0.310269935
			 */
		case 26:
			x >>= 8;
			y = x & 0xFF;
			flt_data = (y / 82.1865) - 0.310269935;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input) --> input in
			 * 68000 word ordering
			 */
		case 28:
			return ((x >> 16) & 0x0000ffff) | (x << 16);

			/*
			 * * Process to primary units where * X = FLOAT(input) --> input = lo
			 * byte (signed)
			 */
		case 30:
			flt_data = (byte) x;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input) --> input = hi
			 * byte (signed)
			 */
		case 32:
			x >>= 8;
			flt_data = (byte) x;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input) --> input = lo
			 * byte (unsigned)
			 */
		case 34:
			flt_data = x & 0xFF;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input) --> input = hi
			 * byte (unsigned)
			 */
		case 36:
			x >>= 8;
			flt_data = x & 0xFF;
			return flt_data;

			/*
			 * * Process to primary units where * X =
			 * (FLOAT(UNS(input))/82.1865)-0.310269935
			 */
		case 38:
			y = x & 0xFF;
			flt_data = (y / 82.1865) - 0.310269935;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input)/256.0
			 */
		case 40:
			flt_data = x / 256.0;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input)/6553.6 --> input =
			 * 16-bit unipolar 10V DAC
			 */
		case 42:
			x &= 0xFFFF;
			flt_data = x / 6553.6;
			return flt_data;

			/*
			 * * Process to primary units where * X = 7-digit BCD decoding --> see
			 * below * * 28 bits (7-digit BCD) are decoded and the resultant decimal *
			 * equivalent FLOATed ... * * +---+---+---+---+ +---+---+---+---+ * | |
			 * 1 | 2 | 3 | | 4 | 5 | 6 | 7 | * +---+---+---+---+ +---+---+---+---+ * *
			 * ... the BCD digits are isolated and converted in order 1 - 7
			 */
		case 44:
			flt_data = 0.0;
			for (i = 0; i < 7; i++) {
				x <<= 4;
				int_temp = (int) ((x >> 28) & 0xF);
				flt_data *= 10.0;
				flt_data += int_temp;
			}
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input) --> input is
			 * unsigned I*4
			 */
		case 46:

			if (x >= 0)
				unsigned_x = x;
			else
				unsigned_x = x + 4294967296L; //(2**32)
			flt_data = unsigned_x;
			return flt_data;

			/*
			 * * Process to primary units where * X = (IEEE to DEC floating)/0.036
			 */
		case 48:
			flt_data = Float.intBitsToFloat(x) / .036;
			return flt_data;

			/*
			 * * Process to primary units where * X = IEEE to DEC float --> result
			 * clamped to +/- 10.24
			 */
		case 50:
			flt_data = Float.intBitsToFloat(x);
			if (flt_data < -10.24)
				flt_data = -10.24;
			if (flt_data > 10.235)
				flt_data = 10.235;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input) --> input =
			 * byte-swapped word (1|2 -> 2|1) * or longword (1|2|3|4 -> 4|3|2|1)
			 */
		case 52:
			if (length == 2) {
				// Byte swap raw data
				y = (((x & 0xff00) >> 8) & 0x00ff) | ((x << 8) & 0xff00);
				flt_data = (short) y;
			} else if (length == 4) {
				y = (x << 24) & 0xff000000;
				y |= ((x & 0x0000ff00) << 8) & 0x00ff0000;
				y |= ((x & 0x00ff0000) >> 8) & 0x0000ff00;
				y |= ((x & 0xff000000) >> 24) & 0x000000ff;
				flt_data = (int) y;
			}
			// flt_data = y;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input)*0.000488+4.0 -->
			 * input = 0x0000 - 0x7FFF * (for PLC devices whose fundamental units
			 * are mA)
			 */

		case 54:
			if ((length != 2) || (((short) data & 0x8000) != 0)) {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Corrupt data");
			}
			flt_data = x * 0.0004882961516 + 4.0;
			return flt_data;

			/*
			 * * Process to primary units where * X = (FLOAT(input)-32768.0)/3276.8
			 * --> input = unsigned word * (for certain stepper motor controllers)
			 */
		case 56:
			if (length != 2) { /* must be a two byte quantity */
				throw new AcnetStatusException(DIO_SCALEFAIL, "PDB data length error - must be two byte quantity.");
			}
			unsigned_x = data & 0x0000ffff;
			flt_data = ((double) unsigned_x - 32768.0) / 3276.8;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(UNS(input))/256.0
			 */
		case 58:
			unsigned_x = data & 0xffffffff;
			flt_data = unsigned_x / 256.0;
			return flt_data;

			/*
			 * * Process to primary units where * X = (IEEE to DEC floating)*500.0;
			 */
		case 60:
			/* NOTE - word swapping not needed */
			flt_data = 500.0 * Float.intBitsToFloat(x);
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input)/6400.0 --> input =
			 * 16-bit unipolar 0 - 10.24V DAC
			 */
		case 62:
			flt_data = x / 6400.0;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input) scaled to +/- 1.0
			 */
		case 64:
			if (length == 1)
				flt_data = x / 128.0;
			else if (length == 2)
				flt_data = x / 32768.0;
			else
				flt_data = x / 2147483648.0;
			return flt_data;

			/*
			 * * Process to primary units where * X = FLOAT(input)/3200.0 --> input =
			 * word > 0
			 */
		case 66:
			if (length != 2)
				throw new AcnetStatusException(DIO_SCALEFAIL, "PDB data length error - must be two byte quantity.");
			if (x <= 0.0)
				throw new AcnetStatusException(DIO_SCALEFAIL, "Corrupt data");
			flt_data = x / 3200.0;
			return flt_data;

			/*
			 * * Special display types
			 */
		case 68:
			throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Required");

			/*
			 * * Process to primary units where * X = FLOAT(input)/1000.0
			 */
		case 70:
			flt_data = x / 1000.0;
			return flt_data;

			/*
			 * * Process to primary units where * X = (FLOAT(input)-32768.0)/3200.0
			 * --> input = unsigned word
			 */
		case 72:
			if (length != 2) { /* must be a two byte quantity */
				throw new AcnetStatusException(DIO_SCALEFAIL, "PDB data length error - must be two byte quantity.");
			}
			unsigned_x = data & 0x0000ffff;
			flt_data = ((double) unsigned_x - 32768.0) / 3200.0;
			return flt_data;
			/*
			 * Process to primary units where * X = FLOAT(input)*0.00064088
			 * --> input = 0x0000 - 0x7FFF (0-21 mA Allen-Bradley PLC)
			 */
		case 74:
			if (length != 2) {
				throw new AcnetStatusException(DIO_SCALEFAIL, "PDB data length error.");
			}
			flt_data = x * 0.00064088;
			return flt_data;
			/*
			 * Process to primary units where * X = FLOAT(input)
			 * --> (word swapped unsigned 32 bit)
			 */
		case 76:
			if (length != 4) {
				throw new AcnetStatusException(DIO_SCALEFAIL, "PDB data length error.");
			}

			x1 = ((x & 0xFFFF0000) >> 16) & 0x0000FFFF;
			x2 = ((x & 0x0000FFFF) << 16) & 0xFFFF0000;
			y  = x1 | x2;
			if (y >= 0)
				unsigned_y = y;
			else
				unsigned_y = (long)y + 0x100000000L; //(2**32)
			flt_data   = unsigned_y;			
			return flt_data;

			/*
			 * * Process to primary units where * X = IEEE to DEC float --> result
			 * clamped to 0.0 - 5.0
			 */
		case 78:
			flt_data = Float.intBitsToFloat(x);
			if (flt_data < 0.0)
				flt_data = 0.0;
			if (flt_data > 5.0)
				flt_data = 5.0;
			return flt_data;

			/*
			 * * Process to primary units where * X = IEEE to DEC float --> result
			 * clamped to 0.0 - 10.0
			 */
		case 80:
			flt_data = Float.intBitsToFloat(x);
			if (flt_data < 0.0)
				flt_data = 0.0;
			if (flt_data > 10.0)
				flt_data = 10.0;
			return flt_data;

			/*
			 ** Process to primary units where
			 *  X = FLOAT(input)/409.5 -->  input = 12-bit unsigned 0 - 10V
			 */
		case 82:
			if (length != 2) { /* must be a two byte quantity */
				throw new AcnetStatusException(DIO_SCALEFAIL, "PDB data length error - must be two byte quantity.");
			}
			if ( (x & 0xf000) != 0) {   /* 12-bit input data */
				throw new AcnetStatusException(DIO_SCALEFAIL, "Corrupted input data ");
			}
			flt_data = x / 409.5;
			return flt_data;

			/*
			 * * Process to primary units where * X = IEEE to native floating 
			 *   byte inverted: abcd -> dcba
			 */
		case 84:
			// Again with the extra bitmasks to fix results
			if (length != 4) { /* must be a 4 byte quantity */
				throw new AcnetStatusException(DIO_SCALEFAIL, "PDB data length error - must be two byte quantity.");
			}
			x1 = (((x & 0xff000000) >> 24) & 0x000000ff);
			y = x1;
			x1 = (((x & 0x00ff0000) >>  8) & 0x0000ff00);
			y = y | x1;
			x1 = (((x & 0x0000ff00) <<  8) & 0x00ff0000);
			y = y | x1;
			x1 = (((x & 0x000000ff) << 24) & 0xff000000);
			y = y | x1;
			flt_data = Float.intBitsToFloat(y);
			return flt_data;

			/*
			 * * NOTE!!! If a new transform is added, the constant PI_MAX *must* be
			 * * ^^^^ changed in order for the transform to be properly recognized
			 */
		default:
			throw new AcnetStatusException(DIO_SCALEFAIL, "Primary transform " + scaling.primary.index + " not found.");
		}
	}

	private short ICSHFT(short value, int nbits) 
	{
		long x = 0, y = 0;
		int n = Math.abs(nbits);

		if (nbits > 0) {
			x = value << n;
			y |= ((0xff00 & x) >> 16) & 0xffff;
			x |= y & 0xffff;
		} else {
			x = value >> n;
			y = (~(0xffff >> n)) & 0xffff & value;
			x |= y & 0xffff;
		}
		return (short) x;
	}

	/**
	 * This function is used to scale a value expressed in primary units to a
	 * corresponding value in unscaled data units.
	 * 
	 * @param x
	 *            double primary value to scale to common units
	 * @return a corresponding value expressed in unscaled data units
	 * @exception AcnetStatusException
	 *                on scaling error
	 */
	int unscale(double value) throws AcnetStatusException 
	{
		final int length = scaling.primary.inputLen;
		double xx = value;
		int x1 = 0, x2 = 0;
		long y = 0;
		short expon;
		double mantis;
		int state = 0;
		int unsigned_state;
		short hbit;
		int temp;

		//if (scaling.primary.index > PRIMARY_MAX)
			//throw new AcnetStatusException(DIO_SCALEFAIL, "Invalid primary transform" + pIndex);

		//xx = (float) x;

		switch (scaling.primary.index) {
		 case 0: /* 10.24 V D/A */
			//state = (int) (xx * 3200.0);
			//if (!checkSignedOverflow(state, size))
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			//return (state);

			return checkSignedOverflow((int) (value * 3200.0), length);

		case 2: /* 10 V D/A */
			//state = (int) (xx * 3276.8);
			//if (!check_signed_overflow(state, size))
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			//return (state);
			return checkSignedOverflow((int) (value * 3276.8), length);

		case 4: /* 5 V D/A */
			//state = (int) (xx * 6553.6);
			//if (!check_signed_overflow(state, size))
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			//return (state);

			return checkSignedOverflow((int) (value * 6553.6), length);

		case 6: /* 2.5 V D/A */
			//state = (int) (xx * 13107.2);
			//if (!check_signed_overflow(state, size))
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			//return (state);
			return checkSignedOverflow((int) (value * 13107.2), length);

		case 8: /* 091 timing modules */
			//state = (int) (xx - 32768.0);
			//if (!check_signed_overflow(state, size))
		//		throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
		//	return (state);
			return checkSignedOverflow((int) (value - 32768.0), length);

		case 10: /* all DEC */
			//state = (int) xx;
			//if (!check_signed_overflow(state, size))
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			//return (state);
			return checkSignedOverflow((int) value, length);

		case 12: /* temperature resistors */
			//state = (int) (xx * 320.0);
			//if (!check_signed_overflow(state, size))
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			//return (state);
			return checkSignedOverflow((int) (value * 320.0), length);

		case 14: /* 2323 timing modules */
			mantis = xx;
			expon = 0;
			while (mantis > 1023.0) {
				mantis /= 10.0;
				expon++;
			}
			state = (int) mantis; /* convert mantissa to an integer */
			expon = ICSHFT(expon, 10);
			state = state | expon; /* put in exponent */
			state = state | 0xc000; /* set special bits */
			return (state);
			/* DISABLE = 80 */

		case 50: /* DEC to IEEE (-10.24 to 10.235) */
			if ((value < -10.24) || (value > 10.235))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Out of valid range.");

		case 16: /* DEC to IEEE (standard) */
			//state = Float.floatToIntBits(xx);
			//return (state);
			return Float.floatToIntBits((float) value);
			/* ENABLE = 80 */

		case 18: /* temperature resistors */
			//state = (int) (xx / 0.001040625);
			//if (!check_signed_overflow(state, size))
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			//return (state);
			return checkSignedOverflow((int) (value / 0.001040625), length);

		case 20: /* 8/16 bit data */
			/*
			 * if (size == 1) if (xx >= 128.0) state = (int)(xx - 256.0); else
			 * state = (int)xx; else if (xx >= 32768.0) state =(int)(xx -
			 * 65536.0); else state = (int) xx; return (state);
			 */
			if (value < 0.0)
				throw new AcnetStatusException(DIO_SCALEFAIL, "Corrupt data");
			if (length == 1) {
				if (value > 255.0)
					throw new AcnetStatusException(DIO_SCALEFAIL, "Corrupt data");
				//state = (int) data;
			} else if (length == 2) {
				if (value > 65535.0)
					throw new AcnetStatusException(DIO_SCALEFAIL, "Corrupt data");
				//state = (int) data;
			} else if (length == 4) {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Bad scale");
			}
			return (int) value;

		case 22: /* VAX/VMS null transform -- convert to DEC float */
			state = Float.floatToIntBits((float) (xx * 4)); // DEC float = 4*IEEE float
			x1 = (((state & 0xffff0000) >> 16) & 0x0000ffff); // A bit of word
			// swap fun
			x2 = (((state & 0x0000ffff) << 16) & 0xffff0000);
			state = x1 | x2;
			return (state);
		case 24: /* DEC to IEEE (68000) */
			state = Float.floatToIntBits((float) xx);
			x1 = (((state & 0xffff0000) >> 16) & 0x0000ffff);// A bit of word
			// swap fun
			x2 = (((state & 0x0000ffff) << 16) & 0xffff0000);
			state = x1 | x2;
			return (state);
		case 26: /* Pbar TWTs (obsolete) */
			mantis = (float) ((xx + 0.310269935) * 82.1865 * 256.0);
			state = (int) Math.abs(mantis);
			return (state);

		case 28: /* 68000 longword */
			state = (int) xx;
			x1 = (((state & 0xffff0000) >> 16) & 0x0000ffff);// A bit of word
			// swap fun
			x2 = (((state & 0x0000ffff) << 16) & 0xffff0000);
			state = x1 | x2;
			return (state);

		case 30: /* low byte signed */
			//state = (int) xx;
			//if (!check_signed_overflow(state, 1))
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			//return (state);
			return checkSignedOverflow((int) value, 1);

		case 32: /* high byte signed */
			//state = (short) xx;
			//if (!check_signed_overflow(state, 1))
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			//i = 8;

			state = checkSignedOverflow((short) value, 1);
			state = ICSHFT((short) state, 8);
			state = (state & 0xff00);
			return state;

		case 34: /* low byte */
			//state = (int) Math.abs(xx);
			//if (!checkOverflow(state, 1))
				//throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			//return (state);
			return checkOverflow((int) Math.abs(value), 1);

		case 36: /* high byte */
			//state = (short) Math.abs(xx);
			//if (!check_signed_overflow(state, 1))
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			//i = 8;
			state = checkSignedOverflow((short) Math.abs(value), 1);
			state = ICSHFT((short) state, 8);
			state = (state & 0xff00);
			return (state);

		case 38: /* Pbar TWTs */
			state = (int) (Math.abs((xx + 0.310269935) * 82.1865));
			state &= 0xFF; /* clear all but low order byte */
			return (state);

		case 40: /* temperature resistors */
			//state = (int) (xx * 256.0);
			//if (!check_signed_overflow(state, size))
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			//return (state);
			return checkSignedOverflow((int) (value * 256.0), 1);

		case 42: /* CAMAC 467/468s */
			xx *= 6553.6;
			if (xx > 32768.0)
				xx -= 65536.0;
			state = (int) xx;
			state = state & 0x0000ffff;
			return (state);

		case 44: /* a decimal encorded into 7-digit BCD format */
			temp = (int) xx;
			state = 0;
			for (int ii = 7; ii >= 1; ii--) {
				hbit = (short) (temp / Math.pow(10, ii));
				if (hbit != 0) {
					temp = mod(temp, (int) Math.pow(10, ii));
					state = (int) (state + hbit * Math.pow(16, ii));
				}
			}
			state = state + temp;
			return (state);

		case 46: /* all DEC () */
			/*
			 * unsigned_state = ( int) xx; if (unsigned_state < 0)
			 * unsigned_state = -1 * unsigned_state; return (unsigned_state);
			 */
			if (xx < 0.0)
				throw new AcnetStatusException(DIO_SCALEFAIL, "Corrupt data");
			unsigned_state = (int) xx;
			if (unsigned_state < 0)
				unsigned_state = -1 * unsigned_state;
			return (unsigned_state);

		case 48: /* temperature resistors */

			state = Float.floatToIntBits((float) (xx * 0.036));
			return (state);

		case 52: /* byte-swap word or longword */
			state = checkSignedOverflow((int) value, length);
			//state = (int) xx;
			//if (check_signed_overflow(state, size)) {
				if (length == 2) {// Byte swap raw data
					y = (((state & 0xff00) >> 8) & 0x00ff)
							| ((state << 8) & 0xff00);
				} else if (length == 4) {
					y = (state << 24) & 0xff000000;
					y |= ((state & 0x0000ff00) << 8) & 0x00ff0000;
					y |= ((state & 0x00ff0000) >> 8) & 0x0000ff00;
					y |= ((state & 0xff000000) >> 24) & 0x000000ff;
				}
			//} else
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Signed overflow.");
			state = (int) (y & 0xffffffff);
			return (int) state;

		case 54: /* 4.0 - 20.0 mA PCLs */
			if ((length != 2) || (xx < 4.0) || (xx > 20.0)) {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Value out of range.");
			}
			//state = (int) ((xx - 4.0) / 0.0004882961516);
			//if (!checkOverflow(state, size)) {
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Signed overflow.");
				// return (state);
			//}
			state = checkOverflow((int) ((value - 4.0) / 0.0004882961516), length);
			if ((state & 0x8000) != 0) /* range is 0-7FFF */
			{
				throw new AcnetStatusException(DIO_SCALEFAIL, "Value out of range.");
			}
			return (state);

		case 56: /* -/+ 10.0V . 0000-FFFF */
			if (length != 2) { /* must be a word */
				throw new AcnetStatusException(DIO_SCALEFAIL, "Data size invalid.");
			}
			//state = (int) ((xx * 3276.8) + 32768.0);
			//if (!checkOverflow(state, size))
				//throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			//return (state);
			return checkOverflow((int) ((xx * 3276.8) + 32768.0), length);

		case 58: /* temperature resistors (?) */
			unsigned_state = (int) (xx * 256.0);
			if (unsigned_state < 0)
				unsigned_state = -1 * unsigned_state;

			try {
				checkUnsignedOverflow(state, length);
				return unsigned_state;
			} catch (Exception ignore) {
				return state;
			}
			//if (!check_unsigned_overflow(state, size)) // Much Simplified from
				// the Winner of the
				// ambigeous C code
				// contest
			//	return (state);
			//return (unsigned_state);

		case 60: /* temperature resistors */
			state = Float.floatToIntBits((float) (xx / 500.0));
			return (state);

		case 62: /* 16-bit unipolar 0-10.24V */
			xx *= 6400.0;
			if (xx > 32768.0)
				xx -= 65536.0;
			state = (int) xx;
			state = state & 0x0000ffff;
			return (state);

		case 64: // Byte/Word/Long +/- 1.0
			if ((xx < -1.0) || (xx > 1.0))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Value out of range.");
			if (length == 1)
				state = (int) (xx * 128.0);
			if (length == 2)
				state = (int) (xx * 32768.0);
			if (length == 4)
				state = (int) (xx * 2147483648.0);
			return (state);

		case 66: // 0.0 - 10.24 V D/A */
			if (xx <= 0.0)
				throw new AcnetStatusException(DIO_SCALEFAIL, "Value out of range.");
			state = (int) (xx * 3200.0);
			return (state);

			/*
			 * * Special display types
			 */
		case 68:
			return (int) xx;
			//throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Required");

			/*
			 * * X = FLOAT(input)/1000.0
			 */
		case 70:
			//state = (int) (xx * 1000.0);
			//if (!check_signed_overflow(state, size))
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Signed overflow.");
			//return (state);
			return checkSignedOverflow((int) (value * 1000.0), length);

		case 72: /* -/+ 10.0V . 0000-FFFF */
			if (length != 2) { /* must be a word */
				throw new AcnetStatusException(DIO_SCALEFAIL, "Data length invalid.");
			}
			//state = (int) ((xx * 3200.0) + 32768.0);
			//if (!checkOverflow(state, size))
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			//return (state);
			return checkOverflow((int) ((xx * 3200.0) + 32768.0), length);

			/*
			 * Process to primary units where * X = FLOAT(input)*0.00064088
			 * --> input = 0x0000 - 0x7FFF (0-21 mA Allen-Bradley PLC)
			 */
		case 74:
			if ((length != 2) || (xx < -21.0) || (xx > 21.0)) {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Data length or data range invalid.");
			}
			//state = (int)(xx / 0.00064088);
			//if (!check_unsigned_overflow(state, size))
			//	throw new AcnetStatusException(DIO_SCALEFAIL, "Unsigned overflow.");
			//return (state);
			return checkUnsignedOverflow((int)(xx / 0.00064088), length);

			/*
			 * Process to primary units where * X = FLOAT(input)
			 * --> (word swapped unsigned 32 bit)
			 */
		case 76:
			if ( length != 4)  {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Data length invalid.");
			}

			if (( xx >= (float)0x100000000L) || (xx < 0) )
				throw new AcnetStatusException(DIO_SCALEFAIL, "Data range invalid.");
			if (xx >= (float)0x80000000L)
				state = (int)(xx - (float)0x80000000L) | 0x80000000;
			else
				state = (int) xx;
			x1 = (((state & 0xffff0000) >> 16) & 0x0000ffff);// A bit of word
			// swap fun
			x2 = (((state & 0x0000ffff) << 16) & 0xffff0000);
			state = x1 | x2;
			return (state);

		case 78: /* DEC to IEEE ( 0.0 to 5.0) */
			if ((xx < 0) || (xx > 5.0))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Out of valid range.");
			state = Float.floatToIntBits((float) xx);
			return (state);

		case 80: /* DEC to IEEE ( 0.0 to 10.0) */
			if ((xx < 0) || (xx > 10.0))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Out of valid range.");
			state = Float.floatToIntBits((float) xx);
			return (state);

		case 82: /* X=FLOAT(input)/409.5; (12-bit unipolar 0.0 - 10.0V) */
			if ( length != 2)  {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Data length invalid.");
			}
			if ((xx < 0)||(xx > 10.0) )
				throw new AcnetStatusException(DIO_SCALEFAIL, "Out of valid range.");
			state = (int)(xx*409.5);
			return (state);

		case 84: /* native float to IEEE (byte inverted) */
			if ( length != 4)  {
				throw new AcnetStatusException(DIO_SCALEFAIL, "Data length invalid.");
			}
			state = Float.floatToIntBits((float) xx);
			x1 = (((state & 0xff000000) >> 24) & 0x000000ff);// A byte of word
			x2 = x1;
			x1 = (((state & 0x00ff0000) >>  8) & 0x0000ff00);// A byte of word
			x2 = x2 | x1;
			x1 = (((state & 0x0000ff00) <<  8) & 0x00ff0000);// A byte of word
			x2 = x2 | x1;
			x1 = (((state & 0x000000ff) << 24) & 0xff000000);// A byte of word
			x2 = x2 | x1;
			state = x2;
			return (state);
		}

		throw new AcnetStatusException(DIO_SCALEFAIL, "Invalid primary transform " + scaling.primary.index);
	}

	private int checkSignedOverflow(int value, int size) throws AcnetStatusException 
	{
		switch (size) {
		 case 1:
			if ((value < -128) || (value > 127))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			break;

		 case 2:
			if ((value < -32768) || (value > 32767))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			break;
		}

		return value;
	}

	private int checkOverflow(int value, int size) throws AcnetStatusException 
	{
		switch (size) {
		 case 1:
			if ((value < 0) || (value > 255))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			break;
		 case 2:
			if ((value < 0) || (value > 65535))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			break;
		}

		return value;
	}

	private int checkUnsignedOverflow(int value, int size) throws AcnetStatusException
	{
		switch (size) {
		 case 1:
			if ((value < 0) || (value > 255))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			break;
		 case 2:
			if ((value < 0) || (value > 65535))
				throw new AcnetStatusException(DIO_SCALEFAIL, "Overflow.");
			break;
		}
		
		return value;
	}

	private int mod(int x, int y) 
	{
		return x - (x / y * y);
	}
}
