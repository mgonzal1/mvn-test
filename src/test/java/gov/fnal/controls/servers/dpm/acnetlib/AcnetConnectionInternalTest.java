package gov.fnal.controls.servers.dpm.acnetlib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;

import static org.mockito.Mockito.*;


@PrepareForTest({Node.class,
        AcnetConnectionInternal.WriteThread.class,
        AcnetRequestContext.class,
        RequestId.class})
public class AcnetConnectionInternalTest {

    @Test
    public void test_getNameWhenNodeNameIsInt() throws AcnetStatusException {
        Node node = mock(Node.class);
        mockStatic(Node.class);
        when(Node.get(1)).thenReturn(node);
        when(node.name()).thenReturn("node");
        AcnetConnectionInternal acnetConnectionInternal = new AcnetConnectionInternal(1, 12, node);
        final String name = acnetConnectionInternal.getName(1);
        Assertions.assertEquals("node", name);
    }

    @Test
    public void test_getNameWhenNodeNameIsString() throws AcnetStatusException {
        Node node = mock(Node.class);
        mockStatic(Node.class);
        when(Node.get("node")).thenReturn(node);
        when(node.value()).thenReturn(12);
        AcnetConnectionInternal acnetConnectionInternal = new AcnetConnectionInternal(1, 12, node);
        final int nodeValue = acnetConnectionInternal.getNode("node");
        Assertions.assertEquals(12, nodeValue);

    }

    @Test
    public void test_sendMessage() throws AcnetStatusException {
        Node node = mock(Node.class);
        mockStatic(Node.class);
        mockStatic(AcnetConnectionInternal.WriteThread.class);
        when(Node.get("node")).thenReturn(node);
        when(Node.get(12)).thenReturn(node);
        when(node.value()).thenReturn(12);
        ByteBuffer byteBuffer = ByteBuffer.allocate(122);
        DatagramChannel channel = mock(DatagramChannel.class);
        AcnetConnectionInternal.WriteThread writeThread1 = new AcnetConnectionInternal.WriteThread(channel, node);
        writeThread1.sendMessage(12, "task", byteBuffer);
    }

    @Test
    public void test_sendReplay() throws AcnetStatusException {
        Node node = mock(Node.class);
        mockStatic(Node.class);
        mockStatic(AcnetConnectionInternal.WriteThread.class);
        when(Node.get("node")).thenReturn(node);
        when(Node.get(12)).thenReturn(node);
        when(node.value()).thenReturn(12);
        AcnetRequest acnetRequest = mock(AcnetRequest.class);
        acnetRequest.client = 12;//remove final
        ByteBuffer byteBuffer = ByteBuffer.allocate(122);
        DatagramChannel channel = mock(DatagramChannel.class);
        AcnetConnectionInternal.WriteThread writeThread1 = new AcnetConnectionInternal.WriteThread(channel, node);
        writeThread1.sendReply(acnetRequest, 10, byteBuffer, 1);
    }

    @Test
    public void test_sendRequestAndSendCancel() throws AcnetStatusException {
        Node node = mock(Node.class);
        mockStatic(Node.class);
        mockStatic(AcnetConnectionInternal.WriteThread.class);
        when(Node.get("node")).thenReturn(node);
        when(Node.get(12)).thenReturn(node);
        when(node.value()).thenReturn(12);

        AcnetRequestContext context = PowerMockito.mock(AcnetRequestContext.class);
        context.node = node;//remove final
        context.task = "test";
        RequestId requestId = PowerMockito.mock(RequestId.class);
        requestId.id = 12;//remove final
        when(requestId.value()).thenReturn(12);
        context.requestId = requestId;//remove final

        AcnetReply reply = mock(AcnetReply.class);
        reply.server = 12;//remove final
        reply.serverTask = 1;
        reply.clientTaskId = 5;
        reply.requestId = requestId;//remove final

        ByteBuffer byteBuffer = ByteBuffer.allocate(122);
        DatagramChannel channel = mock(DatagramChannel.class);
        AcnetConnectionInternal.WriteThread writeThread1 = new AcnetConnectionInternal.WriteThread(channel, node);
        writeThread1.sendRequest(context, byteBuffer);
        writeThread1.sendCancel(context);
        writeThread1.sendCancel(reply);
    }

    @Test
    public void test_acnetAuxHandler_taskHandlerWhenTypeIs0() {
        AcnetConnectionInternal.AcnetAuxHandler acnetAuxHandler = new AcnetConnectionInternal.AcnetAuxHandler();
        AcnetRequest acnetRequest = mock(AcnetRequest.class);
        acnetAuxHandler.taskHandler(0, acnetRequest);
    }

    @Test
    public void test_acnetAuxHandler_taskHandlerWhenTypeIs1() {
        AcnetConnectionInternal acnetConnectionInternal = mock(AcnetConnectionInternal.class);

        ArrayList<AcnetConnectionInternal> connectionsById = new ArrayList<>();
        acnetConnectionInternal.receiving = true;
        acnetConnectionInternal.name = 12;
        acnetConnectionInternal.taskId = 53;
        connectionsById.add(acnetConnectionInternal);
        AcnetConnectionInternal.connectionsById = connectionsById;

        AcnetConnectionInternal.AcnetAuxHandler acnetAuxHandler = new AcnetConnectionInternal.AcnetAuxHandler();
        AcnetRequest acnetRequest = mock(AcnetRequest.class);
        acnetAuxHandler.taskHandler(1, acnetRequest);
    }

    @Test
    public void test_acnetAuxHandler_taskHandlerWhenTypeIs2() {
        AcnetConnectionInternal acnetConnectionInternal = mock(AcnetConnectionInternal.class);

        ArrayList<AcnetConnectionInternal> connectionsById = new ArrayList<>();
        acnetConnectionInternal.receiving = true;
        acnetConnectionInternal.name = 12;
        acnetConnectionInternal.taskId = 53;
        connectionsById.add(acnetConnectionInternal);
        AcnetConnectionInternal.connectionsById = connectionsById;

        AcnetConnectionInternal.AcnetAuxHandler acnetAuxHandler = new AcnetConnectionInternal.AcnetAuxHandler();
        AcnetRequest acnetRequest = mock(AcnetRequest.class);
        acnetAuxHandler.taskHandler(2, acnetRequest);
    }

    @Test
    public void test_acnetAuxHandler_handle() {
        AcnetConnectionInternal acnetConnectionInternal = mock(AcnetConnectionInternal.class);

        ArrayList<AcnetConnectionInternal> connectionsById = new ArrayList<>();
        acnetConnectionInternal.receiving = true;
        acnetConnectionInternal.name = 12;
        acnetConnectionInternal.taskId = 53;
        connectionsById.add(acnetConnectionInternal);
        AcnetConnectionInternal.connectionsById = connectionsById;

        AcnetConnectionInternal.AcnetAuxHandler acnetAuxHandler = new AcnetConnectionInternal.AcnetAuxHandler();
        AcnetRequest acnetRequest = mock(AcnetRequest.class);
        ByteBuffer byteBuffer = ByteBuffer.allocate(4578);
        byteBuffer.putInt(542323);
        when(acnetRequest.data()).thenReturn(byteBuffer);
        acnetAuxHandler.handle(acnetRequest);
    }

    @Test
    public void test_idBank_alloc() throws AcnetStatusException {
        AcnetConnectionInternal.IdBank idBank = new AcnetConnectionInternal.IdBank(12);
        idBank.alloc();

    }
}
