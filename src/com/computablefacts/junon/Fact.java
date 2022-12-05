package com.computablefacts.junon;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CheckReturnValue;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * <pre>
 * {
 *     "type": "FACT TYPE",
 *     "values": ["fact value 1", "fact value 2", ...],
 *     "is_valid": false,
 *     "authorizations": "AUTH1|AUTH2|AUTH3",
 *     "confidence_score": 0.87,
 *     "start_date": "2019-06-01T00:00:00Z",
 *     "end_date": "2019-06-02T00:00:00Z",
 *     "external_id": "3azvw|2019-10-21T12:11:27.599Z"
 *     "metadata": [...],
 *     "provenances": [...]
 * }
 * </pre>
 */
@CheckReturnValue
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
final public class Fact extends HasId {

  @JsonProperty("metadata")
  private final List<Metadata> metadata_ = new ArrayList<>();
  @JsonProperty("provenances")
  private final List<Provenance> provenances_ = new ArrayList<>();
  @JsonProperty("values")
  private final List<String> values_ = new ArrayList<>();
  @JsonProperty("type")
  private final String type_;
  @JsonProperty("is_valid")
  private final Boolean isValid_;
  @JsonProperty("authorizations")
  private final String authorizations_;
  @JsonProperty("confidence_score")
  private final double confidenceScore_;
  @JsonProperty("start_date")
  private final String startDate_;
  @JsonProperty("end_date")
  private final String endDate_;

  @Deprecated
  @SuppressWarnings("unchecked")
  public static Fact fromLegacy(Map<String, Object> obj) {

    Integer id = (Integer) obj.get("id");
    String type = (String) obj.get("type");
    List<String> values = (List<String>) obj.get("values");
    Boolean isValid = (Boolean) obj.get("is_valid");
    Double confidenceScore = (Double) obj.get("confidence_score");
    String externalId = (String) obj.get("external_id");
    String startDate = (String) obj.get("start_date");
    String endDate = (String) obj.get("end_date");
    String updatedAt = (String) obj.get("updated_at");
    List<Map<String, Object>> metadata = (List<Map<String, Object>>) obj.get("metadata");
    List<Map<String, Object>> provenances = (List<Map<String, Object>>) obj.get("provenances");

    Preconditions.checkState(provenances.size() == 1);

    Map<String, Object> provenance = provenances.get(0);
    String sourceStore = (String) provenance.get("source_store");
    String sourceType = (String) provenance.get("source_type");
    String sourceReliability = null;
    String string = null;
    String span = (String) provenance.get("string_span");
    Integer startIndex = (Integer) provenance.get("start_index");
    Integer endIndex = (Integer) provenance.get("end_index");
    String extractionDate = null;
    String modificationDate = null;
    String spanHash = (String) provenance.get("string_span_hash");
    int page;

    if (values.size() == 5 && sourceStore.contains("/vam/")) {
      try {
        page = Integer.parseInt(values.get(1), 10);
      } catch (NumberFormatException e) {
        page = 0;
      }
    } else if (values.size() == 3 && sourceStore.contains("/dab/")) {
      try {
        page = Integer.parseInt(values.get(2), 10);
      } catch (NumberFormatException e) {
        page = 0;
      }
    } else {
      try {
        page = (Integer) provenance.get("page");
      } catch (NumberFormatException e) {
        page = 0;
      }
    }

    if (page <= 0) {
      return null; // pages are 1-based
    }

    // Extract metadata
    List<Metadata> newMetadata = metadata.stream()
        .map(m -> new Metadata((String) m.get("type"), (String) m.get("key"), (String) m.get("value")))
        .collect(Collectors.toList());

    // Extract provenance
    Provenance newProvenance = new Provenance(sourceStore, sourceType, sourceReliability, string, span, startIndex,
        endIndex, extractionDate, modificationDate, spanHash, page);

    List<Provenance> newProvenances = new ArrayList<>();
    newProvenances.add(newProvenance);

    // Create fact
    Fact fact = new Fact(externalId, newMetadata, newProvenances, values, type, isValid, null, confidenceScore,
        startDate, endDate);
    fact.id_ = id;

    return fact;
  }

  public Fact(String type, double confidenceScore) {
    this(type, confidenceScore, null, new Date(), null, null);
  }

  public Fact(String type, double confidenceScore, String authorizations) {
    this(type, confidenceScore, authorizations, new Date(), null, null);
  }

