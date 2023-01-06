package com.computablefacts.jupiter.storage.datastore;

import static com.computablefacts.jupiter.storage.Constants.AUTH_ADM;

import com.computablefacts.jupiter.Configurations;
import com.computablefacts.jupiter.Data;
import com.computablefacts.jupiter.MiniAccumuloClusterTest;
import com.computablefacts.jupiter.MiniAccumuloClusterUtils;
import com.computablefacts.jupiter.storage.blobstore.Blob;
import com.google.errorprone.annotations.Var;
import java.util.List;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.junit.Assert;
import org.junit.Test;

public class DataStoreTest extends MiniAccumuloClusterTest {

  @Test
  public void testCreateIsReadyAndDestroy() throws Exception {

    String tableName = nextTableName();
    Configurations configurations = MiniAccumuloClusterUtils.newConfiguration(accumulo());
    DataStore dataStore = new DataStore(configurations, tableName);

    Assert.assertFalse(dataStore.isReady());
    Assert.assertTrue(dataStore.create());
    Assert.assertTrue(dataStore.isReady());
    Assert.assertTrue(dataStore.destroy());
    Assert.assertFalse(dataStore.isReady());
  }

  @Test
  public void testTruncate() throws Exception {

    DataStore dataStore = newDataStore(AUTH_ADM);

    Assert.assertTrue(dataStore.persist("dataset_1", "row_1", Data.json2(1)));
    Assert.assertTrue(dataStore.persist("dataset_2", "row_1", Data.json3(1)));

    dataStore.flush();

    @Var List<Blob<Value>> blobs1 = dataStore.jsons(AUTH_ADM, "dataset_1", null).toList();
    @Var List<Blob<Value>> blobs2 = dataStore.jsons(AUTH_ADM, "dataset_2", null).toList();

    Assert.assertEquals(1, blobs1.size());
    Assert.assertEquals(1, blobs2.size());

    Assert.assertTrue(dataStore.truncate());

    blobs1 = dataStore.jsons(AUTH_ADM, "dataset_1", null).toList();
    blobs2 = dataStore.jsons(AUTH_ADM, "dataset_2", null).toList();

    Assert.assertEquals(0, blobs1.size());
    Assert.assertEquals(0, blobs2.size());
  }

  @Test
  public void testRemoveDataset() throws Exception {

    DataStore dataStore = newDataStore(AUTH_ADM);

    Assert.assertTrue(dataStore.persist("dataset_1", "row_1", Data.json2(1)));
    Assert.assertTrue(dataStore.persist("dataset_2", "row_1", Data.json3(1)));

    dataStore.flush();

    @Var List<Blob<Value>> blobs1 = dataStore.jsons(AUTH_ADM, "dataset_1", null).toList();
    @Var List<Blob<Value>> blobs2 = dataStore.jsons(AUTH_ADM, "dataset_2", null).toList();

    Assert.assertEquals(1, blobs1.size());
    Assert.assertEquals(1, blobs2.size());

    Assert.assertTrue(dataStore.remove("dataset_1"));

    blobs1 = dataStore.jsons(AUTH_ADM, "dataset_1", null).toList();
    blobs2 = dataStore.jsons(AUTH_ADM, "dataset_2", null).toList();

    Assert.assertEquals(0, blobs1.size());
    Assert.assertEquals(1, blobs2.size());
  }

  private DataStore newDataStore(Authorizations auths) throws Exception {
    return newDataStore(auths, nextUsername());
  }

  private DataStore newDataStore(Authorizations auths, String username) throws Exception {

    String tableName = nextTableName();

    MiniAccumuloClusterUtils.newUser(accumulo(), username);
    MiniAccumuloClusterUtils.setUserAuths(accumulo(), username, auths);
    MiniAccumuloClusterUtils.setUserSystemPermissions(accumulo(), username);

    Configurations configurations = MiniAccumuloClusterUtils.newConfiguration(accumulo(), username);
    DataStore dataStore = new DataStore(configurations, tableName);

    Assert.assertTrue(dataStore.create());
    Assert.assertTrue(dataStore.grantReadPermissionOnBlobStore(username));
    Assert.assertTrue(dataStore.grantWritePermissionOnBlobStore(username));

    return dataStore;
  }
}
