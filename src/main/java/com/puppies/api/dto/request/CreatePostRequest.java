package com.puppies.api.dto.request;

import lombok.Data;

@Data
public class CreatePostRequest {
    private String imageUrl;
    private String textContent;
}