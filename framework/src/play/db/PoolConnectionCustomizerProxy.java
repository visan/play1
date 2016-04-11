package play.db;

import com.mchange.v2.c3p0.ConnectionCustomizer;

import java.sql.Connection;

/**
* Created by aliev on 14/04/15
*/
public class PoolConnectionCustomizerProxy implements ConnectionCustomizer {
    @Override
    public void onAcquire(Connection connection, String s) throws Exception {
        PoolConnectionCustomizer.getInstance().fireAcquire(connection, s);
    }

    @Override
    public void onDestroy(Connection connection, String s) throws Exception {
        PoolConnectionCustomizer.getInstance().fireDestroy(connection, s);
    }

    @Override
    public void onCheckOut(Connection connection, String s) throws Exception {
        PoolConnectionCustomizer.getInstance().fireCheckOut(connection, s);
    }

    @Override
    public void onCheckIn(Connection connection, String s) throws Exception {
        PoolConnectionCustomizer.getInstance().fireCheckIn(connection, s);
    }
}
