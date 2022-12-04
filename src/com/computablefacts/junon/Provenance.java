package com.computablefacts.junon;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.errorprone.annotations.CheckReturnValue;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

/**
 * <pre>
 * {
 *     "source_store": "doc_id",
 *     "source_type": "pdf",
 *     "source_reliability": "completely reliable",
 *     "page": "Page number of the page stored in the string field.",
 *     "string": "Text from which the string_span is extracted."
 *     "string_span": "This sentence contains the fact.",
 *     "string_span_hash": "hash of the string_span attribute",
 *     "start_index": 26,
 *     "end_index": 30,
 *     "extraction_date": "2019-06-01T00:00:00Z",
 *     "modification_date": "2019-06-01T00:11:11Z",
 * }
 * </pre>
 */
@CheckReturnValue
@JsonInclude(JsonInclude.Include.NON_NULL)
final public class Provenance {

  private final static HashFunction MURMUR3_128 = Hashing.murmur3_128();

  @JsonProperty("source_store")
  private final String sourceStore_;
  @JsonProperty("source_type")
  private final String sourceType_;
  @JsonProperty("source_reliability")
  private final String sourceReliability_;
  @JsonProperty("string")
  private final String string_; // full page
  @JsonProperty("string_span")
  private final String span_; // snippet
  @JsonProperty("start_index")
  private final Integer startIndex_; // fact start position in snippet
  @JsonProperty("end_index")
  private final Integer endIndex_; // fact end position in snippet
  @JsonProperty("extraction_date")
  private final String extractionDate_;
  @JsonProperty("modification_date")
  private final String modificationDate_;
  @JsonProperty("string_span_hash")
  private final String spanHash_; // snippet's hash
  @JsonProperty("page")
  private final Integer page_; // page number

  public Provenance(String sourceType, String sourceStore, String sourceReliability, Date extractionDate,
      Date modificationDate) {

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

    sourceType_ = sourceType;
    sourceStore_ = sourceStore;
    sourceReliability_ = sourceReliability;
    string_ = null;
    span_ = null;
    spanHash_ = null;
    startIndex_ = null;
    endIndex_ = null;
    page_ = null;
    extractionDate_ = extractionDate == null ? null : sdf.format(extractionDate);
    modificationDate_ = modificationDate == null ? null : sdf.format(modificationDate);
  }

  public Provenance(String sourceType, String sourceStore, String sourceReliability, Date extractionDate,
      Date modificationDate, int page, String string) {

    Preconditions.checkArgument(page > 0, "page number should be > 0");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(string), "string should neither be null nor empty");

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

    sourceType_ = sourceType;
    sourceStore_ = sourceStore;
    sourceReliability_ = sourceReliability;
    string_ = string;
    span_ = null;
    spanHash_ = null;
    startIndex_ = null;
    endIndex_ = null;
    page_ = page;
    extractionDate_ = extractionDate == null ? null : sdf.format(extractionDate);
    modificationDate_ = modificationDate == null ? null : sdf.format(modificationDate);
  }

  public Provenance(String sourceType, String sourceStore, String sourceReliability, Date extractionDate,
      Date modificationDate, int page, String string, String span, int startIndex, int endIndex) {

    Preconditions.checkArgument(page >= 0, "page number should be >= 0");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(span), "span should neither be null nor empty");
    Preconditions.checkArgument(startIndex >= 0 && startIndex <= span.length());
    Preconditions.checkArgument(endIndex >= 0 && endIndex <= span.length());
    Preconditions.checkArgument(startIndex <= endIndex);

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

    sourceType_ = sourceType;
    sourceStore_ = sourceStore;
    sourceReliability_ = sourceReliability;
    string_ = string;
    span_ = span;
    spanHash_ = MURMUR3_128.newHasher().putString(span, StandardCharsets.UTF_8).hash().toString();
    startIndex_ = startIndex;
    endIndex_ = endIndex;
    page_ = page;
    extractionDate_ = extractionDate == null ? null : sdf.format(extractionDate);
    modificationDate_ = modificationDate == null ? null : sdf.format(modificationDate);
  }

  @JsonCreator
  public Provenance(@JsonProperty("source_store") String sourceStore, @JsonProperty("source_type") String sourceType,
      @JsonProperty("source_reliability") String sourceReliability, @JsonProperty("string") String string,
      @JsonProperty("string_span") String span, @JsonProperty("start_index") Integer startIndex,
      @JsonProperty("end_index") Integer endIndex, @JsonProperty("extraction_date") String extractionDate,
      @JsonProperty("modification_date") String modificationDate, @JsonProperty("string_span_hash") String spanHash,
      @JsonProperty("page") Integer page) {

    Preconditions.checkArgument(page >= 0, "page number should be >= 0");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(span), "span should neither be null nor empty");
    Preconditions.checkArgument(startIndex >= 0 && startIndex <= span.length());
    Preconditions.checkArgument(endIndex >= 0 && endIndex <= span.length());
    Preconditions.checkArgument(startIndex <= endIndex);

    sourceStore_ = sourceStore;
    sourceType_ = sourceType;
    sourceReliability_ = sourceReliability;
    string_ = string;
    span_ = span;
    startIndex_ = startIndex;
    endIndex_ = endIndex;
    extractionDate_ = extractionDate;
    modificationDate_ = modificationDate;
    spanHash_ = spanHash;
    page_ = page;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Provenance)) {
      return false;
    }
    Provenance provenance = (Provenance) o;
    return Objects.equals(sourceStore_, provenance.sourceStore_) && Objects.equals(sourceType_, provenance.sourceType_)
        && Objects.equals(sourceReliability_, provenance.sourceReliability_) && Objects.equals(extractionDate_,
        provenance.extractionDate_) && Objects.equals(modificationDate_, provenance.modificationDate_)
        && Objects.equals(string_, provenance.string_) && Objects.equals(span_, provenance.span_) && Objects.equals(
        spanHash_, provenance.spanHash_) && Objects.equals(startIndex_, provenance.startIndex_) && Objects.equals(
        endIndex_, provenance.endIndex_) && Objects.equals(page_, provenance.page_);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceStore_, sourceType_, sourceReliability_, extractionDate_, modificationDate_, string_,
        span_, spanHash_, startIndex_, endIndex_, page_);
  }

  public String storage() {
    return sourceStore_.substring(0, sourceStore_.indexOf('/'));
  }

  public String root() {
    int idx = sourceStore_.indexOf('/') + 1;
    return sourceStore_.substring(idx, sourceStore_.indexOf('/', idx));
  }

  public String dataset() {
    int idx1 = sourceStore_.indexOf('/') + 1;
    int idx2 = sourceStore_.indexOf('/', idx1) + 1;
    return sourceStore_.substring(idx2, sourceStore_.lastIndexOf('/'));
  }

  public String docId() {
    return sourceStore_.substring(sourceStore_.lastIndexOf('/') + 1);
  }

  public String sourceStore() {
    return sourceStore_;
  }

  public String sourceType() {
    return sourceType_;
  }

  public String sourceReliability() {
    return sourceReliability_;
  }

  public String string() {
    return string_;
  }

  public String span() {
    return span_;
  }

  public int startIndex() {
    return startIndex_ == null ? -1 : startIndex_;
  }

  public int endIndex() {
    return endIndex_ == null ? -1 : endIndex_;
  }

  public String extractionDate() {
    return extractionDate_;
  }

  public String modificationDate() {
    return modificationDate_;
  }

  public String spanHash() {
    return spanHash_;
  }

  public int page() {
    return page_ == null ? -1 : page_;
  }
}
