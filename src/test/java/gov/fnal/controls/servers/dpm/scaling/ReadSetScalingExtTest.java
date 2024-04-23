package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class ReadSetScalingExtTest {

    public static final byte[] BYTES = new byte[]{
            (byte) 0xe0, 0x4f, (byte) 0xd0, 0x20, (byte) 0xea, 0x3a, 0x69, 0x10,
            (byte) 0xa2, (byte) 0xd8, 0x08, 0x00, 0x2b, 0x30, 0x30, (byte) 0x9d
    };

    //constants[0] = 2 and Length < 2
    @Test
    public void test_rawToStringWhenFirstConstantIsTwoAndLengthIsLessThan2() throws SQLException {
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(getReadSetScaling(new double[]{2, 16, 1, 45, 56, 44}));
        assertThrows(AcnetStatusException.class, () -> {
            final String rawToString = readSetScalingExt.rawToString(BYTES, 2, 1);
            assertEquals("20d0", rawToString);
        });
    }

    //constants[0] = 2 and Length > 2
    @Test
    public void test_rawToStringWhenFirstConstantIsTwoAndLengthIsGreaterThan2() throws AcnetStatusException, SQLException {
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(getReadSetScaling(new double[]{2, 16, 1, 45, 56, 44}));
        final String rawToString = readSetScalingExt.rawToString(BYTES, 2, 2);
        assertEquals("20d0", rawToString);
    }

    @Test
    public void test_rawToStringAndFirstConstantIsTwoAndLengthIsLessThan4() throws SQLException {
        ReadSetScalingExt readSetScaling = new ReadSetScalingExt(getReadSetScaling(new double[]{3, 16, 2, 45, 56, 44}));
        assertThrows(AcnetStatusException.class, () -> {
            final String rawToString = readSetScaling.rawToString(BYTES, 2, 3);
            assertEquals("ea3ad020", rawToString);
        });
    }

    @Test
    public void test_rawToStringAndFirstConstantIsTwoAndLengthIsGreaterThan4() throws AcnetStatusException, SQLException {
        ReadSetScalingExt readSetScaling = new ReadSetScalingExt(getReadSetScaling(new double[]{3, 16, 2, 45, 56, 44}));
        final String rawToString = readSetScaling.rawToString(BYTES, 2, 4);
        assertEquals("ea3ad020", rawToString);
    }

    @Test
    public void test_rawToStringIndexIs8() throws AcnetStatusException, SQLException {
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(getReadSetScaling(new double[]{8, 16, 0, 45, 56, 44}));
        final String rawToString = readSetScalingExt.rawToString(BYTES, 2, 4);
        assertEquals("\uFFD0 ￪:", rawToString);
    }

    //Constants[2] == 1 or c2 == 1
    @Test
    public void test_rawToStringIndexIs8WhenConstantTwoIsEqualToOne() throws AcnetStatusException, SQLException {
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(getReadSetScaling(new double[]{8, 16, 1, 45, 56, 44}));
        final String rawToString = readSetScalingExt.rawToString(BYTES, 2, 4);
        assertEquals(" \uFFD0:￪", rawToString);
    }

    //Constants[2] >= 2 or c2 >=2
    @Test
    public void test_rawToStringIndexIs8WhenConstantTwoIsEqualToTwo() throws SQLException {
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(getReadSetScaling(new double[]{8, 16, 2, 45, 56, 44}));
        assertThrows(AcnetStatusException.class, () -> {
            final String rawToString = readSetScalingExt.rawToString(BYTES, 2, 4);
            assertEquals(" \uFFD0:￪", rawToString);
        });
    }

    @Test
    public void test_rawToStringIndexIs12() throws AcnetStatusException, SQLException {
        DeviceInfo.ReadSetScaling readSetScaling = getReadSetScaling(new double[]{12, 16, 1, 45, 56, 44});
        readSetScaling.common.enums = getEnumStringValues();//remove final common.enums
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(readSetScaling);
        final String rawToString = readSetScalingExt.rawToString(BYTES, 2, 4);
        assertEquals("shortText", rawToString);
    }

    @Test
    public void test_rawToStringIndexIs12AndLengthLessThan4() throws SQLException {
        DeviceInfo.ReadSetScaling readSetScaling = getReadSetScaling(new double[]{12, 16, 1, 45, 56, 44});
        readSetScaling.common.enums = getEnumStringValues();//remove final common.enums
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(readSetScaling);
        assertThrows(AcnetStatusException.class, () -> {
            readSetScalingExt.rawToString(BYTES, 2, 3);
        });
    }


    @Test
    public void test_rawToStringIndexIs288AndLengthEqualToOne() throws SQLException {
        DeviceInfo.ReadSetScaling readSetScaling = getReadSetScaling(new double[]{288, 16, 1, 45, 56, 44});
        readSetScaling.common.enums = getEnumStringValues();//remove final common.enums
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(readSetScaling);
        assertThrows(AcnetStatusException.class, () -> {
            readSetScalingExt.rawToString(BYTES, 2, 1);
        });
    }

    @Test
    public void test_rawToStringIndexIs289AndLengthEqualToTwo() throws SQLException {
        DeviceInfo.ReadSetScaling readSetScaling = getReadSetScaling(new double[]{289, 16, 1, 45, 56, 44});
        readSetScaling.common.enums = getEnumStringValues();//remove final common.enums
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(readSetScaling);
        assertThrows(AcnetStatusException.class, () -> {
            readSetScalingExt.rawToString(BYTES, 2, 2);
        });
    }

//    @Test(expected = AcnetStatusException.class)
//    public void test_rawToStringIndexIs289AndLengthNotEqualToTwo() throws AcnetStatusException, SQLException {
//        DeviceInfo.ReadSetScaling readSetScaling = getReadSetScaling(new double[]{289, 16, 2, 45, 56, 44});
//        readSetScaling.common.enums = getEnumStringValues();//remove final common.enums
//        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(readSetScaling);
//        final String rawToString = readSetScalingExt.rawToString(BYTES, 2, 2);
//        assertEquals("shortText", rawToString);
//    }

    @Test
    public void test_rawToStringIndexIs347() throws AcnetStatusException, SQLException {
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(getReadSetScaling(new double[]{347, 16, 1, 45, 56, 44}));
        final String rawToString = readSetScalingExt.rawToString(BYTES, 2, 4);
        assertEquals("234.58.208.32", rawToString);
    }

    @Test
    public void test_rawToStringIndexIs21() throws AcnetStatusException, SQLException {
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(getReadSetScaling(new double[]{21, 16, 1, 45, 56, 44}));
        final String rawToString = readSetScalingExt.rawToString(BYTES, 2, 4);
        assertEquals("234 2150458", rawToString);
    }

    @Test
    public void test_rawToStringIndexIs21AndLengthLessThan2() throws SQLException {
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(getReadSetScaling(new double[]{21, 16, 1, 45, 56, 44}));
        assertThrows(AcnetStatusException.class, () -> {
            readSetScalingExt.rawToString(BYTES, 2, 1);
        });
    }

    @Test
    public void test_rawToStringIndexIs22() throws AcnetStatusException, SQLException {
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(getReadSetScaling(new double[]{22, 16, 1, 45, 56, 44}));
        final String rawToString = readSetScalingExt.rawToString(BYTES, 2, 4);
        assertEquals("234 2150458", rawToString);
    }

    @Test
    public void test_rawToStringIndexIs22AndLengthLessThan2() throws SQLException {
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(getReadSetScaling(new double[]{22, 16, 1, 45, 56, 44}));
        assertThrows(AcnetStatusException.class, () -> {
            readSetScalingExt.rawToString(BYTES, 2, 1);
        });

    }


    @Test
    public void test_rawToStringIndexIs29() throws AcnetStatusException, SQLException {
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(getReadSetScaling(new double[]{29, 16, 1, 45, 56, 44}));
        final String rawToString = readSetScalingExt.rawToString(BYTES, 2, 4);
        assertEquals("25-Dec-1969 03:52:38", rawToString);
    }

    @Test
    public void test_rawToStringIndexIs280() throws AcnetStatusException, SQLException {
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(getReadSetScaling(new double[]{280, 16, 1, 45, 56, 44}));
        final String rawToString = readSetScalingExt.rawToString(BYTES, 2, 4);
        assertEquals("09-Jan-1970 13:34:28", rawToString);
    }

    @Test
    public void test_unscaleCase280() throws AcnetStatusException, SQLException {
        DeviceInfo.ReadSetScaling readSetScaling = getReadSetScaling(new double[]{280, 16, 1, 45, 56, 44});
        readSetScaling.common.index = 60;
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(readSetScaling);
        final byte[] stringToRaw = readSetScalingExt.unscale("09-Jan-1970 13:34:28", 2);
        assertEquals(68, stringToRaw[0]);
        assertEquals(-97, stringToRaw[1]);
    }

    @Test
    public void test_unscaleCase12() throws AcnetStatusException, SQLException {
        DeviceInfo.ReadSetScaling readSetScaling = getReadSetScaling(new double[]{12, 16, 1, 45, 56, 44});
        readSetScaling.common.index = 60;
        readSetScaling.common.enums = getEnumSettings();
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(readSetScaling);
        final byte[] stringToRaw = readSetScalingExt.unscale("458", 2);
        assertEquals(-54, stringToRaw[0]);
        assertEquals(1, stringToRaw[1]);
    }

    @Test
    public void test_stringToRawCase280() throws AcnetStatusException, SQLException {
        DeviceInfo.ReadSetScaling readSetScaling = getReadSetScaling(new double[]{280, 16, 1, 45, 56, 44});
        readSetScaling.common.index = 60;
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(readSetScaling);
        final byte[] stringToRaw = readSetScalingExt.stringToRaw("09-Jan-1970 13:34:28");
        assertEquals(68, stringToRaw[0]);
        assertEquals(-97, stringToRaw[1]);
    }

    @Test
    public void test_stringToRawCase669() throws AcnetStatusException, SQLException {
        DeviceInfo.ReadSetScaling readSetScaling = getReadSetScaling(new double[]{472, 16, 1, 45, 56, 44});
        readSetScaling.common.index = 84;
        ReadSetScalingExt readSetScalingExt = new ReadSetScalingExt(readSetScaling);
        final byte[] stringToRaw = readSetScalingExt.stringToRaw("09-Jan-1970 13:34:28");
        assertEquals(-37, stringToRaw[0]);
        assertEquals(-1, stringToRaw[1]);
    }

    private DeviceInfo.ReadSetScaling getReadSetScaling(double[] constants) throws SQLException {
        DeviceInfo.ReadSetScaling readSetScaling = mock(DeviceInfo.ReadSetScaling.class);
        DeviceInfo.ReadSetScaling.Primary primary = new DeviceInfo.ReadSetScaling.Primary(mock(ResultSet.class));
        primary.inputLen = 1;
        primary.index = 10;
        readSetScaling.primary = primary;
        DeviceInfo.ReadSetScaling.Common common = new DeviceInfo.ReadSetScaling.Common(mock(ResultSet.class));
        common.index = 3;
        common.units = "ser";
        common.constants = constants;
        readSetScaling.common = common;

        return readSetScaling;
    }

    private Map<Object, DeviceInfo.ReadSetScaling.Common.EnumString> getEnumStringValues() {
        Map<Object, DeviceInfo.ReadSetScaling.Common.EnumString> enums = new HashMap<>();

        List<DeviceInfo.ReadSetScaling.Common.EnumString> enumStrings = new ArrayList<>();
        DeviceInfo.ReadSetScaling.Common.EnumString enumString = new DeviceInfo.ReadSetScaling.Common.EnumString();
        enumString.value = 550517482;//remove final
        enumString.shortText = "shortText";//remove final
        enumString.longText = "long Text";//remove final
        enumStrings.add(enumString);
        for (DeviceInfo.ReadSetScaling.Common.EnumString enumStringValue : enumStrings) {
            enums.put(enumStringValue.value, enumStringValue);
        }
        return enums;
    }

    private Map<Object, DeviceInfo.ReadSetScaling.Common.EnumString> getEnumSettings() {
        Map<Object, DeviceInfo.ReadSetScaling.Common.EnumString> enums = new HashMap<>();

        List<DeviceInfo.ReadSetScaling.Common.EnumString> enumStrings = new ArrayList<>();
        DeviceInfo.ReadSetScaling.Common.EnumString enumString = new DeviceInfo.ReadSetScaling.Common.EnumString();
        enumString.value = Integer.parseInt("458");//remove final
        enumString.shortText = "shortText";//remove final
        enumString.longText = "long Text";//remove final
        enumStrings.add(enumString);
        for (DeviceInfo.ReadSetScaling.Common.EnumString enumStringValue : enumStrings) {
            enums.put(String.valueOf(enumStringValue.value), enumStringValue);
        }
        return enums;
    }
}