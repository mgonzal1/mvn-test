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
//import static org.powermock.api.mockito.PowerMockito.mock;
//
//
//public class PrimaryTest {
//
//
//    private static Primary getPrimary(int inputLen, int index) throws SQLException {
//        DeviceInfo.ReadSetScaling readSetScaling = mock(DeviceInfo.ReadSetScaling.class);
//        ResultSet resultSet = mock(ResultSet.class);
//        Mockito.when(resultSet.getInt("primary_index")).thenReturn(index);
//        Mockito.when(resultSet.getInt("scaling_length")).thenReturn(inputLen);
//        Mockito.when(resultSet.getString("primary_text")).thenReturn("primary_unites");
//        readSetScaling.primary = new DeviceInfo.ReadSetScaling.Primary(resultSet);
//        return new Primary(readSetScaling);
//    }
//
//    //checking default condition if scaling.primary.inputLen is neither 1,2,4
//    @Test
//    public void test_scale() throws AcnetStatusException, SQLException {
//        final Primary primary = getPrimary(10, 0);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(10);
//        });
//    }
//
//    //checking condition when input data length is 8 bits and takes primary transform index as 0
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndexZero() throws AcnetStatusException, SQLException {
//        final Primary primary = getPrimary(1, 0);
//        final double scale = primary.scale(10);
//        assertEquals(0.003125, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 2
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndexTwo() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 2);
//        final double scale = primary.scale(10);
//        assertEquals(0.0030517578125, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 4
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndexFour() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 4);
//        final double scale = primary.scale(10);
//        assertEquals(0.00152587890625, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 6
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndexSix() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 6);
//        final double scale = primary.scale(11);
//        assertEquals(8.392333984375E-4, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 8
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndexEight() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 8);
//        final double scale = primary.scale(18);
//        assertEquals(32786.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 10
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndexTen() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 10);
//        final double scale = primary.scale(21);
//        assertEquals(21.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 12
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndexTwelve() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 12);
//        final double scale = primary.scale(13);
//        assertEquals(0.040625, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 14
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex14() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 14);
//        final double scale = primary.scale(768);
//        assertEquals(0.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 16
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex16() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 16);
//        final double scale = primary.scale(6);
//        assertEquals(8.407790785948902E-45, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 18
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex18() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 18);
//        final double scale = primary.scale(43);
//        assertEquals(0.0447458, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index 20 and float data >= 0.
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex20FDGreaterThanZero() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 20);
//        final double scale = primary.scale(12);
//        assertEquals(12.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits, primary transform index 20, Input length 1 and flt_data < 0
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex20InputLengthOne() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 20);
//        final double scale = primary.scale(-1);
//        assertEquals(255.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 22
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex22() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 22);
//        final double scale = primary.scale(4);
//        assertEquals(9.183549615799121E-41, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 24
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex24() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 24);
//        final double scale = primary.scale(11);
//        assertEquals(1.0101904577379033E-39, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 26
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex26() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 26);
//        final double scale = primary.scale(12);
//        assertEquals(-0.310269935, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 28
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex28() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 28);
//        final double scale = primary.scale(11);
//        assertEquals(720896.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 30
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex30() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 30);
//        final double scale = primary.scale(18);
//        assertEquals(18.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 32
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex32() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 32);
//        final double scale = primary.scale(91);
//        assertEquals(0.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 34
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex34() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 34);
//        final double scale = primary.scale(10);
//        assertEquals(10.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 36
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex36() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 36);
//        final double scale = primary.scale(12);
//        assertEquals(0.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 38
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex38() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 38);
//        final double scale = primary.scale(32);
//        assertEquals(0.07908841460729565, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 40
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex40() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 40);
//        final double scale = primary.scale(43);
//        assertEquals(0.16796875, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 42
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex42() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 42);
//        final double scale = primary.scale(54);
//        assertEquals(0.00823974609375, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 44
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex44() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 44);
//        final double scale = primary.scale(14);
//        assertEquals(14.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 46
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex46() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 46);
//        final double scale = primary.scale(11);
//        assertEquals(11.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 46 and x < 0
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex46_xLessThanZero() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 46);
//        final double scale = primary.scale(-3);
//        assertEquals(4.294967293E9, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 48
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex48() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 48);
//        final double scale = primary.scale(7);
//        assertEquals(2.7247470139649223E-43, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 50
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex50() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 50);
//        final double scale = primary.scale(2);
//        assertEquals(2.802596928649634E-45, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 52
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex52WhenLengthIs2() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 52);
//        final double scale = primary.scale(2);
//        assertEquals(512.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 52
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex52WhenLengthIs4() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(4, 52);
//        final double scale = primary.scale(5);
//        assertEquals(8.388608E7, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 52
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex52WhenLengthIs1() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 52);
//        final double scale = primary.scale(8);
//        assertEquals(0.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 54
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex52WhenLengthIsNot2() throws SQLException {
//        Primary primary = getPrimary(1, 54);
//        assertThrows(AcnetStatusException.class, () -> {
//            final double scale = primary.scale(31);
//            assertEquals(4.0151371806996, scale, 0);
//        });
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 56 and handles exception condition
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex56() throws SQLException {
//        Primary primary = getPrimary(1, 56);
//        assertThrows(AcnetStatusException.class, () -> {
//            final double scale = primary.scale(12);
//            assertEquals(12.0, scale, 0);
//        });
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 58
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex58() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 58);
//        final double scale = primary.scale(17);
//        assertEquals(0.06640625, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 60
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex60() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 60);
//        final double scale = primary.scale(10);
//        assertEquals(7.006492321624085E-42, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits and when primary transform index as 62
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex62() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 62);
//        final double scale = primary.scale(22);
//        assertEquals(0.0034375, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits, primary transform index as 64 and Input Length as 1.
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex64() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 64);
//        final double scale = primary.scale(7);
//        assertEquals(0.0546875, scale, 0);
//    }
//
//    //Here we are handling AcnetStatusException and checking condition when input data length is 8 bits, primary transform index as 66, Input Length as 1.
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex66() throws SQLException {
//        Primary primary = getPrimary(1, 66);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(0);
//        });
//    }
//
//    //checking condition when input data length is 8 bits, primary transform index as 68
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex68() throws SQLException {
//        Primary primary = getPrimary(1, 68);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(9);
//        });
//    }
//
//    //checking condition when input data length is 8 bits, primary transform index as 70
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex70() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 70);
//        final double scale = primary.scale(122);
//        assertEquals(0.122, scale, 0);
//    }
//
//    //Here we are handling AcnetStatusException and checking condition when input data length is 8 bits, primary transform index as 72, Input Length as 1.
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex72() throws SQLException {
//        Primary primary = getPrimary(1, 72);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(0);
//        });
//    }
//
//    //Here we are handling AcnetStatusException and checking condition when input data length is 8 bits, primary transform index as 74, Input Length as 1.
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex74() throws SQLException {
//        Primary primary = getPrimary(1, 74);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(0);
//        });
//    }
//
//    //checking condition when input data length is 8 bits, primary transform index as 74
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex74WhenLengthIsEqualTo2() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 74);
//        final double scale = primary.scale(42);
//        assertEquals(0.02691696, scale, 0);
//    }
//
//    //Checking condition when input data length is 8 bits, primary transform index as 76, Input Length as 1.
//    @Test
//    public void test_scaleFor8bitAndPrimaryIndex76() throws SQLException {
//        Primary primary = getPrimary(1, 76);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(0);
//        });
//    }
//
//    //checking condition when input data length is 8 bits, primary transform index as 78
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex78() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 78);
//        final double scale = primary.scale(0);
//        assertEquals(0.0, scale, 0);
//    }
//
//    //checking condition when input data length is 8 bits, primary transform index as 80
//    @Test
//    public void test_scaleFor_8bitAndPrimaryIndex80() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 80);
//        final double scale = primary.scale(12);
//        assertEquals(1.6815581571897805E-44, scale, 0);
//    }
//
//    //Checking condition when input data length is 8 bits, primary transform index as 82, Input Length as 1.
//    @Test
//    public void test_scaleFor8bitAndPrimaryIndex82() throws SQLException {
//        Primary primary = getPrimary(1, 82);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(0);
//        });
//    }
//
//    //Checking condition when input data length is 8 bits, primary transform index as 84, Input Length as 1.
//    @Test
//    public void test_scaleFor8bitAndPrimaryIndex84() throws SQLException {
//        Primary primary = getPrimary(1, 84);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(0);
//        });
//    }
//
//    //checking condition when input data length is 16 bits and takes primary transform index is 0
//    @Test
//    public void test_scaleFor_16bitAndPrimaryIndexZero() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 0);
//        final double scale = primary.scale(10);
//        assertEquals(0.003125, scale, 0);
//    }
//
//    //checking condition when input data length is 16 bits and when primary transform index as 2
//    @Test
//    public void test_scaleFor_16bitAndPrimaryIndexTwo() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 2);
//        final double scale = primary.scale(5);
//        assertEquals(0.00152587890625, scale, 0);
//    }
//
//    //checking condition when input data length is 16 bits, primary transform index as 20, InputLength not equal to One and flt_data greater than 0.
//    @Test
//    public void test_scaleFor_16bitAndPrimaryIndex20AndFDGreaterThanZero() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 20);
//        final double scale = primary.scale(6);
//        assertEquals(6.0, scale, 0);
//    }
//
//    //checking condition when input data length is 16 bits, primary transform index as 20, InputLength 2 and flt_data less than zero
//    @Test
//    public void test_scaleFor_16bitAndPrimaryIndex20AndFDLessThanZero() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 20);
//        final double scale = primary.scale(-1);
//        assertEquals(65535.0, scale, 0);
//    }
//
//    //checking condition when input data length is 16 bits and when primary transform index as 22
//    @Test
//    public void test_scaleFor_16bitAndPrimaryIndex22() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 2);
//        final double scale = primary.scale(7);
//        assertEquals(0.00213623046875, scale, 0);
//    }
//
//    //checking condition when input data length is 16 bits and when primary transform index as 56
//    @Test
//    public void test_scaleFor_16bitAndPrimaryIndex56() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 56);
//        final double scale = primary.scale(7);
//        assertEquals(-9.99786376953125, scale, 0);
//    }
//
//    //checking condition when input data length is 16 bits and when primary transform index as 64
//    @Test
//    public void test_scaleFor_16bitAndPrimaryIndex64() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 64);
//        final double scale = primary.scale(7);
//        assertEquals(2.13623046875E-4, scale, 0);
//    }
//
//    //Checking condition when input data length is 16 bits, primary transform index as 66, Input Length as 2.
//    @Test
//    public void test_scaleFor16bitAndPrimaryIndex66() throws SQLException {
//        Primary primary = getPrimary(2, 66);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(0);
//        });
//    }
//
//    //Checking condition when input data length is 16 bits, primary transform index as 66, Input Length as 2 and scale data greater than zero.
//    @Test
//    public void test_scaleFor16bitAndPrimaryIndex66AndDataGreaterThan0() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 66);
//        final double scale = primary.scale(11);
//        assertEquals(0.0034375, scale, 0);
//    }
//
//    //Checking condition when input data length is 16 bits, primary transform index as 72, Input Length as 2.
//    @Test
//    public void test_scaleFor16bitAndPrimaryIndex72() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 72);
//        final double scale = primary.scale(4);
//        assertEquals(-10.23875, scale, 0);
//    }
//
//    //Checking condition when input data length is 16 bits, primary transform index as 72, Input Length as 2.
//    @Test
//    public void test_scaleFor16bitAndPrimaryIndex74() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 74);
//        final double scale = primary.scale(4);
//        assertEquals(0.00256352, scale, 0);
//    }
//
//    //Checking condition when input data length is 16 bits, primary transform index as 76, Input Length as 2.
//    @Test
//    public void test_scaleFor16bitAndPrimaryIndex76() throws SQLException {
//        Primary primary = getPrimary(2, 76);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(0);
//        });
//    }
//
//    //Checking condition when input data length is 16 bits, primary transform index as 82, Input Length as 2 and Scale_Data greater than 0.
//    @Test
//    public void test_scaleFor16bitAndPrimaryIndex82DataGreaterThan0() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 82);
//        final double scale = primary.scale(4);
//        assertEquals(0.009768009768009768, scale, 0);
//    }
//
//    //Checking condition when input data length is 16 bits, primary transform index as 82, Input Length as 2 and Scale_Data less than 0.
//    @Test
//    public void test_scaleFor16bitAndPrimaryIndex82DataLessThan0() throws SQLException {
//        Primary primary = getPrimary(2, 82);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(-2);
//        });
//
//    }
//
//    //Checking condition when input data length is 16 bits, primary transform index as 84, Input Length as 2.
//    @Test
//    public void test_scaleFor16bitAndPrimaryIndex84() throws SQLException {
//        Primary primary = getPrimary(2, 84);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(0);
//        });
//
//    }
//
//    //checking condition when input data length is 32 bits and takes primary index as 0.
//    @Test
//    public void test_scaleFor_32bitAndPrimaryIndexZero() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(4, 0);
//        final double scale = primary.scale(14);
//        assertEquals(0.004375, scale, 0);
//    }
//
//    //checking condition when input data length is 16 bits, primary transform index as 20, InputLength 2 and flt_data less than zero
//    @Test
//    public void test_scaleFor_32bitAndPrimaryIndex20AndFDGreaterThanZero() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 20);
//        final double scale = primary.scale(9);
//        assertEquals(9.0, scale, 0);
//    }
//
//    //checking condition when input_data_length 32 bits, primary_index 20, Input_Length is equal to 4 and flt_data less than 0.
//    @Test
//    public void test_scaleFor_32bitAndPrimaryIndex20AndFDLessThanZero() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(4, 20);
//        final double scale = primary.scale(-1);
//        assertEquals(65535.0, scale, 0);
//    }
//
//    //checking condition when input data length is 32 bits, primary transform index as 64 and Input Length as 4.
//    @Test
//    public void test_scaleFor_32bitAndPrimaryIndex64() throws AcnetStatusException, SQLException {
//
//        Primary primary = getPrimary(4, 64);
//        final double scale = primary.scale(7);
//        assertEquals(3.259629011154175E-9, scale, 0);
//    }
//
//    //Here we are handling AcnetStatusException and checking condition when input data length is 32 bits, primary transform index as 66, Input Length as 4.
//    @Test
//    public void test_scaleFor_32bitAndPrimaryIndex66() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(4, 66);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(0);
//        });
//    }
//
//    //Here we are handling AcnetStatusException and checking condition when input data length is 32 bits, primary transform index as 72, Input Length as 4.
//    @Test
//    public void test_scaleFor_32bitAndPrimaryIndex72() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(4, 72);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(0);
//        });
//    }
//
//    //Here we are handling AcnetStatusException and checking condition when input data length is 32 bits, primary transform index as 74, Input Length as 4.
//    @Test
//    public void test_scaleFor_32bitAndPrimaryIndex74() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(4, 74);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(0);
//        });
//    }
//
//    //checking condition when input data length is 32 bits, primary transform index as 76 and Input Length as 4.
//    //given scale_data greater than zero which calculates y using x1 and x2 values. (y>=0)
//    @Test
//    public void test_scaleFor32bitAndPrimaryIndex76DataGreaterThanZero() throws AcnetStatusException, SQLException {
//
//        Primary primary = getPrimary(4, 76);
//        final double scale = primary.scale(13);
//        assertEquals(851968.0, scale, 0);
//    }
//
//    //checking condition when input data length is 32 bits, primary transform index as 76 and Input Length as 4.
//    //given scale_data less than zero which calculates y using x1 and x2 values. (y<0)
//    @Test
//    public void test_scaleFor32bitAndPrimaryIndex76DataLessThanZero() throws AcnetStatusException, SQLException {
//
//        Primary primary = getPrimary(4, 76);
//        final double scale = primary.scale(-1);
//        assertEquals(4.294967295E9, scale, 0);
//    }
//
//    //Checking condition when input data length is 32 bits, primary transform index as 82, Input Length as 4.
//    @Test
//    public void test_scaleFor32bitAndPrimaryIndex82() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(4, 82);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(0);
//        });
//    }
//
//    //Checking condition when input data length is 32 bits, primary transform index as 84, Input Length as 4.
//    @Test
//    public void test_scaleFor32bitAndPrimaryIndex84() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(4, 84);
//        final double scale = primary.scale(5);
//        assertEquals(6.018531076210112E-36, scale, 0);
//    }
//
//
//    //Below are the possible unit test cases for unscale method that takes value as input
//    //unscale function is used to scale a value expressed in primary units to a corresponding value in unscaled data units.
//
//    @Test
//    public void test_scaleForInvalidPrimaryTransformIndex() throws SQLException {
//        Primary primary = getPrimary(4, 87);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.scale(0);
//        });
//    }
//
//    //Here we are handling AcnetStatusException "overflow", input_length 1, index  0 and given value 5.
//    // Checks "case 1" condition (value < -128) || (value > 127), true throws exception
//    @Test
//    public void test_unscale_WhenLengthIs1AndPrimaryIndexIs0() throws SQLException {
//        Primary primary = getPrimary(1, 0);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(128);
//        });
//    }
//
//    //Given InputLength as 2 and Index is 2, checks (value < -32768) || (value > 32767) condition is false so returns "value".
//    @Test
//    public void test_unscale_WhenLengthIs1AndIndexIs2() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 8);
//        final int unscale = primary.unscale(32768);
//        assertEquals(0, unscale);
//    }
//
//    //Here we are handling AcnetStatusException "overflow", input_length 2, index  0 and unscale_value -34.
//    // Checks "case 2" condition (value < -32768) || (value > 32767), true throws exception
//    @Test
//    public void test_unscale_WhenLengthIs2AndPrimaryIndexIs0() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 0);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(-34);
//        });
//
//    }
//
//    //Given InputLength as 2 and Index is 2, checks (value < -32768) || (value > 32767) condition false so returns "value".
//    @Test
//    public void test_unscale_WhenLengthIs2AndIndexIs2() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 2);
//        final int unscale = primary.unscale(5);
//        assertEquals(16384, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex2() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 2);
//        final int unscale = primary.unscale(7);
//        assertEquals(22937, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex4() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 4);
//        final int unscale = primary.unscale(6);
//        assertEquals(39321, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex6() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 6);
//        final int unscale = primary.unscale(8);
//        assertEquals(104857, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex8() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 8);
//        final int unscale = primary.unscale(32);
//        assertEquals(-32736, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex10() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 10);
//        final int unscale = primary.unscale(32768);
//        assertEquals(32768, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex12() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 12);
//        final int unscale = primary.unscale(21);
//        assertEquals(6720, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex14() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 14);
//        final int unscale = primary.unscale(9);
//        assertEquals(49161, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex14AndValueGreaterThan1023() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 14);
//        final int unscale = primary.unscale(1123);
//        assertEquals(50288, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLengthIs3AndPrimaryIndexIs50() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 50);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(12);
//        });
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex16() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 16);
//        final int unscale = primary.unscale(456);
//        assertEquals(1139015680, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex18() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 18);
//        final int unscale = primary.unscale(4);
//        assertEquals(3843.0, unscale, 0);
//    }
//
//    //Input Length 3 and Index 20, Handling exception for value < 0
//    @Test
//    public void test_unscale_WhenLengthIs3AndPrimaryIndexIs20() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 20);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(-1);
//        });
//    }
//
//    //Input Length 1 and Index 20, Handling exception for value > 255
//    @Test
//    public void test_unscale_WhenLengthIs1AndPrimaryIndexIs20AndValueGreaterThan255() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 20);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(345);
//        });
//    }
//
//    //Input Length 1 and Index 20, Checking condition for value < 255
//    @Test
//    public void test_unscale_WhenLength1AndIndex20() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 20);
//        final int unscale = primary.unscale(96);
//        assertEquals(96, unscale);
//    }
//
//    //Input Length 2 and Index 20, Handling exception for value > 65535
//    @Test
//    public void test_unscale_WhenLengthIs2AndPrimaryIndexIs20AndValueGreaterThan65535() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 20);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(65576);
//        });
//    }
//
//    //Input Length 2 and Index 20, Checking condition for value < 65535
//    @Test
//    public void test_unscale_WhenLength2AndIndex20ValueLessThan65535() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 20);
//        final int unscale = primary.unscale(655);
//        assertEquals(655, unscale);
//    }
//
//    //Input Length 4 and Index 20, Handling exception
//    @Test
//    public void test_unscale_WhenLengthIs4AndPrimaryIndexIs20() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(4, 20);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(345);
//        });
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex22() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 22);
//        final int unscale = primary.unscale(8);
//        assertEquals(16896, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex24() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 24);
//        final int unscale = primary.unscale(11);
//        assertEquals(16688, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex26() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 26);
//        final int unscale = primary.unscale(21);
//        assertEquals(448362, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex28() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 28);
//        final int unscale = primary.unscale(234);
//        assertEquals(15335424, unscale);
//    }
//
//    //case 30: Size which is input length is passed as 1.
//    @Test
//    public void test_unscale_WhenLengthIs1AndPrimaryIndexIs30() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 30);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(231);
//        });
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex32() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 32);
//        final int unscale = primary.unscale(96);
//        assertEquals(24576, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex34() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 34);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(765);
//        });
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex36() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 36);
//        final int unscale = primary.unscale(96);
//        assertEquals(24576, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex38() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 38);
//        final int unscale = primary.unscale(32);
//        assertEquals(95, unscale);
//    }
//
//    //case 40: Size which is input length is passed as 1.
//    @Test
//    public void test_unscale_WhenLengthIs1AndPrimaryIndexIs40() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 40);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(-231);
//        });
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex42() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 42);
//        final int unscale = primary.unscale(3276889);
//        assertEquals(65535, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLength3AndIndex44() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 44);
//        final int unscale = primary.unscale(96);
//        assertEquals(150, unscale);
//    }
//
//    //Length 3, Primary Index 46 and Handles exception case by passing value < 0
//    @Test
//    public void test_unscale_WhenLengthIs3AndPrimaryIndexIs46() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 46);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(-231);
//        });
//    }
//
//    //Length 3, Primary Index 46 and Handles exception case by passing value > 0
//    @Test
//    public void test_unscale_WhenLengthIs3AndPrimaryIndexIs46ValueGreaterThan0() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 46);
//        final int unscale = primary.unscale(23);
//        assertEquals(23, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLengthIs3AndPrimaryIndexIs48() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 48);
//        final int unscale = primary.unscale(6);
//        assertEquals(1.046294299E9, unscale, 0);
//    }
//
//    @Test
//    public void test_unscale_WhenLengthIs2AndPrimaryIndexIs52() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 52);
//        final int unscale = primary.unscale(5);
//        assertEquals(1280.0, unscale, 0);
//    }
//
//    @Test
//    public void test_unscale_WhenLengthIs4AndPrimaryIndexIs52() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(4, 52);
//        final int unscale = primary.unscale(7);
//        assertEquals(1.17440512E8, unscale, 0);
//    }
//
//    //Length != 1 or Value < 4 or Value > 20
//    @Test
//    public void test_unscale_WhenLengthIsNotEqualTo2AndPrimaryIndexIs54() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 54);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(33);
//        });
//    }
//
//    @Test
//    public void test_unscale_WhenLengthIs2AndIndexIs54() throws SQLException {
//        Primary primary = getPrimary(2, 54);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(31);
//        });
//    }
//
//    @Test
//    public void test_unscale_WhenLengthIsNotEqualTo2AndPrimaryIndexIs56() throws SQLException {
//        Primary primary = getPrimary(3, 56);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(33);
//        });
//    }
//
//    @Test
//    public void test_unscale_WhenLengthIs2AndPrimaryIndexIs56() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 56);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(51);
//        });
//    }
//
//    @Test
//    public void test_unscale_WhenLengthIs2AndPrimaryIndexIs58() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 58);
//        final int unscale = primary.unscale(655399);
//        assertEquals(1.67782144E8, unscale, 0);
//    }
//
//    @Test
//    public void test_unscale_WhenLengthIs2AndPrimaryIndexIs60() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 60);
//        final int unscale = primary.unscale(51);
//        assertEquals(1.037100384E9, unscale, 0);
//    }
//
//    @Test
//    public void test_unscale_WhenLengthIs3AndPrimaryIndexIs60() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 60);
//        final int unscale = primary.unscale(5);
//        assertEquals(1.00898177E9, unscale, 0);
//    }
//
//    @Test
//    public void test_unscale_WhenLengthIs3AndPrimaryIndexIs62() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 62);
//        final int unscale = primary.unscale(1300);
//        assertEquals(62464, unscale);
//    }
//
//    //Handles exception in index 64, any input length and value should be either < -1.0 or value > 1.0
//    @Test
//    public void test_unscale_WhenLengthIs3AndPrimaryIndexIs64() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 64);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(-33);
//        });
//    }
//
//    //Index 64, any input length = 1 and !(value < -1.0) || (value > 1.0)
//    @Test
//    public void test_unscale_WhenLengthIs1AndPrimaryIndexIs64() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 64);
//        final int unscale = primary.unscale(-1);
//        assertEquals(-128, unscale);
//    }
//
//    //Index 64, any input length = 2 and !(value < -1.0) || (value > 1.0)
//    @Test
//    public void test_unscale_WhenLengthIs2AndPrimaryIndexIs64() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 64);
//        final int unscale = primary.unscale(-1);
//        assertEquals(-32768, unscale);
//    }
//
//    //Index 64, any input length = 4 and !(value < -1.0) || (value > 1.0)
//    @Test
//    public void test_unscale_WhenLengthIs4AndPrimaryIndexIs64() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(4, 64);
//        final int unscale = primary.unscale(-1);
//        assertEquals(-2147483648, unscale);
//    }
//
//    //handles exception - index 66, value <= 0
//    @Test
//    public void test_unscale_WhenLengthIs3AndPrimaryIndexIs66() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 66);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(0);
//        });
//    }
//
//    //Index 66, value > 0
//    @Test
//    public void test_unscale_WhenLengthIs3AndPrimaryIndexIs66AndValueGreaterThan0() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 66);
//        final int unscale = primary.unscale(54);
//        assertEquals(172800, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLengthIs3AndPrimaryIndexIs68() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 68);
//        final int unscale = primary.unscale(52);
//        assertEquals(52, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLengthIs3AndPrimaryIndexIs70() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 70);
//        final int unscale = primary.unscale(8);
//        assertEquals(8000, unscale);
//    }
//
//    @Test
//    public void test_unscale_WhenLengthNotEqualTo2AndPrimaryIndexIs72() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 72);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(22);
//        });
//    }
//
//    @Test
//    public void test_unscale_WhenLengthEqualToTwoAndPrimaryIndexIs72() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 72);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(21);
//        });
//    }
//
//    @Test
//    public void test_unscale_WhenLengthNotEqualTo2AndPrimaryIndexIs74() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 74);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(32);
//        });
//    }
//
//    @Test
//    public void test_unscale_WhenLengthEqualToTwoAndPrimaryIndexIs74AndValueLEssThan0() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 74);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(-3);
//        });
//    }
//
//    //handles exception when input length not equal to 4
//    @Test
//    public void test_unscale_WhenLengthNotEqualTo4AndPrimaryIndexIs76() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 76);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(22);
//        });
//    }
//
//    //handles exception when input length equal to 4
//    @Test
//    public void test_unscale_WhenLength4AndPrimaryIndexIs76() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(4, 76);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(-324);
//        });
//    }
//
//    @Test
//    public void test_unscale_WhenLength4AndPrimaryIndexIs76AndValueGreaterThan0() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(4, 76);
//        final int unscale = primary.unscale(4);
//        assertEquals(262144, unscale);
//    }
//
//    //Handling exception with Input length as 3, Primary index 78 and value ((xx < 0) || (xx > 5.0))
//    @Test
//    public void test_unscale_WhenLength3AndPrimaryIndexIs78() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 78);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(6);
//        });
//    }
//
//    //Input length 3, Primary index 78 and value !((xx < 0) || (xx > 5.0))
//    @Test
//    public void test_unscale_WhenLength3AndPrimaryIndexIs78AndValue4() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 78);
//        final int unscale = primary.unscale(4);
//        assertEquals(1082130432, unscale);
//    }
//
//    //Handling exception with Input length as 3, Primary index 80 and value ((xx < 0) || (xx > 10.0))
//    @Test
//    public void test_unscale_WhenLength3AndPrimaryIndexIs80() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 80);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(-3);
//        });
//    }
//
//    //Input length 3, Primary index 80 and value !((xx < 0) || (xx > 10.0))
//    @Test
//    public void test_unscale_WhenLength3AndPrimaryIndexIs80AndValue4() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(3, 80);
//        final int unscale = primary.unscale(4);
//        assertEquals(1082130432, unscale);
//    }
//
//    //Handling exception with Input length as 1, Primary index 82
//    @Test
//    public void test_unscale_WhenLength1AndPrimaryIndexIs82() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 82);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(6);
//        });
//    }
//
//    //Length = 2, Index = 82 and Value should be between ((xx < 0)||(xx > 10.0) )
//    @Test
//    public void test_unscale_WhenLength2AndPrimaryIndexIs82AndValue13() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 82);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(13);
//        });
//    }
//
//    //Length = 2, Index = 82 and Value !((xx < 0)||(xx > 10.0) )
//    @Test
//    public void test_unscale_WhenLength2AndPrimaryIndexIs82AndValue4() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(2, 82);
//        final int unscale = primary.unscale(4);
//        assertEquals(1638, unscale);
//    }
//
//    //Handles Exception - Length not equal to 4 and primary index 84
//    @Test
//    public void test_unscale_WhenLength1AndPrimaryIndexIs84() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(1, 84);
//        assertThrows(AcnetStatusException.class, () -> {
//            primary.unscale(13);
//        });
//    }
//
//    //Length equal to 4 and primary index 84
//    @Test
//    public void test_unscale_WhenLength4AndPrimaryIndexIs84() throws AcnetStatusException, SQLException {
//        Primary primary = getPrimary(4, 84);
//        final int unscale = primary.unscale(13);
//        assertEquals(20545, unscale);
//    }
//}