package gov.fnal.controls.servers.dpm.protocols.acnet.pc;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequest;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.protocols.DPMProtocolHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class DPMListPCTest {

    @Test
    public void test_dispose() throws AcnetStatusException {
        DPMProtocolHandler dpmProtocolHandler = mock(DPMProtocolHandler.class);
        AcnetRequest acnetRequest = mock(AcnetRequest.class);
        doNothing().when(acnetRequest).sendLastStatus(12);
        DPMListPC dpmListPC = new DPMListPC(dpmProtocolHandler, acnetRequest);
        Assertions.assertTrue(dpmListPC.dispose(12));
    }
}
