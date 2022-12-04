package com.computablefacts.asterix;

import com.computablefacts.Generated;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.computablefacts.asterix.ml.GoldLabel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Link a {@link com.computablefacts.junon.Fact}, i.e. a span of text associated with a label, to its underlying
 * {@link Document}, i.e. the document from which the fact has been extracted.
 */
@CheckReturnValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class FactAndDocument {

  private static final char FORM_FEED = '\f';

  @JsonProperty(value = "fact", required = true)
  private final Map<String, Object> fact_;
  @JsonProperty(value = "document", required = true)
  private Map<String, Object> document_;

  private FactAndDocument(Map<String, Object> fact) {
    this(fact, null);
  }

  @JsonCreator
  private FactAndDocument(@JsonProperty(value = "fact") Map<String, Object> fact,
      @JsonProperty(value = "document") Map<String, Object> document) {

    Preconditions.checkNotNull(fact, "fact should not be null");

    fact_ = fact;
    document_ = document;
  }

  /**
   * Merge facts and documents together. Export the page associated with each fact as a gold label.
   * <ul>
   *  <li>{@code args[0]} the facts as a gzipped JSONL file.</li>
   * <li>{@code args[1]} the documents as a gzipped JSONL file.</li>
   * </ul>
   */
  @Beta
  @Generated
  public static void main(String[] args) {

    File facts = new File(args[0]);
    File documents = new File(args[1]);

    Preconditions.checkArgument(facts.exists(), "missing facts: %s", facts);
    Preconditions.checkArgument(documents.exists(), "missing documents: %s", documents);

    File dataset = new File(String.format("%sfacts_and_documents.jsonl.gz", facts.getParent() + File.separator));

    System.out.printf("Facts dataset is %s\n", facts);
    System.out.printf("Documents dataset is %s\n", documents);
    System.out.printf("Merged dataset is %s\n", dataset);
    System.out.println("Merging facts and documents...");

    if (!dataset.exists()) {
      if (!save(dataset, merge(facts, documents, null).displayProgress(5000))) {
        System.out.println("An error occurred.");
        return;
      }
    }

    System.out.println("Facts and documents merged.");

    File goldLabels = new File(String.format("%sgold_labels.jsonl.gz", facts.getParent() + File.separator));

    System.out.printf("Gold labels dataset is %s\n", goldLabels);
    System.out.println("Exporting gold labels...");

    if (GoldLabel.save(goldLabels, load(dataset, null).flatten(fad -> {
      View<GoldLabel> view =
          (fad.isAccepted() || fad.isRejected()) && !Strings.isNullOrEmpty(fad.matchedPage()) ? View.of(
              fad.pageAsGoldLabel()) : View.of();
      return fad.isAccepted() ? view.concat(fad.syntheticPagesAsGoldLabels()) : view;
    }).displayProgress(5000))) {
      System.out.println("Gold labels exported.");
    } else {
      System.out.println("An error occurred.");
    }
  }

  /**
   * Load elements from a gzipped JSONL file.
   *
   * @param file  the input file.
   * @param label the specific gold labels to load. If {@code label} is set to {@code null}, all gold labels will be
   *              loaded.
   * @return a set of elements.
   */
  @SuppressWarnings("unchecked")
  public static View<FactAndDocument> load(File file, String label) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file file does not exist : %s", file);

    return View.of(file, true).filter(row -> !Strings.isNullOrEmpty(row) /* remove empty rows */).map(row -> {

      Map<String, Object> element = JsonCodec.asObject(row);
      Map<String, Object> fact = (Map<String, Object>) element.get("fact");
      Map<String, Object> document = (Map<String, Object>) element.get("document");

      return new FactAndDocument(fact, document);
    }).filter(fact -> label == null || label.equals(fact.label()));
  }

  /**
   * Load elements from raw gzipped JSONL files.
   *
   * @param facts     the 'fact' file as a gzipped JSONL file.
   * @param documents the 'document' file as a gzipped JSONL file.
   * @param label     the specific gold labels to load. If {@code label} is set to {@code null}, all gold labels will be
   *                  loaded.
   * @return a set of elements.
   */
  public static View<FactAndDocument> merge(File facts, File documents, String label) {

    Preconditions.checkNotNull(facts, "facts should not be null");
    Preconditions.checkArgument(facts.exists(), "facts file does not exist : %s", facts);
    Preconditions.checkNotNull(documents, "documents should not be null");
    Preconditions.checkArgument(documents.exists(), "documents file does not exist : %s", documents);

    // Load facts
    Map<String, List<FactAndDocument>> factsIndexedByDocId = View.of(facts, true)
        .filter(row -> !Strings.isNullOrEmpty(row) /* remove empty rows */).map(JsonCodec::asObject)
        .map(FactAndDocument::new).filter(fact -> label == null || label.equals(fact.label()))
        .groupAll(FactAndDocument::id);

    // Load documents and associate them with facts
    return Document.of(documents, true)
        .takeWhile(row -> !factsIndexedByDocId.isEmpty() /* exit as soon as all facts are associated with a document */)
        .filter(doc -> {

          // Ignore documents that are not linked to at least one fact
          return factsIndexedByDocId.containsKey(doc.docId());
        }).flatten(doc -> {

          // Remove useless document attributes
          doc.unindexedContent("bbox", null);
          doc.unindexedContent("tika", null);

          // Associate the current document with the relevant facts
          factsIndexedByDocId.get(doc.docId()).forEach(fact -> fact.document(doc));

          // Remove the processed facts from the list of facts to be processed
          return View.of(factsIndexedByDocId.remove(doc.docId()));
        });
  }

  /**
   * Save facts and documents to a gzipped JSONL file.
   *
   * @param file  the output file.
   * @param facts the facts and documents to save.
   * @return true iif the facts have been written to the file, false otherwise.
   */
  @CanIgnoreReturnValue
  public static boolean save(File file, View<FactAndDocument> facts) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkNotNull(facts, "facts should not be null");

    if (!file.exists()) {
      facts.toFile(JsonCodec::asString, file, false, true);
      return true;
    }
    return false;
  }

  /**
   * Returns each accepted or rejected fact's underlying page as a gold label.
   *
   * @param facts some facts and documents.
   * @return a set of gold labels.
   */
  public static View<GoldLabel> pagesAsGoldLabels(View<FactAndDocument> facts) {

    Preconditions.checkNotNull(facts, "facts should not be null");

    return facts.filter(fact -> fact.isAccepted() || fact.isRejected())
        .filter(fact -> !Strings.isNullOrEmpty(fact.matchedPage())).map(FactAndDocument::pageAsGoldLabel);
  }

  /**
   * Returns each accepted or rejected fact as a gold label.
   *
   * @param facts  some facts and documents.
   * @param resize true iif the fact should be enlarged when less than 300 characters, false otherwise.
   * @return a set of gold labels.
   */
  public static View<GoldLabel> factsAsGoldLabels(View<FactAndDocument> facts, boolean resize) {

    Preconditions.checkNotNull(facts, "facts should not be null");

    return facts.filter(fact -> fact.isAccepted() || fact.isRejected())
        .filter(fact -> !Strings.isNullOrEmpty(fact.fact())).map(fact -> fact.factAsGoldLabel(resize));
  }

  /**
   * For each accepted fact returns the unmatched pages as 'true negative' gold labels.
   *
   * @param facts a set of facts and documents.
   * @return a set of gold labels.
   */
  public static View<GoldLabel> syntheticPagesAsGoldLabels(View<FactAndDocument> facts) {

    Preconditions.checkNotNull(facts, "facts should not be null");

    return facts.filter(FactAndDocument::isAccepted).flatten(fact -> View.of(fact.syntheticPagesAsGoldLabels()));
  }

  /**
   * For each accepted fact returns a single random span from each unmatched pages as 'true negative' gold labels.
   *
   * @param facts a set of facts and documents.
   * @return a set of gold labels.
   */
  public static View<GoldLabel> syntheticFactsAsGoldLabels(View<FactAndDocument> facts) {

    Preconditions.checkNotNull(facts, "facts should not be null");

    return facts.filter(FactAndDocument::isAccepted).flatten(fact -> View.of(fact.syntheticFactsAsGoldLabels()));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof FactAndDocument)) {
      return false;
    }
    FactAndDocument fact = (FactAndDocument) obj;
    return Objects.equals(fact_, fact.fact_) && Objects.equals(document_, fact.document_);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fact_, document_);
  }

  @Generated
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("fact", fact_).add("document", document_).omitNullValues().toString();
  }

  /**
   * Returns the fact's underlying document (if any).
   *
   * @return a well-formed document if any, an exception otherwise.
   */
  public Document document() {

    Preconditions.checkState(document_ != null, "document should not be null");

    return new Document(document_);
  }

  /**
   * Returns the fact's underlying document identifier (if any).
   *
   * @return the fact's underlying document identifier if any, an empty string otherwise.
   */
  public String id() {
    return source().filter(map -> map.containsKey("doc_id")).map(map -> (String) map.get("doc_id")).orElse("");
  }

  /**
   * Returns the extracted fact name.
   *
   * @return the fact name.
   */
  public String label() {
    return (String) fact_.getOrDefault("type", "");
  }

  /**
   * Returns the page that contains the extracted fact.
   *
   * @return a single page if any, an empty string otherwise.
   */
  public String matchedPage() {
    return pages().map(pages -> page().filter(page -> page > 0 && page <= pages.size())
        .map(page -> pages.get(page - 1 /* page is 1-based */)).orElse("")).orElse("");
  }

  /**
   * Returns the list of pages that do not contain the extracted fact.
   *
   * @return a list of pages if any, an empty list otherwise.
   */
  public List<String> unmatchedPages() {
    return pages().map(pages -> {

      List<String> newPages = new ArrayList<>(pages);
      page().filter(page -> page > 0 && page <= newPages.size())
          .ifPresent(page -> newPages.remove(page - 1 /* page is 1-based */));

      return newPages;
    }).orElse(new ArrayList<>());
  }

  /**
   * Returns the extracted fact i.e. the span of text extracted from {@link #matchedPage()}.
   * <p>
   * Note that it does not imply that {@code matchedPage().indexOf(snippet()) >= 0}.
   *
   * @return a text fragment if any, an empty string otherwise.
   */
  public String fact() {
    return provenance().filter(
            map -> startIndex().isPresent() && endIndex().isPresent() && map.containsKey("string_span"))
        .map(map -> (String) map.get("string_span")).map(span -> {

          int begin = Math.max(0, startIndex().orElse(0));
          int end = Math.min(span.length(), endIndex().orElse(span.length()));
          String fact = span.substring(begin, end);

          return fact;
        }).orElse("");
  }

  /**
   * Returns true iif the fact has been accepted.
   *
   * @return true if the fact has been accepted, false otherwise.
   */
  public boolean isAccepted() {
    Boolean isValid = (Boolean) fact_.get("is_valid");
    return isValid != null && isValid;
  }

  /**
   * Returns true iif the fact has been rejected.
   *
   * @return true if the fact has been rejected, false otherwise.
   */
  public boolean isRejected() {
    Boolean isValid = (Boolean) fact_.get("is_valid");
    return isValid != null && !isValid;
  }

  /**
   * Returns true iif the fact should be verified.
   *
   * @return true if the fact should be verified, false otherwise.
   */
  public boolean isVerified() {
    Boolean isValid = (Boolean) fact_.get("is_valid");
    return isValid != null;
  }

  /**
   * Returns the fact's underlying page as a gold label.
   *
   * @return a gold label.
   */
  public GoldLabel pageAsGoldLabel() {

    Preconditions.checkState(isAccepted() || isRejected(), "unverified facts cannot be treated as gold labels");
    Preconditions.checkState(!Strings.isNullOrEmpty(matchedPage()), "empty pages cannot be used as as gold labels");

    return new GoldLabel(id(), label(), matchedPage(), isRejected(), isAccepted(), false, false);
  }

  /**
   * Returns the fact as a gold label.
   *
   * @param resize true iif the fact should be enlarged when less than 300 characters, false otherwise.
   * @return a gold label.
   */
  public GoldLabel factAsGoldLabel(boolean resize) {

    Preconditions.checkState(isAccepted() || isRejected(), "unverified facts cannot be treated as gold labels");

    int minLength = 300;
    String fact = fact();
    String page = matchedPage();

    if (!resize || fact.length() >= minLength || !page.contains(fact)) {
      return new GoldLabel(id(), label(), fact, isRejected(), isAccepted(), false, false);
    }

    int begin = Math.max(0, page.indexOf(fact) - 50);
    int end = Math.min(page.length(), begin + minLength);
    String newFact = page.substring(begin, end);

    return new GoldLabel(id(), label(), newFact, isRejected(), isAccepted(), false, false);
  }

  /**
   * If the current fact has been accepted, returns unmatched pages as 'true negative' gold labels.
   *
   * @return a set of synthetic gold labels.
   */
  public Set<GoldLabel> syntheticPagesAsGoldLabels() {

    Preconditions.checkState(isAccepted(),
        "unverified or rejected facts cannot be used to create synthetic gold labels");

    return unmatchedPages().stream().filter(page -> !Strings.isNullOrEmpty(page))
        .map(page -> new GoldLabel(id(), label(), page, true, false, false, false)).collect(Collectors.toSet());
  }

  /**
   * If the current fact has been accepted, returns a single random span from unmatched pages as 'true negative' gold
   * labels.
   *
   * @return a set of synthetic gold labels.
   */
  public Set<GoldLabel> syntheticFactsAsGoldLabels() {

    Preconditions.checkState(isAccepted(),
        "unverified or rejected facts cannot be used to create synthetic gold labels");

    Random random = new Random();
    return unmatchedPages().stream().filter(page -> !Strings.isNullOrEmpty(page)).filter(page -> page.length() > 300)
        .map(page -> {
          int begin = random.nextInt(page.length() - 300);
          int end = begin + 300;
          return new GoldLabel(id(), label(), page.substring(begin, end), true, false, false, false);
        }).collect(Collectors.toSet());
  }

  /**
   * Set the fact's underlying document.
   *
   * @param document a well-formed document.
   */
  private void document(Document document) {

    Preconditions.checkNotNull(document, "document should not be null");

    document_ = document.json();
  }

  private Optional<List<String>> pages() {
    return Optional.ofNullable(document_).map(doc -> document()).map(doc -> (String) doc.text())
        .map(text -> Splitter.on(FORM_FEED).splitToList(text));
  }

  private Optional<Integer> page() {
    return values().map(list -> {
      if (list.size() == 5 /* vam */) {
        try {
          return Integer.parseInt(list.get(1), 10);
        } catch (NumberFormatException e) {
          return 0;
        }
      }
      if (list.size() == 3 /* dab */) {
        try {
          return Integer.parseInt(list.get(2), 10);
        } catch (NumberFormatException e) {
          return 0;
        }
      }
      return 0;
    }).filter(page -> page > 0 /* page is 1-based */);
  }

  private Optional<Integer> startIndex() {
    return provenance().filter(map -> map.containsKey("start_index")).map(map -> (Integer) map.get("start_index"));
  }

  private Optional<Integer> endIndex() {
    return provenance().filter(map -> map.containsKey("end_index")).map(map -> (Integer) map.get("end_index"));
  }

  @SuppressWarnings("unchecked")
  private Optional<List<String>> values() {
    return Optional.ofNullable((List<String>) fact_.get("values"));
  }

  @SuppressWarnings("unchecked")
  private Optional<Map<String, Object>> source() {
    return provenance().filter(map -> map.containsKey("source")).map(map -> (Map<String, Object>) map.get("source"));
  }

  private Optional<Map<String, Object>> provenance() {
    return provenances().filter(list -> !list.isEmpty()).map(list -> list.get(0));
  }

  @SuppressWarnings("unchecked")
  private Optional<List<Map<String, Object>>> provenances() {
    return Optional.ofNullable((List<Map<String, Object>>) fact_.get("provenances"));
  }
}
