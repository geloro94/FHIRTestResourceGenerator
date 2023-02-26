import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Reference;

public class ResourceReferenceContainer<T extends IBaseResource> extends
    Reference {

  private final T resource;
  private final Reference reference;

  private ResourceReferenceContainer(T resource, Reference reference) {
    this.resource = resource;
    this.reference = reference;
  }

  public static <T extends IBaseResource> ResourceReferenceContainer<T> of(T resource,
      Reference reference) {
    return new ResourceReferenceContainer<>(resource, reference);
  }

  public T resource() {
    return resource;
  }

  public Reference reference() {
    return reference;
  }
}
