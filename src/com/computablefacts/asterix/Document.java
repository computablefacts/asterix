package com.computablefacts.asterix;

import com.computablefacts.Generated;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.computablefacts.junon.Fact;
import com.computablefacts.junon.Provenance;
import com.computablefacts.logfmt.LogFormatter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CheckReturnValue
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
final public class Document {

  public static final String ID_MAGIC_KEY = "_id";
  public static final String PATH = "path";
  public static final String TEXT = "text";
  public static final String METADATA = "metadata";
  public static final String CONTENT = "content";
  public static final String FACTS = "facts";
  public static final String CONTENT_TYPE = "content_type";
  public static final String PRODUCER = "producer";
  public static final String NB_PAGES = "nb_pages";
  public static final String CREATOR = "creator";
  public static final String CREATION_DATE = "creation_date";
  public static final String MODIFICATION_DATE = "modification_date";
  public static final String AUTHOR = "author";
  public static final String TITLE = "title";
  public static final String SUBJECT = "subject";
  public static final String DESCRIPTION = "description";
  public static final String LANGUAGE = "language";
  private static final Logger logger_ = LoggerFactory.getLogger(Document.class);
  @JsonProperty(value = "doc_id")
  private final String docId_;
  @JsonProperty(value = "metadata")
  private final Map<String, Object> metadata_ = new HashMap<>();
  @JsonProperty(value = "content")
  private final Map<String, Object> content_ = new HashMap<>();
  @JsonProperty(value = "facts")
  private final List<Fact> facts_ = new ArrayList<>();

  public Document(String docId) {
    docId_ = Preconditions.checkNotNull(docId, "docId should not be null");
  }

  // public Document(String json) {
  // this(Codecs.asObject(Preconditions.checkNotNull(json, "json should not be null")));
  // }

  @SuppressWarnings("unchecked")
  public Document(Map<String, Object> json) {

    Preconditions.checkNotNull(json, "json should not be null");
    Preconditions.checkState(json.containsKey(ID_MAGIC_KEY), "Missing id in %s", json);

    docId_ = (String) json.get(ID_MAGIC_KEY);

    Preconditions.checkState(json.containsKey(METADATA), "Missing metadata in %s", json);

    metadata_.putAll((Map<String, Object>) json.get(METADATA));

    if (!metadata_.containsKey(CONTENT_TYPE)) { // TODO : remove ASAP

      // Default content-type is "application/pdf"
      metadata_.put(CONTENT_TYPE, "application/pdf");
    }

    Preconditions.checkState(json.containsKey(CONTENT), "Missing content in %s", json);

    content_.putAll((Map<String, Object>) json.get(CONTENT));
  }

  @JsonCreator
  public Document(@JsonProperty(value = "doc_id") String docId,
      @JsonProperty(value = "metadata") Map<String, Object> metadata,
      @JsonProperty(value = "content") Map<String, Object> content, @JsonProperty(value = "facts") List<Fact> facts) {

    Preconditions.checkNotNull(docId, "docId should not be null");

    docId_ = docId;

    if (metadata != null) {
      metadata_.putAll(metadata);
    }
    if (content != null) {
      content_.putAll(content);
    }
    if (facts != null) {
      facts_.addAll(facts);
    }
  }

  /**
   * Load a corpus of documents.
   *
   * @param file      the corpus of documents as a gzipped JSONL file.
   * @param onlyTexts true iif non-textual documents (ex. JSON objects) must be filtered out.
   * @return a {@link View} over the corpus of documents.
   */
  public static View<Document> of(File file, boolean onlyTexts) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file file does not exist : %s", file);

