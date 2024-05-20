package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OneShotDaqPoolTest {

    @Test
    public void test_insert(){
        Node node = Mockito.mock(Node.class);

        ReceiveData receiveData = Mockito.mock(ReceiveData.class);
        WhatDaq whatDaq1 = Mockito.mock(WhatDaq.class);
        WhatDaq whatDaq = new WhatDaq(whatDaq1, 487, 10, 457, receiveData);
        List<WhatDaq> whatDaqCollections = new ArrayList<>();
        whatDaqCollections.add(whatDaq);
        OneShotDaqPool pool = new OneShotDaqPool(node);
        pool.insert(whatDaqCollections);
    }

    @Test
    public void test_process(){
        Node node = Mockito.mock(Node.class);
        OneShotDaqPool pool = new OneShotDaqPool(node);
       Assertions.assertFalse(pool.process(true));
    }
}
