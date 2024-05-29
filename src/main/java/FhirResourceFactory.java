import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.FhirPathExecutionException;
import ca.uhn.fhir.parser.IParser;
import java.beans.IntrospectionException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.UrlType;


public class FhirResourceFactory {

  private static final FhirContext ctx = FhirContext.forR4();
  private static final IParser parser = ctx.newJsonParser().setPrettyPrint(true);

  public static <T extends IBaseResource> IBaseResource createTestResource(
    Class<T> resourceType,
    String resourceToModifyPath)
    throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
      var json = new String(Files.readAllBytes(Paths.get(resourceToModifyPath)));
      var resource = parser.parseResource(resourceType, json);
      return resource;
    }


  

  public static <T extends IBaseResource> List<IBaseResource> createModifiedTestResource(
      Class<T> resourceType,
      String resourceToModifyPath, HashMap<String, String> fhirPathToValueFunction)
      throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    var json = new String(Files.readAllBytes(Paths.get(resourceToModifyPath)));
    var resource = parser.parseResource(resourceType, json);
    return modifyResource(ctx, resource, fhirPathToValueFunction);
  }

  public static void printResource(IBaseResource resource) {
    if (resource != null) {
      System.out.println(parser.encodeResourceToString(resource));
    } else {
      System.out.println("Resource is null");
    }
  }

  public static List<IBaseResource> createTestResourceFromBluePrint(
      HashMap<String, String> bluePrint) {
    String resourceName = BluePrintLoader.getResourceName(bluePrint);
    try {
      switch (resourceName) {
        case "Condition":
          return createModifiedTestResource(Condition.class,
              "src/main/resources/FhirProfileToModify/DefaultCondition.json", bluePrint);
        case "Consent":
          return createModifiedTestResource(Consent.class,
              "src/main/resources/FhirProfileToModify/DefaultConsent.json", bluePrint);
        case "Observation":
          if (bluePrint.containsKey("Observation.value as Quantity")) {
            return createModifiedTestResource(Observation.class,
                "src/main/resources/FhirProfileToModify/DefaultQuantityObservation.json",
                bluePrint);
          } else if (bluePrint.containsKey("(Observation.value as CodeableConcept).coding")) {
            return createModifiedTestResource(Observation.class,
                "src/main/resources/FhirProfileToModify/DefaultCodeableConceptObservation.json",
                bluePrint);
          }
          break;
        case "Procedure":
          return createModifiedTestResource(Procedure.class,
              "src/main/resources/FhirProfileToModify/DefaultProcedure.json", bluePrint);
        case "Specimen":
          return createModifiedTestResource(Specimen.class,
              "src/main/resources/FhirProfileToModify/DefaultSpecimen.json", bluePrint);
        case "Patient":
          return createModifiedTestResource(org.hl7.fhir.r4.model.Patient.class,
              "src/main/resources/FhirProfileToModify/DefaultPatient.json", bluePrint);
        case "MedicationAdministration":
          return createModifiedTestResource(MedicationAdministration.class,
              "src/main/resources/FhirProfileToModify/DefaultMedicationAdministration.json",
              bluePrint);

      }
    } catch (IOException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
      e.printStackTrace();
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
  public static <T extends IBase> List<IBaseResource> modifyResource(FhirContext ctx, T resource,
      HashMap<String, String> fhirPathToValueFunction)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    List<IBaseResource> resultingResources = new ArrayList<>();
    for (var entry : fhirPathToValueFunction.entrySet()) {
      var fhirPath = entry.getKey();
      var valueFunction = entry.getValue();
      JavaFunctionParser.FunctionResult result = JavaFunctionParser.parse(valueFunction);
      if (result == null) {
        throw new IllegalArgumentException("Could not parse " + valueFunction);
      }
      var valueType = result.return_type();
      var value = result.result();

      if (valueType == ResourceReferenceContainer.class) {
        var extractedResources = extractResource(value).orElse(Collections.emptyList());
        resultingResources.addAll(extractedResources);
        value = ((ResourceReferenceContainer) value).reference();
        valueType = Reference.class;
      }

      try {
        var evalResult = ctx.newFhirPath().evaluateFirst(resource, fhirPath, valueType);
        Object finalValue = value;
        evalResult.ifPresentOrElse(old_value -> FhirResourceFactory.updateObject(old_value,
                finalValue),
            () -> {
              System.out.println("No value found for " + fhirPath);
//              throw new RuntimeException("No value found for " + fhirPath);
            });
      } catch (FhirPathExecutionException e) {
        if (fhirPath.endsWith(".status")) {
          handleStatus(resource, fhirPath, value);
        } else {
          e.printStackTrace();
        }
      }
    }
    resultingResources.add((IBaseResource) resource);
    return resultingResources;
  }

  private static Optional<List<IBaseResource>> extractResource(Object value) {
    if (value instanceof ResourceReferenceContainer) {
      return Optional.ofNullable(((ResourceReferenceContainer) value).resources());
    }
    return Optional.empty();
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
      var statusName =
          resourceName.split("\\.")[resourceName.split("\\.").length - 1].equals("Consent")
              ? "State" : "Status";
      var factoryClassName =
          resourceName.split("\\.")[resourceName.split("\\.").length - 1] + statusName
              + "EnumFactory";
      var statusClassName =
          resourceName.split("\\.")[resourceName.split("\\.").length - 1] + statusName;
      statusClass = (Class<? extends Enumeration<?>>) Class.forName(
          resourceName + "$" + statusClassName);
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
      System.out.println("Invalid arguments");
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

  public static <T extends IBaseResource> void writeResource(T resource, String filename) {
    try (FileWriter writer = new FileWriter(filename, false)) {
      String encoded = parser.setPrettyPrint(true).encodeResourceToString(resource);
      writer.write(encoded);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void writeNDJson(List<IBaseResource> resources, String filename) {
    try (FileWriter writer = new FileWriter(filename, false)) {
      for (IBaseResource resource : resources) {
        Resource r = (Resource) resource;
        parser.setPrettyPrint(false);
        parser.encodeResourceToWriter(r, writer);
        writer.append("\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Parameters createParameters(HashMap<String, String> fileNameByType) {
    List<ParametersParameterComponent> parameterList = new ArrayList<>();
    for (String resourceType : fileNameByType.keySet()) {
      String typeFilename = String.format("%s", fileNameByType.get(resourceType));
      String fileUrl = String.format("file:///%s", typeFilename);
      ParametersParameterComponent parameter = new ParametersParameterComponent();
      parameter.setName("source");
      parameter.addPart().setName("resourceType").setValue(new CodeType(resourceType));
      parameter.addPart().setName("url").setValue(new UrlType(fileUrl));
      parameterList.add(parameter);
    }
    Parameters parameters = new Parameters();
    parameters.setParameter(parameterList);

    return parameters;
  }

  public static Parameters writeNDJsonByResourceType(List<IBaseResource> resources,
      String filename) {
    Map<String, List<IBaseResource>> resourcesByType = new HashMap<>();
    for (IBaseResource resource : resources) {
      String resourceType = resource.fhirType();
      List<IBaseResource> typeResources = resourcesByType.getOrDefault(resourceType,
          new ArrayList<>());
      typeResources.add(resource);
      resourcesByType.put(resourceType, typeResources);
    }
    var fileNameByType = new HashMap<String, String>();
    for (String resourceType : resourcesByType.keySet()) {
      String typeFilename = String.format("%s-%s.ndjson", filename, resourceType);
      fileNameByType.put(resourceType, typeFilename);
      List<IBaseResource> typeResources = resourcesByType.get(resourceType);
      writeNDJson(typeResources, typeFilename);
    }
    return createParameters(fileNameByType);
  }
}