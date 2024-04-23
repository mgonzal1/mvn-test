package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class DigitalAlarmScalingTest {

    @Test
    public void test_scale() throws AcnetStatusException {
        DigitalAlarmScaling digitalAlarmScaling = new DigitalAlarmScaling(ScalingUtilities.getDeviceInfoData());
        assertEquals(20432, digitalAlarmScaling.scale(ScalingUtilities.BYTES, 1), 0);
    }

    @Test
    public void test_abortInhibited() throws AcnetStatusException {
        DigitalAlarmScaling digitalAlarmScaling = new DigitalAlarmScaling(ScalingUtilities.getDeviceInfoData());
        assertFalse(digitalAlarmScaling.abortInhibited());
    }

    @Test
    public void test_inAlarm() throws AcnetStatusException {
        DigitalAlarmScaling digitalAlarmScaling = new DigitalAlarmScaling(ScalingUtilities.getDeviceInfoData());
        assertFalse(digitalAlarmScaling.inAlarm());
    }

    @Test
    public void test_isEnabled() throws AcnetStatusException {
        DigitalAlarmScaling digitalAlarmScaling = new DigitalAlarmScaling(ScalingUtilities.getDeviceInfoData());
        assertFalse(digitalAlarmScaling.isEnabled());
    }

    @Test
    public void test_mask() throws AcnetStatusException {
        DigitalAlarmScaling digitalAlarmScaling = new DigitalAlarmScaling(ScalingUtilities.getDeviceInfoData());
        assertEquals(0, digitalAlarmScaling.mask());
    }

    @Test
    public void test_nom() throws AcnetStatusException {
        DigitalAlarmScaling digitalAlarmScaling = new DigitalAlarmScaling(ScalingUtilities.getDeviceInfoData());
        assertEquals(0, digitalAlarmScaling.nom());
    }

    @Test
    public void test_triesNeeded() throws AcnetStatusException {
        DigitalAlarmScaling digitalAlarmScaling = new DigitalAlarmScaling(ScalingUtilities.getDeviceInfoData());
        assertEquals(0, digitalAlarmScaling.triesNeeded());
    }

    @Test
    public void test_triesNow() throws AcnetStatusException {
        DigitalAlarmScaling digitalAlarmScaling = new DigitalAlarmScaling(ScalingUtilities.getDeviceInfoData());
        assertEquals(0, digitalAlarmScaling.triesNow());
    }
}
