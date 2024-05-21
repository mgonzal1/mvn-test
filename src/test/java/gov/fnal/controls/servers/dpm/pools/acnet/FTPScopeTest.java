package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.events.DeltaTimeEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FTPScopeTest {

    @Test
    public void test_getRate(){
        FTPScope ftpScope  = new FTPScope(125.45, (byte) 100,true,45.65);
        final double rate = ftpScope.getRate();
        Assertions.assertEquals(125.45,rate);

    }

    @Test
    public void test_getGroupEventCode(){
        FTPScope ftpScope = new FTPScope("147.254=147.254,147.254=79954.245");
        final int groupEventCode = ftpScope.getGroupEventCode();
        Assertions.assertEquals(0,groupEventCode);
    }

    @Test
    public void test_getDuration(){
        FTPScope ftpScope  = new FTPScope(125.45, (byte) 100,true,45.65);
        final double duration = ftpScope.getDuration();
        Assertions.assertEquals(45.65,duration);
    }

    @Test
    public void test_ftpCollectionEvent(){
        FTPScope ftpScope  = new FTPScope(125.45, (byte) 100,true,45.65);
        final DeltaTimeEvent deltaTimeEvent = ftpScope.ftpCollectionEvent(1248365);
        Assertions.assertInstanceOf(DeltaTimeEvent.class , deltaTimeEvent);
    }
}
