package play;

import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by aliev on 02/12/15
 */
public class InternalCache {
  private static final Map<String, List<Class>> assignalbeClassesMap = new ConcurrentHashMap<String, List<Class>>(1000);
  private static final Map<String, AnnotationResolution> fieldAnnotationMap = new ConcurrentHashMap<String, AnnotationResolution>(1000);
  private static final boolean isDebug = false;

  public static List<Class> getAssignableClasses(Class clazz) {
    return assignalbeClassesMap.get(clazz.getName());
  }

  public static void cacheAssignableClasses(Class clazz, List<Class> results) {
    synchronized (assignalbeClassesMap) {
      if (!assignalbeClassesMap.containsKey(clazz.getName())) {
        assignalbeClassesMap.put(clazz.getName(), results);
      }
    }
  }

  static class AnnotationResolution {
    private final boolean resolverd;
    private final Annotation annotation;

    public AnnotationResolution(boolean resolverd, Annotation annotation) {
      this.resolverd = resolverd;
      this.annotation = annotation;
    }
  }

  public static Annotation getAnnotation(Class annotationClass, Field field) {
    return getAnnotationResolution(annotationClass, field).annotation;
  }

  private static AnnotationResolution getAnnotationResolution(Class annotationClass, Field field) {
    String key = field.getDeclaringClass().getName() + "_" + field.getName() + "_" + annotationClass.getName();
    if (isDebug) System.out.println("AnnotationResolution: key:" + key);
    AnnotationResolution annotationResolution = fieldAnnotationMap.get(key);
    if (annotationResolution == null) {
      synchronized (fieldAnnotationMap) {
        annotationResolution = fieldAnnotationMap.get(key);
        if (annotationResolution == null) {
          if (isDebug) System.out.println("AnnotationResolution: mis cache. key:" + key);
          Annotation annotation = field.getAnnotation(annotationClass);
          annotationResolution = new AnnotationResolution(annotation != null, annotation);
          fieldAnnotationMap.put(key, annotationResolution);
        }
      }
    } else {
      if (isDebug) System.out.println("AnnotationResolution: hit cache. key:" + key);
    }
    return annotationResolution;
  }

  public static boolean isAnnotationPresent(Class annotationClass, Field field) {
    AnnotationResolution annotationResolution = getAnnotationResolution(annotationClass, field);
    return annotationResolution.resolverd;
  }

  static class Card {
    @Id
    @Column(name = "CARD_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "IDF_CARD_SEQ")
    @SequenceGenerator(name = "IDF_CARD_SEQ", sequenceName = "IDF_CARD_SEQ", allocationSize = 100, initialValue = 10000)
    private Long id;

    @Column(name = "RBS_ID", nullable = false, unique = true, length = 15)
    private Long rbsId;

    public Long getId() {
      return id;
    }


    public Object _key() {
      return getId();
    }

    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId; // replace on product

    @Column(name = "UC_TYPE", nullable = false, length = 1)
    private String ucType;

    @Column(name = "SV_CARD_STATUS", nullable = false, length = 10)
    private String svCardStatus;

    @Column(name = "SV_CARD_TYPE", nullable = false, length = 10)
    private String svCardType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "RELEASED")
    private Date svReleased;

    @ManyToOne
    @JoinColumn(name = "CONSUMER_ID", nullable = false)
    @ForeignKey(name = "CARD_CONSUMER_ID_FK")
    private Consumer consumer;


    @ManyToOne
    @JoinColumn(name = "CONTRACT_ID", nullable = false)
    @ForeignKey(name = "CARD_CONTRACT_ID_FK")
    private Contract contract;
  }

  static class Consumer {

  }

  static class Contract {

  }

  public static void main(String[] args) {
    Set<Field> fields = new HashSet<Field>();
    Class clazz = Card.class;
    while (!clazz.equals(Object.class)) {
      Collections.addAll(fields, clazz.getDeclaredFields());
      clazz = clazz.getSuperclass();
    }
    for (Field field : fields) {
      field.setAccessible(true);
      if (Modifier.isTransient(field.getModifiers())) {
        continue;
      }
      if (InternalCache.isAnnotationPresent(ManyToOne.class, field)) {
        CascadeType[] cascadeTypes = ((ManyToOne) InternalCache.getAnnotation(ManyToOne.class, field)).cascade();
        System.out.println(cascadeTypes);
      } else {
        System.out.println("Annotation NOT present.");
      }
    }
  }
}
