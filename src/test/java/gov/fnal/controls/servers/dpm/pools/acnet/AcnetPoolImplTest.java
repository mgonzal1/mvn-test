package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.SettingData;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AcnetPoolImplTest {

    @Test
    public void test_addRequest() {
        AcnetPoolImpl acnetPool = new AcnetPoolImpl();
        WhatDaq whatDaq = mock(WhatDaq.class);
        acnetPool.addRequest(whatDaq);
        Assertions.assertEquals(1, acnetPool.requests().size());
    }

    @Test
    public void test_addSettings() throws AcnetStatusException {
        AcnetPoolImpl acnetPool = new AcnetPoolImpl();
        WhatDaq whatDaq = mock(WhatDaq.class);
        SettingData setting = mock(SettingData.class);
        acnetPool.addSetting(whatDaq, setting);
    }

    @Test
    public void test_handle() throws AcnetStatusException {
        AcnetPoolImpl acnetPool = new AcnetPoolImpl();
        gov.fnal.controls.servers.dpm.pools.ReceiveData receiveData = new ReceiveDataStub();
        WhatDaq whatDaq = new WhatDaq(mock(WhatDaq.class), 5, 10, 10, receiveData);
        byte[] setting = {65, 66, 67, 68, 69};
        acnetPool.handle(whatDaq, setting);
    }
    private class ReceiveDataStub implements ReceiveData {
        @Override
        public void receiveStatus(int status) {
            ReceiveData.super.receiveStatus(status);
        }

        @Override
        public void receiveData(ByteBuffer data, long timestamp, long cycle) {

        }
    }
}
