package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class DPMBasicStatusScalingImplTest {
    public static final byte[] BYTES = new byte[]{
            (byte) 0xe0, 0x4f, (byte) 0xd0, 0x20, (byte) 0xea, 0x3a, 0x69, 0x10,
            (byte) 0xa2, (byte) 0xd8, 0x08, 0x00, 0x2b, 0x30, 0x30, (byte) 0x9d
    };

    private static WhatDaq getWhatDaq() {
        WhatDaq whatDaq = mock(WhatDaq.class);
        whatDaq.di = 10;//remove final
        whatDaq.pi = 12;//remove final and protected
        whatDaq.length = 10;//remove protected
        DeviceInfo.PropertyInfo propertyInfo = mock(DeviceInfo.PropertyInfo.class);
        propertyInfo.ftd = 15;//remove final
        whatDaq.pInfo = propertyInfo;//remove final
        DeviceInfo deviceInfo = getDeviceInfoData();
        DeviceInfo.AnalogAlarm analogAlarm = mock(DeviceInfo.AnalogAlarm.class);
        analogAlarm.text = "text value";//remove final
        whatDaq.dInfo = deviceInfo;
        whatDaq.dInfo.analogAlarm = analogAlarm;//remove final

        return whatDaq;
    }

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

        return deviceInfo;
    }

    @Test
    public void test_scaleWithDoubleValue() throws AcnetStatusException {
        DPMBasicStatusScalingImpl basicStatusScaling = new DPMBasicStatusScalingImpl(getWhatDaq());
        basicStatusScaling.scale(getWhatDaq(), 12);
    }

    @Test
    public void test_scaleWithByteBuffer() throws AcnetStatusException {
        DPMBasicStatusScalingImpl basicStatusScaling = new DPMBasicStatusScalingImpl(getWhatDaq());
        ByteBuffer byteBuffer = ByteBuffer.allocate(64 * 1024);
        byteBuffer.put(0, (byte) 0xFF);
        byteBuffer.position(5);
        byteBuffer.put((byte) 0xFF);
        basicStatusScaling.scale(getWhatDaq(), byteBuffer);
    }

    @Test
    public void test_scaleWithByteData() throws AcnetStatusException {
        DPMBasicStatusScalingImpl basicStatusScaling = new DPMBasicStatusScalingImpl(getWhatDaq());
        basicStatusScaling.length = 2;//remove final
        basicStatusScaling.scale(getWhatDaq(), BYTES, 1);
    }

    @Test
    public void test_textValue() throws AcnetStatusException {
        DPMBasicStatusScalingImpl basicStatusScaling = new DPMBasicStatusScalingImpl(getWhatDaq());
        basicStatusScaling.rawStatus = 2;//remove final
        assertEquals("character    ", basicStatusScaling.textValue());
    }

    @Test
    public void test_hasOn() throws AcnetStatusException {
        DPMBasicStatusScalingImpl basicStatusScaling = new DPMBasicStatusScalingImpl(getWhatDaq());
        basicStatusScaling.rawStatus = 2;//remove final
        assertTrue(basicStatusScaling.hasOn());
    }

    @Test
    public void test_isOn() throws AcnetStatusException {
        DPMBasicStatusScalingImpl basicStatusScaling = new DPMBasicStatusScalingImpl(getWhatDaq());
        basicStatusScaling.rawStatus = 2;//remove final
        assertFalse(basicStatusScaling.isOn());
    }

    @Test
    public void test_hasReady() throws AcnetStatusException {
        DPMBasicStatusScalingImpl basicStatusScaling = new DPMBasicStatusScalingImpl(getWhatDaq());
        basicStatusScaling.rawStatus = 2;//remove final
        basicStatusScaling.defined[1] = true;//remove final
        assertTrue(basicStatusScaling.hasReady());
    }

    @Test
    public void test_isReady() throws AcnetStatusException {
        DPMBasicStatusScalingImpl basicStatusScaling = new DPMBasicStatusScalingImpl(getWhatDaq());
        basicStatusScaling.rawStatus = 2;//remove final
        basicStatusScaling.defined[1] = true;//remove final
        assertFalse(basicStatusScaling.isReady());
    }

    @Test
    public void test_isRemote() throws AcnetStatusException {
        DPMBasicStatusScalingImpl basicStatusScaling = new DPMBasicStatusScalingImpl(getWhatDaq());
        basicStatusScaling.rawStatus = 2;//remove final
        basicStatusScaling.defined[2] = true;//remove final
        assertFalse(basicStatusScaling.isRemote());
    }

    @Test
    public void test_isPositive() throws AcnetStatusException {
        DPMBasicStatusScalingImpl basicStatusScaling = new DPMBasicStatusScalingImpl(getWhatDaq());
        basicStatusScaling.rawStatus = 2;//remove final
        basicStatusScaling.defined[3] = true;//remove final
        assertFalse(basicStatusScaling.isPositive());
    }

    @Test
    public void test_hasPositive() throws AcnetStatusException {
        DPMBasicStatusScalingImpl basicStatusScaling = new DPMBasicStatusScalingImpl(getWhatDaq());
        basicStatusScaling.rawStatus = 2;//remove final
        basicStatusScaling.defined[3] = true;//remove final
        assertTrue(basicStatusScaling.hasPositive());
    }

    @Test
    public void test_isRamp() throws AcnetStatusException {
        DPMBasicStatusScalingImpl basicStatusScaling = new DPMBasicStatusScalingImpl(getWhatDaq());
        basicStatusScaling.rawStatus = 2;//remove final
        basicStatusScaling.defined[4] = true;//remove final
        assertFalse(basicStatusScaling.isRamp());
    }

    @Test
    public void test_hasRamp() throws AcnetStatusException {
        DPMBasicStatusScalingImpl basicStatusScaling = new DPMBasicStatusScalingImpl(getWhatDaq());
        basicStatusScaling.rawStatus = 2;//remove final
        basicStatusScaling.defined[4] = true;//remove final
        assertTrue(basicStatusScaling.hasRamp());
    }

    @Test
    public void test_rawStatus() throws AcnetStatusException {
        DPMBasicStatusScalingImpl basicStatusScaling = new DPMBasicStatusScalingImpl(getWhatDaq());
        basicStatusScaling.rawStatus = 2;//remove final
        assertEquals(2, basicStatusScaling.rawStatus(), 0);
    }
}