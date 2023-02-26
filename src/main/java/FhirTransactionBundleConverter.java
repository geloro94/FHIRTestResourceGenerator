import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Resource;

public class FhirTransactionBundleConverter {

  public static <T extends IBaseResource> Bundle convertToFhirTransactionBundle(List<T> resources) {

    // Create a FHIR transaction bundle and add a PUT request for each resource
    Bundle bundle = new Bundle();
    bundle.setType(BundleType.TRANSACTION);
    for (IBaseResource resource : resources) {
      Resource r = (Resource) resource;
      BundleEntryComponent entry = new BundleEntryComponent();
      entry.setResource(r);
      entry.getRequest().setMethod(HTTPVerb.PUT);
      entry.getRequest().setUrl(r.fhirType() + "/" + r.getIdElement().getValue());
      bundle.addEntry(entry);
    }
    return bundle;
  }
}