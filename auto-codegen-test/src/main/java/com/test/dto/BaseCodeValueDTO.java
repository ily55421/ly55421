// ---Auto Generated by Only4Play ---
package com.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class BaseCodeValueDTO {
  @Schema(
      title = "k"
  )
  private String k;

  @Schema(
      title = "v"
  )
  private String v;

  @Schema(
      title = "l"
  )
  private String l;

  protected BaseCodeValueDTO() {
  }

  public String getK() {
    return k;
  }

  public void setK(String k) {
    this.k = k;
  }

  public String getV() {
    return v;
  }

  public void setV(String v) {
    this.v = v;
  }

  public String getL() {
    return l;
  }

  public void setL(String l) {
    this.l = l;
  }
}