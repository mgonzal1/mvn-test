//package gov.fnal.controls.servers.dpm.pools.acnet;
//
//import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
//import gov.fnal.controls.servers.dpm.pools.ReceiveData;
//import gov.fnal.controls.servers.dpm.pools.WhatDaq;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//
//import static org.mockito.Mockito.mock;
//
//public class SnapRequestTest {
//
//    @Test
//    public void test_getPlotRequests() throws AcnetStatusException {
//        ReceiveData receiveData = mock(ReceiveData.class);
//        WhatDaq whatDaq1 = mock(WhatDaq.class);
//        ClassCode classCode = mock(ClassCode.class);
//        Trigger trigger = mock(Trigger.class);
//        SnapScope snapScope = mock(SnapScope.class);
//        ReArm reArm = new ReArm("rearm=false");
//        WhatDaq whatDaq = new WhatDaq(whatDaq1, 487, 10, 457, receiveData);
//        SnapRequest snapRequest = new SnapRequest(whatDaq,classCode,trigger,snapScope,receiveData,reArm);
//        snapRequest.getPlotRequests();
//        Assertions.assertEquals(1,snapRequest.getPlotRequests().size());
//    }
//}
