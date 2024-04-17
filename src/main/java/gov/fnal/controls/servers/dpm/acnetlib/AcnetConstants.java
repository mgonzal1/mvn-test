// $Id: AcnetConstants.java,v 1.1 2024/02/22 16:29:45 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

interface AcnetConstants
{
	final static int ACNET_PORT = 6801;
	static final int ACNET_HEADER_SIZE = 18;

    static final int ACNET_FLG_CAN = 0x0200;
    static final int ACNET_FLG_TYPE = 0x000e;
    static final int ACNET_FLG_USM = 0x0000;
    static final int ACNET_FLG_REQ = 0x0002;
    static final int ACNET_FLG_RPY = 0x0004;
    static final int ACNET_FLG_MLT = 0x0001;

    static final int REPLY_NORMAL = 0x00;
    static final int REPLY_ENDMULT = 0x02;
}
