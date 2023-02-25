package com.computablefacts.jupiter.storage.datastore;

import com.computablefacts.Generated;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.computablefacts.asterix.codecs.StringCodec;
import com.computablefacts.jupiter.Configurations;
import com.computablefacts.jupiter.Users;
import com.computablefacts.jupiter.storage.blobstore.Blob;
import com.computablefacts.jupiter.storage.blobstore.BlobStore;
import com.computablefacts.logfmt.LogFormatter;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.TablePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This storage layer acts as a generic data store.
 * </p>
 *
 * <p>
 * This data store is not meant to be efficient but is intended to be easy to use.
 * </p>
 */
@Deprecated
@CheckReturnValue
final public class DataStore implements AutoCloseable {

  private static final Base64.Decoder b64Decoder_ = Base64.getDecoder();
  private static final Logger logger_ = LoggerFactory.getLogger(DataStore.class);

  private final String name_;
  private final BlobStore blobStore_;

  private final AbstractBlobProcessor blobProcessor_;

  public DataStore(Configurations configurations, String name) {
    name_ = Preconditions.checkNotNull(name, "name should neither be null nor empty");
    blobStore_ = new BlobStore(configurations, blobStoreName(name));
    blobProcessor_ = new AccumuloBlobProcessor(blobStore_);
  }

  @Beta
  public DataStore(String name, BlobStore blobStore, AbstractBlobProcessor blobProcessor) {
    name_ = Preconditions.checkNotNull(name, "name should neither be null nor empty");
    blobStore_ = Preconditions.checkNotNull(blobStore, "blobStore should neither be null nor empty");
    blobProcessor_ = blobProcessor;
  }

  static String normalize(String str) {
    return StringCodec.removeDiacriticalMarks(StringCodec.normalize(str)).toLowerCase();
  }

  @Generated
  public static String blobStoreName(String name) {
    return name + "Blobs";
  }

  @Deprecated
  @Generated
  public static String termStoreName(String name) {
    return name + "Terms";
  }

  @Deprecated
  @Generated
  public static String cacheName(String name) {
    return name + "Cache";
  }

  @Generated
  @Override
  public void close() {
    flush();
  }

  @Generated
  @Override
  protected void finalize() {
    flush();
  }

  /**
   * Get a direct access to the underlying blob store.
   *
   * @return {@link BlobStore}
   */
  @Generated
  public BlobStore blobStore() {
    return blobStore_;
  }

  /**
   * Get the table configuration.
   *
   * @return the table configuration.
   */
  @Generated
  public Configurations configurations() {
    return blobStore_.configurations();
  }

  /**
   * Get the DataStore name.
   *
   * @return the DataStore name.
   */
  @Generated
  public String name() {
    return name_;
  }

  public void flush() {
    if (blobProcessor_ != null) {
      try {
        blobProcessor_.close();
      } catch (Exception e) {
        logger_.error(LogFormatter.create().message(e).formatError());
      }
    }
  }

  public boolean grantWritePermissionOnBlobStore(String username) {

    Preconditions.checkNotNull(username, "username should not be null");

    return Users.grantPermission(blobStore_.configurations().connector(), username, blobStoreName(name()),
        TablePermission.WRITE);
  }

  public boolean grantReadPermissionOnBlobStore(String username) {

    Preconditions.checkNotNull(username, "username should not be null");

    return Users.grantPermission(blobStore_.configurations().connector(), username, blobStoreName(name()),
        TablePermission.READ);
  }

  public boolean revokeWritePermissionOnBlobStore(String username) {

    Preconditions.checkNotNull(username, "username should not be null");

    return Users.revokePermission(blobStore_.configurations().connector(), username, blobStoreName(name()),
        TablePermission.WRITE);
  }

  public boolean revokeReadPermissionOnBlobStore(String username) {

    Preconditions.checkNotNull(username, "username should not be null");

    return Users.revokePermission(blobStore_.configurations().connector(), username, blobStoreName(name()),
        TablePermission.READ);
  }

  /**
   * Check if the storage layer has been initialized.
   *
   * @return true if the storage layer is ready to be used, false otherwise.
   */
  public boolean isReady() {
    return blobStore_.isReady();
  }

  /**
   * Initialize the storage layer.
   *
   * @return true if the storage layer already exists or has been successfully initialized, false otherwise.
   */
  public boolean create() {
    return blobStore_.create();
  }

  /**
   * Destroy the storage layer.
   *
   * @return true if the storage layer does not exist or has been successfully destroyed, false otherwise.
   */
  public boolean destroy() {
    return blobStore_.destroy();
  }

  /**
   * Remove all data.
   *
   * @return true if the operation succeeded, false otherwise.
   */
  public boolean truncate() {
    return blobStore_.truncate();
  }

  /**
   * Remove all data for a given dataset.
   *
   * @param dataset dataset.
   * @return true if the operation succeeded, false otherwise.
   */
  public boolean remove(String dataset) {

    Preconditions.checkNotNull(dataset, "dataset should not be null");

    return blobStore_.removeDataset(dataset);
  }

