package hu.rycus.rtweetwear.common.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Media {

    private long id;

    private String type;

    private String url;

    private int[] indices;

    @JsonProperty("display_url")
    private String displayUrl;

    @JsonProperty("expanded_url")
    private String expandedUrl;

    @JsonProperty("media_url")
    private String mediaUrl;

    @JsonProperty("media_url_https")
    private String mediaUrlHttps;

    private Sizes sizes;

    @JsonProperty("source_status_id")
    private long sourceStatusId;

}