  public Fact(String type, double confidenceScore, String authorizations, Date startDate, Date endDate,
      Boolean isValid) {

    super();

    Preconditions.checkArgument(!Strings.isNullOrEmpty(type), "type should neither be null nor empty");
    Preconditions.checkArgument(confidenceScore >= 0.0 && confidenceScore <= 1.0,
        "confidenceScore should be >= 0 and <= 1");

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

    type_ = type;
    confidenceScore_ = confidenceScore;
    authorizations_ = authorizations;
    startDate_ = startDate == null ? null : sdf.format(startDate);
    endDate_ = endDate == null ? null : sdf.format(endDate);
    isValid_ = isValid;
  }

  @JsonCreator
  public Fact(@JsonProperty("external_id") String externalId, @JsonProperty("metadata") List<Metadata> metadata,
      @JsonProperty("provenances") List<Provenance> provenances, @JsonProperty("values") List<String> values,
      @JsonProperty("type") String type, @JsonProperty("is_valid") Boolean isValid,
      @JsonProperty("authorizations") String authorizations, @JsonProperty("confidence_score") double confidenceScore,
      @JsonProperty("start_date") String startDate, @JsonProperty("end_date") String endDate) {

    super(externalId);

    Preconditions.checkArgument(!Strings.isNullOrEmpty(type), "type should neither be null nor empty");
    Preconditions.checkArgument(confidenceScore >= 0.0 && confidenceScore <= 1.0,
        "confidenceScore should be >= 0 and <= 1");

    type_ = type;
    isValid_ = isValid;
    authorizations_ = authorizations;
    confidenceScore_ = confidenceScore;
    startDate_ = startDate;
    endDate_ = endDate;

    if (metadata != null) {
      metadata_.addAll(metadata);
    }
    if (provenances != null) {
      provenances_.addAll(provenances);
    }
    if (values != null) {
      values_.addAll(values);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Fact)) {
      return false;
    }
    Fact fact = (Fact) o;
    return Objects.equals(type_, fact.type_) && Objects.equals(isValid_, fact.isValid_) && Objects.equals(
        authorizations_, fact.authorizations_) && Objects.equals(confidenceScore_, fact.confidenceScore_)
        && Objects.equals(startDate_, fact.startDate_) && Objects.equals(endDate_, fact.endDate_) && Objects.equals(
        metadata_, fact.metadata_) && Objects.equals(provenances_, fact.provenances_) && Objects.equals(values_,
        fact.values_);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type_, isValid_, authorizations_, confidenceScore_, startDate_, endDate_, metadata_,
        provenances_, values_);
  }

  public void value(String value) {

    Preconditions.checkNotNull(value);

    values_.add(value);
  }

  public String value(int position) {

    Preconditions.checkArgument(0 <= position && position < values_.size());

    return values_.get(position);
  }

  public List<String> values() {
    return ImmutableList.copyOf(values_);
  }

  public void metadata(Collection<Metadata> metadata) {

    Preconditions.checkNotNull(metadata);

    this.metadata_.addAll(metadata);
  }

  public void metadata(Metadata metadata) {

    Preconditions.checkNotNull(metadata);

    this.metadata_.add(metadata);
  }

  public List<Metadata> metadata() {
    return ImmutableList.copyOf(metadata_);
  }

  public void provenance(Provenance provenance) {

    Preconditions.checkNotNull(provenance);

    provenances_.add(provenance);
  }

  public Provenance provenance() {

    Preconditions.checkState(provenances_.size() == 1);

    return provenances_.get(0);
  }

  public List<Provenance> provenances() {
    return ImmutableList.copyOf(provenances_);
  }

  public String type() {
    return type_;
  }

  public Boolean isValid() {
    return isValid_;
  }

  /**
   * Returns true iif the fact has been accepted.
   *
   * @return true if the fact has been accepted, false otherwise.
   */
  public boolean isAccepted() {
    return isValid_ != null && isValid_;
  }

  /**
   * Returns true iif the fact has been rejected.
   *
   * @return true if the fact has been rejected, false otherwise.
   */
  public boolean isRejected() {
    return isValid_ != null && !isValid_;
  }

  /**
   * Returns true iif the fact should be verified.
   *
   * @return true if the fact should be verified, false otherwise.
   */
  public boolean isVerified() {
    return isValid_ != null;
  }

  public String authorizations() {
    return authorizations_;
  }

  public double confidenceScore() {
    return confidenceScore_;
  }

  public String startDate() {
    return startDate_;
  }

  public String endDate() {
    return endDate_;
  }
}
