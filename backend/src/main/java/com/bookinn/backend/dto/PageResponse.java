package com.bookinn.backend.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable paged-response envelope for search results. We map Spring Data's {@link Page} into this
 * record rather than serialising a {@code PageImpl} directly: the JSON shape of {@code PageImpl} is
 * an internal detail Spring warns against exposing, so pinning our own contract keeps the API
 * stable across upgrades.
 *
 * @param content the page's items
 * @param page zero-based page index
 * @param size page size
 * @param totalElements total matching rows across all pages
 * @param totalPages total number of pages
 * @param <T> item type
 */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  /**
   * Projects a Spring Data {@link Page} into the stable envelope.
   *
   * @param page the source page
   * @param <T> item type
   * @return the envelope
   */
  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
