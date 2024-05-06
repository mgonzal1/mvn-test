package gov.fnal.controls.servers.dpm.acnetlib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class StatSetTest {

    @Test
    public void test_messageSent() {
        StatSet stat = new StatSet();
        stat.messageSent();
    }

    @Test
    public void test_requestSent() {
        StatSet stat =new StatSet();
        stat.requestSent();
    }

    @Test
    public void test_replaySent() {
        StatSet stat = new StatSet();
        stat.replySent();
    }

    @Test
    public void test_messageReceived() {
        StatSet stat =new StatSet();
        stat.messageReceived();
    }

    @Test
    public void test_requestReceived() {
        StatSet stat = new StatSet();
        stat.requestReceived();
    }

    @Test
    public void test_replyReceived() {
        StatSet stat = new StatSet();
        stat.replyReceived();
    }

    @Test
    public void test_putStats() {
        StatSet stat = new StatSet();
        ByteBuffer byteBuffer = ByteBuffer.allocate(254);
        final ByteBuffer byteBuffer1 = stat.putStats(byteBuffer);
        Assertions.assertNotNull(byteBuffer1);
    }
}
