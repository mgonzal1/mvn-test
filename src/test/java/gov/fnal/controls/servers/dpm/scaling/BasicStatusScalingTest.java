package gov.fnal.controls.servers.dpm.scaling;


import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class BasicStatusScalingTest {
    public static final byte[] BYTES = new byte[]{
            (byte) 0xe0, 0x4f, (byte) 0xd0, 0x20, (byte) 0xea, 0x3a, 0x69, 0x10,
            (byte) 0xa2, (byte) 0xd8, 0x08, 0x00, 0x2b, 0x30, 0x30, (byte) 0x9d
    };


    @Test
    public void test_scaleWithOfLengthValue() throws SQLException, AcnetStatusException {
        BasicStatusScaling basicStatusScaling = new BasicStatusScaling(getDeviceInfoData());
        assertEquals(-366948273, basicStatusScaling.scale(BYTES, 1, 4));
    }

    @Test
    public void test_scaleWithOfLength1() throws SQLException, AcnetStatusException {
        BasicStatusScaling basicStatusScaling = new BasicStatusScaling(getDeviceInfoData());
        assertEquals(79, basicStatusScaling.scale(BYTES, 1, 1));
    }

    @Test
    public void test_scaleWithOfLength2() throws SQLException, AcnetStatusException {
        BasicStatusScaling basicStatusScaling = new BasicStatusScaling(getDeviceInfoData());
        assertEquals(53327, basicStatusScaling.scale(BYTES, 1, 2));
    }

    @Test
    public void test_scaleWithOffsetValue() throws SQLException, AcnetStatusException {
        BasicStatusScaling basicStatusScaling = new BasicStatusScaling(getDeviceInfoData());
        assertThrows(AcnetStatusException.class, () -> {
            basicStatusScaling.scale(BYTES, 1);
        });
    }

    @Test
    public void test_unscaleWithStringData() throws SQLException, AcnetStatusException {
        BasicStatusScaling basicStatusScaling = new BasicStatusScaling(getDeviceInfoData());
        assertThrows(AcnetStatusException.class, () -> {
            basicStatusScaling.unscale("Test", 1);
        });
    }

    @Test
    public void test_unscaleWithDoubleValue() throws SQLException, AcnetStatusException {
        BasicStatusScaling basicStatusScaling = new BasicStatusScaling(getDeviceInfoData());
        assertThrows(AcnetStatusException.class, () -> {
            basicStatusScaling.unscale(457, 1);
        });
    }

    @Test
    public void test_getStatusString() throws SQLException, AcnetStatusException {
        DeviceInfo deviceInfoData = getDeviceInfoData();
        deviceInfoData.di = 2;
        BasicStatusScaling basicStatusScaling = new BasicStatusScaling(deviceInfoData);
        final String statusString = basicStatusScaling.getStatusString(1);
        assertEquals("character    ", statusString);
    }

    @Test
    public void test_getDefined() throws SQLException, AcnetStatusException {
        DeviceInfo deviceInfoData = getDeviceInfoData();
        deviceInfoData.di = 2;
        BasicStatusScaling basicStatusScaling = new BasicStatusScaling(deviceInfoData);
        final boolean[] defined = basicStatusScaling.getDefined();
        assertTrue(defined[0]);
    }


    public DeviceInfo getDeviceInfoData() throws SQLException {
        DeviceInfo deviceInfo = mock(DeviceInfo.class);
        deviceInfo.name = "test Name";//remove final
        deviceInfo.di = 10;//remove final

        DeviceInfo.Status status = mock(DeviceInfo.Status.class);
        status.dataLen = 2;//remove final

        List<DeviceInfo.Status.Attribute> attributes = new ArrayList<>();
        DeviceInfo.Status.Attribute attribute = mock(DeviceInfo.Status.Attribute.class);
        attribute.mask = 14255;//remove final
        attribute.match = 54987;//remove final
        attribute.inverted = true;//remove final
        attribute.shortName = "Short name";//remove final
        attribute.longName = "long Name";//remove final

        DeviceInfo.Status.Attribute.Display display = mock(DeviceInfo.Status.Attribute.Display.class);
        display.character = "character";//remove final
        display.color = 10;//remove final
        display.text = "test";
        attribute.trueDisplay = display;//remove final

        attributes.add(attribute);

        status.attributes = attributes;
        deviceInfo.status = status;//remove final

        return deviceInfo;
    }
}
