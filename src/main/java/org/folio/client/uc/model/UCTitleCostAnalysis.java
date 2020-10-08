package org.folio.client.uc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class UCTitleCostAnalysis {

  UCTitleCostAnalysisDetails current;
  UCTitleCostAnalysisDetails previous;
}