  /**
   * Persist a single JSON object.
   *
   * @param dataset dataset.
   * @param docId   unique identifier.
   * @param json    JSON object.
   * @return true if the operation succeeded, false otherwise.
   */
  public boolean persist(String dataset, String docId, String json) {
    return persistJson(dataset, docId, json);
  }

  /**
   * Persist a single JSON object.
   *
   * @param dataset dataset.
   * @param docId   unique identifier.
   * @param json    JSON object.
   * @return true if the operation succeeded, false otherwise.
   */
  public boolean persist(String dataset, String docId, Map<String, Object> json) {
    return persistJson(dataset, docId, JsonCodec.asString(json));
  }

  /**
   * Get all JSON from the blob storage layer (sorted).
   * <p>
   * The {@code <dataset>_RAW_DATA} auth is not enough to get access to the full JSON document. The user must also have
   * the {@code <dataset>_<field>} auth for each requested field.
   *
   * @param authorizations authorizations.
   * @param dataset        dataset.
   * @param fields         JSON fields to keep (optional).
   * @return list of documents. No particular order should be expected from the returned iterator if
   * {@code nbQueryThreads} is set to a value above 1.
   */
  public View<Blob<Value>> jsonsSortedByKey(Authorizations authorizations, String dataset, Set<String> fields) {

    Preconditions.checkNotNull(authorizations, "authorizations should not be null");
    Preconditions.checkNotNull(dataset, "dataset should not be null");

    return blobStore_.jsonsSortedByKey(authorizations, dataset, null, fields);
  }

  /**
   * Get all JSON from the blob storage layer (unsorted).
   * <p>
   * The {@code <dataset>_RAW_DATA} auth is not enough to get access to the full JSON document. The user must also have
   * the {@code <dataset>_<field>} auth for each requested field.
   *
   * @param authorizations authorizations.
   * @param dataset        dataset.
   * @param fields         JSON fields to keep (optional).
   * @return list of documents. No particular order should be expected from the returned iterator if
   * {@code nbQueryThreads} is set to a value above 1.
   */
  public View<Blob<Value>> jsons(Authorizations authorizations, String dataset, Set<String> fields) {

    Preconditions.checkNotNull(authorizations, "authorizations should not be null");
    Preconditions.checkNotNull(dataset, "dataset should not be null");

    return blobStore_.jsons(authorizations, dataset, null, fields);
  }

  /**
   * Get JSON from the blob storage layer (sorted).
   * <p>
   * The {@code <dataset>_RAW_DATA} auth is not enough to get access to the full JSON document. The user must also have
   * the {@code <dataset>_<field>} auth for each requested field.
   *
   * @param authorizations authorizations.
   * @param dataset        dataset.
   * @param fields         JSON fields to keep (optional).
   * @param docsIds        documents unique identifiers.
   * @return list of documents. No particular order should be expected from the returned iterator if
   * {@code nbQueryThreads} is set to a value above 1.
   */
  public View<Blob<Value>> jsonsSortedByKey(Authorizations authorizations, String dataset, Set<String> fields,
      Set<String> docsIds) {

    Preconditions.checkNotNull(authorizations, "authorizations should not be null");
    Preconditions.checkNotNull(dataset, "dataset should not be null");
    Preconditions.checkNotNull(docsIds, "docsIds should not be null");

    return blobStore_.jsonsSortedByKey(authorizations, dataset, docsIds, fields);
  }

  /**
   * Get JSON from the blob storage layer (unsorted).
   * <p>
   * The {@code <dataset>_RAW_DATA} auth is not enough to get access to the full JSON document. The user must also have
   * the {@code <dataset>_<field>} auth for each requested field.
   *
   * @param authorizations authorizations.
   * @param dataset        dataset.
   * @param fields         JSON fields to keep (optional).
   * @param docsIds        documents unique identifiers.
   * @return list of documents. No particular order should be expected from the returned iterator if
   * {@code nbQueryThreads} is set to a value above 1.
   */
  public View<Blob<Value>> jsons(Authorizations authorizations, String dataset, Set<String> fields,
      Set<String> docsIds) {

    Preconditions.checkNotNull(authorizations, "authorizations should not be null");
    Preconditions.checkNotNull(dataset, "dataset should not be null");
    Preconditions.checkNotNull(docsIds, "docsIds should not be null");

    return blobStore_.jsons(authorizations, dataset, docsIds, fields);
  }

  /**
   * Persist a single JSON object.
   *
   * @param dataset the dataset.
   * @param docId   the document identifier
   * @param json    the JSON object as a String.
   * @return true if the operation succeeded, false otherwise.
   */
  private boolean persistJson(String dataset, String docId, String json) {

    Preconditions.checkNotNull(dataset, "dataset should not be null");
    Preconditions.checkNotNull(docId, "docId should not be null");
    Preconditions.checkNotNull(json, "json should not be null");

    return blobProcessor_ == null || blobProcessor_.write(dataset, docId, json);
  }
}
