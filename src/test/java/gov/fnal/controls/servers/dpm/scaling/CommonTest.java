package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static java.lang.Float.NaN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class CommonTest {


    private static Common getCommon(int index, double[] constants) throws SQLException{
        DeviceInfo.ReadSetScaling readSetScaling = mock(DeviceInfo.ReadSetScaling.class);

        DeviceInfo.ReadSetScaling.Common common = new DeviceInfo.ReadSetScaling.Common(mock(ResultSet.class));//added new constructor
        common.index = index;//remove final for index
        common.units = "ser";//remove final for unite
        common.constants = constants;//remove final for constants
        readSetScaling.common = common;//Remove final for common
        return new Common(readSetScaling);
    }

    private static Common getCommonUnscale(int index, double[] constants) throws SQLException {
        DeviceInfo.ReadSetScaling readSetScaling = mock(DeviceInfo.ReadSetScaling.class);
        DeviceInfo.ReadSetScaling.Primary primary = new DeviceInfo.ReadSetScaling.Primary(mock(ResultSet.class));
        primary.index = index;
        readSetScaling.primary = primary;
        DeviceInfo.ReadSetScaling.Common common = new DeviceInfo.ReadSetScaling.Common(mock(ResultSet.class));//added new constructor
        common.index = index;//remove final for index
        common.units = "ser";//remove final for unite
        common.constants = constants;//remove final for constants
        readSetScaling.common = common;//Remove final for common
        return new Common(readSetScaling);
    }

    //Index 2, Number of constants less than 3
    @Test
    public void test_ScaleForIndex2() throws SQLException {
        final double[] constants = {12, 24};
        Common common1 = getCommon(2, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(40);
        });
    }

    //Index 2, C[1] != 0
    @Test
    public void test_ScaleForIndex2AndFirstValueIsNotZero() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 24, 34};
        Common common1 = getCommon(2, constants);
        final double scale = common1.scale(20);
        assertEquals(44.0, scale, 0);
    }

    //Index 2, c[1]=0
    @Test
    public void test_ScaleForIndex2AndFirstValueIsZero() throws SQLException {
        final double[] constants = {12, 0, 34};
        Common common1 = getCommon(2, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(20);
        });
    }

    //Index 4, Number of constants less than 2
    @Test
    public void test_ScaleForIndex4() throws SQLException {
        final double[] constants = {12};
        Common common1 = getCommon(4, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(40);
        });
    }

    //Index 4, C[1] != 0
    @Test
    public void test_ScaleForIndex4AndFirstValueIsNotZero() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 24, 34};
        Common common1 = getCommon(4, constants);
        final double scale = common1.scale(30);
        assertEquals(0.75, scale, 0);
    }

    //Index 4, c[1]=0
    @Test
    public void test_ScaleForIndex4AndFirstValueIsZero() throws SQLException {
        final double[] constants = {12, 0, 34};
        Common common1 = getCommon(4, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(20);
        });
    }

    //Index 6, Number of constants less than 2
    @Test
    public void test_ScaleForIndex6() throws SQLException {
        final double[] constants = {12};
        Common common1 = getCommon(6, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(60);
        });
    }

    //Index 6, C[1] != 0
    @Test
    public void test_ScaleForIndex6AndFirstValueIsNotZero() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 24, 34};
        Common common1 = getCommon(6, constants);
        final double scale = common1.scale(30);
        assertEquals(15.0, scale, 0);
    }

    //Index 6, c[1]=0
    @Test
    public void test_ScaleForIndex6AndFirstValueIsZero() throws SQLException {
        final double[] constants = {12, 0, 34};
        Common common1 = getCommon(6, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(20);
        });
    }

    //Index 8, Number of constants less than 4
    @Test
    public void test_ScaleForIndex8() throws SQLException {
        final double[] constants = {12, 55, 2};
        Common common1 = getCommon(8, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(60);
        });
    }

    //Index 8, Number of constants less than 4 and flt_temp NOT EQUAL to 0
    @Test
    public void test_ScaleForIndex8AndTempIsNotZero() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 55, 2, 76};
        Common common1 = getCommon(8, constants);
        common1.scale(60);
    }

    //Index 8, Number of constants less than 4 and c[2], c[1] EQUAL TO ZERO
    @Test
    public void test_ScaleForIndex8AndFirstSecondIndexIsZero() throws SQLException {
        final double[] constants = {12, 0, 0, 76};
        Common common1 = getCommon(8, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(33);
        });
    }

    //Index 10, Number of constants less than 2
    @Test
    public void test_ScaleForIndex10() throws SQLException {
        final double[] constants = {12};
        Common common1 = getCommon(10, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(50);
        });
    }

    //Index 10, C[0] != 0
    @Test
    public void test_ScaleForIndex10AndFirstValueIsNotZero() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 24, 34};
        Common common1 = getCommon(10, constants);
        final double scale = common1.scale(30);
        assertEquals(34.06666666666667, scale, 0);
    }

    //Index 10, c[0]=0
    @Test
    public void test_ScaleForIndex10AndFirstValueIsZero() throws SQLException {
        final double[] constants = {0, 12, 34};
        Common common1 = getCommon(10, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(20);
        });
    }

    //Index 12, Number of constants less than 5
    @Test
    public void test_ScaleForIndex12() throws SQLException {
        final double[] constants = {12, 23, 14, 34};
        Common common1 = getCommon(12, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(50);
        });
    }

    //Index 12, Number of constants greater than 5
    @Test
    public void test_ScaleForIndex12FiveConstants() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 23, 14, 34, 36};
        Common common1 = getCommon(12, constants);
        final double scale = common1.scale(50);
        assertEquals(7.7911736E7, scale, 0);
    }

    //Index 14, Number of constants less than 6
    @Test
    public void test_ScaleForIndex14() throws SQLException {
        final double[] constants = {12, 23, 14, 34};
        Common common1 = getCommon(14, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(50);
        });
    }

    //Index 14, Number of constants greater than 6
    @Test
    public void test_ScaleForIndex14FiveConstants() throws AcnetStatusException, SQLException {
        final double[] constants = {5, 2, 11, 4, 6, 1};
        Common common1 = getCommon(14, constants);
        final double scale = common1.scale(1);
        assertEquals(1.446257064290475E12, scale, 0);
    }

    //Index 16, Number of constants less than 4
    @Test
    public void test_ScaleForIndex16() throws SQLException {
        final double[] constants = {12, 23, 21};
        Common common1 = getCommon(16, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(32);
        });
    }

    //Index 16, number of constants greater than four, C[0] != 0 and c[2] != 0
    @Test
    public void test_ScaleForIndex16AndFirstValueIsNotZero() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 24, 34, 11};
        Common common1 = getCommon(16, constants);
        final double scale = common1.scale(33);
        assertEquals(5.701729919148687, scale, 0);
    }

    //Index 16, number of constants greater than four, c[0]=0 and c[2]=0
    @Test
    public void test_ScaleForIndex16AndFirstThirdValueIsZero() throws SQLException {
        final double[] constants = {0, 4, 0, 18};
        Common common1 = getCommon(16, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(13);
        });
    }

    //Index 18, Number of constants less than 6
    @Test
    public void test_ScaleForIndex18AndConstantsLessThan6() throws SQLException {
        final double[] constants = {12, 23, 21, 43, 11};
        Common common1 = getCommon(18, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(32);
        });
    }

    //Index 18, number of constants greater than 6
    @Test
    public void test_ScaleForIndex18AndConstantsGreaterThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 24, 34, 11, 14, 9};
        Common common1 = getCommon(18, constants);
        final double scale = common1.scale(3);
        assertEquals(7.542102011631089E157, scale, 0);
    }

    //Index 20, Number of constants less than 3
    @Test
    public void test_ScaleForIndex20AndConstantsLessThan3() throws SQLException {
        final double[] constants = {12, 23};
        Common common1 = getCommon(20, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(11);
        });
    }

    //Index 20, number of constants greater than 3
    @Test
    public void test_ScaleForIndex20AndConstantsGreaterThan3() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 34, 11};
        Common common1 = getCommon(20, constants);
        final double scale = common1.scale(1);
        assertEquals(34.0, scale, 0);
    }

    //Index 20, Number of constants greater than 3, c[1] = 0
    @Test
    public void test_ScaleForIndex20AndSecondValueZero() throws SQLException {
        final double[] constants = {12, 0, 21, 11};
        Common common1 = getCommon(20, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(1);
        });
    }

    //Index 22, Number of constants less than2
    @Test
    public void test_ScaleForIndex22AndConstantsLessThan2() throws SQLException {
        final double[] constants = {12};
        Common common1 = getCommon(22, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(11);
        });
    }

    //Index 22, number of constants greater than 2 and c[0] != 0
    @Test
    public void test_ScaleForIndex22AndConstantsGreaterThan2() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 34, 11};
        Common common1 = getCommon(22, constants);
        final double scale = common1.scale(1);
        assertEquals(4.846110634514353, scale, 0);
    }

    //Index 22, number of constants greater than 2 and c[0] = 0
    @Test
    public void test_ScaleForIndex22AndConstantsGreaterThan2AndFirstValueEqualToZero() throws SQLException {
        final double[] constants = {0, 4, 34, 11};
        Common common1 = getCommon(22, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(6);
        });
    }

    //Index 24, Number of constants less than 6
    @Test
    public void test_ScaleForIndex24AndConstantsLessThan6() throws SQLException {
        final double[] constants = {12};
        Common common1 = getCommon(24, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(11);
        });
    }

    //Index 24, number of constants greater than 6 and x < c[0]
    @Test
    public void test_ScaleForIndex24AndConstantsGreaterThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 34, 11, 2, 5};
        Common common1 = getCommon(24, constants);
        final double scale = common1.scale(1);
        assertEquals(180.0, scale, 0);
    }

    //Index 24, number of constants greater than 6 and x > c[0]
    @Test
    public void test_ScaleForIndex24AndConstantsGreaterThan6AndDataGreaterThanFirstValue() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 34, 11, 2, 5};
        Common common1 = getCommon(24, constants);
        final double scale = common1.scale(17);
        assertEquals(3.4637360169597498E17, scale, 0);
    }

    //Index 26, Number of constants less than 6
    @Test
    public void test_ScaleForIndex26AndConstantsLessThan6() throws SQLException {
        final double[] constants = {12, 32, 13};
        Common common1 = getCommon(26, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(15);
        });
    }

    //Index 26, number of constants greater than 6
    @Test
    public void test_ScaleForIndex26AndConstantsGreaterThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 34, 11, 2, 5, 21};
        Common common1 = getCommon(26, constants);
        final double scale = common1.scale(17);
        assertEquals(1.7542628E7, scale, 0);
    }

    //Index 28, Number of constants less than 4
    @Test
    public void test_ScaleForIndex28AndConstantsLessThan4() throws SQLException {
        final double[] constants = {12, 32, 13};
        Common common1 = getCommon(28, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(15);
        });
    }

    //Index 28, number of constants greater than 4
    @Test
    public void test_ScaleForIndex28AndConstantsGreaterThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 34, 11, 2};
        Common common1 = getCommon(28, constants);
        final double scale = common1.scale(17);
        assertEquals(11.163461538461538, scale, 0);
    }

    //Index 28, Number of constants greater than 4 and c[0] = 0 and c[1] = 0
    @Test
    public void test_ScaleForIndex28AndConstantsGreaterThan4AndFirstSecondValueZero() throws SQLException {
        final double[] constants = {0, 0, 13, 22};
        Common common1 = getCommon(28, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(15);
        });
    }

    //Index 30, Number of constants less than 6
    @Test
    public void test_ScaleForIndex30AndConstantsLessThan6() throws SQLException {
        final double[] constants = {12, 32, 13};
        Common common1 = getCommon(30, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(15);
        });
    }

    //Index 30, number of constants greater than 6 and x < c[0]
    @Test
    public void test_ScaleForIndex30AndConstantsGreaterThan6AndDataLessThanFirstValue() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 34, 11, 2, 18, 32};
        Common common1 = getCommon(30, constants);
        final double scale = common1.scale(10);
        assertEquals(18.0, scale, 0);
    }

    //Index 30, number of constants greater than 6 and x > c[0]
    @Test
    public void test_ScaleForIndex30AndConstantsGreaterThan6AndDataGreaterThanFirstValue() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 34, 11, 2, 18, 32};
        Common common1 = getCommon(30, constants);
        final double scale = common1.scale(20);
        assertEquals(45822.0, scale, 0);
    }

    //Index 32, Number of constants less than 4
    @Test
    public void test_ScaleForIndex32AndConstantsLessThan4() throws SQLException {
        final double[] constants = {12, 32, 13};
        Common common1 = getCommon(32, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(10);
        });
    }

    //Index 32, number of constants greater than 4
    @Test
    public void test_ScaleForIndex32AndConstantsGreaterThan4() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 34, 11, 22};
        Common common1 = getCommon(32, constants);
        final double scale = common1.scale(20);
        assertEquals(56.101811756527134, scale, 0);
    }

    @Test
    public void test_ScaleForIndex34AndConstantsLessThan4() throws SQLException {
        final double[] constants = {12, 24, 34};
        Common common1 = getCommon(34, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(40);
        });
    }

    //Index 34 and constants equal to 4
    @Test
    public void test_ScaleForIndex34AndConstantsEquals4() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 24, 34, 56};
        Common common1 = getCommon(34, constants);
        final double scale = common1.scale(40);
        assertEquals(0.3559322033898305, scale, 0);
    }

    //Index 34 and 4 Constants and c[1], c[2] equal to ZERO
    @Test
    public void test_ScaleForIndex34AndConstantsEquals4AndZeroForSecondAndThirdValue() throws SQLException {
        final double[] constants = {12, 24, 0, 0};
        Common common1 = getCommon(34, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(40);
        });
    }

    //Index 36, Number of constants less than 3
    @Test
    public void test_ScaleForIndex36AndConstantsLessThan3() throws SQLException {
        final double[] constants = {12, 32};
        Common common1 = getCommon(36, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(18);
        });
    }

    //Index 36, number of constants greater than 3
    @Test
    public void test_ScaleForIndex36AndConstantsGreaterThan3() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 34, 11};
        Common common1 = getCommon(36, constants);
        final double scale = common1.scale(20);
        assertEquals(56.62741699796952, scale, 0);
    }

    //Index 36, number of constants greater than 3 and c[0] = 0 and Data value should be negative
    //to make flt_temp less than 0
    @Test
    public void test_ScaleForIndex36AndConstantsGreaterThan3AndFirstValueIs0() throws SQLException {
        final double[] constants = {0, 11, 5, 11};
        Common common1 = getCommon(36, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(-433);
        });
    }

    //Index 38, Number of constants less than 6
    @Test
    public void test_ScaleForIndex38AndConstantsLessThan6() throws SQLException {
        final double[] constants = {12, 32};
        Common common1 = getCommon(38, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(18);
        });
    }

    //Index 38, number of constants greater than 6 and x > c[5] and x != 0 and c[2] != 0
    @Test
    public void test_ScaleForIndex38AndConstantsGreaterThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {3, 4, 6, 1, 4, 2};
        Common common1 = getCommon(38, constants);
        final double scale = common1.scale(3);
        assertEquals(1.9543363818224094E136, scale, 0);
    }

    //Index 38, number of constants greater than 6 and x > c[5] and x != 0 and c[2] = 0
    @Test
    public void test_ScaleForIndex38AndConstantsGreaterThan6AndThirdValueEqualToZero() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 2};
        Common common1 = getCommon(38, constants);
        final double scale = common1.scale(3);
        assertEquals(1.2915496650148828E28, scale, 0);
    }

    //Index 38, Number of constants greater than 6 and x = 0
    @Test
    public void test_ScaleForIndex38AndConstantsGreaterThan6AndDataEqualZero() throws SQLException {
        final double[] constants = {12, 32, 6, 8, 7, -3};
        Common common1 = getCommon(38, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(0);
        });
    }

    //Index 38, number of constants greater than 6 and x < c[5]
    @Test
    public void test_ScaleForIndex38AndConstantsGreaterThan6AndDataLessThanSixthValue() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 5};
        Common common1 = getCommon(38, constants);
        final double scale = common1.scale(4);
        assertEquals(760000.0, scale, 0);
    }

    //Index 40, Number of constants less than 6
    @Test
    public void test_ScaleForIndex40AndConstantsLessThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 23, 11, 5};
        Common common1 = getCommon(40, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(8);
        });
    }

    //Index 40, number of constants greater than 6
    @Test
    public void test_ScaleForIndex40AndConstantsGreaterThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 2};
        Common common1 = getCommon(40, constants);
        final double scale = common1.scale(3);
        assertEquals(9.0, scale, 0);
    }

    //Index 40, number of constants greater than 6 and c[1] = 0
    @Test
    public void test_ScaleForIndex40AndConstantsGreaterThan6AndSecondValueIsZero() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 0, 2, 11, 4, 2};
        Common common1 = getCommon(40, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(3);
        });
    }

    //Index 42, Number of constants less than 6
    @Test
    public void test_ScaleForIndex42AndConstantsLessThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 23, 11, 5};
        Common common1 = getCommon(42, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(8);
        });
    }

    //Index 42, number of constants greater than 6 and x < c[0]
    @Test
    public void test_ScaleForIndex42AndConstantsGreaterThan6AndDataLessThanFirstValue() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 2};
        Common common1 = getCommon(42, constants);
        final double scale = common1.scale(3);
        assertEquals(47.0, scale, 0);
    }

    //Index 42, number of constants greater than 6 and x > c[0]
    @Test
    public void test_ScaleForIndex42AndConstantsGreaterThan6AndDataGreaterThanFirstValue() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 2};
        Common common1 = getCommon(42, constants);
        final double scale = common1.scale(30);
        assertEquals(3.854666269441281E53, scale, 0);
    }

    //Index 44, Number of constants less than 5
    @Test
    public void test_ScaleForIndex44AndConstantsLessThan5() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 23, 11, 5};
        Common common1 = getCommon(44, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(8);
        });
    }

    //Index 44, number of constants greater than 5 and x < c[0]
    @Test
    public void test_ScaleForIndex44AndConstantsGreaterThan5AndDataLessThanFirstValue() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 2};
        Common common1 = getCommon(44, constants);
        final double scale = common1.scale(3);
        assertEquals(4.0, scale, 0);
    }

    //Index 44, number of constants greater than 5 and x > c[0]
    @Test
    public void test_ScaleForIndex44AndConstantsGreaterThan5AndDataGreaterThanFirstValue() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 2};
        Common common1 = getCommon(44, constants);
        final double scale = common1.scale(30);
        assertEquals(1.4345989662329955E53, scale, 0);
    }

    //Index 46, Number of constants less than 6
    @Test
    public void test_ScaleForIndex46AndConstantsLessThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 23, 11, 5};
        Common common1 = getCommon(46, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(8);
        });
    }

    //Index 46, number of constants greater than 6 and x < c[0]
    @Test
    public void test_ScaleForIndex46AndConstantsGreaterThan6AndDataLessThanFirstValue() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 2};
        Common common1 = getCommon(46, constants);
        final double scale = common1.scale(3);
        assertEquals(8.585743191436642E14, scale, 0);
    }

    //Index 46, number of constants greater than 6 and x > c[0]
    @Test
    public void test_ScaleForIndex46AndConstantsGreaterThan6AndDataGreaterThanFirstValue() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 2};
        Common common1 = getCommon(46, constants);
        final double scale = common1.scale(30);
        assertEquals(4.568029559262737E26, scale, 0);
    }

    //Index 48, Number of constants less than 3
    @Test
    public void test_ScaleForIndex48AndConstantsLessThan3() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 23};
        Common common1 = getCommon(48, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(8);
        });
    }

    //Index 48, number of constants greater than 3
    @Test
    public void test_ScaleForIndex48AndConstantsGreaterThan3() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 2};
        Common common1 = getCommon(48, constants);
        final double scale = common1.scale(30);
        assertEquals(12.567529473847522, scale, 0);
    }

    //Index 50, Number of constants less than 2
    @Test
    public void test_ScaleForIndex50AndConstantsLessThan2() throws AcnetStatusException, SQLException {
        final double[] constants = {12};
        Common common1 = getCommon(50, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(8);
        });
    }

    //Index 50, number of constants greater than 2
    @Test
    public void test_ScaleForIndex50AndConstantsGreaterThan2() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 2};
        Common common1 = getCommon(50, constants);
        final double scale = common1.scale(3);
        assertEquals(8.672810973760988, scale, 0);
    }

    //Index 52, Number of constants less than 5
    @Test
    public void test_ScaleForIndex52AndConstantsLessThan5() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 23, 33, 11};
        Common common1 = getCommon(52, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(8);
        });
    }

    //Index 52, number of constants greater than 5 and x < c[0]
    @Test
    public void test_ScaleForIndex52AndConstantsGreaterThan5() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 2};
        Common common1 = getCommon(52, constants);
        final double scale = common1.scale(3);
        assertEquals(162754.79141900392, scale, 0);
    }

    //Index 52, number of constants greater than 5 and x > c[0]
    @Test
    public void test_ScaleForIndex52AndConstantsGreaterThan5AndDataGreaterThanFirstValue() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 2};
        Common common1 = getCommon(52, constants);
        final double scale = common1.scale(14);
        assertEquals(4.154589706104022E68, scale, 0);
    }

    //Index 54, Number of constants less than 6
    @Test
    public void test_ScaleForIndex54AndConstantsLessThan6() throws SQLException {
        final double[] constants = {12, 23, 33, 11, 3};
        Common common1 = getCommon(54, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(8);
        });
    }

    //Index 54, number of constants greater than 6 and x < c[0]
    @Test
    public void test_ScaleForIndex54AndConstantsGreaterThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 2};
        Common common1 = getCommon(54, constants);
        final double scale = common1.scale(3);
        assertEquals(2.5813128861900675E20, scale, 0);
    }

