import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.FhirPathExecutionException;
import ca.uhn.fhir.parser.IParser;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Observation;


public class FhirResourceFactory {

  private static final FhirContext ctx = FhirContext.forR4();
  private static final IParser parser = ctx.newJsonParser().setPrettyPrint(true);


  public static <T extends IBaseResource> T createTestResource(Class<T> resourceType,
      String resourceToModifyPath, HashMap<String, String> fhirPathToValueFunction)
      throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    var json = new String(Files.readAllBytes(Paths.get(resourceToModifyPath)));
    var resource = parser.parseResource(resourceType, json);
    modifyResource(ctx, resource, fhirPathToValueFunction);
    return resource;
  }

  public static void printResource(IBaseResource resource) {
    if (resource != null) {
      System.out.println(parser.encodeResourceToString(resource));
    } else {
      System.out.println("Resource is null");
    }
  }

  @SuppressWarnings("unchecked")
  public static <T extends IBaseResource> T createTestResourceFromBluePrint(
      HashMap<String, String> bluePrint) {
    var resourceName = BluePrintLoader.getResourceName(bluePrint);
    if (resourceName.equals("Observation")) {
      if (bluePrint.containsKey("Observation.value as Quantity")) {
        try {
          return (T) createTestResource(Observation.class,
              "src/main/resources/FhirProfileToModify/DefaultQuantityObservation.json", bluePrint);
        } catch (IOException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
          e.printStackTrace();
        }
      }
      else if(bluePrint.containsKey("(Observation.value as CodeableConcept).coding")) {
        try {
          return (T) createTestResource(Observation.class,
              "src/main/resources/FhirProfileToModify/DefaultCodeableConceptObservation.json", bluePrint);
        } catch (IOException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    } else if (resourceName.equals("Condition")) {
      try {
        return (T) createTestResource(Condition.class,
            "src/main/resources/FhirProfileToModify/DefaultCondition.json", bluePrint);
      } catch (IOException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  /**
   * Modify the resource by setting the value of the fhirPath to the value returned by the
   * valueFunction.
   *
   * @param ctx                     FhirContext
   * @param resource                resource to modify
   * @param fhirPathToValueFunction map of fhirPath to valueFunction
   * @param <T>                     type of resource
   */
  public static <T extends IBase> void modifyResource(FhirContext ctx, T resource,
      HashMap<String, String> fhirPathToValueFunction)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    for (var entry : fhirPathToValueFunction.entrySet()) {
      var fhirPath = entry.getKey();
      var valueFunction = entry.getValue();
      JavaFunctionParser.FunctionResult result = JavaFunctionParser.parse(valueFunction);
      if (result == null) {
        System.out.println("Could not parse " + valueFunction);
        continue;
      }
      var valueType = result.return_type();
      var value = result.result();
      try {
        var evalResult = ctx.newFhirPath().evaluateFirst(resource, fhirPath, valueType);
        evalResult.ifPresentOrElse(old_value -> FhirResourceFactory.updateObject(old_value, value),
            () -> {
              System.out.println("No value found for " + fhirPath);
//              throw new RuntimeException("No value found for " + fhirPath);
            });
      } catch (FhirPathExecutionException e) {
        if (fhirPath.endsWith(".status")) {
          handleStatus(resource, fhirPath, value);
        }
      }
    }
  }

  /**
   * Handle the status field of a resource.
   *
   * @param resource resource to modify
   * @param fhirPath fhirPath to the status field
   * @param value    value to set the status field to
   * @param <T>      type of resource
   */
  @SuppressWarnings("unchecked")
  public static <T extends IBase> void handleStatus(T resource, String fhirPath, Object value)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> statusClassFactory;
    Class<? extends Enumeration<?>> statusClass;
    try {
      var resourceName = resource.getClass().getName();
      var factoryClassName =
          resourceName.split("\\.")[resourceName.split("\\.").length - 1] + "StatusEnumFactory";
      var statusClassName = resourceName.split("\\.")[resourceName.split("\\.").length - 1] + "Status";
      statusClass = (Class<? extends Enumeration<?>>) Class.forName(resourceName + "$" + statusClassName);
      statusClassFactory = Class.forName(resourceName + "$" + factoryClassName);
      var factoryConstructor = statusClassFactory.getConstructor();
      var statusFactory = factoryConstructor.newInstance();
      var statusInstance = statusClassFactory.getMethod("fromCode", String.class)
          .invoke(statusFactory, value.toString());
      var evalResult = ctx.newFhirPath().evaluateFirst(resource, fhirPath, Enumeration.class);
      if (evalResult.isPresent()) {
        var method = resource.getClass().getMethod("setStatus", statusClass);
        method.invoke(resource, statusClass.cast(statusInstance));
      } else {
        System.out.println("No value found for " + fhirPath);
      }
    } catch (ClassNotFoundException | InstantiationException e) {
      e.printStackTrace();
    }
  }

  /**
   * Update the target object with the values from the source object.
   *
   * @param target target object
   * @param source source object
   * @param <T>    type of object
   */
  public static <T> void updateObject(T target, T source) {
    if (target == null || source == null || !target.getClass().equals(source.getClass())) {
      // Handle null input or different class types
      return;
    }

    try {
      var beanInfo = new SetPropertyNamingConventionBeanInfo(target.getClass());
      var propertyDescriptors = beanInfo.getPropertyDescriptors();

      for (var propertyDescriptor : propertyDescriptors) {
        var getter = propertyDescriptor.getReadMethod();
        var setter = propertyDescriptor.getWriteMethod();
        if (getter != null && setter != null) {
          Object value = getter.invoke(source);
          if (value != null) {
            setter.invoke(target, value);
          }
        }
      }
    } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
      // Handle exception
    }
  }
}