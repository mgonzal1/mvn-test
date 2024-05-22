package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.pools.PoolUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class RepetitiveMultiShotPoolTest {

    @Test
    public void test_completed(){
        Node node = mock(Node.class);
        RepetitiveMultiShotPool repetitiveMultiShotPool = new RepetitiveMultiShotPool(node,451346,7895622);
        repetitiveMultiShotPool.completed(458);
    }

    @Test
    public void test_cancel(){
        Node node = mock(Node.class);
        PoolUser poolUser = mock(PoolUser.class);
        RepetitiveMultiShotPool repetitiveMultiShotPool = new RepetitiveMultiShotPool(node,451346,7895622);
        repetitiveMultiShotPool.cancel(poolUser,458);
    }

    @Test
    public void test_process(){
        Node node = mock(Node.class);
        RepetitiveMultiShotPool repetitiveMultiShotPool = new RepetitiveMultiShotPool(node,451346,7895622);
        Assertions.assertTrue(repetitiveMultiShotPool.process(true));
    }
}
