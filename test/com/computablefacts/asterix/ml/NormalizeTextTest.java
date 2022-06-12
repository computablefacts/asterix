package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.codecs.StringCodec;
import javax.validation.constraints.NotNull;
import org.junit.Assert;
import org.junit.Test;

public class NormalizeTextTest {

  @Test
  public void testNormalizeNullString() {

    String text = new NormalizeText().apply(null);

    Assert.assertEquals(0, text.length());
  }

  @Test
  public void testNormalize() {

    NormalizeText ttnt = new NormalizeText(true) {

      @Override
      protected String normalize(@NotNull String text) {
        return super.normalize(StringCodec.normalize(text));
      }
    };
    String text = ttnt.apply(text());

    Assert.assertEquals(430, text.length());
    Assert.assertEquals(
        "welcome to yahoo!, the world's most visited home page. quickly find what you're searching for, get in touch with friends and stay in-the-know with the latest news and information. cloudsponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including yahoo, gmail and hotmail/msn as well as popular desktop address books such as mac address book and outlook.",
        text);
  }

  private String text() {
    return "Welcome to Yahoo!, the world’s most visited home page. Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information. CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.";
  }
}
