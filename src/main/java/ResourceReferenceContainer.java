import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Reference;

public class ResourceReferenceContainer extends
    Reference {

  private final List<IBaseResource> resources;
  private final Reference reference;

  private ResourceReferenceContainer(
      List<IBaseResource> resources,
      Reference reference) {
    this.resources = resources;
    this.reference = reference;
  }

  public static <T extends IBaseResource> ResourceReferenceContainer of(T resource,
      Reference reference) {
    return new ResourceReferenceContainer(List.of(resource), reference);
  }

  public static ResourceReferenceContainer of(List<IBaseResource> resources,
      Reference reference) {
    return new ResourceReferenceContainer(List.copyOf(resources), reference);
  }

  public List<IBaseResource> resources() {
    return resources;
  }

  public Reference reference() {
    return reference;
  }
}
