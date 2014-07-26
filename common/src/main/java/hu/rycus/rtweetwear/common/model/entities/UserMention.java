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
public class UserMention {

    private long id;

    private int[] indices;

    private String name;

    @JsonProperty("screen_name")
    private String screenName;

}
