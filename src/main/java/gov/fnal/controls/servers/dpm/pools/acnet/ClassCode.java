// $Id: ClassCode.java,v 1.5 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

class ClassCode
{
	final static int DAE_FTP_CLASS_CODE = 23;
	final static int DAE_SNAP_CLASS_CODE = 23;
	final static int SNAP_MINIMUM_CLIP_POINTS = 11;
	final static int FTP_720 = 11; 
	final static int FTP_1000 = 12; 
	final static int FTP_15 = 15;  
	final static int FTP_1440_290 = 16;  
	final static int FTP_60 = 18; 
	final static int FTP_1440 = 19;  
	final static int FTP_240 = 20;  
	final static int FTP_DAE_1 = 22; 
	final static int FTP_720_OLD = 1;
	final static int FTP_1000_OLD = 2; 
	final static int FTP_15_OLD = 5; 
	final static int FTP_1440_290_OLD = 6;
	final static int FTP_60_OLD = 8; 
	final static int FTP_1440_OLD = 9; 
	final static int FTP_240_OLD = 10;

	private final int snap;
	private final int ftp;
	private final String snapDescription;
	private final String ftpDescription;
	private final boolean ftpFromPool;
	private final boolean snapFromPool;
	private final int error;
	private final int maxPoints;
	private final int maxSnapRate;
	private final int maxContinuousRate;
	private final boolean supportsSnapshots;
	private final boolean hasTimeStamps;
	private final boolean supportsTriggers;
	private final boolean skipFirstPoint;
	private final boolean isTriggerDevice;
	private final int retrievalMax;
	private final boolean isFixedNumberPoints;

