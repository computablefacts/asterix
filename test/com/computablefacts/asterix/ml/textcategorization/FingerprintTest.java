package com.computablefacts.asterix.ml.textcategorization;

import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Assert;
import org.junit.Test;

public class FingerprintTest {

  @Test
  public void testEqualsAndHashCode() {
    EqualsVerifier.forClass(Fingerprint.class).suppress(Warning.NONFINAL_FIELDS).verify();
  }

  @Test
  public void testCopyConstructor() {

    Fingerprint fpEn = new Fingerprint();
    fpEn.category("EN");
    sentencesEn().forEach(fpEn::add);

    Fingerprint fpEnCopy = new Fingerprint(fpEn);
    String text = "Wenn ich ans Telefon gehe, sage ich einfach \"Hi\".";
    Fingerprint fp = new Fingerprint();
    fp.add(text);

    Assert.assertEquals(139441, fp.distanceTo(fpEn));
    Assert.assertEquals(139441, fp.distanceTo(fpEnCopy));
  }

  @Test
  public void testDistanceTo() {

    Fingerprint fpEn = new Fingerprint();
    fpEn.category("EN");
    sentencesEn().forEach(fpEn::add);

    Fingerprint fpDe = new Fingerprint();
    fpDe.category("DE");
    sentencesDe().forEach(fpDe::add);

    String text = "Wenn ich ans Telefon gehe, sage ich einfach \"Hi\".";

    Fingerprint fp = new Fingerprint();
    fp.add(text);

    Assert.assertEquals(139441, fp.distanceTo(fpEn));
    Assert.assertEquals(53198, fp.distanceTo(fpDe));
  }

  @Test
  public void testCategorize() {

    Fingerprint fpEn = new Fingerprint();
    fpEn.category("EN");
    sentencesEn().forEach(fpEn::add);

    Fingerprint fpDe = new Fingerprint();
    fpDe.category("DE");
    sentencesDe().forEach(fpDe::add);

    List<Fingerprint> categories = Lists.newArrayList(fpEn, fpDe);

    String text = "Wenn ich ans Telefon gehe, sage ich einfach \"Hi\".";

    Fingerprint fp = new Fingerprint();
    fp.add(text);

    Map<String, Integer> distances = fp.categorize(categories);

    Assert.assertEquals(2, distances.size());
    Assert.assertEquals(139441L, (long) distances.get("EN"));
    Assert.assertEquals(53198L, (long) distances.get("DE"));
  }

  @Test
  public void testToString() {

    String text = "Wenn ich ans Telefon gehe, sage ich einfach \"Hi\".";

    Fingerprint fp = new Fingerprint();
    fp.add(text);

    Assert.assertEquals(
        "_\t13\n" + "e\t7\n" + "n\t5\n" + "h\t4\n" + "i\t4\n" + "a\t3\n" + "c\t3\n" + "ch\t3\n" + "h_\t3\n" + "ch_\t3\n"
            + "\"\t2\n" + "f\t2\n" + "g\t2\n" + "s\t2\n" + "\"_\t2\n" + "_\"\t2\n" + "_i\t2\n" + "e_\t2\n" + "ge\t2\n"
            + "ic\t2\n" + "n_\t2\n" + "_\"_\t2\n" + "_ic\t2\n" + "ich\t2\n" + "_ich\t2\n" + "ich_\t2\n" + "_ich_\t2\n"
            + ",\t1\n" + ".\t1\n" + "H\t1\n" + "T\t1\n" + "W\t1\n" + "l\t1\n" + "o\t1\n" + ",_\t1\n" + "._\t1\n"
            + "Hi\t1\n" + "Te\t1\n" + "We\t1\n" + "_,\t1\n" + "_.\t1\n" + "_H\t1\n" + "_T\t1\n" + "_W\t1\n" + "_a\t1\n"
            + "_e\t1\n" + "_g\t1\n" + "_s\t1\n" + "ac\t1\n" + "ag\t1\n" + "an\t1\n" + "ef\t1\n" + "eh\t1\n" + "ei\t1\n"
            + "el\t1\n" + "en\t1\n" + "fa\t1\n" + "fo\t1\n" + "he\t1\n" + "i_\t1\n" + "in\t1\n" + "le\t1\n" + "nf\t1\n"
            + "nn\t1\n" + "ns\t1\n" + "on\t1\n" + "s_\t1\n" + "sa\t1\n" + "Hi_\t1\n" + "Tel\t1\n" + "Wen\t1\n"
            + "_,_\t1\n" + "_._\t1\n" + "_Hi\t1\n" + "_Te\t1\n" + "_We\t1\n" + "_an\t1\n" + "_ei\t1\n" + "_ge\t1\n"
            + "_sa\t1\n" + "ach\t1\n" + "age\t1\n" + "ans\t1\n" + "efo\t1\n" + "ehe\t1\n" + "ein\t1\n" + "ele\t1\n"
            + "enn\t1\n" + "fac\t1\n" + "fon\t1\n" + "ge_\t1\n" + "geh\t1\n" + "he_\t1\n" + "inf\t1\n" + "lef\t1\n"
            + "nfa\t1\n" + "nn_\t1\n" + "ns_\t1\n" + "on_\t1\n" + "sag\t1\n" + "Tele\t1\n" + "Wenn\t1\n" + "_Hi_\t1\n"
            + "_Tel\t1\n" + "_Wen\t1\n" + "_ans\t1\n" + "_ein\t1\n" + "_geh\t1\n" + "_sag\t1\n" + "ach_\t1\n"
            + "age_\t1\n" + "ans_\t1\n" + "efon\t1\n" + "ehe_\t1\n" + "einf\t1\n" + "elef\t1\n" + "enn_\t1\n"
            + "fach\t1\n" + "fon_\t1\n" + "gehe\t1\n" + "infa\t1\n" + "lefo\t1\n" + "nfac\t1\n" + "sage\t1\n"
            + "Telef\t1\n" + "Wenn_\t1\n" + "_Tele\t1\n" + "_Wenn\t1\n" + "_ans_\t1\n" + "_einf\t1\n" + "_gehe\t1\n"
            + "_sage\t1\n" + "efon_\t1\n" + "einfa\t1\n" + "elefo\t1\n" + "fach_\t1\n" + "gehe_\t1\n" + "infac\t1\n"
            + "lefon\t1\n" + "nfach\t1\n" + "sage_\t1\n", fp.toString());
  }

  @Test
  public void testSaveThenLoad() throws Exception {

    Fingerprint fp1 = new Fingerprint();
    fp1.category("EN");
    sentencesEn().forEach(fp1::add);

    String path = java.nio.file.Files.createTempDirectory("test-").toFile().getPath();
    File file = new File(path + File.separator + "fingerprint.tsv.gz");
    fp1.save(file);

    Assert.assertTrue(file.exists());

    Fingerprint fp2 = new Fingerprint();
    fp2.load(file);

    Assert.assertNotEquals(fp1, fp2);

    fp2.category("EN");

    Assert.assertEquals(fp1, fp2);
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
