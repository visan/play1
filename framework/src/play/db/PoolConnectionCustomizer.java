package play.db;

import com.mchange.v2.c3p0.ConnectionCustomizer;

import java.sql.Connection;

/**
 * Created by adg on 24.03.2015.
 */
public class PoolConnectionCustomizer implements ConnectionCustomizer {

    public static PoolConnectionCustomizer getInstance() {
        return null;
    }

    @Override
    public void onAcquire(Connection connection, String s) throws Exception {

    }

    @Override
    public void onDestroy(Connection connection, String s) throws Exception {

    }

    @Override
    public void onCheckOut(Connection connection, String s) throws Exception {

    }

    @Override
    public void onCheckIn(Connection connection, String s) throws Exception {

    }
}
