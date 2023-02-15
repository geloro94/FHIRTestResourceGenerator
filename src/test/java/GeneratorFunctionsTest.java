import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.hl7.fhir.r4.model.Address.AddressType;
import org.junit.jupiter.api.Test;


public class GeneratorFunctionsTest {

  @Test
  public void testRandomCoding() {
    var coding = GeneratorFunctions.randomCoding("http://hl7.org/fhir/ValueSet/iso3166-1-2");
    assertNotNull(coding);
    assertNotNull(coding.getSystem());
    assertNotNull(coding.getCode());
    assertNotNull(coding.getDisplay());
  }

  @Test
  public void testRandomQuantity() {
    var quantity = GeneratorFunctions.randomQuantity(null, null,
        "http://unitsofmeasure.org", null, null);
    assertNotNull(quantity);
    assertNotNull(quantity.getSystem());
    assertNotNull(quantity.getValue());
  }

  @Test
  public void testRandomCode() {
    var code = GeneratorFunctions.randomCode("http://hl7.org/fhir/ValueSet/iso3166-1-2");
    assertNotNull(code);
    assertNotNull(code.getValue());
  }

  @Test
  public void testRandomFamilyName() {
    var familyName = GeneratorFunctions.randomFamilyName();
    assertNotNull(familyName);
  }

  @Test
  public void testRandomGivenName() {
    var givenName = GeneratorFunctions.randomGivenName();
    assertNotNull(givenName);
  }

  @Test
  public void testRandomStreetWithNumber() {
    var streetAddress = GeneratorFunctions.randomStreetWithNumber();
    assertNotNull(streetAddress);
  }

  @Test
  public void testRandomCity() {
    var city = GeneratorFunctions.randomCity();
    assertNotNull(city);
  }

  @Test
  public void testRandomState() {
    var state = GeneratorFunctions.randomState();
    assertNotNull(state);
  }

  @Test
  public void testRandomPostalCode() {
    var postalCode = GeneratorFunctions.randomPostalCode();
    assertNotNull(postalCode);
  }

  @Test
  public void testRandomPatientReference() {
    var patientReference = GeneratorFunctions.randomPatientReference();
    assertNotNull(patientReference);
    assertNotNull(patientReference.getReference());
  }

  @Test
  public void testRandomDateTime() {
    var dateTime = GeneratorFunctions.randomDateTime();
    assertNotNull(dateTime);
  }

  @Test
  public void testFixedCoding() {
    var coding = GeneratorFunctions.fixedCoding(
        "http://hl7.org/fhir/ValueSet/iso3166-1-2", "US","2022",
        "United States of America");
    assertNotNull(coding);
    assertNotNull(coding.getSystem());
    assertNotNull(coding.getCode());
    assertNotNull(coding.getDisplay());
  }

  @Test
  public void testFixedCode() {
    var code = GeneratorFunctions.fixedCode("US");
    assertNotNull(code);
    assertNotNull(code.getValue());
  }

  @Test
  public void testFixedURI() {
    var uri = GeneratorFunctions.fixedURI("http://hl7.org/fhir/ValueSet/iso3166-1-2");
    assertNotNull(uri);
    assertNotNull(uri.getValue());
  }

  @Test
  public void testRandomPeriod() {
    var period = GeneratorFunctions.randomPeriod();
    assertNotNull(period);
    assertNotNull(period.getStart());
    assertNotNull(period.getEnd());
  }

  @Test
  public void testRandomIdentifierSystemURI() {
    var identifier = GeneratorFunctions.randomIdentifierSystemURI();
    assertNotNull(identifier);
    assertNotNull(identifier.getValue());
  }

  @Test
  public void testRandomOrganizationReference() {
    var organizationReference = GeneratorFunctions.randomOrganizationReference();
    assertNotNull(organizationReference);
    assertNotNull(organizationReference.getReference());
  }

  @Test
  public void testCreateAddress() {
    var address = GeneratorFunctions.createAddress(AddressType.PHYSICAL, List.of("123 Main St"),
        "San Francisco", "CA", "94105", "US");
    assertNotNull(address);
    assertNotNull(address.getCity());
    assertNotNull(address.getCountry());
    assertNotNull(address.getLine());
    assertNotNull(address.getPostalCode());
    assertNotNull(address.getState());
  }

  @Test
  public void randomIdentifierCodeValue() {
    var identifier = GeneratorFunctions.randomIdentifierCodeValue();
    assertNotNull(identifier);
    assertNotNull(identifier.getValue());
  }
}
