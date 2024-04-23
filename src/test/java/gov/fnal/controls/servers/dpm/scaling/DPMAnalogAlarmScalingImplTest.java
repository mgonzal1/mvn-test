package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DPMAnalogAlarmScalingImplTest {

    private static WhatDaq getWhatDaq() throws SQLException {
        WhatDaq whatDaq = mock(WhatDaq.class);
        whatDaq.di = 10;//remove final
        whatDaq.pi = 12;//remove final and protected
        whatDaq.length = 10;//remove protected
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getInt("pi")).thenReturn(10);
        DeviceInfo.PropertyInfo propertyInfo = mock(DeviceInfo.PropertyInfo.class);
        propertyInfo.ftd = 15;//remove final
        whatDaq.pInfo = propertyInfo;//remove final
        DeviceInfo deviceInfo = mock(DeviceInfo.class);
        DeviceInfo.AnalogAlarm analogAlarm = mock(DeviceInfo.AnalogAlarm.class);
        analogAlarm.text = "text value";//remove final
        whatDaq.dInfo = deviceInfo;
        whatDaq.dInfo.analogAlarm = analogAlarm;//remove final

        return whatDaq;
    }

    private static DeviceInfo.ReadSetScaling getReadSetScaling() throws SQLException {
        DeviceInfo.ReadSetScaling readSetScaling = mock(DeviceInfo.ReadSetScaling.class);
        DeviceInfo.ReadSetScaling.Primary primary = new DeviceInfo.ReadSetScaling.Primary(mock(ResultSet.class));
        primary.index = 8;
        readSetScaling.primary = primary;
        DeviceInfo.ReadSetScaling.Common common = new DeviceInfo.ReadSetScaling.Common(mock(ResultSet.class));//added new constructor
        common.index = 8;
        common.units = "ser";
        common.constants = new double[]{30, 16, 34, 60};
        readSetScaling.common = common;
        return readSetScaling;
    }

    @Test
    public void test_textValue() throws SQLException, AcnetStatusException {
        DPMAnalogAlarmScalingImpl analogAlarmScaling = new DPMAnalogAlarmScalingImpl(getWhatDaq(), getReadSetScaling());
        assertEquals("BYPASSED", analogAlarmScaling.textValue());
    }

    @Test
    public void test_min() throws AcnetStatusException, SQLException {
        DPMAnalogAlarmScalingImpl analogAlarmScaling = new DPMAnalogAlarmScalingImpl(getWhatDaq(), getReadSetScaling());
        assertEquals(0.0, analogAlarmScaling.min(), 0);
    }

    @Test
    public void test_max() throws AcnetStatusException, SQLException {
        DPMAnalogAlarmScalingImpl analogAlarmScaling = new DPMAnalogAlarmScalingImpl(getWhatDaq(), getReadSetScaling());
        assertEquals(0.0, analogAlarmScaling.max(), 0);
    }

    @Test
    public void test_rawMin() throws AcnetStatusException, SQLException {
        DPMAnalogAlarmScalingImpl analogAlarmScaling = new DPMAnalogAlarmScalingImpl(getWhatDaq(), getReadSetScaling());
        assertEquals(0.0, analogAlarmScaling.rawMin(), 0);
    }

    @Test
    public void test_rawMax() throws AcnetStatusException, SQLException {
        DPMAnalogAlarmScalingImpl analogAlarmScaling = new DPMAnalogAlarmScalingImpl(getWhatDaq(), getReadSetScaling());
        assertEquals(0.0, analogAlarmScaling.rawMax(), 0);
    }

    @Test
    public void test_nom() throws AcnetStatusException, SQLException {
        DPMAnalogAlarmScalingImpl analogAlarmScaling = new DPMAnalogAlarmScalingImpl(getWhatDaq(), getReadSetScaling());
        assertEquals(0.0, analogAlarmScaling.nom(), 0);
    }

    @Test
    public void test_tol() throws AcnetStatusException, SQLException {
        DPMAnalogAlarmScalingImpl analogAlarmScaling = new DPMAnalogAlarmScalingImpl(getWhatDaq(), getReadSetScaling());
        assertEquals(0.0, analogAlarmScaling.tol(), 0);
    }

    @Test
    public void test_rawNom() throws AcnetStatusException, SQLException {
        DPMAnalogAlarmScalingImpl analogAlarmScaling = new DPMAnalogAlarmScalingImpl(getWhatDaq(), getReadSetScaling());
        assertEquals(0.0, analogAlarmScaling.rawNom(), 0);
    }

    @Test
    public void test_rawTol() throws AcnetStatusException, SQLException {
        DPMAnalogAlarmScalingImpl analogAlarmScaling = new DPMAnalogAlarmScalingImpl(getWhatDaq(), getReadSetScaling());
        assertEquals(0.0, analogAlarmScaling.rawTol(), 0);
    }

    @Test
    public void test_enabled() throws AcnetStatusException, SQLException {
        DPMAnalogAlarmScalingImpl analogAlarmScaling = new DPMAnalogAlarmScalingImpl(getWhatDaq(), getReadSetScaling());
        //analogAlarmScaling.enabled();
        assertFalse(analogAlarmScaling.enabled());
    }
    //start with line number 174
}
