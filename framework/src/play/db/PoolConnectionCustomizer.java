package play.db;

import com.mchange.v2.c3p0.ConnectionCustomizer;

import java.sql.Connection;

/**
 * Created by adg on 24.03.2015.
 */
public class PoolConnectionCustomizer implements ConnectionCustomizer {

  private static volatile PoolConnectionCustomizer instance;
  private static volatile Object lock=new Object();

  public static PoolConnectionCustomizer getInstance() {
    if (instance == null) {
      synchronized (lock) {
        if (instance == null) {
          instance = new PoolConnectionCustomizer();
        }
      }
    }
    return instance;
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
