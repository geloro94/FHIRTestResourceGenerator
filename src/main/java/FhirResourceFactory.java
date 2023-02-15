import ca.uhn.fhir.context.FhirContext;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import org.hl7.fhir.instance.model.api.IBase;
import org.json.JSONObject;

public class FhirResourceFactory {

  public static HashMap<String, String> loadFhirPathToValueFunctionFromJson(Path jsonPath) throws IOException {
    String json = Files.readString(jsonPath);
    JSONObject jsonObject = new JSONObject(json);

    HashMap<String, String> fhirPathToValueFunction = new HashMap<>();
    for (String key : jsonObject.keySet()) {
      fhirPathToValueFunction.put(key, jsonObject.getString(key));
    }

    return fhirPathToValueFunction;
  }


  /**
   * Modify the resource by setting the value of the fhirPath to the value returned by the
   * valueFunction.
   * @param ctx FhirContext
   * @param resource resource to modify
   * @param fhirPathToValueFunction map of fhirPath to valueFunction
   * @param <T> type of resource
   */
  public static <T extends IBase> void modifyResource(FhirContext ctx, T resource,
      HashMap<String, String> fhirPathToValueFunction) {
    for (var entry : fhirPathToValueFunction.entrySet()) {
      var fhirPath = entry.getKey();
      var valueFunction = entry.getValue();
      JavaFunctionParser.FunctionResult result = JavaFunctionParser.parse(valueFunction);
      assert result != null;
      var valueType = result.return_type();
      var value = result.result();
      var evalResult = ctx.newFhirPath().evaluateFirst(resource, fhirPath, valueType);
      evalResult.ifPresentOrElse(
          old_value -> FhirResourceFactory.updateObject(old_value, value),
          () -> {
            throw new RuntimeException("No value found for " + fhirPath);
          }
      );
    }
  }



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