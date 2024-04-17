package gov.fnal.controls.servers.dpm.acnetlib;

import org.junit.Assert;
import org.junit.Test;

public class Rad50Test {

    @Test
    public void test_encode(){
        //In encode method remove final and add public
        Assert.assertEquals(2097184219,Rad50.encode("TEST"));
    }

    @Test
    public void test_decode(){
        //In encode method remove final and add public
        Assert.assertEquals("TEST",Rad50.decode(2097184219).trim());
    }
}
