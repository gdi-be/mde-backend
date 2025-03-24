package de.terrestris.mde.mde_backend.model.dto;

import java.util.List;

public record SearchResponse<T>(
  List<T> results,
  long totalHitCount
) { }
