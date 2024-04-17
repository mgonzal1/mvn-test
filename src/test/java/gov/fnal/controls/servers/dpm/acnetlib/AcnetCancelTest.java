package gov.fnal.controls.servers.dpm.acnetlib;

import org.junit.Test;

import java.util.HashMap;

import static org.mockito.Mockito.mock;

public class AcnetCancelTest {

    @Test
    public void handle_test() {
        Node node = mock(Node.class);
        Buffer buf = BufferCache.alloc();

        AcnetConnection acnetConnection = new AcnetConnectionInternal(5, 10, node);
        HashMap<ReplyId, AcnetRequest> requestsIn = new HashMap<>();

        requestsIn.put(new ReplyId(0, 0), new AcnetRequest(10, buf));
        requestsIn.put(new ReplyId(20, 5), new AcnetRequest(20, buf));

        acnetConnection.requestsIn = requestsIn;//Remove final in AcnetConnection requestsIn
        AcnetCancel acnetCancel = new AcnetCancel(buf);
        acnetCancel.handle(acnetConnection);
    }
}
