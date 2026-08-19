package com.zeta.business.service;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class SortOrderHelper {

  private static final int STEP = 10;

  private SortOrderHelper() {
  }

  static <T> int resolveForCreate(
      Integer requested,
      Collection<T> siblings,
      Function<T, Integer> sortOrderGetter) {
    if (requested == null) {
      return nextAvailable(siblings, sortOrderGetter);
    }
    ensureUnique(requested, siblings, sortOrderGetter);
    return requested;
  }

  static <T> int resolveForUpdate(
      Integer requested,
      Integer current,
      Collection<T> siblings,
      Function<T, Integer> sortOrderGetter,
      Function<T, Long> idGetter,
      Long currentId) {
    int next = requested == null ? current == null ? 0 : current : requested;
    if (Objects.equals(next, current)) {
      return next;
    }
    ensureUnique(next, siblings, sortOrderGetter, idGetter, currentId);
    return next;
  }

  private static <T> int nextAvailable(
      Collection<T> siblings,
      Function<T, Integer> sortOrderGetter) {
    int max = 0;
    for (T sibling : siblings) {
      Integer sortOrder = sortOrderGetter.apply(sibling);
      if (sortOrder != null && sortOrder > max) {
        max = sortOrder;
      }
    }
    int candidate = max + STEP;
    while (contains(candidate, siblings, sortOrderGetter)) {
      candidate += STEP;
    }
    return candidate;
  }

  private static <T> void ensureUnique(
      Integer sortOrder,
      Collection<T> siblings,
      Function<T, Integer> sortOrderGetter) {
    ensureUnique(sortOrder, siblings, sortOrderGetter, null, null);
  }

  private static <T> void ensureUnique(
      Integer sortOrder,
      Collection<T> siblings,
      Function<T, Integer> sortOrderGetter,
      Function<T, Long> idGetter,
      Long currentId) {
    if (contains(sortOrder, siblings, sortOrderGetter, idGetter, currentId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "排序编号已存在，请更换编号或使用自动排序");
    }
  }

  private static <T> boolean contains(
      Integer sortOrder,
      Collection<T> siblings,
      Function<T, Integer> sortOrderGetter) {
    return contains(sortOrder, siblings, sortOrderGetter, null, null);
  }

  private static <T> boolean contains(
      Integer sortOrder,
      Collection<T> siblings,
      Function<T, Integer> sortOrderGetter,
      Function<T, Long> idGetter,
      Long currentId) {
    for (T sibling : siblings) {
      if (idGetter != null && Objects.equals(idGetter.apply(sibling), currentId)) {
        continue;
      }
      if (Objects.equals(sortOrderGetter.apply(sibling), sortOrder)) {
        return true;
      }
    }
    return false;
  }
}
