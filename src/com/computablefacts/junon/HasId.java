package com.computablefacts.junon;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HasId {

  @JsonProperty("external_id")
  public final String externalId_;
  @JsonIgnore
  public int id_;
  
  protected HasId() {
    externalId_ = new RandomString(5).nextString() + '|' + Instant.now().toString();
  }

  protected HasId(@JsonProperty("external_id") String externalId) {
    externalId_ = externalId;
  }
}