//Index 56, number of constants greater than 3
//    @Test
//    public void test_ScaleForIndex56AndConstantsGreaterThan3() throws AcnetStatusException, SQLException {
//        final double[] constants = {12, 4, 0, 11};
//        Common common1 = getCommon(56,constants);
//        final double scale = common1.scale(3);
//        assertEquals(2.5813128861900675E20, scale, 0);
//    }

    //Index 54, number of constants greater than 6 and x > c[0]
    @Test
    public void test_ScaleForIndex54AndConstantsGreaterThan6AndDataGreaterThanFirstValue() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 4, 0, 11, 4, 2};
        Common common1 = getCommon(54, constants);
        final double scale = common1.scale(14);
        assertEquals(1.545538935590104E25, scale, 0);
    }

    //Index 56, Number of constants less than 3
    @Test
    public void test_ScaleForIndex56AndConstantsLessThan3() throws SQLException {
        final double[] constants = {12, 23};
        Common common1 = getCommon(56, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(8);
        });
    }

    //index 58, number of constants greater than 3 is also related to scalingInternal
    //Index 58, Number of constants less than 3
    @Test
    public void test_ScaleForIndex58AndConstantsLessThan3() throws SQLException {
        final double[] constants = {12, 23};
        Common common1 = getCommon(58, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(8);
        });
    }

    //Index 60
    @Test
    public void test_ScaleForIndex60() throws SQLException {
        final double[] constants = {12, 23};
        Common common1 = getCommon(60, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(8);
        });
    }

    //Index 62, Number of constants less than 3
    @Test
    public void test_ScaleForIndex62AndConstantsLessThan3() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 23};
        Common common1 = getCommon(62, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(8);
        });
    }

    //Index 62, number of constants greater than 3 and c[0] = 0
    @Test
    public void test_ScaleForIndex62AndConstantsGreaterThan3AndFirstValueIsZero() throws AcnetStatusException, SQLException {
        final double[] constants = {0, 4, 10, 11};
        Common common1 = getCommon(62, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(14);
        });
    }

    //Index 62, number of constants greater than 3 and c[0] != 0
    @Test
    public void test_ScaleForIndex62AndConstantsGreaterThan3AndFirstValueIsNotZero() throws AcnetStatusException, SQLException {
        final double[] constants = {11, 4, 10, 11};
        Common common1 = getCommon(62, constants);
        final double scale = common1.scale(14);
        assertEquals(114.95269691441536, scale, 0);
    }

    //Index 64, Number of constants less than 1
    @Test
    public void test_ScaleForIndex64AndConstantsLessThan1() throws SQLException {
        final double[] constants = {20};
        Common common1 = getCommon(64, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(18);
        });
    }

    //Index 64, number of constants greater than 1 and c[0] = 0 and x <= 4.825
    @Test
    public void test_ScaleForIndex64AndConstantsGreaterThan1() throws AcnetStatusException, SQLException {
        final double[] constants = {0, 4, 10, 11};
        Common common1 = getCommon(64, constants);
        final double scale = common1.scale(3);
        assertEquals(94.58970000000001, scale, 0);
    }

    //Index 64, number of constants greater than 1 and c[0] = 0 and x > 4.825
    @Test
    public void test_ScaleForIndex64AndConstantsGreaterThan1AndFirstValueIsZero() throws AcnetStatusException, SQLException {
        final double[] constants = {0, 4, 10, 11};
        Common common1 = getCommon(64, constants);
        final double scale = common1.scale(5);
        assertEquals(299.1889000032097, scale, 0);
    }

    //Index 64, number of constants greater than 1 and c[0] = 1 and x <= 0.9148
    @Test
    public void test_ScaleForIndex64AndConstantsGreaterThan1AndFirstValueIsOne() throws AcnetStatusException, SQLException {
        final double[] constants = {1, 4, 10, 11};
        Common common1 = getCommon(64, constants);
        final double scale = common1.scale(-1);
        assertEquals(-0.8392499999999998, scale, 0);
    }

    //Index 64, number of constants greater than 1 and c[0] = 1 and x > 0.9148
    @Test
    public void test_ScaleForIndex64AndConstantsGreaterThan1andFirstValueIsOne() throws AcnetStatusException, SQLException {
        final double[] constants = {1, 4, 10, 11};
        Common common1 = getCommon(64, constants);
        final double scale = common1.scale(1);
        assertEquals(5.263, scale, 0);
    }

    //Index 64, number of constants greater than 1 and c[0] != 0,1
    //checking default condition
    @Test
    public void test_ScaleForIndex64AndConstantsGreaterThan1AndFirstValueIs3() throws SQLException {
        final double[] constants = {3, 4, 10, 11};
        Common common1 = getCommon(64, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(31);
        });
    }

    //Index 66, Number of constants less than 4
    @Test
    public void test_ScaleForIndex66AndConstantsLessThan4() throws SQLException {
        final double[] constants = {20, 33, 11};
        Common common1 = getCommon(66, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(18);
        });
    }

    //Index 66, Number of constants greater than 4
    @Test
    public void test_ScaleForIndex66AndConstantsGreaterThan4() throws AcnetStatusException, SQLException {
        final double[] constants = {1, 4, 10, 11};
        Common common1 = getCommon(66, constants);
        final double scale = common1.scale(1);
        assertEquals(1.7592186044427E13, scale, 0);
    }

    //Index 68, Number of constants less than 6
    @Test
    public void test_ScaleForIndex66AndConstantsLessThan6() throws SQLException {
        final double[] constants = {20, 33, 11};
        Common common1 = getCommon(68, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(18);
        });
    }

    //Index 68, Number of constants greater than 6, c[0] != 0 and both X = 0, c[3] = 0
    @Test
    public void test_ScaleForIndex68AndConstantsGreaterThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {2, 4, 10, 0, 33, 21};
        Common common1 = getCommon(68, constants);
        final double scale = common1.scale(0);
        assertEquals(0.0, scale, 0);
    }

    //Index 68, Number of constants greater than 6 and c[0] = 0
    @Test
    public void test_ScaleForIndex68AndConstantsGreaterThan6AndFirstValueEqualsZero() throws AcnetStatusException, SQLException {
        final double[] constants = {0, 4, 1, 1, 3, 2};
        Common common1 = getCommon(68, constants);
        final double scale = common1.scale(0);
        assertEquals(0.0, scale, 0);
    }

    //Index 70, Number of constants less than 6
    @Test
    public void test_ScaleForIndex70AndConstantsLessThan6() throws SQLException {
        final double[] constants = {20, 33, 11};
        Common common1 = getCommon(70, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(11);
        });
    }

    //Index 70, Number of constants greater than 6
    @Test
    public void test_ScaleForIndex70AndConstantsGreaterThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {23, 4, 1, 1, 3, 2};
        Common common1 = getCommon(70, constants);
        final double scale = common1.scale(5);
        assertEquals(10.842603270655154, scale, 0);
    }

    //Index 72, Number of constants less than 6
    @Test
    public void test_ScaleForIndex72AndConstantsLessThan6() throws SQLException {
        final double[] constants = {20, 33, 11};
        Common common1 = getCommon(72, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(11);
        });
    }

    //Index 72, Number of constants greater than 6
    @Test
    public void test_ScaleForIndex72AndConstantsGreaterThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {23, 4, 1, 1, 3, 2};
        Common common1 = getCommon(72, constants);
        final double scale = common1.scale(5);
        assertEquals(3.7473247368374035E7, scale, 0);
    }

    //Index 74, Number of constants less than 6
    @Test
    public void test_ScaleForIndex74AndConstantsLessThan6() throws SQLException {
        final double[] constants = {20, 33, 11};
        Common common1 = getCommon(74, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(11);
        });
    }

    //Index 74, Number of constants greater than 6
    @Test
    public void test_ScaleForIndex74AndConstantsGreaterThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {23, 4, 1, 1, 3, 2};
        Common common1 = getCommon(74, constants);
        final double scale = common1.scale(5);
        assertEquals(1.0303030303030303, scale, 0);
    }

    //Index 76, Number of constants less than 6
    @Test
    public void test_ScaleForIndex76AndConstantsLessThan6() throws SQLException {
        final double[] constants = {20, 33, 11};
        Common common1 = getCommon(76, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(11);
        });
    }

    //Index 76, Number of constants greater than 6 and x < c[0]
    @Test
    public void test_ScaleForIndex76AndConstantsGreaterThan6AndDataLessThanFirstValue() throws AcnetStatusException, SQLException {
        final double[] constants = {23, 4, 12, 11, 3, 2};
        Common common1 = getCommon(76, constants);
        final double scale = common1.scale(5);
        assertEquals(9.765625E8, scale, 0);
    }

    //Index 76, Number of constants greater than 6 and x > c[0]
    @Test
    public void test_ScaleForIndex76AndConstantsGreaterThan6AndDataGreaterThanFirstValue() throws AcnetStatusException, SQLException {
        final double[] constants = {23, 4, 12, 11, 3, 2};
        Common common1 = getCommon(76, constants);
        final double scale = common1.scale(51);
        assertEquals(2.275293259120472E68, scale, 0);
    }

    //Index 78, Number of constants less than 4
    @Test
    public void test_ScaleForIndex78AndConstantsLessThan4()throws SQLException{
        final double[] constants = {20, 33, 11};
        Common common1 = getCommon(78, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(11);
        });
    }

    //Index 78, Number of constants greater than 4
    @Test
    public void test_ScaleForIndex78AndConstantsGreaterThan4A() throws AcnetStatusException, SQLException {
        final double[] constants = {23, 4, 12, 11, 3, 2};
        Common common1 = getCommon(78, constants);
        final double scale = common1.scale(51);
        assertEquals(2.3E217, scale, 0);
    }

    @Test
    public void test_ScaleForIndex80() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 24, 34, 56};
        Common common = getCommon(80, constants);
        final double scale = common.scale(40);
        assertEquals(40, scale, 0);
    }

    //Index 82, Number of constants less than 4
    @Test
    public void test_ScaleForIndex82AndConstantsLessThan4() throws SQLException{
        final double[] constants = {20, 33, 11};
        Common common1 = getCommon(82, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(11);
        });
    }

    //Index 82, Number of constants greater than 4
    @Test
    public void test_ScaleForIndex82AndConstantsGreaterThan4() throws AcnetStatusException, SQLException {
        final double[] constants = {23, 4, 12, 11, 3, 2};
        Common common1 = getCommon(82, constants);
        final double scale = common1.scale(51);
        assertEquals(24.293406809547605, scale, 0);
    }

    //Index 84
    @Test
    public void test_ScaleForIndex84() throws AcnetStatusException, SQLException {
        final double[] constants = {20, 33, 11};
        Common common1 = getCommon(84, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(11);
        });
    }

    //Index 86, Number of constants less than 6
    @Test
    public void test_ScaleForIndex86AndConstantsLessThan6() throws SQLException {
        final double[] constants = {20, 33, 11};
        Common common1 = getCommon(86, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(11);
        });
    }

    //Index 86, Number of constants greater than 6 and x < c[0]
    @Test
    public void test_ScaleForIndex86AndConstantsGreaterThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {23, 4, 12, 11, 3, 2};
        Common common1 = getCommon(86, constants);
        final double scale = common1.scale(5);
        assertEquals(71.0, scale, 0);
    }

    //Index 86, Number of constants greater than 6 and x > c[0] and x > c[1]
    @Test
    public void test_ScaleForIndex86AndConstGrThan6AndDataGrThanFirstSecondValue() throws AcnetStatusException, SQLException {
        final double[] constants = {23, 4, 12, 11, 3, 2};
        Common common1 = getCommon(86, constants);
        final double scale = common1.scale(32);
        assertEquals(3.637970947608805E42, scale, 0);
    }

    //Index 86, Number of constants greater than 6 and x > c[0] and x < c[1]
    @Test
    public void test_ScaleForIndex86AndConstGrThan6AndDataGrThanFirstValueAndLessThanSecondValue() throws AcnetStatusException, SQLException {
        final double[] constants = {10, 14, 12, 11, 3, 2};
        Common common1 = getCommon(86, constants);
        final double scale = common1.scale(13);
        assertEquals(7.261656921388385E14, scale, 0);
    }

    //Index 88, Number of constants less than 6
    @Test
    public void test_ScaleForIndex88AndConstantsLessThan6() throws SQLException {
        final double[] constants = {20, 33, 11};
        Common common1 = getCommon(88, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common1.scale(11);
        });
    }

    //Index 88, Number of constants greater than 6
    @Test
    public void test_ScaleForIndex88AndConstantsGreaterThan6() throws AcnetStatusException, SQLException {
        final double[] constants = {10, 14, 12, 11, 3, 2};
        Common common1 = getCommon(88, constants);
        final double scale = common1.scale(13);
        assertEquals(0.4400396432111001, scale, 0);
    }

    @Test
    public void test_ScaleForIndex90() throws SQLException {
        final double[] constants = {12, 24};
        Common common = getCommon(90, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.scale(40);
        });
    }

    //Index 201, Number of constants less than 8
    @Test
    public void test_ScaleForIndex201() throws SQLException {
        final double[] constants = {12, 24, 33, 11};
        Common common = getCommon(201, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.scale(40);
        });
    }

    //Index 201, Number of constants greater than 8
    @Test
    public void test_ScaleForIndex201AndConstantsGreaterThan8() throws AcnetStatusException, SQLException {
        final double[] constants = {10, 14, 12, 11, 3, 2, 22, 15};
        Common common1 = getCommon(201, constants);
        final double scale = common1.scale(13);
        assertEquals(6.99837413E8, scale, 0);
    }

    //Index 204 for handling default exception
    @Test
    public void test_ScaleForIndex204() throws SQLException {
        final double[] constants = {12, 24, 33, 11};
        Common common = getCommon(204, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.scale(40);
        });
    }

    //unscale possible test cases
    @Test
    public void test_unScaleForIndex0() throws AcnetStatusException, SQLException {
        final double[] constants = {30, 16, 34, 60};
        Common common = getCommonUnscale(0, constants);
        final double unscale = common.unscale(15);
        assertEquals(15.0, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex2() throws AcnetStatusException, SQLException {
        final double[] constants = {30, 16, 34, 6};
        Common common = getCommonUnscale(2, constants);
        final double unscale = common.unscale(15);
        assertEquals(-10.133333333333333, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex4() throws AcnetStatusException, SQLException {
        final double[] constants = {30, 16, 34, 6};
        Common common = getCommonUnscale(4, constants);
        final double unscale = common.unscale(15);
        assertEquals(270, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex6() throws AcnetStatusException, SQLException {
        final double[] constants = {30, 16, 34, 6};
        Common common = getCommonUnscale(6, constants);
        final double unscale = common.unscale(15);
        assertEquals(8.0, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex8() throws AcnetStatusException, SQLException {
        final double[] constants = {30, 16, 34, 60};
        Common common = getCommonUnscale(8, constants);
        final double unscale = common.unscale(15);
        assertEquals(-2.04, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex8AndThrowException() throws SQLException {
        final double[] constants = {0, 16, 34, 45};
        Common common = getCommonUnscale(8, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.unscale(45);
        });
    }

    @Test
    public void test_unScaleForIndex10WhenFirstValueIsZero() throws SQLException {
        final double[] constants = {0, 16, 34, 45};
        Common common = getCommonUnscale(10, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.unscale(45);
        });
    }

    @Test
    public void test_unScaleForIndex10() throws AcnetStatusException, SQLException {
        final double[] constants = {12, 16, 34, 45};
        Common common = getCommonUnscale(10, constants);
        final double unscale = common.unscale(15);
        assertEquals(-0.07017543859649122, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex12ThrowsException() throws SQLException {
        final double[] constants = {30, 16, 34, 60, 56, 78};
        Common common = getCommonUnscale(12, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.unscale(15);
        });
    }

    @Test
    public void test_unScaleForIndex12() throws AcnetStatusException, SQLException {
        final double[] constants = {0, 0, 34, 80, 56, 78};
        Common common = getCommonUnscale(12, constants);
        final double unscale = common.unscale(15);
        assertEquals(-0.7543323501644904, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex20WhenThirdValueISZero() throws AcnetStatusException, SQLException {
        final double[] constants = {11, 0, 0, 80, 56, 78};
        Common common = getCommonUnscale(20, constants);
        final double unscale = common.unscale(15);
        assertEquals(1.0012694597244263, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex20() throws AcnetStatusException, SQLException {
        final double[] constants = {11, 0, 10, 80, 56, 78};
        Common common = getCommonUnscale(20, constants);
        final double unscale = common.unscale(15);
        assertEquals(1.0038132667541504, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex20WhenFirstValueIsNotZero() throws SQLException {
        final double[] constants = {11, 12, 10, 80, 56, 78};
        Common common = getCommonUnscale(20, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.unscale(15);
        });
    }

    @Test
    public void test_unScaleForIndex28WhenFirstValueIsZero() throws SQLException {
        final double[] constants = {0, 12, 10, 3};
        Common common = getCommonUnscale(28, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.unscale(15);
        });
    }

    @Test
    public void test_unScaleForIndex28() throws AcnetStatusException, SQLException {
        final double[] constants = {11, 10, 16, 8};
        Common common = getCommonUnscale(28, constants);
        final double unscale = common.unscale(15);
        assertEquals(-0.7012987012987013, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex32() throws AcnetStatusException, SQLException {
        final double[] constants = {11, 10, 16, 8};
        Common common = getCommonUnscale(32, constants);
        final double unscale = common.unscale(12);
        assertEquals(-7.939061641693115, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex34() throws AcnetStatusException, SQLException {
        final double[] constants = {11, 10, 16, 8};
        Common common = getCommonUnscale(34, constants);
        final double unscale = common.unscale(12);
        assertEquals(-0.47513812154696133, unscale, 0);
    }

    //c[0] and c[2] = 0
    @Test
    public void test_unScaleForIndex34WhenFirstThirdValueIsZero() throws SQLException {
        final double[] constants = {0, 10, 0, 8};
        Common common = getCommonUnscale(34, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.unscale(12);
        });
    }

    @Test
    public void test_unScaleForIndex36() throws AcnetStatusException, SQLException {
        final double[] constants = {11, 10, 16, 8};
        Common common = getCommonUnscale(36, constants);
        final double unscale = common.unscale(12);
        assertEquals(-10.84, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex38() throws AcnetStatusException, SQLException {
        final double[] constants = {0, 0, 34, 80, 56, 78};
        Common common = getCommonUnscale(38, constants);
        final double unscale = common.unscale(15);
        assertEquals(-4, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex50() throws AcnetStatusException, SQLException {
        final double[] constants = {34, 80, 56, 78};
        Common common = getCommonUnscale(50, constants);
        final double unscale = common.unscale(15);
        assertEquals(72.33999633789062, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex60() throws AcnetStatusException, SQLException {
        final double[] constants = {11, 10, 32, 8};
        Common common = getCommonUnscale(60, constants);
        final double unscale = common.unscale(15);
        assertEquals(15.0, unscale, 0);
    }

    // c[0] = 0 and c[1] = 0
    @Test
    public void test_unScaleForIndex62WhenFirstSecondValueIsZero() throws SQLException {
        final double[] constants = {0, 0, 0, 8};
        Common common = getCommonUnscale(62, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.unscale(12);
        });
    }

    // c[0] != 0 and c[1] != 0
    @Test
    public void test_unScaleForIndex62WhenFirstSecondValueNotZero() throws SQLException {
        final double[] constants = {10, 20, 40, 8};
        Common common = getCommonUnscale(62, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.unscale(15);
        });
    }

    @Test
    public void test_unScaleForIndex62() throws AcnetStatusException, SQLException {
        final double[] constants = {10, 10, 3, 8};
        Common common = getCommonUnscale(62, constants);
        final double unscale = common.unscale(50);
        assertEquals(3.0102999566398116, unscale, 0);
    }

    @Test
    public void test_unScaleForIndex64() throws SQLException {
        final double[] constants = {10, 10, 3, 8};
        Common common = getCommonUnscale(64, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.unscale(-6);
        });
    }

    @Test
    public void test_unScaleForIndex66() throws AcnetStatusException, SQLException {
        final double[] constants = {10, 10, 3, 8};
        Common common = getCommonUnscale(66, constants);
        final double unscale = common.unscale(50);
        assertEquals(-0.0929610672108602, unscale, 0);
    }

    //index 68 , data < 0
    @Test
    public void test_unScaleForIndex68WhenDataIsLessThanZero() throws SQLException {
        final double[] constants = {10, 10, 3, 8};
        Common common = getCommonUnscale(68, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.unscale(-6);
        });
    }

    @Test
    public void test_unScaleForIndex74() throws SQLException {
        final double[] constants = {10, 10, 3, 8, 21, 5};
        Common common = getCommonUnscale(74, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.unscale(50);
        });
    }

    // 'a' value is calculated as, a = c[1]*Math.pow(c[0], c[2]);
    @Test
    public void test_unScaleForIndex76WhenDataIsLessThanValue_a() throws AcnetStatusException, SQLException {
        final double[] constants = {1, 16, 34, 45, 2};
        Common common = getCommonUnscale(76, constants);
        final double unscale = common.unscale(15);
        assertEquals(0.9981036086285222, unscale, 0);
    }

    // 'a' value is calculated as, a = c[1]*Math.pow(c[0], c[2]);
    @Test
    public void test_unScaleForIndex76WhenDataIsGreaterThanValue_a() throws AcnetStatusException, SQLException {
        final double[] constants = {1, 16, 34, 45, 2, 4};
        Common common = getCommonUnscale(76, constants);
        final double unscale = common.unscale(18);
        assertEquals(-2.4581453659370776, unscale, 0);
    }

    //index 76 and when c[1] < 0
    @Test
    public void test_unScaleForIndex76() throws AcnetStatusException, SQLException {
        final double[] constants = {1, -1, 34, 45, -2, 4};
        Common common = getCommonUnscale(76, constants);
        final double unscale = common.unscale(18);
        assertEquals(NaN, unscale, 0);
    }

    //index 76 and when c[1] > 0
    //(c[1] > 0)&&(b < 0)  and (xx < a)
    @Test
    public void test_unScaleForIndex76WhenSecondValueIsGreaterThanZero() throws AcnetStatusException, SQLException {
        final double[] constants = {1, 12, 34, 45, -2, 4};
        Common common = getCommonUnscale(76, constants);
        final double unscale = common.unscale(11);
        assertEquals(0.9974441137067975, unscale, 0);
    }

    //(c[1] > 0)&&(b < 0)  and (xx > a)
    @Test
    public void test_unScaleForIndex76WhenSecondValueGreaterThanZeroAndDataLessThanZero() throws SQLException {
        final double[] constants = {1, 12, 34, 45, -2, 4};
        Common common = getCommonUnscale(76, constants);
        assertThrows(AcnetStatusException.class, () -> {
            common.unscale(18);
        });
    }

    @Test
    public void test_unScaleForIndex78() throws AcnetStatusException, SQLException {
        final double[] constants = {0, 16, 34, 45};
        Common common = getCommonUnscale(78, constants);
        final double unscale = common.unscale(15);
        assertEquals(NaN, unscale, 0);
    }
}