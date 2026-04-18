package com.skybooker.booking.dto;

import lombok.Data;

@Data
public class AddOnRequest {
    private String mealPreference;   // VEG | NON_VEG | JAIN | VEGAN — null = no change
    private Double extraLuggageKg;   // additional kg to add — null = no change
    private Double additionalCost;   // cost of add-ons (added to totalFare)
}
