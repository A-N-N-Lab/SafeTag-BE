//번호 돌려주는 dto
package com.example.SafeTag_BE.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RelayResolveResponse {

    private String e164;
    private String telUri;

}
