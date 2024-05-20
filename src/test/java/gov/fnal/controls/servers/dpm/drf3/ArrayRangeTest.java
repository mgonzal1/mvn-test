package gov.fnal.controls.servers.dpm.drf3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ArrayRangeTest {

    @Test
    public void tes_getType() {
        ArrayRange range = new ArrayRange(12, 32758);
        final RangeType type = range.getType();
        assertEquals("ARRAY", type.name());
    }
    @Test
    public void tes_getStartIndex() {
        ArrayRange range = new ArrayRange(12, 32758);
        final int startIndex = range.getStartIndex();
        assertEquals(12, startIndex);
    }
    @Test
    public void tes_getEndIndex() {
        ArrayRange range = new ArrayRange(12, 32758);
        final int endIndex = range.getEndIndex();
        assertEquals(32758, endIndex);
    }
    @Test
    public void tes_getLength() {
        ArrayRange range = new ArrayRange(12, 32758);
        final int length = range.getLength();
        assertEquals(32747, length);
    }

    @Test //startIndex < 0 || startIndex > MAX_INDEX (32767)
    public void test_InvalidStartIndex(){
        assertThrows(IllegalArgumentException.class, () -> new ArrayRange(-1, 32768));
    }

    @Test //endIndex != MAXIMUM && (endIndex < startIndex || endIndex > MAX_INDEX
    public void test_InvalidEndIndex() {
        assertThrows(IllegalArgumentException.class, () -> new ArrayRange(30, 23));
    }

    @Test //startIndex == 0 && endIndex == MAXIMUM
    public void test_InvalidFullRange() {
        assertThrows(IllegalArgumentException.class, () -> new ArrayRange(0,-2147483648));
    }
}
