import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class BluePrintLoader {

  /**
   * Loads a single blueprint from a json file.
   *
   * @param jsonPath Path to the json file.
   * @return A map of fhirPath to value function. I.e. {"Observation.code.coding":
   * "fixedCoding(...)"}
   */
  public static HashMap<String, String> loadFhirPathToValueFunctionFromJson(Path jsonPath)
      throws IOException {
    String json = Files.readString(jsonPath);
    JSONObject jsonObject = new JSONObject(json);

    return toPathFunctionMap(jsonObject);
  }

  /**
   * Loads a list of blueprints from a json file.
   *
   * @param filePath Path to the json file.
   * @return A list of maps of fhirPath to value function. I.e. {"Observation.code.coding":
   * "randomCoding(...)"}
   */
  public static List<HashMap<String, String>> loadBluePrints(String filePath) {
    Stream<JSONObject> bluePrints = Stream.empty();
    try {
      bluePrints = loadJsonArrayFromFile(filePath);
    } catch (JSONException | FileNotFoundException e) {
      e.printStackTrace();
    }

    return bluePrints.map(BluePrintLoader::toPathFunctionMap).toList();

  }

  @NotNull
  private static HashMap<String, String> toPathFunctionMap(JSONObject jsonObject) {
    return jsonObject.toMap().entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString(), (v1, v2) -> v1,
            HashMap::new));
  }


  private static Stream<JSONObject> loadJsonArrayFromFile(String filePath)
      throws FileNotFoundException, JSONException {
    var file = new File(filePath);
    var reader = new FileReader(file);
    var jsonArray = new JSONArray(new JSONTokener(reader));
    return jsonArrayToJsonObjectStream(jsonArray);
  }

  private static Stream<JSONObject> jsonArrayToJsonObjectStream(JSONArray jsonArray) {
    return IntStream.range(0, jsonArray.length()).mapToObj(jsonArray::getJSONObject);
  }


  public static String getResourceName(HashMap<String, String> bluePrint) {
    var path = (String) bluePrint.keySet().toArray()[0];
    return path.split("\\.")[0];
  }
}
