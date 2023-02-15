import java.lang.reflect.Method;
import org.hl7.fhir.instance.model.api.IBase;

public class JavaFunctionParser {

  /**
   * A class to hold the result and the return type of java method call.
   */
  public record FunctionResult(Object result, Class<? extends IBase> return_type) {

    public static FunctionResult of(Object result, Class<? extends IBase> return_type) {
      return new FunctionResult(result, return_type);
    }
  }


  /**
   * Parses a java method call string and returns the result. Supports nested method calls. and
   * method calls with parameters.
   *
   * @param input - the method call string
   * @return - the result of the method call
   */
  public static FunctionResult parse(String input) {
    int startParen = input.indexOf('(');
    int endParen = input.lastIndexOf(')');
    String functionName = input.substring(0, startParen);

    try {
      Method method = getMethod(GeneratorFunctions.class, functionName);
      var return_type = method.getReturnType();
      method.setAccessible(true);

      String[] paramsStr = {};
      if (endParen > startParen + 1) {
        paramsStr = input.substring(startParen + 1, endParen).split(",");
      }
      Object[] params = new Object[paramsStr.length];

      for (int i = 0; i < paramsStr.length; i++) {
        if (paramsStr[i].contains("(") && paramsStr[i].contains(")")) {
          params[i] = parse(paramsStr[i]);
        } else {
          params[i] = paramsStr[i].trim();
        }
        assert params[i] != null;
        if (params[i].equals("None")) {
          params[i] = null;
        }
      }
      Object result;

      if (params.length == 0) {
        result = method.invoke(null);
      } else {
        result = method.invoke(null, params);
      }
      return FunctionResult.of(result, (Class<? extends IBase>) return_type);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Gets the method with the given name from the GeneratorFunctions class.
   *
   * @param functionName - the name of the method
   * @return - the method
   * @throws NoSuchMethodException - if no method with the given name is found
   */
  public static <T> Method getMethod(Class<T> clazz, String functionName)
      throws NoSuchMethodException {
    Method method = null;
    for (Method m : clazz.getDeclaredMethods()) {
      if (functionName.equals(m.getName())) {
        method = m;
      }
    }
    if (method == null) {
      throw new NoSuchMethodException("No method found with name: " + functionName);
    }
    return method;
  }
}
