package com.computablefacts.jupiter;

import com.google.common.collect.Sets;
import java.util.Set;
import org.apache.accumulo.core.security.Authorizations;
import org.junit.Assert;
import org.junit.Test;

public class UsersTest {

  @Test
  public void testTheNullStringMapsToTheEmptyAuth() {
    Assert.assertEquals(Authorizations.EMPTY, Users.authorizations((String) null));
  }

  @Test
  public void testTheEmptyStringMapsToTheEmptyAuth() {
    Assert.assertEquals(Authorizations.EMPTY, Users.authorizations(""));
  }

  @Test
  public void testACommaSeparatedStringMapsToAnAccumuloAuth() {
    Assert.assertEquals(new Authorizations("A", "B", "C"), Users.authorizations("A, B, C"));
  }

  @Test
  public void testACommaSeparatedStringWithAnEmptyOneMapsToAnAccumuloAuth() {
    Assert.assertEquals(new Authorizations("A", "C"), Users.authorizations("A, , C"));
  }

  @Test
  public void testTheNullSetMapsToTheEmptyAuth() {
    Assert.assertEquals(Authorizations.EMPTY, Users.authorizations((Set<String>) null));
  }

  @Test
  public void testTheEmptySetMapsToTheEmptyAuth() {
    Assert.assertEquals(Authorizations.EMPTY, Users.authorizations(Sets.newHashSet()));
  }

  @Test
  public void testASetOfStringsMapsToAnAccumuloAuth() {
    Assert.assertEquals(new Authorizations("A", "B", "C"), Users.authorizations(Sets.newHashSet("A", "B", "C")));
  }

  @Test
  public void testASetOfStringsWithAnEmptyOneMapsToAnAccumuloAuth() {
    Assert.assertEquals(new Authorizations("A", "C"), Users.authorizations(Sets.newHashSet("A", "", "C")));
  }
}