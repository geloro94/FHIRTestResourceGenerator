import java.util.Collection;
import java.util.Objects;

public class Application {


  public static void main(String[] args) {
    var bluePrints = BluePrintLoader.loadBluePrints(
        "src/main/resources/BluePrint/TestDataResourceBluePrint.json");
    var resources = bluePrints.stream().map(FhirResourceFactory::createTestResourceFromBluePrint)
        .filter(Objects::nonNull).flatMap(
            Collection::stream)
        .toList();
    var bundle = FhirTransactionBundleConverter.convertToFhirTransactionBundle(resources);
    FhirResourceFactory.writeBundle(bundle, "src/main/resources/Bundle/GeneratedBundle.json");

  }

}
