# FHIR Resource Generator

This program allows you to generate a bulk of FHIR resources in JSON format with randomly generated
data. At its core, the program takes a JSON representation of a FHIR resource and applies a map of
FHIRPath expressions to generator functions to obtain the value for the value path and sets the
value in the resource whose initial value is obtained from the JSON representation.

## Prerequisites

- Java 17
- Maven
- Access to a FHIR Terminology Server that supports the $expand Operation for ValueSets and has all
  required ValueSets

## Usage

Load a FHIR Resource using Hapis IParser. Load a TestDataBluePrint using the
FhirResourceFactory.loadFhirPathToValueFunctionFromJson function. Use the
FhirResourceFactory.modifyResource function to modify the loaded resource based on the Blueprint
FHIR Defintions. GeneratorFunctions provides the defintion of available functions to create the
value defined by the fhir path key in the BluePrint. If you have many different profiles i.e.
because you have a separate profile for each LaboratoryValue with its own specific ucum code. You
want to generate the TestDataBluePrint based on the Profile Information or other sources. While the
values might change for each profile the required FHIR Resource you want to modify want need to be
changed for each case as long as all but also only the fhir path you want to have in your final
resource are present. A good starting point for creating a resource you want to modify is ask
Chatgpt to generate a FHIR Resource of specific type with a value for all fhir paths in your
blueprint.

### Example

```json
{
  "resourceType": "Condition",
  "code": {
    "coding": [
      {
        "system": "http://snomed.info/sct",
        "code": "195967001",
        "display": "Hypertension"
      }
    ]
  },
  "subject": {
    "reference": "Patient/1234"
  },
  "recordedDate": "2023-02-15T08:30:00Z"
}
```

Defines a Condition that can be modified using the TestDataBlueprint information:

```json
{
  "Condition.code.coding": "randomCoding(http://fhir.de/ValueSet/bfarm/icd-10-gm)",
  "Condition.subject": "randomPatientReference()",
  "Condition.recordedDate": "randomDateTime()"
}
```

A modified Condition might look something like this:

```json
{
  "resourceType": "Condition",
  "code": {
    "coding": [
      {
        "system": "http://fhir.de/CodeSystem/bfarm/icd-10-gm",
        "code": "I11.0",
        "display": "Hypertensive Herzkrankheit mit (kongestiver) Herzinsuffizienz"
      }
    ]
  },
  "subject": {
    "reference": "Patient/c91a530a-72c0-46f3-b337-fcd097166e69",
    "display": "Berat Kustermann"
  },
  "recordedDate": "2006-07-21T09:26:24+02:00"
}
```

The modified FHIR resource can now be used for testing or other purposes. All test data generated
have no real-world medical relation at all.

The program utalizes:

- https://github.com/DiUS/java-faker for fake names, adresses etc.
- https://github.com/hapifhir/hapi-fhir for FHIR Resource parsing and evaluating FHIR Path
  expressions.

### Application Example

```java
public class Application {


  public static void main(String[] args) {
    var bluePrints = BluePrintLoader.loadBluePrints(
            "src/main/resources/BluePrint/TestDataResourceBluePrint.json");
    var resources = bluePrints.stream().map(FhirResourceFactory::createTestResourceFromBluePrint)
            .filter(Objects::nonNull).flatMap(
                    Collection::stream)
            .toList();
    var bundle = FhirTransactionBundleConverter.convertToFhirTransactionBundle(resources);
    FhirResourceFactory.writeResource(bundle, "src/main/resources/Bundle/GeneratedBundle.json");
    var params = FhirResourceFactory.writeNDJsonByResourceType(resources,
            "src/main/resources/NDJson/test_resources");
    FhirResourceFactory.writeResource(params,
            "src/main/resources/Parameters/GeneratedParameters.json");
  }

}

```

#### Upload to FHIR Server

In Application.java you can find an example of how to use the program. Based on the BluePrints the
test resources are generated. You can convert them two a FHIR Transaction Bundle to upload them to
Standard FHIR Server like
[Hapi](https://github.com/hapifhir/hapi-fhir) or [blaze](https://github.com/samply/blaze).

#### Upload to [Pathling](https://github.com/aehrc/pathling)

To upload the generated resources to Pathling we need to create a NDJSON file for each resource
type. writeNDJsonByResourceType does exactly that. And returns a Parameter Resource that can be used
to import the resources into Pathling. Using pathlings .../fhir/$import operation. Make sure you
mount the generated NDJSON files into the pathling container and eventually update the valueUrl in
the Parameter Resource to match the path of the mounted files.

```json
{
  "parameter": [
    {
      "name": "source",
      "part": [
        {
          "name": "resourceType",
          "valueCode": "Condition"
        },
        {
          "name": "url",
          "valueUrl": "file:///src/main/resources/NDJson/test_resources-Condition.ndjson"
        }
      ]
    }
  ]
}
```

