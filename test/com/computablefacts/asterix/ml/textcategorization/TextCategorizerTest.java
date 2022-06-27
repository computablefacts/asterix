package com.computablefacts.asterix.ml.textcategorization;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TextCategorizerTest {

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
