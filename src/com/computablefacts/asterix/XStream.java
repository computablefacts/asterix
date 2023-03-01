package com.computablefacts.asterix;

import static com.computablefacts.asterix.IO.eCompressionAlgorithm.GZIP;

import com.computablefacts.logfmt.LogFormatter;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import com.thoughtworks.xstream.security.ArrayTypePermission;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CheckReturnValue
final public class XStream {

  private static final Logger logger_ = LoggerFactory.getLogger(XStream.class);

  public static <T> void save(File file, T obj) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!file.exists(), "file already exists : %s", file);
    Preconditions.checkNotNull(obj, "obj should not be null");

    try (BufferedWriter writer = IO.newFileWriter(file, false, GZIP)) {
      xStream().toXML(obj, writer);
    } catch (IOException e) {
      logger_.error(LogFormatter.create().message(e).formatError());
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T load(File file) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exists : %s", file);

    try (BufferedReader reader = IO.newFileReader(file, GZIP)) {
      return (T) xStream().fromXML(reader);
    } catch (IOException e) {
      logger_.error(LogFormatter.create().message(e).formatError());
    }
    return null;
  }

  private static com.thoughtworks.xstream.XStream xStream() {

    com.thoughtworks.xstream.XStream xStream = new com.thoughtworks.xstream.XStream();
    xStream.addPermission(NoTypePermission.NONE);
    xStream.addPermission(NullPermission.NULL);
    xStream.addPermission(PrimitiveTypePermission.PRIMITIVES);
    xStream.addPermission(ArrayTypePermission.ARRAYS);
    xStream.allowTypeHierarchy(Collection.class);
    xStream.allowTypesByWildcard(
        new String[]{"com.computablefacts.**", "com.google.common.collect.**", "java.io.**", "java.lang.**",
            "java.util.**", "smile.classification.**", "smile.regression.**", "smile.math.**", "smile.base.**",
            "smile.data.**", "smile.neighbor.**"});

    return xStream;
  }
}