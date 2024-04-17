package gov.fnal.controls.servers.dpm.acnetlib;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AcnetConnectionTCPTest {

    @Test
    public void handle_test() {
        Node node = mock(Node.class);
        InetSocketAddress address = mock(InetSocketAddress.class);
        Buffer buf = BufferCache.alloc();
        AcnetMessage acnetMessage = new AcnetMessage(buf);

        AcnetConnectionTCP acnetConnectionTCP = new AcnetConnectionTCP("test", node, address);
        acnetConnectionTCP.handle(acnetMessage);
    }

    @Test
    public void disposed_test() {
        Node node = mock(Node.class);
        InetSocketAddress address = mock(InetSocketAddress.class);
        AcnetConnectionTCP acnetConnectionTCP = new AcnetConnectionTCP("test", node, address);
        final boolean disposed = acnetConnectionTCP.disposed();
        assertTrue(disposed);

    }
    @Test(expected = AcnetUnavailableException.class)
    public void send_test() throws AcnetStatusException {
        Node node = mock(Node.class);
        node.name = "node name";//remove final
        SocketChannel socketChannel = mock(SocketChannel.class);

        InetSocketAddress address = mock(InetSocketAddress.class);
        AcnetConnectionTCP acnetConnectionTCP = new AcnetConnectionTCP("test", node, address);
        acnetConnectionTCP.channel  = socketChannel;//change private to public
        final ByteBuffer byteBuffer = acnetConnectionTCP.sendCommand(12, 13, null);
        System.out.println(byteBuffer);

    }
}
