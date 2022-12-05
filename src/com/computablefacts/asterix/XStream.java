package com.computablefacts.asterix;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import com.thoughtworks.xstream.security.ArrayTypePermission;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import java.io.File;
import java.util.Collection;

@CheckReturnValue
final public class XStream {

  public static <T> void save(File file, T obj) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!file.exists(), "file already exists : %s", file);
    Preconditions.checkNotNull(obj, "obj should not be null");

    Preconditions.checkState(IO.writeCompressedText(file, xStream().toXML(obj), false), "%s cannot be written",
        file.getAbsolutePath());
  }

  @SuppressWarnings("unchecked")
  public static <T> T load(File file) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exists : %s", file);

    return (T) xStream().fromXML(View.of(file, true).join(x -> x, "\n"));
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
