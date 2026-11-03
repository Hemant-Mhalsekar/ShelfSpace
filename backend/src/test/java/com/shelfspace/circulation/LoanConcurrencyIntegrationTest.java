package com.shelfspace.circulation;

import com.shelfspace.catalog.BookCopy;
import com.shelfspace.catalog.BookCopyRepository;
import com.shelfspace.catalog.CopyStatus;
import com.shelfspace.common.exception.BusinessRuleException;
import com.shelfspace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ten members try to borrow the last copy at the same instant.
 * Exactly one must win; everyone else gets a conflict or "no copies available". Never a double booking.
 */
class LoanConcurrencyIntegrationTest extends IntegrationTest {

    private static final int MEMBERS = 10;

    @Autowired
    private LoanService loanService;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookCopyRepository copyRepository;

    @Test
    void onlyOneMemberGetsTheLastCopy() throws Exception {
        long bookId = newBook(1);
        List<TestMember> members = new ArrayList<>();
        for (int i = 0; i < MEMBERS; i++) {
            members.add(newMember());
        }

        ExecutorService pool = Executors.newFixedThreadPool(MEMBERS);
        CountDownLatch ready = new CountDownLatch(MEMBERS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Outcome>> futures = new ArrayList<>();

        for (TestMember m : members) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();                       // everyone fires at the same moment
                try {
                    loanService.request(m.id(), bookId);
                    return Outcome.SUCCESS;
                } catch (OptimisticLockingFailureException e) {
                    return Outcome.CONFLICT;
                } catch (BusinessRuleException e) {
                    return Outcome.NO_COPIES;
                }
            }));
        }

        ready.await(10, TimeUnit.SECONDS);
        go.countDown();

        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Outcome> f : futures) {
            outcomes.add(f.get(30, TimeUnit.SECONDS));   // an unexpected exception fails the test here
        }
        pool.shutdown();

        long winners = outcomes.stream().filter(o -> o == Outcome.SUCCESS).count();
        assertThat(winners).as("outcomes: %s", outcomes).isEqualTo(1);

        List<BookCopy> copies = copyRepository.findByBookIdOrderByIdAsc(bookId);
        assertThat(copies).hasSize(1);
        assertThat(copies.get(0).getStatus()).isEqualTo(CopyStatus.ON_HOLD);

        long membersHoldingIt = members.stream()
                .filter(m -> loanRepository.existsByMemberIdAndCopyBookIdAndStatusIn(
                        m.id(), bookId, EnumSet.of(LoanStatus.REQUESTED)))
                .count();
        assertThat(membersHoldingIt).isEqualTo(1);
    }

    private enum Outcome { SUCCESS, CONFLICT, NO_COPIES }
}
