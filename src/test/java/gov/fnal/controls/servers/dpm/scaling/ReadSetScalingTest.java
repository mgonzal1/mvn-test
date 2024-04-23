package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.drf3.ClockType;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class ReadSetScalingTest {

    public static final byte[] BYTES = new byte[]{
            (byte) 0xe0, 0x4f, (byte) 0xd0, 0x20, (byte) 0xea, 0x3a, 0x69, 0x10,
            (byte) 0xa2, (byte) 0xd8, 0x08, 0x00, 0x2b, 0x30, 0x30, (byte) 0x9d
    };
    public static DeviceInfo.ReadSetScaling readSetScaling;

    @BeforeAll
    public static void init() throws SQLException {
        readSetScaling = mock(DeviceInfo.ReadSetScaling.class);
        DeviceInfo.ReadSetScaling.Primary primary = new DeviceInfo.ReadSetScaling.Primary(mock(ResultSet.class));
        primary.inputLen = 1;
        primary.index = 10;
        readSetScaling.primary = primary;
        DeviceInfo.ReadSetScaling.Common common = new DeviceInfo.ReadSetScaling.Common(mock(ResultSet.class));
        common.index = 0;
        common.units = "ser";
        common.constants = new double[]{0, 16, 34, 45};
        readSetScaling.common = common;
    }

    @Test
    public void scale_test() throws AcnetStatusException {
        ReadSetScaling setScaling = new ReadSetScaling(readSetScaling);
        final double scale = setScaling.scale(BYTES, 2);
        assertEquals(9.88422352E8, scale, 0);
    }

    @Test
    public void test_scaleLength2() throws AcnetStatusException {
        readSetScaling.primary.inputLen = 2;
        ReadSetScaling setScaling = new ReadSetScaling(readSetScaling);

        final double scale = setScaling.scale(BYTES, 2);
        assertEquals(8400, scale, 0);
    }

    @Test
    public void test_scaleLength4() throws AcnetStatusException {
        readSetScaling.primary.inputLen = 4;
        ReadSetScaling setScaling = new ReadSetScaling(readSetScaling);

        final double scale = setScaling.scale(BYTES, 2);
        assertEquals(9.88422352E8, scale, 0);
    }

    @Test
    public void test_unscaleWhenDoubleData() throws AcnetStatusException {
        readSetScaling.primary.inputLen = 4;
        ReadSetScaling setScaling = new ReadSetScaling(readSetScaling);

        final byte[] unscale = setScaling.unscale(45, 2);
        assertEquals(45, unscale[0]);
        assertEquals(0, unscale[1]);
    }

    @Test
    public void test_unscaleWhenStringData() {
        ReadSetScaling setScaling = new ReadSetScaling(readSetScaling);
        assertThrows(AcnetStatusException.class, () -> {
            setScaling.unscale("ser", 2);
        });

    }

    @Test
    public void test_rawToCommon() throws AcnetStatusException {
        ReadSetScaling setScaling = new ReadSetScaling(readSetScaling);

        final double rawToCommon = setScaling.rawToCommon(45);
        assertEquals(45, rawToCommon, 0);
    }

    @Test
    public void scale() throws AcnetStatusException {
        ReadSetScaling setScaling = new ReadSetScaling(readSetScaling);

        final double rawToCommon = setScaling.scale(BYTES, 2, 1);
        assertEquals(208, rawToCommon, 0);
    }
}