//package gov.fnal.controls.servers.dpm.scaling;
//
//import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
//import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//
//import java.sql.ResultSet;
//import java.sql.SQLException;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//public class ReadSetScalingTest {
//
//    public static final byte[] BYTES = new byte[]{
//            (byte) 0xe0, 0x4f, (byte) 0xd0, 0x20, (byte) 0xea, 0x3a, 0x69, 0x10,
//            (byte) 0xa2, (byte) 0xd8, 0x08, 0x00, 0x2b, 0x30, 0x30, (byte) 0x9d
//    };
//
//    private DeviceInfo.ReadSetScaling getReadSetScaling(int length) throws SQLException {
//        DeviceInfo.ReadSetScaling readSetScaling = mock(DeviceInfo.ReadSetScaling.class);
//        ResultSet primaryResult = mock(ResultSet.class);
//        Mockito.when(primaryResult.getInt("primary_index")).thenReturn(length);
//        Mockito.when(primaryResult.getInt("scaling_length")).thenReturn(1);
//        readSetScaling.primary = new DeviceInfo.ReadSetScaling.Primary(primaryResult);
//        ResultSet commonResultSet = mock(ResultSet.class);
//        when(commonResultSet.getInt("common_index")).thenReturn(0);
//        when(commonResultSet.getString("common_text")).thenReturn("ser");
//        DeviceInfo.ReadSetScaling.Common common = new DeviceInfo.ReadSetScaling.Common(commonResultSet);
//        common.constants = new double[]{0, 16, 34, 45};
//        readSetScaling.common = common;
//        return readSetScaling;
//    }
//
//    @Test
//    public void scale_test() throws AcnetStatusException, SQLException {
//        ReadSetScaling setScaling = new ReadSetScaling(getReadSetScaling(0));
//        final double scale = setScaling.scale(BYTES, 2);
//        assertEquals(-0.015, scale, 0);
//    }
//
//    @Test
//    public void test_scaleLength2() throws AcnetStatusException, SQLException {
//        ReadSetScaling setScaling = new ReadSetScaling(getReadSetScaling(2));
//
//        final double scale = setScaling.scale(BYTES, 2);
//        assertEquals(-0.0146484375, scale, 0);
//    }
//
//    @Test
//    public void test_scaleLength4() throws AcnetStatusException, SQLException {
//        ReadSetScaling setScaling = new ReadSetScaling(getReadSetScaling(4));
//
//        final double scale = setScaling.scale(BYTES, 2);
//        assertEquals(-0.00732421875, scale, 0);
//    }
//
//    @Test
//    public void test_unscaleWhenStringData() throws SQLException {
//        ReadSetScaling setScaling = new ReadSetScaling(getReadSetScaling(0));
//        assertThrows(AcnetStatusException.class, () -> {
//            setScaling.unscale("ser", 2);
//        });
//
//    }
//
//    @Test
//    public void test_rawToCommon() throws AcnetStatusException, SQLException {
//        ReadSetScaling setScaling = new ReadSetScaling(getReadSetScaling(0));
//
//        final double rawToCommon = setScaling.rawToCommon(45);
//        assertEquals(0.0140625, rawToCommon, 0);
//    }
//
//    @Test
//    public void scale() throws AcnetStatusException, SQLException {
//        ReadSetScaling setScaling = new ReadSetScaling(getReadSetScaling(0));
//
//        final double rawToCommon = setScaling.scale(BYTES, 2, 1);
//        assertEquals(-0.015, rawToCommon, 0);
//    }
//}