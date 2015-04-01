package play.db.jpa;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceException;

import org.slf4j.LoggerFactory;
import play.Logger;
import play.exceptions.JPAException;

/**
 * JPA Support
 */
public class JPAContext {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JPAContext.class.getName());

    private JPAConfig jpaConfig;
    private EntityManager entityManager;
    private boolean readonly = true;
  private long startTs;
  private int batchCount;

    protected JPAContext(JPAConfig jpaConfig, boolean readonly, boolean beginTransaction) {
        batchCount = 0;
        this.jpaConfig = jpaConfig;

        EntityManager manager = jpaConfig.newEntityManager();
        manager.setFlushMode(FlushModeType.COMMIT);
        manager.setProperty("org.hibernate.readOnly", readonly);

        if (beginTransaction) {
            manager.getTransaction().begin();
          startTs = System.currentTimeMillis();
            log.trace("tx[{}]: begined. ({})",jpaConfig.getConfigName(),readonly?"ro":"rw");
        }

        entityManager = manager;
        this.readonly = readonly;
    }

    public JPAConfig getJPAConfig() {
        return jpaConfig;
    }

    /**
     * clear current JPA context and transaction
     * @param rollback shall current transaction be committed (false) or cancelled (true)
     */
    public void closeTx(boolean rollback) {
      long duration=System.currentTimeMillis()-startTs;
        try {
            if (entityManager.getTransaction().isActive()) {
                if (readonly || rollback || entityManager.getTransaction().getRollbackOnly()) {
                    entityManager.getTransaction().rollback();
                    log.trace("tx[{}]: rollbacked.(Took {} ms.)",jpaConfig.getConfigName(),duration);
                } else {
                    try {
                        entityManager.getTransaction().commit();
                        log.trace("tx[{}]: commited.(Took {} ms.)",jpaConfig.getConfigName(),duration);
                    } catch (Throwable e) {
                        for (int i = 0; i < 10; i++) {
                            if (e instanceof PersistenceException && e.getCause() != null) {
                                e = e.getCause();
                                break;
                            }
                            e = e.getCause();
                            if (e == null) {
                                break;
                            }
                        }
                        throw new JPAException("Cannot commit.(Took "+duration+" ms.)", e);
                    }
                }
            }else {
                log.trace("tx[{}]: is NOT active. Nothing to commit.(Took {} ms.)",jpaConfig.getConfigName(),duration);
            }
        } finally {
            entityManager.close();
            //clear context
            jpaConfig.clearJPAContext();
        }

    }

    protected void close() {
        entityManager.close();;
    }

    /*
     * Retrieve the current entityManager
     */
    public EntityManager em() {
        return entityManager;
    }

    /*
     * Tell to JPA do not commit the current transaction
     */
    public void setRollbackOnly() {
        entityManager.getTransaction().setRollbackOnly();
    }


    /**
     * Execute a JPQL query
     */
    public int execute(String query) {
        return entityManager.createQuery(query).executeUpdate();
    }

    /**
     * @return true if current thread is running inside a transaction
     */
    public boolean isInsideTransaction() {
        return entityManager.getTransaction() != null;
    }

  public int getBatchCount() {
    return batchCount;
  }

  public void increaseBatch() {
      batchCount++;
    }
}
