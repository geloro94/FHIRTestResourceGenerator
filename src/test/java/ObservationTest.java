import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.Test;

public class ObservationTest {


  @Test
  public void testSetFHIRPathToFixedCoding() throws IOException {
    JavaFunctionParser.FunctionResult result = JavaFunctionParser.parse(
        "fixedCoding('http://hl7.org/fhir/ValueSet/iso3166-1-2', "
            + "'US', 'United States of America', '2002')");
    assert result != null;
    Coding coding = (Coding) result.return_type().cast(result.result());
    var ctx = FhirContext.forR4();
    var parser = ctx.newJsonParser();
    var json = new String(
        Files.readAllBytes(
            Paths.get("src/main/resources/FhirProfileToModify/DefaultQuantityObservation.json")));
    var observation = parser.parseResource(Observation.class, json);
    var fhirPath = "Observation.category.coding";
    var valueType = result.return_type();
    var evalResult = ctx.newFhirPath().evaluateFirst(observation, fhirPath, valueType);
    if (evalResult.isPresent()) {
      var old_coding = evalResult.get();
      FhirResourceFactory.updateObject(old_coding, coding);
      assertTrue(observation.getCategoryFirstRep().getCodingFirstRep().equalsDeep(coding));
    }
  }


  @Test
  void testGenerateObservation() throws Exception {
    var ctx = FhirContext.forR4();
    var parser = ctx.newJsonParser();
    var json = new String(
        Files.readAllBytes(
            Paths.get("src/main/resources/FhirProfileToModify/DefaultQuantityObservation.json")));
    var defaultObservation = parser.parseResource(Observation.class, json);

    var fhirPathToValueFunction = BluePrintLoader.loadFhirPathToValueFunctionFromJson(
        Path.of("src/main/resources/TestDataBluePrint/AldostroneObservationBluePrint.json"));

    var observation = FhirResourceFactory.createTestResource(Observation.class,
        "src/main/resources/FhirProfileToModify/DefaultQuantityObservation.json",
        fhirPathToValueFunction);
    assertFalse(defaultObservation.equalsDeep(observation));
  }

  /**
   * We aim to generate 10,000 observations in less than 20 seconds.
   *
   * @throws Exception if an error occurs Passes if the generation takes less than 20 seconds.
   */
  @Test
  void testGenerateObservationPerformance() throws Exception {
    int numberToCreate = 10000;
    var ctx = FhirContext.forR4();
    var parser = ctx.newJsonParser();
    var json = new String(
        Files.readAllBytes(
            Paths.get("src/main/resources/FhirProfileToModify/DefaultQuantityObservation.json")));
    var defaultObservation = parser.parseResource(Observation.class, json);

    var fhirPathToValueFunction = BluePrintLoader.loadFhirPathToValueFunctionFromJson(
        Path.of("src/main/resources/TestDataBluePrint/AldostroneObservationBluePrint.json"));

    var startTime = System.currentTimeMillis();
    var observations = java.util.stream.IntStream.range(0, numberToCreate)
        .parallel()
        .mapToObj(i -> {
          Observation observation = defaultObservation.copy();
          try {
            FhirResourceFactory.modifyResource(ctx, observation, fhirPathToValueFunction);
          } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
          }
          return observation;
        })
        .toList();
    var endTime = System.currentTimeMillis();
    var elapsedTime = endTime - startTime;

    assertTrue(elapsedTime < 20000); // Ensure generation took less than 1 second
    assertFalse(observations.get(numberToCreate - 1).equalsDeep(observations.get(0)));
  }


  @Test
  void testGenerateConditionPerformance() throws Exception {
    int numberToCreate = 10000;
    var ctx = FhirContext.forR4();
    var parser = ctx.newJsonParser();
    var json = new String(
        Files.readAllBytes(
            Paths.get("src/main/resources/FhirProfileToModify/DefaultCondition.json")));
    var defaultCondition = parser.parseResource(Condition.class, json);

    var fhirPathToValueFunction = BluePrintLoader.loadFhirPathToValueFunctionFromJson(
        Path.of("src/main/resources/TestDataBluePrint/ConditionBluePrint.json"));

    var startTime = System.currentTimeMillis();
    var conditions = java.util.stream.IntStream.range(0, numberToCreate)
        .parallel()
        .mapToObj(i -> {
          var condition = defaultCondition.copy();
          try {
            FhirResourceFactory.modifyResource(ctx, condition, fhirPathToValueFunction);
          } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
          }
          return condition;
        })
        .toList();
    var endTime = System.currentTimeMillis();
    var elapsedTime = endTime - startTime;
    assertTrue(elapsedTime < 20000); // Ensure generation took less than 1 second
    assertFalse(conditions.get(numberToCreate - 1).equalsDeep(conditions.get(0)));
    parser.setPrettyPrint(true);
    System.out.println(parser.encodeResourceToString(conditions.get(0)));
  }


}
