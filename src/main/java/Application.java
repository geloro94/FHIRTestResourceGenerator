import org.hl7.fhir.instance.model.api.IBaseResource;

public class Application {


  public static void main(String[] args) {
    var bluePrints = BluePrintLoader.loadBluePrints(
        "src/main/resources/BluePrint/TestDataResourceBluePrint.json");
    bluePrints.stream().map(FhirResourceFactory::createTestResourceFromBluePrint)
        .forEach(resource -> FhirResourceFactory.printResource((IBaseResource) resource));
  }

}
