package gov.fnal.controls.servers.dpm.acnetlib;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AcnetConnectionUDPTest {

    @Test
    public void test_sendCommand() throws AcnetStatusException {
        Node node = mock(Node.class);
        when(node.name()).thenReturn("test");
//        when(Node.get(12)).thenReturn(node);
//        when(node.value()).thenReturn(12);
        AcnetConnectionUDP acnetConnectionUDP = new AcnetConnectionUDP("test", node);
        ByteBuffer byteBuffer = ByteBuffer.allocate(4578);
        byteBuffer.putInt(542323);
        acnetConnectionUDP.sendCommand(10, 12, byteBuffer);
    }
}
