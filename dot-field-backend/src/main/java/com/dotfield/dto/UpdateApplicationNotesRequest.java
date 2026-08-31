package com.dotfield.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApplicationNotesRequest {

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;
}
