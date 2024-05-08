package gov.fnal.controls.servers.dpm.events;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SavedDataEventTest {

    @Test
    public void test_isRepetitive(){
        int[] files = {10,20,30,40};
        int[] collections = {10,25,35,45,78};
        SavedDataEvent savedDataEvent = new SavedDataEvent(files,collections);
        Assertions.assertTrue(savedDataEvent.isRepetitive());
    }
}
