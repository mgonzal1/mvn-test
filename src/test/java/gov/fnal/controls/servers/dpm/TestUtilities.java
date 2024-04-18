package gov.fnal.controls.servers.dpm;

import java.nio.ByteBuffer;

public class TestUtilities {

    public static ByteBuffer getByteBuffer(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(4578);
        byteBuffer.putInt(542323);
        return byteBuffer;
    }
}
