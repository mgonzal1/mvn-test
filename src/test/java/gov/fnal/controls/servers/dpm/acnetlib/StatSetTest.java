package gov.fnal.controls.servers.dpm.acnetlib;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class StatSetTest {

    @Test
    public void test_messageSent() {
        StatSet stat = getStat();
        stat.messageSent();
        Assert.assertEquals(11, stat.stats[0]);
    }
    @Test
    public void test_requestSent() {
        StatSet stat = getStat();
        stat.requestSent();
        Assert.assertEquals(16, stat.stats[1]);
    }

    @Test
    public void test_replaySent() {
        StatSet stat = getStat();
        stat.replySent();
        Assert.assertEquals(21, stat.stats[2]);
    }

    @Test
    public void test_messageReceived() {
        StatSet stat = getStat();
        stat.messageReceived();
        Assert.assertEquals(26, stat.stats[3]);
    }
    @Test
    public void test_requestReceived() {
        StatSet stat = getStat();
        stat.requestReceived();
        Assert.assertEquals(31, stat.stats[4]);
    }
    @Test
    public void test_replyReceived() {
        StatSet stat = getStat();
        stat.replyReceived();
        Assert.assertEquals(36, stat.stats[5]);
    }

    @Test
    public void test_putStats() {
        StatSet stat = getStat();
        ByteBuffer byteBuffer = ByteBuffer.allocate(254);
        final ByteBuffer byteBuffer1 = stat.putStats(byteBuffer);
        Assert.assertNotNull(byteBuffer1);
    }

    private StatSet getStat() {
        StatSet statSet = new StatSet();
        int j = 10;
        for (int i = 0; i < 6; i++) {
            statSet.stats[i] = j;//remove final
            j = j + 5;
        }
        return statSet;
    }
}
