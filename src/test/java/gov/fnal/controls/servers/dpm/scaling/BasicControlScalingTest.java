package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class BasicControlScalingTest {
    public static final byte[] BYTES = new byte[]{
            (byte) 0xe0, 0x4f, (byte) 0xd0, 0x20, (byte) 0xea, 0x3a, 0x69, 0x10,
            (byte) 0xa2, (byte) 0xd8, 0x08, 0x00, 0x2b, 0x30, 0x30, (byte) 0x9d
    };

    @Test(expected = AcnetStatusException.class)
    public void test_scale() throws SQLException, AcnetStatusException {
        BasicControlScaling basicControlScaling = new BasicControlScaling(getDeviceInfoData());
        basicControlScaling.scale(BYTES, 1);
    }

    @Test
    public void test_unscaleWhenDoubleValue() throws SQLException, AcnetStatusException {
        BasicControlScaling basicControlScaling = new BasicControlScaling(getDeviceInfoData());
        basicControlScaling.defined[1] = true;
        final byte[] unscale = basicControlScaling.unscale(1, 4);
        assertEquals(0, unscale[0]);
    }

    @Test
    public void test_unscaleWhenStringValueWithShortText() throws SQLException, AcnetStatusException {
        BasicControlScaling basicControlScaling = new BasicControlScaling(getDeviceInfoData());
        basicControlScaling.defined[12] = true;
        final byte[] unscale = basicControlScaling.unscale("short Name", 4);
        assertEquals(10, unscale[0]);
    }
    @Test
    public void test_unscaleWhenStringValueWithLongText() throws SQLException, AcnetStatusException {
        BasicControlScaling basicControlScaling = new BasicControlScaling(getDeviceInfoData());
        basicControlScaling.defined[12] = true;
        final byte[] unscale = basicControlScaling.unscale("test Name", 4);
        assertEquals(10, unscale[0]);
    }

    public DeviceInfo getDeviceInfoData() throws SQLException {
        DeviceInfo deviceInfo = mock(DeviceInfo.class);
        DeviceInfo.Control control = mock(DeviceInfo.Control.class);

        List<DeviceInfo.Control.Attribute> tmp = new ArrayList<>();
        DeviceInfo.Control.Attribute attribute = mock(DeviceInfo.Control.Attribute.class);
        attribute.longName = "test Name";//remove final
        attribute.order = 12;//remove final
        attribute.shortName = "short Name";//remove final
        attribute.value = 10;//remove final
        tmp.add(attribute);//add public to constructor
        control.attributes = tmp.toArray(new DeviceInfo.Control.Attribute[0]);//remove final
        deviceInfo.control = control;//remove final

        return deviceInfo;
    }
}
