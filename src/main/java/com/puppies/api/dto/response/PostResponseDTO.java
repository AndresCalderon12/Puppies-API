package com.puppies.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostResponseDTO {

    private Long id;
    private String imageUrl;
    private String textContent;
    private LocalDateTime date;
    private Long userId;
    private String userName;
    private long likeCount;
}
