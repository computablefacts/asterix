package com.computablefacts.asterix.ml.textcategorization;

import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.KO;
import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.OK;

import com.computablefacts.asterix.DocumentTest;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.computablefacts.asterix.ml.GoldLabel;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.re2j.Pattern;
import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class TextCategorizerTest {

  @Test
  public void testCallCommandLine() throws Exception {

    Set<String> ok = new HashSet<>();
    Set<String> ko = new HashSet<>();

    Pattern pattern = Pattern.compile(".*crowdsourcing.*",
        Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    DocumentTest.papers().map(doc -> (String) doc.text()).flatten(text -> View.of(Splitter.on('\f').split(text)))
        .forEachRemaining(page -> {
          if (pattern.matches(page)) {
            ok.add(page);
          } else {
            ko.add(page);
          }
        });

    String path = Files.createTempDirectory("").toFile().getAbsolutePath();
    File file = new File(path + File.separator + "gold-labels.jsonl.gz");

    View.of(ok).zip(View.repeat(OK)).concat(View.of(ko).zip(View.repeat(KO)).sample(2 * ok.size()))
        .filter(entry -> !Strings.isNullOrEmpty(entry.getKey())).map(
            entry -> new GoldLabel("xxx", "crowdsourcing", entry.getKey(), entry.getValue() == OK, entry.getValue() == KO,
                false, false)).toFile(JsonCodec::asString, file, false, true);

    TextCategorizer.main(new String[]{file.getAbsolutePath(), "crowdsourcing"});

    Assert.assertTrue(
        new File(String.format("%stext-categorizer-crowdsourcing.xml.gz", file.getParent() + File.separator)).exists());
  }

  @Test
  public void testCategorizeShortText() {

    Fingerprint fpEn = new Fingerprint();
    fpEn.category("EN");
    sentencesEn().forEach(fpEn::add);

    Fingerprint fpDe = new Fingerprint();
    fpDe.category("DE");
    sentencesDe().forEach(fpDe::add);

    TextCategorizer categorizer = new TextCategorizer();
    categorizer.add(fpEn);
    categorizer.add(fpDe);

    String text = "Welcome";

    Assert.assertEquals("<UNK>", categorizer.categorize(text));
  }

  @Test
  public void testCategorizeLongText() {

    Fingerprint fpEn = new Fingerprint();
    fpEn.category("EN");
    sentencesEn().forEach(fpEn::add);

    Fingerprint fpDe = new Fingerprint();
    fpDe.category("DE");
    sentencesDe().forEach(fpDe::add);

    TextCategorizer categorizer = new TextCategorizer();
    categorizer.add(fpEn);
    categorizer.add(fpDe);

    String textDe = "Wenn ich ans Telefon gehe, sage ich einfach \"Hi\".";

    Assert.assertEquals("DE", categorizer.categorize(textDe));

    String textEn = "Welcome to Yahoo!, the world’s most visited home page.";

    Assert.assertEquals("EN", categorizer.categorize(textEn));
  }

  private List<String> sentencesEn() {
    return Lists.newArrayList("Welcome to Yahoo!, the world’s most visited home page.",
        "Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information.",
        "CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.");
  }

  private List<String> sentencesDe() {
    return Lists.newArrayList("Wenn ich jemanden zum ersten Mal treffe, sage ich gerne \"Hallo.\"",
        "\"Guten Tag\" ist eine Begrüßung, die man vom Mittag bis zum frühen Abend gebraucht.",
        "Hallo Peter, mein Name ist Richard und es freut mich dich kennen zu lernen.");
  }
}
