import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.github.javafaker.Faker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.jetbrains.annotations.NotNull;

/**
 * This class contains functions that can be used as the values for fhir attributes.
 */
public class GeneratorFunctions {

  private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

  private static final IGenericClient CLIENT = FHIR_CONTEXT.newRestfulGenericClient(
      "https://ontoserver.imi.uni-luebeck.de/fhir/");

  private static final Faker FAKER = new Faker(new Locale("de"));

  /**
   * Cache for value set codes.
   */
  private static final LoadingCache<String, List<ValueSetExpansionContainsComponent>> VALUE_SET_CODES_CACHE = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterWrite(30, TimeUnit.MINUTES)
      .build(new CacheLoader<>() {
        @NotNull
        @Override
        public List<ValueSetExpansionContainsComponent> load(@NotNull String valueSetUrl) {
          return getValueSet(valueSetUrl);
        }
      });


  /**
   * Generates a random Patient.
   *
   * @return a random Patient
   */
  public static Patient randomPatient() {
    Patient patient = new Patient();
    patient.setId(UUID.randomUUID().toString());
    patient.addName().setFamily(FAKER.name().lastName()).addGiven(FAKER.name().firstName());
    patient.addAddress().setCity(FAKER.address().city()).setPostalCode(FAKER.address().zipCode())
        .setCountry(FAKER.address().country());
    return patient;
  }

  /**
   * Generates a random Organization.
   *
   * @return a random Organization
   */
  public static Organization randomOrganization() {
    Organization organization = new Organization();
    organization.setId(UUID.randomUUID().toString());
    organization.setName(FAKER.company().name());
    Address address = new Address();
    address.setCity(FAKER.address().city());
    address.setPostalCode(FAKER.address().zipCode());
    address.setCountry(FAKER.address().country());
    organization.addAddress(address);
    return organization;
  }


  /**
   * Generates a random CodeType.
   *
   * @param valueSet the value set to choose the code from
   * @return a random CodeType
   */
  public static CodeType randomCode(String valueSet) {
    return new CodeType(randomCoding(valueSet).getCode());
  }

  /**
   * Get the expansion contains of a value set.
   *
   * @param valueSetUrl the value set to choose the code from
   * @return the expansion contains of the value set
   */
  public static List<ValueSetExpansionContainsComponent> getValueSet(String valueSetUrl) {
    ValueSetExpansionComponent expansion = CLIENT.operation().onType(ValueSet.class)
        .named("$expand")
        .withParameter(Parameters.class, "url", new StringType(valueSetUrl)).useHttpGet()
        .returnResourceType(ValueSet.class)
        .execute().getExpansion();
    return expansion.getContains();
  }

  /**
   * Generates a random Coding from a value set.
   * @param valueSetUri the value set to choose the code from
   * @return a random Coding
   */
  public static Coding randomCoding(String valueSetUri) {
    var codes = VALUE_SET_CODES_CACHE.getUnchecked(valueSetUri);
    int numCodes = codes.size();
    Random rand = new Random();
    ValueSetExpansionContainsComponent chosen = codes.get(rand.nextInt(numCodes));

    // Create a Coding object from the chosen code
    Coding coding = new Coding();
    coding.setSystem(chosen.getSystem());
    coding.setCode(chosen.getCode());
    coding.setDisplay(chosen.getDisplay());

    return coding;
  }

  /**
   * Generates a random Reference to a Patient.
   *
   * @return a random Reference to a Patient
   */
  public static Reference randomPatientReference() {
    Patient patient = randomPatient();
    Reference reference = new Reference();
    reference.setReference("Patient/" + patient.getIdElement().getIdPart());
    reference.setDisplay(patient.getNameFirstRep().getNameAsSingleString());
    return reference;
  }

  /**
   * Generates a random DateTimeType.
   * @return a random DateTimeType
   */
  public static DateTimeType randomDateTime() {
    Instant now = Instant.now();
    long minDay = Instant.parse("2000-01-01T00:00:00.00Z").getEpochSecond();
    long maxDay = now.getEpochSecond();
    long randomDay = minDay + (long) (Math.random() * (maxDay - minDay));
    return new DateTimeType(Date.from(Instant.ofEpochSecond(randomDay)));
  }

  /**
   * Generates a Coding
   * @param system the system of the coding
   * @param code the code of the coding
   * @param version the version of the coding
   * @param display the display of the coding
   * @return a Coding based on the given parameters
   */
  public static Coding fixedCoding(String system, String code, String version,
      String display) {
    Coding coding = new Coding();
    coding.setSystem(system);
    coding.setCode(code);
    coding.setVersion(version);
    coding.setDisplay(display);
    return coding;
  }

  public static CodeType fixedCode(String value) {
    return new CodeType(value);
  }

  public static UriType fixedURI(String value) {
    return new UriType(value);
  }