    return View.of(file, true).filter(row -> !Strings.isNullOrEmpty(row) /* remove empty rows */).map(row -> {
      try {
        return new Document(JsonCodec.asObject(row));
      } catch (Exception ex) {
        logger_.error(LogFormatter.create().message(ex).add("line_number", row).formatError());
      }
      return new Document("UNK");
    }).filter(doc -> {

      // Ignore documents when an exception occurred
      if ("UNK".equals(doc.docId())) {
        return false;
      }

      // Ignore empty documents
      if (doc.isEmpty()) {
        return false;
      }

      // Ignore non-textual files
      if (onlyTexts && !(doc.text() instanceof String)) {
        return false;
      }
      return true;
    });
  }

  /**
   * Load a corpus of documents.
   *
   * @param documents the 'document' file as a gzipped JSONL file.
   * @param facts     the 'fact' file as a gzipped JSONL file.
   * @return a {@link View} over the corpus of documents.
   */
  public static View<Document> of(File documents, File facts) {

    Preconditions.checkNotNull(documents, "documents should not be null");
    Preconditions.checkArgument(documents.exists(), "documents file does not exist : %s", documents);
    Preconditions.checkNotNull(facts, "facts should not be null");
    Preconditions.checkArgument(facts.exists(), "facts file does not exist : %s", facts);

    // Load facts
    Map<String, List<Fact>> factsIndexedByDocId = View.of(facts, true)
        .filter(row -> !Strings.isNullOrEmpty(row) /* remove empty rows */).map(JsonCodec::asObject)
        .map(Fact::fromLegacy).filter(fact -> fact != null && fact.isVerified())
        .groupAll(fact -> fact.provenance().docId());

    // Load documents and associate them with the loaded facts
    return of(documents, true).takeWhile(
            doc -> !factsIndexedByDocId.isEmpty() /* exit as soon as all facts are associated with a document */)
        .filter(doc -> {

          // Ignore documents that are not linked to at least one fact
          return factsIndexedByDocId.containsKey(doc.docId());
        }).map(doc -> {

          // Remove useless document attributes
          doc.unindexedContent("bbox", null);
          doc.unindexedContent("tika", null);

          // Associate the current document with the relevant facts
          doc.facts_.addAll(View.of(factsIndexedByDocId.get(doc.docId())).map(fact -> {

            int page = fact.provenance().page();
            List<String> pages = Splitter.on('\f').splitToList((String) doc.text());

            Preconditions.checkState(0 < page && page <= pages.size());
            Preconditions.checkState(pages.get(page - 1).contains(fact.provenance().span()));

            Provenance newProvenance = new Provenance(fact.provenance().sourceStore(), fact.provenance().sourceType(),
                fact.provenance().sourceReliability(), pages.get(page - 1), fact.provenance().span(),
                fact.provenance().startIndex(), fact.provenance().endIndex(), fact.provenance().extractionDate(),
                fact.provenance().modificationDate(), fact.provenance().spanHash(), page);

            return new Fact(fact.externalId_, fact.metadata(), Lists.newArrayList(newProvenance), fact.values(),
                fact.type(), fact.isValid(), fact.authorizations(), fact.confidenceScore(), fact.startDate(),
                fact.endDate());
          }).toList());

          // Remove the processed facts from the list of facts to be processed
          factsIndexedByDocId.remove(doc.docId());

          return doc;
        });
  }

  @Generated
  @Override
  public String toString() {
    return JsonCodec.asString(json());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Document)) {
      return false;
    }
    Document other = (Document) obj;
    return Objects.equal(docId_, other.docId_) && Objects.equal(metadata_, other.metadata_) && Objects.equal(content_,
        other.content_) && Objects.equal(facts_, other.facts_);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(docId_, metadata_, content_, facts_);
  }

  public Map<String, Object> json() {

    Map<String, Object> json = new HashMap<>();
    json.put(ID_MAGIC_KEY, docId_);
    json.put(METADATA, metadata_);
    json.put(CONTENT, content_);

    if (!facts_.isEmpty()) {
      json.put(FACTS, facts_);
    }
    return json;
  }

  public boolean isEmpty() {
    return metadata_.isEmpty() && content_.isEmpty() && facts_.isEmpty();
  }

  public String docId() {
    return docId_;
  }

  public boolean isJsonArray() {
    return "json_array".equals(metadata_.get("cf_text_type"));
  }

  public boolean isJsonObject() {
    return "json_object".equals(metadata_.get("cf_text_type"));
  }

  public boolean isText() {
    return "text".equals(metadata_.get("cf_text_type"));
  }

  public void setJsonArrayType() {
    metadata_.put("cf_text_type", "json_array");
  }

  public void setJsonObjectType() {
    metadata_.put("cf_text_type", "json_object");
  }

  public void setTextType() {
    metadata_.put("cf_text_type", "text");
  }

  public String originalFile() {
    return Strings.nullToEmpty((String) metadata_.get("cf_original_file"));
  }

  public String originalFileType() {
    return Strings.nullToEmpty((String) metadata_.get("cf_original_file_type"));
  }

  public String transformedFile() {
    return Strings.nullToEmpty((String) metadata_.get("cf_transformed_file"));
  }

  public String transformedFileType() {
    return Strings.nullToEmpty((String) metadata_.get("cf_transformed_file_type"));
  }

  public void originalFile(String file, String extension) {
    if (!Strings.isNullOrEmpty(file) && !Strings.isNullOrEmpty(extension)) {
      metadata_.put("cf_original_file", file);
      metadata_.put("cf_original_file_type", extension);
    }
  }

  public void transformedFile(String file, String extension) {
    if (!Strings.isNullOrEmpty(file) && !Strings.isNullOrEmpty(extension)) {
      metadata_.put("cf_transformed_file", file);
      metadata_.put("cf_transformed_file_type", extension);
    }
  }

  public void metadata(Map<String, String> obj) {
    if (obj != null) {
      metadata_.putAll(obj);
    }
  }

  public Map<String, Object> metadata() {
    return new HashMap<>(metadata_);
  }

  public void content(Map<String, Object> obj) {
    if (obj != null) {
      content_.putAll(obj);
    }
  }

  public Map<String, Object> content() {
    return new HashMap<>(content_);
  }

  public List<Fact> facts() {
    return ImmutableList.copyOf(facts_);
  }

  public void contentType(String obj) {
    if (obj != null) {
      metadata_.put(CONTENT_TYPE, obj);
    }
  }

  public String contentType() {
    return metadata_.containsKey(CONTENT_TYPE) ? (String) metadata_.get(CONTENT_TYPE) : null;
  }

  public void producer(String obj) {
    if (obj != null) {
      metadata_.put(PRODUCER, obj);
    }
  }

  public String producer() {
    return metadata_.containsKey(PRODUCER) ? (String) metadata_.get(PRODUCER) : null;
  }

  public void nbPages(String obj) {
    if (obj != null) {
      metadata_.put(NB_PAGES, obj);
    }
  }

  public String nbPages() {
    return metadata_.containsKey(NB_PAGES) ? (String) metadata_.get(NB_PAGES) : null;
  }

  public void creator(String obj) {
    if (obj != null) {
      metadata_.put(CREATOR, obj);
    }
  }

  public String creator() {
    return metadata_.containsKey(CREATOR) ? (String) metadata_.get(CREATOR) : null;
  }

  public void creationDate(String obj) {
    if (obj != null) {
      metadata_.put(CREATION_DATE, obj);
    }
  }

  public String creationDate() {
    return metadata_.containsKey(CREATION_DATE) ? (String) metadata_.get(CREATION_DATE) : null;
  }

  public void modificationDate(String obj) {
    if (obj != null) {
      metadata_.put(MODIFICATION_DATE, obj);
    }
  }

  public String modificationDate() {
    return metadata_.containsKey(MODIFICATION_DATE) ? (String) metadata_.get(MODIFICATION_DATE) : null;
  }

  public void author(String obj) {
    if (obj != null) {
      metadata_.put(AUTHOR, obj);
    }
  }

  public String author() {
    return metadata_.containsKey(AUTHOR) ? (String) metadata_.get(AUTHOR) : null;
  }

  public void title(String obj) {
    if (obj != null) {
      metadata_.put(TITLE, obj);
    }
  }

  public String title() {
    return metadata_.containsKey(TITLE) ? (String) metadata_.get(TITLE) : null;
  }

  public void subject(String obj) {
    if (obj != null) {
      metadata_.put(SUBJECT, obj);
    }
  }

  public String subject() {
    return metadata_.containsKey(SUBJECT) ? (String) metadata_.get(SUBJECT) : null;
  }

  public void description(String obj) {
    if (obj != null) {
      metadata_.put(DESCRIPTION, obj);
    }
  }

  public String description() {
    return metadata_.containsKey(DESCRIPTION) ? (String) metadata_.get(DESCRIPTION) : null;
  }

  public void language(String obj) {
    if (obj != null) {
      metadata_.put(LANGUAGE, obj);
    }
  }

  public String language() {
    return metadata_.containsKey(LANGUAGE) ? (String) metadata_.get(LANGUAGE) : null;
  }

  public void path(String obj) {
    if (obj != null) {
      metadata_.put(PATH, obj);
    }
  }

  public String path() {
    return metadata_.containsKey(PATH) ? (String) metadata_.get(PATH) : null;
  }

  public void text(Object obj) {
    content_.put(TEXT, obj);
  }

  public Object text() {
    return content_.get(TEXT);
  }

  public boolean fileExists() {
    return new File(Strings.nullToEmpty(path())).exists();
  }

  public void indexedContent(String key, Object obj) {

    // Attributes starting with an underscore aren't indexed
    content_.put(key.startsWith("_") ? key.substring(1) : key, obj);
  }

  public Object indexedContent(String key) {
    return content_.get(key.startsWith("_") ? key.substring(1) : key);
  }

  public void unindexedContent(String key, Object obj) {

    // Attributes starting with an underscore aren't indexed
    content_.put(key.startsWith("_") ? key : "_" + key, obj);
  }

  public Object unindexedContent(String key) {
    return content_.get(key.startsWith("_") ? key : "_" + key);
  }
}
