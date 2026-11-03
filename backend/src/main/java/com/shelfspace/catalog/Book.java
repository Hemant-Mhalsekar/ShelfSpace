package com.shelfspace.catalog;

import com.shelfspace.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A title in the catalog. Physical items are {@link BookCopy}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "books")
@SQLRestriction("deleted_at IS NULL")
public class Book extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "replacement_cost", precision = 10, scale = 2)
    private BigDecimal replacementCost;

    /** Soft delete: books with loan history are never physically removed. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