  /**
   * Generates a random Period.
   * @return a random Period
   */
  public static Period randomPeriod() {
    LocalDateTime startDate = LocalDateTime.of(2000, 1, 1, 0, 0);
    LocalDateTime endDate = LocalDateTime.now();
    long startMillis = startDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    long endMillis = endDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    long randomMillis = ThreadLocalRandom.current().nextLong(startMillis, endMillis);
    LocalDateTime randomDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(randomMillis),
        ZoneId.systemDefault());

    Period period = new Period();
    period.setStartElement(new DateTimeType(String.valueOf(randomDateTime)));
    period.setEndElement(new DateTimeType(
        String.valueOf(randomDateTime.plusDays(ThreadLocalRandom.current().nextInt(1, 365)))));
    return period;
  }

  /**
   * Generates a random Identifier code value.
   * @return a random Identifier code value
   */
  public static StringType randomIdentifierCodeValue() {
    SecureRandom random = new SecureRandom();
    String randomString = new BigInteger(50, random).toString(32);
    return new StringType(randomString.substring(0, 5));
  }

  public static UriType randomIdentifierSystemURI() {
    UriType uri = new UriType();
    uri.setValue("urn:uuid:" + UUID.randomUUID());
    return uri;
  }

  /**
   * Generates a random Reference to an Organization.
   *
   * @return a random Reference to an Organization
   */
  public static Reference randomOrganizationReference() {
    var organization = randomOrganization();
    Reference reference = new Reference();
    reference.setReference("Organization/" + organization.getIdElement().getIdPart());
    reference.setDisplay(organization.getName());
    return reference;
  }

  /**
   * Generates a random Quantity. If the value is null, a random value between -100 and 100 is
   * generated. If the comparator is not null, the value is adjusted to be within the comparator
   * range.
   * @param value the value of the quantity
   * @param unit the unit of the quantity
   * @param system the system of the quantity
   * @param code the code of the quantity
   * @param comparator the comparator of the quantity
   * @return a random Quantity
   */
  public static Quantity randomQuantity(String value, String unit, String system, String code,
      String comparator) {
    DecimalFormat decimalFormat = new DecimalFormat("#,##");
    Quantity quantity = new Quantity();
    if (value == null) {
      double randomValue = ThreadLocalRandom.current().nextDouble(-100, 100);
      quantity.setValue(Double.parseDouble(decimalFormat.format(randomValue)));
    } else {
      quantity.setValue(Double.parseDouble(decimalFormat.format(Double.parseDouble(value))));
    }
    if (comparator != null && quantity.getValue() != null) {
      if (comparator.equals("<")) {
        double upperBound = Double.parseDouble(String.valueOf(quantity.getValue())) - 1;
        double randomValue = ThreadLocalRandom.current().nextDouble(-100, upperBound);
        quantity.setValue(Double.parseDouble(decimalFormat.format(randomValue)));
      } else if (comparator.equals(">")) {
        double lowerBound = Double.parseDouble(String.valueOf(quantity.getValue())) + 1;
        double randomValue = ThreadLocalRandom.current().nextDouble(lowerBound, 100);
        quantity.setValue(Double.parseDouble(decimalFormat.format(randomValue)));
      }
    }
    quantity.setUnit(unit);
    quantity.setSystem(system);
    quantity.setCode(code);
    return quantity;
  }

  /**
   * Generates an Address.
   * @param type the type of the address (i.e. postal, physical, etc.)
   * @param lines the lines of the address (i.e. street address, suite number, etc.)
   * @param city the city of the address
   * @param state the state of the address
   * @param postalCode the postal code of the address
   * @param country the country of the address
   * @return an Address based on the given parameters
   */
  public static Address createAddress(Address.AddressType type, List<String> lines, String city,
      String state,
      String postalCode, String country) {
    Address address = new Address();
    address.setType(type);
    for (String line : lines) {
      address.addLine(line);
    }
    address.setCity(city);
    address.setState(state);
    address.setPostalCode(postalCode);
    address.setCountry(country);
    return address;
  }

  /**
   * Generates a random Street Address with a number.
   * @return a random Street Address with a number
   */
  public static String randomStreetWithNumber() {
    return FAKER.address().streetAddress();
  }

  /** Generates a random City.
   * @return a random City
   */
  public static String randomCity() {
    return FAKER.address().city();
  }

  /**
   * Generates a random State.
   * @return a random State
   */
  public static String randomState() {
    return FAKER.address().state();
  }

  /**
   * Generates a random Postal Code.
   * @return a random Postal Code
   */
  public static String randomPostalCode() {
    return FAKER.address().zipCode();
  }

  /**
   * Generates a random Family Name.
   * @return a random Family Name
   */
  public static String randomFamilyName() {
    return FAKER.name().lastName();
  }

  /**
   * Generates a random Given Name.
   * @return a random Given Name
   */
  public static String randomGivenName() {
    return FAKER.name().firstName();
  }

}