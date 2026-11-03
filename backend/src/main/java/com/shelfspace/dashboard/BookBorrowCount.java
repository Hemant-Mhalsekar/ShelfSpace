package com.shelfspace.dashboard;

public interface BookBorrowCount {

    Long getBookId();

    String getTitle();

    String getAuthor();

    Long getTimesBorrowed();
}
