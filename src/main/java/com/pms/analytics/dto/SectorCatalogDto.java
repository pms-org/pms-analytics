package com.pms.analytics.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SectorCatalogDto {
    private String sector;
    private List<String> symbols;
}

