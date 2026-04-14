package org.example.ailoldraftingcheck.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MyResponse {
    String answer;

    public MyResponse(String answer) {
        this.answer = answer;
    }
}
