package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.servers.dpm.pools.DeviceInfo;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class ScalingUtilities {
    public static final byte[] BYTES = new byte[]{
            (byte) 0xe0, 0x4f, (byte) 0xd0, 0x20, (byte) 0xea, 0x3a, 0x69, 0x10,
            (byte) 0xa2, (byte) 0xd8, 0x08, 0x00, 0x2b, 0x30, 0x30, (byte) 0x9d,
            (byte) 0xa2, (byte) 0xd8, 0x08, 0x08,  (byte) 0xa2
    };
    public static DeviceInfo getDeviceInfoData() {
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
        deviceInfo.digitalAlarm = mock(DeviceInfo.DigitalAlarm.class);//remove final

        return deviceInfo;
    }
}
