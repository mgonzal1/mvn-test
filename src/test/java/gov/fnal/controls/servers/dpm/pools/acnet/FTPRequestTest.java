//package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
//import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

//public class FTPRequestTest {
//
//    private static FTPRequest getFtpRequest() throws AcnetStatusException {
//        ReceiveData receiveData = mock(ReceiveData.class);
//        WhatDaq whatDaq1 = mock(WhatDaq.class);
//        ClassCode classCode = mock(ClassCode.class);
//        Trigger trigger = mock(Trigger.class);
//        FTPScope ftpScope = new FTPScope(125.45, (byte) 100, true, 45.65);
//        ReArm reArm = new ReArm("rearm=false");
//        WhatDaq whatDaq = new WhatDaq(whatDaq1, 487, 10, 457, receiveData);
//        return new FTPRequest(whatDaq, classCode, trigger, ftpScope, receiveData, reArm);
//    }
//
//    @Test
//    public void test_isArmEvent() throws AcnetStatusException {
//        final FTPRequest ftpRequest = getFtpRequest();
//        DataEvent dataEvent = mock(DataEvent.class);
//        Assertions.assertFalse(ftpRequest.isArmEvent(dataEvent));
//    }

//    @Test
//    public void test_watchArmEvents() throws AcnetStatusException {
//        final FTPRequest ftpRequest = getFtpRequest();
//        ftpRequest.watchArmEvents();
//        Assertions.assertEquals(125.45, ftpRequest.ftpRate);
//    }

//    @Test
//    public void test_stopWatchingArmEvents() throws AcnetStatusException {
//        final FTPRequest ftpRequest = getFtpRequest();
//        ftpRequest.stopWatchingArmEvents();
//        Assertions.assertFalse(ftpRequest.waitArm);
//    }

//    @Test
//    public void test_lastCall() throws AcnetStatusException {
//        final FTPRequest ftpRequest = getFtpRequest();
//        ftpRequest.lastCall(true, 123);
//        Assertions.assertTrue(ftpRequest.delete);
//    }

//    @Test
//    public void test_plotData() throws AcnetStatusException {
//        FTPRequest ftpRequest = getFtpRequest();
//        ftpRequest.trigger = null;
//        long[] miceSecs = {15894469, 14799159, 498914891};
//        int[] nanoSecs = {15894469, 14799159, 498914891};
//        double[] values = {15894469.146, 14799159.149, 498914891.134};
//        ftpRequest.plotData(1447596525, 123, 2, miceSecs, nanoSecs, values);
//        Assertions.assertFalse(ftpRequest.delete);
//    }
//}
