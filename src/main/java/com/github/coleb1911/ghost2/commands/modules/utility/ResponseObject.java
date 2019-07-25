package com.github.coleb1911.ghost2.commands.modules.utility;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Generated;
import java.util.List;

/**
 * A POJO representing the response from the Rest API
 */
@Generated("com.robohorse.robopojogenerator")
class ResponseObject {

    @JsonProperty("defs")
    private List<String> defs;

    @JsonProperty("score")
    private int score;

    @JsonProperty("word")
    private String word;

    public void setDefs(List<String> defs) {
        this.defs = defs;
    }

    public List<String> getDefs() {
        return defs;
    }

    //TODO Try ignoring score by deleting field.
    public void setScore(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getWord() {
        return word;
    }

    @Override
    public String toString() {
        return
                "ResponseObject{" +
                        "defs = '" + defs + '\'' +
                        ",score = '" + score + '\'' +
                        ",word = '" + word + '\'' +
                        "}";
    }
}
