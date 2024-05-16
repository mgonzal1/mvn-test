package gov.fnal.controls.servers.dpm.drf3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ByteRangeTest {

    @Test
    public void test_getType() {
        ByteRange range = new ByteRange(9, 20);
        final RangeType type = range.getType();
        assertEquals("BYTE",type.name());
    }

    @Test
    public void test_getStartIndex() {
        ByteRange range = new ByteRange(9, 20);
        final int startIndex = range.getStartIndex();
        assertEquals(9,startIndex);
    }

    @Test
    public void test_getEndIndex() {
        ByteRange range = new ByteRange(10, 30);
        final int endIndex = range.getEndIndex();
        assertEquals(39,endIndex);
    }

    @Test
    public void test_getLength() {
        ByteRange range = new ByteRange(11, 23);
        final int length = range.getLength();
        assertEquals(23, length);
    }

    @Test //offset less than zero
    public void test_InvalidOffset(){

        assertThrows(IllegalArgumentException.class, () -> new ByteRange(-1, 20));
    }

    @Test //length != MAXIMUM && length < 0, MAXIMUM = -2147483648
    public void test_InvalidLength() {

        assertThrows(IllegalArgumentException.class, () -> new ByteRange(2, -2));
    }

    @Test //offset == 0 && length == MAXIMUM
    public void test_InvalidFullRange() {

        assertThrows(IllegalArgumentException.class, () -> new ByteRange(0,-2147483648));
    }
}