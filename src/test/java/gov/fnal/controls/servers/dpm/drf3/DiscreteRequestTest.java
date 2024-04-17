package gov.fnal.controls.servers.dpm.drf3;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class DiscreteRequestTest {

    @Test
    public void testParseDiscrete() throws RequestFormatException {
        assertEquals("", ((DiscreteRequest) DiscreteRequest.parseDiscrete("Str")).modelStr);
        assertEquals("STR", ((DiscreteRequest) DiscreteRequest.parseDiscrete("Str")).getDevice());
        assertEquals("Str", ((DiscreteRequest) DiscreteRequest.parseDiscrete("Str")).device);
        assertNull(((DiscreteRequest) DiscreteRequest.parseDiscrete("Str")).text);
        assertFalse(((DiscreteRequest) DiscreteRequest.parseDiscrete("Str")).hasEvent());
        assertFalse(((DiscreteRequest) DiscreteRequest.parseDiscrete("Str")).hasRange());
        assertArrayEquals(new String[]{"", ""}, ((DiscreteRequest) DiscreteRequest.parseDiscrete("Str")).getFields());
        assertThrows(RequestFormatException.class, () -> DiscreteRequest.parseDiscrete(""));
    }


    @Test
    public void testHasRange() throws RequestFormatException {
        assertFalse((new DiscreteRequest("Str")).hasRange());
    }

    @Test
    public void testHasRange2() throws IllegalArgumentException {
        // Arrange
        FullRange range = new FullRange();

        // Act and Assert
        assertTrue((new DiscreteRequest("Device", "Property", range, "Field", DefaultEvent.parseDefault("U"))).hasRange());
    }


    @Test
    public void testHasEvent() throws RequestFormatException {
        // Arrange, Act and Assert
        assertFalse((new DiscreteRequest("Str")).hasEvent());
    }


    @Test
    public void testHasEvent2() throws IllegalArgumentException {
        // Arrange
        FullRange range = new FullRange();

        // Act and Assert
        assertTrue((new DiscreteRequest("Device", "Property", range, "Field", DefaultEvent.parseDefault("U"))).hasEvent());
    }

    @Test
    public void testNewDiscreteRequest() throws RequestFormatException {
        // Arrange and Act
        DiscreteRequest actualDiscreteRequest = new DiscreteRequest("Str");

        // Assert
        assertEquals("", actualDiscreteRequest.modelStr);
        assertEquals("STR", actualDiscreteRequest.getDevice());
        assertEquals("Str", actualDiscreteRequest.device);
        assertNull(actualDiscreteRequest.text);
        assertFalse(actualDiscreteRequest.hasEvent());
        assertFalse(actualDiscreteRequest.hasRange());
        assertArrayEquals(new String[]{"", ""}, actualDiscreteRequest.getFields());
    }


    @Test
    public void testNewDiscreteRequest2() throws RequestFormatException {
        // Arrange, Act and Assert
        assertThrows(RequestFormatException.class, () -> new DiscreteRequest(""));
    }


    @Test
    public void testNewDiscreteRequest3() throws RequestFormatException {
        // Arrange and Act
        DiscreteRequest actualDiscreteRequest = new DiscreteRequest("Str<-xxxxx.UUU[99].UUU@xxx");

        // Assert
        assertEquals("STR", actualDiscreteRequest.getDevice());
        assertEquals("Str", actualDiscreteRequest.device);
        assertEquals("xxxxx.UUU[99].UUU@xxx", actualDiscreteRequest.modelStr);
        assertNull(actualDiscreteRequest.text);
        assertFalse(actualDiscreteRequest.hasEvent());
        assertFalse(actualDiscreteRequest.hasRange());
        assertArrayEquals(new String[]{"", ""}, actualDiscreteRequest.getFields());
    }

    @Test
    public void testNewDiscreteRequest4() throws IllegalArgumentException {
        // Arrange
        FullRange range = new FullRange();

        // Act
        DiscreteRequest actualDiscreteRequest = new DiscreteRequest("Device", "Property", range, "Field",
                DefaultEvent.parseDefault("U"));

        // Assert
        assertEquals("", actualDiscreteRequest.modelStr);
        assertEquals("DEVICE", actualDiscreteRequest.getDevice());
        assertEquals("Device", actualDiscreteRequest.device);
        assertNull(actualDiscreteRequest.text);
        assertTrue(actualDiscreteRequest.hasEvent());
        assertTrue(actualDiscreteRequest.hasRange());
        assertArrayEquals(new String[]{null, null}, actualDiscreteRequest.getFields());
    }
}
