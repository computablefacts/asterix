package com.computablefacts.asterix;

import static com.computablefacts.asterix.Document.ID_MAGIC_KEY;

import com.computablefacts.asterix.codecs.JsonCodec;
import com.computablefacts.asterix.nlp.SnippetExtractor;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Test;

public class DocumentTest {

  public static View<Document> papers() {
    File input = new File("test/resources/papers.jsonl.gz");
    return View.of(input, true).map(JsonCodec::asObject).map(json -> {

      String id = json.getOrDefault("Id", "").toString();
      String title = (String) json.getOrDefault("Title", "");
      String eventType = (String) json.getOrDefault("EventType", "");
      String pdfName = (String) json.getOrDefault("PdfName", "");
      String paperAbstract = (String) json.getOrDefault("Abstract", "");
      String paperText = (String) json.getOrDefault("PaperText", "");

      Map<String, String> metadata = new HashMap<>();
      metadata.put("dataset", "NIPS2015 papers");
      metadata.put("event_type", eventType);
      metadata.put("abstract", paperAbstract);

      Document document = new Document(id);
      document.setTextType();
      document.title(title);
      document.path(pdfName);
      document.metadata(metadata);
      document.text(paperText);

      return document;
    });
  }

  public static File facts() throws Exception {

    // Use a random directory because FactAndDocument.main() uses it
    String path = Files.createTempDirectory("").toFile().getAbsolutePath();
    File facts = new File(path + File.separator + "facts.jsonl.gz");

    papers().index().flatten(e -> {

      int id = e.getKey();
      String docId = e.getValue().docId();
      String type = "contains_crowdsourcing";
      Pattern pattern = Pattern.compile(".*(crowdsourcing).*",
          Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

      List<Map<String, Object>> factz = new ArrayList<>();
      List<String> pages = Splitter.on('\f').splitToList((String) e.getValue().text());

      for (int i = 0; i < pages.size(); i++) {

        String page = pages.get(i);
        Matcher matcher = pattern.matcher(page);

        while (matcher.find()) {

          int begin = matcher.start(1);
          int end = matcher.end(1);
          String span = page.substring(begin, end);
          String snippet = SnippetExtractor.extract(Lists.newArrayList(span), page, 300, 50, "");
          Map<String, Object> fact = newFact(id, type, docId, i + 1, page, page.indexOf(snippet),
              page.indexOf(snippet) + snippet.length());

          factz.add(fact);
        }
      }
      return View.of(factz);
    }).map(JsonCodec::asString).toFile(facts, false, true);

    return facts;
  }

  public static File documents() throws Exception {

    // Use a random directory because FactAndDocument.main() uses it
    String path = Files.createTempDirectory("").toFile().getAbsolutePath();
    File documents = new File(path + File.separator + "documents.jsonl.gz");

    papers().map(doc -> {

      Map<String, Object> json = doc.json();
      json.put(ID_MAGIC_KEY, doc.docId());
      return json;
    }).map(JsonCodec::asString).toFile(documents, false, true);

    return documents;
  }

  private static Map<String, Object> newFact(int id, String type, String docId, int page, String text, int startIdx,
      int endIdx) {

    List<String> values = Lists.newArrayList("ref0", "ref1", Integer.toString(page, 10));

    Map<String, Object> source = new HashMap<>();
    source.put("storage", "ACCUMULO");
    source.put("root", "cf_prod");
    source.put("dataset", "papers");
    source.put("doc_id", docId);

    Map<String, Object> provenance = new HashMap<>();
    provenance.put("source_store", "ACCUMULO/cf_prod/papers/" + docId);
    provenance.put("source_type", "STORAGE/ROOT/DATASET/DOC_ID");
    provenance.put("string_span", text);
    provenance.put("string_span_hash", null);
    provenance.put("start_index", startIdx);
    provenance.put("end_index", endIdx);
    provenance.put("source", source);
    provenance.put("page", page);

    Map<String, Object> fact = new HashMap<>();
    fact.put("id", id);
    fact.put("type", type);
    fact.put("values", values);
    fact.put("is_valid", true);
    fact.put("confidence_score", 1.0);
    fact.put("external_id", null);
    fact.put("start_date", Instant.now().toString());
    fact.put("end_date", null);
    fact.put("metadata", Lists.newArrayList());
    fact.put("provenances", Lists.newArrayList(provenance));

    return fact;
  }

  @Test
  public void testNewDocumentWithDocId() {
    Document document = new Document(docId());
    Assert.assertEquals(docId(), document.docId());
    Assert.assertTrue(document.isEmpty());
  }

  @Test(expected = NullPointerException.class)
  public void testNewDocumentWithoutDocId() {
    Document document = new Document((String) null);
  }

  @Test(expected = IllegalStateException.class)
  public void testMissingId() {

    Map<String, Object> doc = new HashMap<>();
    doc.put(Document.METADATA, metadata());
    doc.put(Document.CONTENT, content());

    Document document = new Document(doc);
  }

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Document.class).verify();
  }

  @Test
  public void testMissingContentType() {

    Map<String, String> metadata = metadata();
    metadata.remove("content_type");

    Map<String, Object> doc = new HashMap<>();
    doc.put(Document.ID_MAGIC_KEY, docId());
    doc.put(Document.METADATA, metadata);
    doc.put(Document.CONTENT, content());

    Assert.assertFalse(((Map<String, Object>) doc.get(Document.METADATA)).containsKey("content_type"));

    Document document = new Document(doc);

    Assert.assertEquals("application/pdf", document.contentType());
  }

  @Test
  public void testToJson() {
    Assert.assertEquals(document(), new Document(document()).json());
  }

  @Test
  public void testIsJsonArray() {

    Document document = new Document(document());
    document.setJsonArrayType();

    Assert.assertTrue(document.isJsonArray());
    Assert.assertFalse(document.isJsonObject());
    Assert.assertFalse(document.isText());
  }

  @Test
  public void testIsJsonObject() {

    Document document = new Document(document());
    document.setJsonObjectType();

    Assert.assertFalse(document.isJsonArray());
    Assert.assertTrue(document.isJsonObject());
    Assert.assertFalse(document.isText());
  }

  @Test
  public void testIsText() {

    Document document = new Document(document());
    document.setTextType();

    Assert.assertFalse(document.isJsonArray());
    Assert.assertFalse(document.isJsonObject());
    Assert.assertTrue(document.isText());
  }

  @Test
  public void testOriginalFile() {

    Document document = new Document(document());
    document.originalFile("lorem-ipsum", "pdf");

    Assert.assertEquals("lorem-ipsum", document.originalFile());
    Assert.assertEquals("pdf", document.originalFileType());
  }

  @Test
  public void testTransformedFile() {

    Document document = new Document(document());
    document.transformedFile("lorem-ipsum", "pdf");

    Assert.assertEquals("lorem-ipsum", document.transformedFile());
    Assert.assertEquals("pdf", document.transformedFileType());
  }

  @Test
  public void testSetNullMetadata() {

    Map<String, Object> doc = new HashMap<>();
    doc.put(Document.ID_MAGIC_KEY, docId());
    doc.put(Document.METADATA, new HashMap<>());
    doc.put(Document.CONTENT, content());

    Document document = new Document(doc);
    document.metadata(null);

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("content_type", "application/pdf");

    Assert.assertEquals(metadata, document.metadata());
  }

  @Test
  public void testSetGetMetadata() {

    Map<String, Object> doc = new HashMap<>();
    doc.put(Document.ID_MAGIC_KEY, docId());
    doc.put(Document.METADATA, new HashMap<>());
    doc.put(Document.CONTENT, content());

    Document document = new Document(doc);
    document.metadata(metadata());

    Assert.assertEquals(metadata(), document.metadata());
  }

  @Test
  public void testSetNullContent() {

    Map<String, Object> doc = new HashMap<>();
    doc.put(Document.ID_MAGIC_KEY, docId());
    doc.put(Document.METADATA, metadata());
    doc.put(Document.CONTENT, new HashMap<>());

    Document document = new Document(doc);
    document.content(null);

    Assert.assertEquals(new HashMap<>(), document.content());
  }

  @Test
  public void testSetGetContent() {

    Map<String, Object> doc = new HashMap<>();
    doc.put(Document.ID_MAGIC_KEY, docId());
    doc.put(Document.METADATA, metadata());
    doc.put(Document.CONTENT, new HashMap<>());

    Document document = new Document(doc);
    document.content(content());

    Assert.assertEquals(content(), document.content());
  }

  @Test
  public void testSetGetContentType() {

    Document document = new Document(document());
    document.contentType("application/msword");

    Assert.assertEquals("application/msword", document.contentType());
  }

  @Test
  public void testSetGetProducer() {

    Document document = new Document(document());
    document.producer("PDFsharp 1.50.666 / www.pdfsharp.com");

    Assert.assertEquals("PDFsharp 1.50.666 / www.pdfsharp.com", document.producer());
  }

  @Test
  public void testSetGetNbPages() {

    Document document = new Document(document());
    document.nbPages("42");

    Assert.assertEquals("42", document.nbPages());
  }

  @Test
  public void testSetGetCreator() {

    Document document = new Document(document());
    document.creator("John Doe");

    Assert.assertEquals("John Doe", document.creator());
  }

  @Test
  public void testCreationDate() {

    Document document = new Document(document());
    document.creationDate("2019-09-07T14:57:07Z");

    Assert.assertEquals("2019-09-07T14:57:07Z", document.creationDate());
  }

  @Test
  public void testSetGetModificationDate() {

    Document document = new Document(document());
    document.modificationDate("2019-10-07T14:57:10Z");

    Assert.assertEquals("2019-10-07T14:57:10Z", document.modificationDate());
  }

  @Test
  public void testSetGetAuthor() {

    Document document = new Document(document());
    document.author("J. Doe");

    Assert.assertEquals("J. Doe", document.author());
  }

  @Test
  public void testSetGetTitle() {

    Document document = new Document(document());
    document.title("Lorem Ipsum");

    Assert.assertEquals("Lorem Ipsum", document.title());
  }

  @Test
  public void testSetGetSubject() {

    Document document = new Document(document());
    document.subject("Lorem Ipsum");

    Assert.assertEquals("Lorem Ipsum", document.subject());
  }

  @Test
  public void testSetGetDescription() {

    Document document = new Document(document());
    document.description("Lorem Ipsum");

    Assert.assertEquals("Lorem Ipsum", document.description());
  }

  @Test
  public void testSetGetLanguage() {

    Document document = new Document(document());
    document.language("en");

    Assert.assertEquals("en", document.language());
  }

  @Test
  public void testSetGetPath() {

    Document document = new Document(document());
    document.path("/var/sftp/sentinel/prod_out/test/lorem-ipsum.processed.docx");

    Assert.assertEquals("/var/sftp/sentinel/prod_out/test/lorem-ipsum.processed.docx", document.path());
  }

  @Test
  public void testSetGetText() {

    Document document = new Document(document());
    document.text(
        "Le Lorem Ipsum est simplement du faux texte employé dans la composition et la mise en page avant impression.");

    Assert.assertEquals(
        "Le Lorem Ipsum est simplement du faux texte employé dans la composition et la mise en page avant impression.",
        document.text());
  }

  @Test
  public void testSetGetIndexedContent() {

    Document document = new Document(document());
    document.indexedContent("_icontent1", "Indexed content n°1.");
    document.indexedContent("icontent2", "Indexed content n°2.");

    Assert.assertEquals("Indexed content n°1.", document.indexedContent("_icontent1"));
    Assert.assertEquals("Indexed content n°1.", document.indexedContent("icontent1"));

    Assert.assertEquals("Indexed content n°2.", document.indexedContent("_icontent2"));
    Assert.assertEquals("Indexed content n°2.", document.indexedContent("icontent2"));

    Assert.assertNull(document.unindexedContent("_icontent1"));
    Assert.assertNull(document.unindexedContent("icontent1"));

    Assert.assertNull(document.unindexedContent("_icontent2"));
    Assert.assertNull(document.unindexedContent("icontent2"));
  }

  @Test
  public void testSetGetUnindexedContent() {

    Document document = new Document(document());
    document.unindexedContent("_ucontent1", "Unindexed content n°1.");
    document.unindexedContent("ucontent2", "Unindexed content n°2.");

    Assert.assertEquals("Unindexed content n°1.", document.unindexedContent("_ucontent1"));
    Assert.assertEquals("Unindexed content n°1.", document.unindexedContent("ucontent1"));

    Assert.assertEquals("Unindexed content n°2.", document.unindexedContent("_ucontent2"));
    Assert.assertEquals("Unindexed content n°2.", document.unindexedContent("ucontent2"));

    Assert.assertNull(document.indexedContent("_ucontent1"));
    Assert.assertNull(document.indexedContent("ucontent1"));

    Assert.assertNull(document.indexedContent("_ucontent2"));
    Assert.assertNull(document.indexedContent("ucontent2"));
  }

  @Test
  public void testIfFileExists() throws IOException {

    List<String> keywords = Lists.newArrayList("sugar");
    Path file = Files.createTempFile("dico-", ".txt");
    Files.write(file, keywords);

    Document document = new Document(document());

    Assert.assertEquals("/var/sftp/sentinel/prod_out/test/doc.processed.pdf", document.path());

    document.path(file.toString());

    Assert.assertTrue(document.fileExists());
  }

  @Test
  public void testSerializationDeserialization() throws Exception {

    List<Document> list = Document.of(documents(), true).toList();
    List<String> serialized = View.of(list).map(JsonCodec::asString).toList();
    List<Document> deserialized = View.of(serialized).map(json -> JsonCodec.objectToDto(Document.class, json))
        .filter(Result::isSuccess).map(Result::successValue).toList();

    Assert.assertEquals(list, deserialized);
  }

  @Test
  public void testOf() throws Exception {

    File documents = documents();
    File facts = facts();

    List<Document> list = Document.of(documents, facts).toList();

    Assert.assertEquals(15, list.size());
    Assert.assertEquals(39, View.of(list).flatten(doc -> View.of(doc.facts())).toList().size());
  }

  private Map<String, Object> document() {

    Map<String, Object> doc = new HashMap<>();
    doc.put(Document.ID_MAGIC_KEY, docId());
    doc.put(Document.METADATA, metadata());
    doc.put(Document.CONTENT, content());

    return doc;
  }

  private String docId() {
    return "c6212|2020-01-08T22:39:35.567Z";
  }

  private Map<String, String> metadata() {

    Map<String, String> metadata = new HashMap<>();
    metadata.put("nb_pages", "41");
    metadata.put("creator", "NAPS2");
    metadata.put("author", null);
    metadata.put("subject", "Image numérisée");
    metadata.put("description", "Image numérisée");
    metadata.put("md5_before", "c306a94dc50cdc8af5bfe9744f858423");
    metadata.put("language", null);
    metadata.put("creation_date", "2019-03-07T14:57:02Z");
    metadata.put("title", "Image numérisée");
    metadata.put("path", "/var/sftp/sentinel/prod_out/test/doc.processed.pdf");
    metadata.put("content_type", "application/pdf");
    metadata.put("modification_date", "2019-03-07T14:57:09Z");
    metadata.put("producer", "PDFsharp 1.50.4589 (www.pdfsharp.com)");
    metadata.put("md5_after", "d2b00e689399ae28d84d123ab34ddb08");

    return metadata;
  }

  private Map<String, Object> content() {

    Map<String, Object> content = new HashMap<>();
    content.put("_bbox",
        "<!DOCTYPE html PUBLIC \\\"-//W3C//DTD XHTML 1.0 Transitional//EN\\\" \\\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\\\"><html xmlns=\\\"http://www.w3.org/1999/xhtml\\\">...</html>\n");
    content.put("text",
        "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.");

    return content;
  }
}
