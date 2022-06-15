package com.computablefacts.asterix;

import com.computablefacts.asterix.codecs.JsonCodec;
import com.computablefacts.asterix.ml.TokenizeText;
import com.computablefacts.asterix.ml.Vocabulary;
import com.computablefacts.logfmt.LogFormatter;
import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CheckReturnValue
final public class Document {

  public static final String ID_MAGIC_KEY = "_id";
  public static final String PATH = "path";
  public static final String TEXT = "text";
  public static final String METADATA = "metadata";
  public static final String CONTENT = "content";
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
  private final String docId_;
  private final Map<String, Object> metadata_ = new HashMap<>();
  private final Map<String, Object> content_ = new HashMap<>();

  public Document(String docId) {
    docId_ = Preconditions.checkNotNull(docId, "docId should not be null");
  }

  // public Document(String json) {
  // this(Codecs.asObject(Preconditions.checkNotNull(json, "json should not be null")));
  // }

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

  /**
   * Load a corpus of documents.
   *
   * @param file the corpus of documents as a gzipped JSONL file.
   * @param onlyTexts true iif non-textual documents (ex. JSON objects) must be filtered out.
   * @return a {@link View} over the corpus of documents.
   */
  public static View<Document> of(File file, boolean onlyTexts) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file file does not exist : %s", file);

    return View.of(file, true).filter(row -> !Strings.isNullOrEmpty(row) /* remove empty rows */)
        .map(row -> {
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
   * Build a vocabulary from a corpus of documents. To be versatile, this method does not attempt to
   * remove stop words, diacritical marks or even lowercase tokens.
   * <ul>
   * <li>{@code args[0]} the corpus of documents as a gzipped JSONL file.</li>
   * <li>{@code args[1]} the threshold under which a token must be excluded from the vocabulary.</li>
   * <li>{@code args[2]} the maximum size of the {@link Vocabulary}.</li>
   * <li>{@code args[3]} the type of tokens to keep.</li>
   * <li>{@code args[4]} the ngrams length that will be part of the vocabulary: 1 = unigrams, 2 = bigrams, 3 = trigrams, etc.</li>
   * </ul>
   */
  @Beta
  @Generated
  public static void main(String[] args) {

    File file = new File(args[0]);
    int minTermFreq = Integer.parseInt(args[1], 10);
    int minDocFreq = Integer.parseInt(args[2], 10);
    int maxVocabSize = Integer.parseInt(args[3], 10);
    Set<String> includeTags = Sets.newHashSet(
        Splitter.on(',').trimResults().omitEmptyStrings().split(args[4]));
    int ngramLength = Integer.parseInt(args[5], 10);

    Preconditions.checkArgument(minTermFreq > 0, "minTermFreq must be > 0");
    Preconditions.checkArgument(minDocFreq > 0, "minDocFreq must be > 0");
    Preconditions.checkArgument(maxVocabSize > 0, "maxVocabSize must be > 0");
    Preconditions.checkArgument(!includeTags.isEmpty(), "includeTags should not be empty");
    Preconditions.checkArgument(ngramLength > 0, "ngramLength must be > 0");

    Vocabulary vocabulary;
    File vocab = new File(
        String.format("%sterms-%d.tsv.gz", file.getParent() + File.separator, ngramLength));
    File pat = new File(
        String.format("%spatterns-%d.tsv.gz", file.getParent() + File.separator, ngramLength));

    System.out.printf("Dataset is %s\n", file);
    System.out.printf("NGrams length is %d\n", ngramLength);
    System.out.printf("Min. term freq. is %d\n", minTermFreq);
    System.out.printf("Min. document freq. is %d\n", minDocFreq);
    System.out.printf("Max. vocab size is %d\n", maxVocabSize);
    System.out.printf("Included tags are %s\n", includeTags);
    System.out.println("Building vocabulary...");

    Stopwatch stopwatch = Stopwatch.createStarted();
    View<SpanSequence> documents = Document.of(file, true).take(100).displayProgress(5000)
        .map(doc -> (String) doc.text()).map(new TokenizeText());
    View<List<String>> ngrams;

    if (ngramLength == 1) {
      ngrams = documents.map(
          seq -> View.of(seq).filter(span -> !Sets.intersection(includeTags, span.tags()).isEmpty())
              .map(Span::text).toList());
    } else {
      ngrams = documents.map(
          seq -> View.of(seq).filter(span -> !Sets.intersection(includeTags, span.tags()).isEmpty())
              .map(Span::text).overlappingWindowWithStrictLength(ngramLength)
              .map(tks -> Joiner.on('_').join(tks)).toList());
    }

    vocabulary = Vocabulary.of(ngrams, minTermFreq, minDocFreq, maxVocabSize);
    vocabulary.save(vocab);
    vocabulary.patterns().save(pat);
    stopwatch.stop();

    System.out.printf("Vocabulary built in %d seconds\n", stopwatch.elapsed(TimeUnit.SECONDS));
    System.out.printf("Vocabulary size is %d\n", vocabulary.size());
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
    return Objects.equal(docId_, other.docId_) && Objects.equal(metadata_, other.metadata_)
        && Objects.equal(content_, other.content_);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(docId_, metadata_, content_);
  }

  public Map<String, Object> json() {
    Map<String, Object> json = new HashMap<>();
    json.put(ID_MAGIC_KEY, docId_);
    json.put(METADATA, metadata_);
    json.put(CONTENT, content_);
    return json;
  }

  public boolean isEmpty() {
    return metadata_.isEmpty() && content_.isEmpty();
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
    return metadata_.containsKey(MODIFICATION_DATE) ? (String) metadata_.get(MODIFICATION_DATE)
        : null;
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
