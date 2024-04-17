package gov.fnal.controls.servers.dpm.pools;

import gov.fnal.controls.db.CachedResultSet;
import gov.fnal.controls.db.DbServer;
import gov.fnal.controls.db.resultset.ResultSetData;
import gov.fnal.controls.db.resultset.SybaseResultSetData;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DeviceCacheTest {

    private static DbServer dbServer;
    private static  ResultSet resultSet;;


    @BeforeClass
    public static void init() throws SQLException {
        final String query = "SELECT di,pi,pv_name FROM accdb.foreign_device_mapping " + "WHERE di IN ";
        dbServer = mock(DbServer.class);

        //ResultSetData resultSetData = mock(SybaseResultSetData.class);
        resultSet = mock(ResultSet.class);
        CachedResultSet cachedResultSet = mock(CachedResultSet.class);
        when(dbServer.executeQuery(query)).thenReturn(cachedResultSet);
        resultSet = dbServer.executeQuery(query);

        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getInt("di")).thenReturn(12);
        when(resultSet.getInt("pi")).thenReturn(12);
        when(resultSet.getString("pv_name")).thenReturn("test name");

    }

    @Test
    public void test_foreignDevice() throws Exception {
        DBMaps dbMaps = new DBMaps(dbServer, "test");
        Map<DIPI, String> dipiString = Whitebox.invokeMethod(dbMaps, "family");
    }
}
