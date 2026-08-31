package com.dotfield.dto;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Source-independent representation of an external raw job.
 * Extends {@link RawJobListing} for seamless compatibility with existing ingestion components.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class RawJob extends RawJobListing {
}
