import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 * This class is used to set the setter method for a property descriptor when the setter method is
 * not named according to the Java Bean convention but uses the setProperty() naming convention. As
 * used in the Hapi FHIR Java API.
 */
public class SetPropertyNamingConventionBeanInfo implements BeanInfo {

  private final BeanInfo delegate;

  public SetPropertyNamingConventionBeanInfo(Class<?> beanClass) throws IntrospectionException {
    delegate = Introspector.getBeanInfo(beanClass);
  }

  @Override
  public BeanDescriptor getBeanDescriptor() {
    return null;
  }

  @Override
  public EventSetDescriptor[] getEventSetDescriptors() {
    return new EventSetDescriptor[0];
  }

  @Override
  public int getDefaultEventIndex() {
    return 0;
  }

  @Override
  public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor[] propertyDescriptors = delegate.getPropertyDescriptors();
    for (PropertyDescriptor pd : propertyDescriptors) {
      Method getter = pd.getReadMethod();
      Method setter = pd.getWriteMethod();
      if (getter != null && setter == null) {
        String propertyName = pd.getName();
        String setterName =
            "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        try {
          Method setterMethod = getter.getDeclaringClass()
              .getMethod(setterName, getter.getReturnType());
          pd.setWriteMethod(setterMethod);
        } catch (NoSuchMethodException | SecurityException ex) {
          // Ignore non-standard setter methods
        } catch (IntrospectionException e) {
          e.printStackTrace();
        }
      }
    }
    return propertyDescriptors;
  }

  @Override
  public int getDefaultPropertyIndex() {
    return delegate.getDefaultPropertyIndex();
  }

  @Override
  public MethodDescriptor[] getMethodDescriptors() {
    return delegate.getMethodDescriptors();
  }

  @Override
  public BeanInfo[] getAdditionalBeanInfo() {
    return delegate.getAdditionalBeanInfo();
  }

  @Override
  public Image getIcon(int iconKind) {
    return delegate.getIcon(iconKind);
  }
}