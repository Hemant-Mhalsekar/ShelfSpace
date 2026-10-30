package com.shelfspace.catalog;

/** Projection for the grouped "copies per book" query. */
public interface CopyCounts {

    Long getBookId();

    Long getTotal();

    Long getAvailable();
}
