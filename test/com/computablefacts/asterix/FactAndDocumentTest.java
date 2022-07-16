package com.computablefacts.asterix;

import static com.computablefacts.asterix.Document.ID_MAGIC_KEY;

import com.computablefacts.asterix.codecs.JsonCodec;
import com.computablefacts.asterix.ml.GoldLabel;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Assert;
import org.junit.Test;

public class FactAndDocumentTest {

  public static File factsAndDocuments() throws Exception {

    // Use a random directory because FactAndDocument.main() uses it
    String path = Files.createTempDirectory("").toFile().getAbsolutePath();
    File dataset = new File(path + File.separator + "facts-and-documents.jsonl.gz");
    FactAndDocument.save(dataset, FactAndDocument.merge(facts(), documents(), null));

    return dataset;
  }

  private static File facts() throws Exception {

    // Use a random directory because FactAndDocument.main() uses it
    String path = Files.createTempDirectory("").toFile().getAbsolutePath();
    File facts = new File(path + File.separator + "facts.jsonl.gz");

    DocumentTest.papers().index().flatten(e -> {

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
    }).toFile(JsonCodec::asString, facts, false, true);

    return facts;
  }

  private static File documents() throws Exception {

    // Use a random directory because FactAndDocument.main() uses it
    String path = Files.createTempDirectory("").toFile().getAbsolutePath();
    File documents = new File(path + File.separator + "documents.jsonl.gz");

    DocumentTest.papers().map(doc -> {

      Map<String, Object> json = doc.json();
      json.put(ID_MAGIC_KEY, doc.docId());
      return json;
    }).toFile(JsonCodec::asString, documents, false, true);

    return documents;
  }

  private static Map<String, Object> newFact(int id, String type, String docId, int page,
      String text, int startIdx, int endIdx) {

    List<String> values = Lists.newArrayList("ref0", "ref1", Integer.toString(page, 10));

    Map<String, Object> source = new HashMap<>();
    source.put("storage", "ACCUMULO");
    source.put("root", "cf_prod");
    source.put("dataset", "papers");
    source.put("doc_id", docId);

    Map<String, Object> provenance = new HashMap<>();
    provenance.put("source_store", "");
    provenance.put("source_type", "");
    provenance.put("string_span", text);
    provenance.put("string_span_hash", null);
    provenance.put("start_index", startIdx);
    provenance.put("end_index", endIdx);
    provenance.put("source", source);

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
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(FactAndDocument.class).suppress(Warning.NONFINAL_FIELDS).verify();
  }

  @Test
  public void testCallCommandLine() throws Exception {

    File facts = facts();
    String[] args = new String[]{facts.getAbsolutePath(), documents().getAbsolutePath()};
    FactAndDocument.main(args);

    String path = facts.getParent();
    List<FactAndDocument> fads = FactAndDocument.load(
        new File(path + File.separator + "facts_and_documents.jsonl.gz"), null).toList();

    Assert.assertEquals(39, fads.size());
    Assert.assertEquals(39,
        FactAndDocument.factsAsGoldLabels(View.of(fads), false).toList().size());
    Assert.assertEquals(39, FactAndDocument.pagesAsGoldLabels(View.of(fads)).toList().size());
    Assert.assertEquals(316,
        FactAndDocument.syntheticFactsAsGoldLabels(View.of(fads)).toList().size());
    Assert.assertEquals(335,
        FactAndDocument.syntheticPagesAsGoldLabels(View.of(fads)).toList().size());

    List<GoldLabel> gls = GoldLabel.load(new File(path + File.separator + "gold_labels.jsonl.gz"),
        null).toList();

    Assert.assertEquals(39 + 335, gls.size());
    Assert.assertEquals(39, gls.stream().filter(GoldLabel::isTruePositive).count());
    Assert.assertEquals(335, gls.stream().filter(GoldLabel::isTrueNegative).count());
    Assert.assertEquals(0, gls.stream().filter(GoldLabel::isFalsePositive).count());
    Assert.assertEquals(0, gls.stream().filter(GoldLabel::isFalseNegative).count());
  }

  @Test
  public void testMergeThenSaveThenLoad() throws Exception {

    List<FactAndDocument> fads = FactAndDocument.load(factsAndDocuments(), null).toList();

    Assert.assertEquals(39, fads.size());
    Assert.assertEquals(39,
        FactAndDocument.factsAsGoldLabels(View.of(fads), false).toList().size());
    Assert.assertEquals(39, FactAndDocument.pagesAsGoldLabels(View.of(fads)).toList().size());
    Assert.assertEquals(316,
        FactAndDocument.syntheticFactsAsGoldLabels(View.of(fads)).toList().size());
    Assert.assertEquals(335,
        FactAndDocument.syntheticPagesAsGoldLabels(View.of(fads)).toList().size());
  }
}
