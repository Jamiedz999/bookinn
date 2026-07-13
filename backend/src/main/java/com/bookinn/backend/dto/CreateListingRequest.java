package com.bookinn.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Payload to create a listing. Amenities are referenced by dictionary id; photos are supplied as an
 * ordered list of URLs whose position becomes their sort order.
 *
 * @param title listing headline
 * @param description free-text details, optional
 * @param city city the property is in
 * @param address street address
 * @param pricePerNight nightly price, positive with up to two decimals
 * @param maxGuests maximum occupancy
 * @param amenityIds ids from the amenity dictionary, may be empty
 * @param photoUrls image URLs in display order, may be empty
 */
public record CreateListingRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 5000) String description,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 255) String address,
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal pricePerNight,
    @NotNull @Min(1) @Max(50) Integer maxGuests,
    Set<Long> amenityIds,
    List<@NotBlank @Size(max = 500) String> photoUrls) {}
