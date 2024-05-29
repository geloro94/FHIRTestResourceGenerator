import java.util.Map;

import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.FhirPathExecutionException;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Observation;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import org.hl7.fhir.instance.model.api.IBase;

/**
 * ExtractTest
 */
public class ExtractTest {

    @Test
    public void testExtractPaths() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        System.out.println("Extracting paths from Observation");
        final Map<String, Class<? extends IBase>> paths = new HashMap<>() {{
            put("Observation.meta", Meta.class);
            put("Observation.value as Quantity", Quantity.class);
            put("Observation.code.coding", Coding.class);
        }};

        FhirContext ctx = FhirContext.forR4();


        var targetObservation = FhirResourceFactory.createTestResource(Observation.class,
                "src/main/resources/FhirProfileToModify/DefaultQuantityObservation.json");



        String jsonObservation = """
            {
              "resourceType": "Observation",
              "meta":
                {
                    "profile": [
                    "http://hl7.org/fhir/StructureDefinition/vitalsigns"
                    ]
                },
              "id": "example",
              "status": "final",
              "category": [
                {
                  "coding": [
                    {
                      "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                      "code": "vital-signs",
                      "display": "Vital Signs"
                    }
                  ]
                }
              ],
              "code": {
                "coding": [
                  {
                    "system": "http://loinc.org",
                    "code": "85354-9",
                    "display": "Blood pressure panel with all children optional"
                  }
                ],
                "text": "Blood pressure systolic & diastolic"
              },
              "subject": {
                "reference": "Patient/example"
              },
              "effectiveDateTime": "2020-09-15T10:00:00+00:00",
              "valueQuantity": {
                "value": 120,
                "unit": "mmHg",
                "system": "http://unitsofmeasure.org",
                "code": "mm[Hg]"
              },
              "interpretation": [
                {
                  "coding": [
                    {
                      "system": "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation",
                      "code": "N",
                      "display": "Normal"
                    }
                  ]
                }
              ],
              "note": [
                {
                  "text": "This is a normal blood pressure reading."
                }
              ]
            }
            """;

        var sourceObservation = ctx.newJsonParser().parseResource(Observation.class, jsonObservation);



        for (Map.Entry<String, Class<? extends IBase>> entry : paths.entrySet()) {
            String fhirPath = entry.getKey();
            var valueType = entry.getValue();

            try {
                var targetEvalResult = ctx.newFhirPath().evaluateFirst(targetObservation, fhirPath, valueType);
                var sourceEvalResult = ctx.newFhirPath().evaluateFirst(sourceObservation, fhirPath, valueType);


                var finalValue = sourceEvalResult.orElseThrow();
                

                targetEvalResult.ifPresentOrElse(old_value -> FhirResourceFactory.updateObject(old_value,
                        finalValue), () -> {
                    System.out.println("No value found in target for " + fhirPath);
                });
            } catch (FhirPathExecutionException e) {
                System.out.println("Error evaluating FHIRPath: " + fhirPath);
            }
        }

        System.out.println("Updated Observation:");

        System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(targetObservation));
                
    }               
}
