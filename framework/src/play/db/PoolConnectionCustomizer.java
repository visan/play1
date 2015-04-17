package play.db;

import com.mchange.v2.c3p0.ConnectionCustomizer;

import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by adg on 24.03.2015.
 */
public class PoolConnectionCustomizer {

  private static volatile PoolConnectionCustomizer instance;
  private static volatile Object lock = new Object();
  private static volatile Map<String, List<ConnectionCustomizer>> customizersMap = new ConcurrentHashMap<String, List<ConnectionCustomizer>>();


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


  void fireAcquire(Connection connection, String poolId) throws Exception {
    List<ConnectionCustomizer> customizerList = getCustomizers(poolId);
    Iterator<ConnectionCustomizer> customizerIterator = customizerList.iterator();
    while (customizerIterator.hasNext()) {
      ConnectionCustomizer connectionCustomizer = customizerIterator.next();
      connectionCustomizer.onAcquire(connection, poolId);
    }
  }


  void fireDestroy(Connection connection, String poolId) throws Exception {
    List<ConnectionCustomizer> customizerList = getCustomizers(poolId);
    Iterator<ConnectionCustomizer> customizerIterator = customizerList.iterator();
    while (customizerIterator.hasNext()) {
      ConnectionCustomizer connectionCustomizer = customizerIterator.next();
      connectionCustomizer.onDestroy(connection, poolId);
    }
  }


  void fireCheckOut(Connection connection, String poolId) throws Exception {
    List<ConnectionCustomizer> customizerList = getCustomizers(poolId);
    Iterator<ConnectionCustomizer> customizerIterator = customizerList.iterator();
    while (customizerIterator.hasNext()) {
      ConnectionCustomizer connectionCustomizer = customizerIterator.next();
      connectionCustomizer.onCheckOut(connection, poolId);
    }
  }


  void fireCheckIn(Connection connection, String poolId) throws Exception {
    List<ConnectionCustomizer> customizerList = getCustomizers(poolId);
    Iterator<ConnectionCustomizer> customizerIterator = customizerList.iterator();
    while (customizerIterator.hasNext()) {
      ConnectionCustomizer connectionCustomizer = customizerIterator.next();
      connectionCustomizer.onCheckIn(connection, poolId);
    }
  }

  public void addCustomizer(String poolId, ConnectionCustomizer connectionCustomizer) {
    List<ConnectionCustomizer> customizerList = getCustomizers(poolId);
    customizerList.add(connectionCustomizer);
  }

  private List<ConnectionCustomizer> getCustomizers(String poolId) {
    if("play".equals(poolId)) poolId = "db";//some phase the framework uses 'play' instead of 'db'. here we fixed it.
    List<ConnectionCustomizer> customizerList = customizersMap.get(poolId);
    if (customizerList == null) {
      synchronized (customizersMap) {
        if (customizerList == null) {
          customizersMap.put(poolId, new CopyOnWriteArrayList<ConnectionCustomizer>());
        }
      }
    }
    return customizersMap.get(poolId);
  }

  public static void main(String[] args) {
    PoolConnectionCustomizer customizer = PoolConnectionCustomizer.getInstance();
    customizer.addCustomizer("db", new ConnectionCustomizer() {
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
    });
  }

}