	ClassCode(int ftp, int snap, int error)
	{
		this.ftp = ftp;
		this.snap = snap;
		this.error = error;

		boolean ftpFromPool = false;

		switch (ftp) {
		case 1:
		case 11:
			this.ftpDescription = "C190 MADC channel";
			this.maxContinuousRate = 720;
			break;

		case 2: // 94042
		case 12: // di =
			this.ftpDescription = "InternetRackMonitor";
			this.maxContinuousRate = 1000;
			break;

		case 3:
		case 13:
			this.ftpDescription = "MRRF MAC MADC channel";
			this.maxContinuousRate = 100;
			break;

		case 4:
		case 14:
			this.ftpDescription = "Booster MAC MADC channel";
			this.maxContinuousRate = 15;
			ftpFromPool = true;
			break;

		case 5: // di = 3679
		case 15:
			this.ftpDescription = "15 Hz (Linac, D/A's etc.)";
			this.maxContinuousRate = 15;
			break;

		case 6: // di = 104251
			this.ftpDescription = "1 Hz from data pool (FRIG)";
			this.maxContinuousRate = 1;
			ftpFromPool = true;
			break;

		case 16:
			this.ftpDescription = "C290 MADC channel";
			this.maxContinuousRate = 1440;
			break;

		case 7: // di = 10
		case 17:
			this.ftpDescription = "15 Hz from data pool";
			this.maxContinuousRate = 15;
			ftpFromPool = true;
			break;

		case 8:
		case 18:
			ftpDescription = "60 Hz internal";
			maxContinuousRate = 60;
			break;

		case 9:
		case 19:
			ftpDescription = "68K (MECAR)";
			maxContinuousRate = 1440;
			break;

		case 10:
		case 20:
			ftpDescription = "Tev Collimators";
			maxContinuousRate = 240;
			break;

		case 21:
			ftpDescription = "IRM 1KHz Digitizer";
			maxContinuousRate = 1000;
			break;

		case 22:
			ftpDescription = "DAE 1 Hz";
			maxContinuousRate = 1;
			ftpFromPool = true;
			break;

		case 23:
			ftpDescription = "DAE 15 Hz";
			maxContinuousRate = 15;
			ftpFromPool = true;
			break;

		default:
			ftpDescription = "Unknown ftp class: " + ftp;
			maxContinuousRate = 15;
			ftpFromPool = true;
			break;
		}

		this.ftpFromPool = ftpFromPool;
		
		boolean snapFromPool = false;
		boolean isTriggerDevice = false;
		boolean isFixedNumberPoints = false;

		switch (snap) {
		case 1:
		case 11: // di = 2
			snapDescription = "C190 MADC channel";
			maxSnapRate = 66666;
			if (snap == 1) {
				isFixedNumberPoints = true;
			}
			maxPoints = 2048;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 2:
		case 12:
			snapDescription = "1440 Hz internal";
			maxSnapRate = 1440;
			if (snap == 2) {
				isFixedNumberPoints = true;
			}
			maxPoints = 2048;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 13:
			snapDescription = "C290 MADC channel";
			maxSnapRate = 90000;
			maxPoints = 2048;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 4:
		case 14:
			snapDescription = "15 Hz internal";
			maxSnapRate = 15;
			if (snap == 4) {
				isFixedNumberPoints = true;
			}
			maxPoints = 2048;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 5:
		case 15:
			snapDescription = "60 Hz internal";
			maxSnapRate = 60;
			if (snap == 5) {
				isFixedNumberPoints = true;
			}
			maxPoints = 2048;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 6:
		case 16:
			snapDescription = "Quick Digitizer (Linac)";
			maxSnapRate = 10000000;
			if (snap == 6) {
				isFixedNumberPoints = true;
			}
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 7:
		case 17:
			snapDescription = "720 Hz internal";
			maxSnapRate = 720;
			if (snap == 7) {
				isFixedNumberPoints = true;
			}
			maxPoints = 2048;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 8:
			snapDescription = "New FRIG Trigger device";
			maxSnapRate = 0;
			isFixedNumberPoints = true;
			maxPoints = 0;
			supportsSnapshots = false;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = false;
			retrievalMax = 0;
			isTriggerDevice = true;
			break;

		case 9: // di = 2907
		case 18:
			snapDescription = "New FRIG circ buffer";
			maxSnapRate = 1000;
			if (snap == 9)
				isFixedNumberPoints = true;
			maxPoints = 16384;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = true;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 19: // di = 116587
			snapDescription = "Swift Digitizer";
			maxSnapRate = 800000;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 20:
			snapDescription = "IRM 20 MHz Quick Digitizer";
			maxSnapRate = 20000000;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 21:
			snapDescription = "IRM 1KHz Digitizer";
			maxSnapRate = 1000;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 22: // di = 116587
			snapDescription = "DAE 1 Hz";
			maxSnapRate = 1;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = true;
			skipFirstPoint = true;
			retrievalMax = 4096;
			snapFromPool = true;
			break;

		case 23: // di = 116587
			snapDescription = "DAE 15 Hz";
			maxSnapRate = 15;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = true;
			skipFirstPoint = true;
			retrievalMax = 4096;
			snapFromPool = true;
			break;

		case 24:
			snapDescription = "IRM 12.5KHz Digitizer";
			maxSnapRate = 12500;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 25:
			snapDescription = "IRM 10KHz Digitizer";
			maxSnapRate = 10000;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 26:
			snapDescription = "IRM 10MHz Digitizer";
			maxSnapRate = 10000000;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 28:
			snapDescription = "New Booster BLM";
			maxSnapRate = 12500;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 0:
		default:
			snapDescription = "Not Snapshotable";
			maxSnapRate = 0;
			maxPoints = 0;
			supportsSnapshots = false;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = false;
			retrievalMax = 0;
			break;
		}

		this.snapFromPool = snapFromPool;
		this.isTriggerDevice = isTriggerDevice;
		this.isFixedNumberPoints = isFixedNumberPoints;
	}

	int snap()
	{
		return snap;
	}

	int ftp()
	{
		return ftp;
	}

	String ftpDescription()
	{
		return ftpDescription;
	}

	String snapDescription()
	{
		return snapDescription;
	}

	boolean skipFirstPoint()
	{
		return skipFirstPoint;
	}

	int maxPoints()
	{
		return maxPoints;
	}

	boolean hasTimeStamps()
	{
		return hasTimeStamps;
	}

	boolean ftpFromPool()
	{
		return ftpFromPool;
	}
	
	boolean supportsSnapshots()
	{
		return supportsSnapshots;
	}
	
	boolean supportsTriggers()
	{
		return supportsTriggers;
	}
	
	boolean isTriggerDevice()
	{
		return isTriggerDevice;
	}
	
	int retrievalMax()
	{
		return retrievalMax;
	}

	boolean snapFromPool()
	{
		return snapFromPool;
	}

	int maxContinuousRate()
	{
		return maxContinuousRate;
	}

	int maxSnapRate()
	{
		return maxSnapRate;
	}

	boolean supportsClippedPoints()
	{
		return snap >= SNAP_MINIMUM_CLIP_POINTS;
	}

	int error()
	{
		return error;
	}

	@Override
	public String toString()
	{
		return "ClassCode: ftp=" + ftp + " snap=" + snap + " error=" + error;
	}
}
