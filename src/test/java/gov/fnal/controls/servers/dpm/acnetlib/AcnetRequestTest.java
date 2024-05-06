package gov.fnal.controls.servers.dpm.acnetlib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.mockito.Mockito.mock;

public class AcnetRequestTest {

    @Test
    public void test_isRequest() {
        Buffer buf = BufferCache.alloc();
        AcnetRequest acnetRequest = new AcnetRequest(10, buf);
        Assertions.assertTrue(acnetRequest.isRequest());
    }

    @Test
    public void test_sendReplay() throws AcnetStatusException {
        Buffer buffer = BufferCache.alloc();
        ByteBuffer buf = ByteBuffer.allocate(12545);
        AcnetConnection acnetConnection = mock(AcnetConnection.class);
        AcnetRequest acnetRequest = new AcnetRequest(10, buffer);
        acnetRequest.connection = acnetConnection;
        acnetRequest.sendReply(buf, 10);
    }

    @Test
    public void test_sendLastReplay() throws AcnetStatusException {
        Buffer buffer = BufferCache.alloc();
        ByteBuffer buf = ByteBuffer.allocate(12545);
        AcnetConnection acnetConnection = mock(AcnetConnection.class);
        AcnetRequest acnetRequest = new AcnetRequest(10, buffer);
        acnetRequest.connection = acnetConnection;
        acnetRequest.sendLastReply(buf, 10);
    }

    @Test
    public void test_sendLastReplayWithNoEx() throws AcnetStatusException {
        Buffer buffer = BufferCache.alloc();
        ByteBuffer buf = ByteBuffer.allocate(12545);
        AcnetConnection acnetConnection = mock(AcnetConnection.class);
        AcnetRequest acnetRequest = new AcnetRequest(10, buffer);
        acnetRequest.connection = acnetConnection;
        acnetRequest.sendLastReplyNoEx(buf, 10);
    }

    @Test
    public void test_handle() throws AcnetStatusException {
        Buffer buffer = BufferCache.alloc();
        ByteBuffer buf = ByteBuffer.allocate(12545);
        AcnetConnection acnetConnection = mock(AcnetConnection.class);
        AcnetRequest acnetRequest = new AcnetRequest(10, buffer);
        acnetRequest.connection = acnetConnection;
        acnetRequest.handle(acnetConnection);
    }
}
