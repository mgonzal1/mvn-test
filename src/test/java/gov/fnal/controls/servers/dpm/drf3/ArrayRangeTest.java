package gov.fnal.controls.servers.dpm.drf3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
