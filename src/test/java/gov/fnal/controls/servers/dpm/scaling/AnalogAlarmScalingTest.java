package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class AnalogAlarmScalingTest {

    @Test
    public void test_ScaleWithExceptions() {
        assertThrows(AcnetStatusException.class,
                () -> (new AnalogAlarmScaling(1, 1, mock(DeviceInfo.ReadSetScaling.class))).scale(null));
        assertThrows(AcnetStatusException.class,
                () -> (new AnalogAlarmScaling(1, 1, mock(DeviceInfo.ReadSetScaling.class))).scale(new byte[]{'A', 20, 'A', 20,
                        'A', 20, 'A', 20, 'A', 20, 'A', 20, 'A', 20, 'A', 20, 'A', 20, 'A', 20, 'A', 20, 'A', 20}, 2));
        assertThrows(AcnetStatusException.class, () -> (new AnalogAlarmScaling(1, 1, null)).scale(new byte[]{'A', 20, 'A',
                20, 'A', 20, 'A', 20, 'A', 20, 'A', 20, 'A', 20, 'A', 20, 'A', 20, 'A', 20, 'A', 20, 'A', 20}, 2));
    }

    @Test
    public void test_Scale() throws AcnetStatusException, SQLException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(100);
        byteBuffer.putShort((short) 0xffff);
        byteBuffer.putShort((short) 423456536);

        DeviceInfo.ReadSetScaling readSetScaling = mock(DeviceInfo.ReadSetScaling.class);
        DeviceInfo.ReadSetScaling.Primary primary = new DeviceInfo.ReadSetScaling.Primary(mock(ResultSet.class));
        primary.inputLen = 4;
        primary.index = 0;
        readSetScaling.primary = primary;//remove final for primary
        DeviceInfo.ReadSetScaling.Common common = new DeviceInfo.ReadSetScaling.Common(mock(ResultSet.class));//added new constructor
        common.index = 80;//remove final for index
        common.units = "ser";//remove final for units
        common.constants = new double[]{12, 24, 34, 56};//remove final for constants
        readSetScaling.common = common;//Remove final for common

        AnalogAlarmScaling analogAlarmScaling = new AnalogAlarmScaling(1, 1, readSetScaling);
        final double scale = analogAlarmScaling.scale(byteBuffer);
        assertEquals(0, scale, 0);
    }


//
//    @Test
//    public void testIsNomTol() {
//        // Arrange, Act and Assert
//        assertTrue((new AnalogAlarmScaling(1, 1, mock(DeviceInfo.ReadSetScaling.class))).isNomTol());
//    }
//
//    /**
//     * Method under test: {@link AnalogAlarmScaling#isNomPctTol()}
//     */
//    @Test
//    public void testIsNomPctTol() {
//        // Arrange, Act and Assert
//        assertFalse((new AnalogAlarmScaling(1, 1, mock(DeviceInfo.ReadSetScaling.class))).isNomPctTol());
//    }
//
//    /**
//     * Method under test: {@link AnalogAlarmScaling#isMinMax()}
//     */
//    @Test
//    public void testIsMinMax() {
//        // Arrange, Act and Assert
//        assertFalse((new AnalogAlarmScaling(1, 1, mock(DeviceInfo.ReadSetScaling.class))).isMinMax());
//    }
//
//    /**
//     * Method under test: {@link AnalogAlarmScaling#abortInhibited()}
//     */
//    @Test
//    public void testAbortInhibited() {
//        // Arrange, Act and Assert
//        assertFalse((new AnalogAlarmScaling(1, 1, mock(DeviceInfo.ReadSetScaling.class))).abortInhibited());
//    }
//
//    /**
//     * Method under test: {@link AnalogAlarmScaling#isAbortCapable()}
//     */
//    @Test
//    public void testIsAbortCapable() {
//        // Arrange, Act and Assert
//        assertFalse((new AnalogAlarmScaling(1, 1, mock(DeviceInfo.ReadSetScaling.class))).isAbortCapable());
//    }
//
//
//    @Test
//    public void testInAlarm() {
//        // Arrange, Act and Assert
//        assertFalse((new AnalogAlarmScaling(1, 1, mock(DeviceInfo.ReadSetScaling.class))).inAlarm());
//    }
//
//    @Test
//    public void testIsEnabled() {
//        // Arrange, Act and Assert
//        assertFalse((new AnalogAlarmScaling(1, 1, mock(DeviceInfo.ReadSetScaling.class))).isEnabled());
//    }
}
