// $Id: Rad50.java,v 1.1 2024/02/22 16:29:45 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

class Rad50
{
	static final char rad50Char[] = { ' ', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 
										'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 
										'X', 'Y', 'Z', '$', '.', '%', '0', '1', '2', '3', '4', '5',
										'6', '7', '8', '9' };
    static final int Rad50CharCount = 6;
    
	final private static int charToIndex(char c) 
	{
		if (c >= 'A' && c <= 'Z')
			return c - 'A' + 1;
		else if (c >= 'a' && c <= 'z')
			return c - 'a' + 1;
		else if (c >= '0' && c <= '9')
			return c - '0' + 30;
		else if (c == '$')
			return 27;
		else if (c == '.')
			return 28;
		else if (c == '%')
			return 29;

		return 0;
	}
    
    public final static int encode(String s) 
	{
		int v1 = 0, v2 = 0;
		int len = s.length();

		for (int ii = 0; ii < Rad50CharCount; ii++) {
			char c = (ii < len ? s.charAt(ii) : ' ');

			if (ii < (Rad50CharCount / 2)) {
				v1 *= 40;
				v1 += charToIndex(c);
			} else {
				v2 *= 40;
				v2 += charToIndex(c);
			}
		}

		return v2 << 16 | v1;
    }

    final static String decode(int rad50) 
	{
		final char s[] = new char[Rad50CharCount];

		int v1 = rad50 & 0xffff;
		int v2 = (rad50 >> 16) & 0xffff;

		for (int ii = 0; ii < (Rad50CharCount / 2); ii++) {
			s[(Rad50CharCount / 2) - ii - 1] = rad50Char[v1 % 40];
			v1 /= 40;
			s[Rad50CharCount - ii - 1] = rad50Char[v2 % 40];
			v2 /= 40;
		}

		return new String(s);
    }
}
