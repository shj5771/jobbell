package io.github.shj5771.jobbell.posting;

import io.github.shj5771.jobbell.IntegrationTest;
import io.github.shj5771.jobbell.support.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/** 배포 중 두 인스턴스가 동시에 수집을 시작하는 상황을 같은 JVM 의 두 스레드로 흉내 낸다. */
@IntegrationTest
class CollectSchedulerLockTest {

    @Autowired
    CollectScheduler scheduler;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        Database.clean(jdbc);
    }

    @Test
    @DisplayName("동시에 두 번 시작해도 수집은 한 번만 돈다")
    void 동시_시작() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<?> a = CompletableFuture.runAsync(() -> awaitThen(start, scheduler::collect), pool);
            CompletableFuture<?> b = CompletableFuture.runAsync(() -> awaitThen(start, scheduler::collect), pool);
            start.countDown();
            CompletableFuture.allOf(a, b).get();
        } finally {
            pool.shutdownNow();
        }

        Integer runs = jdbc.queryForObject("SELECT COUNT(DISTINCT run_id) FROM collect_run", Integer.class);
        assertThat(runs).isEqualTo(1);
    }

    @Test
    @DisplayName("방금 끝난 수집은 1분 안에 다시 돌지 않는다 — 다른 인스턴스가 곧바로 이어 돌리는 것을 막는다")
    void 연달아_시작() {
        scheduler.collect();
        scheduler.collect();

        Integer runs = jdbc.queryForObject("SELECT COUNT(DISTINCT run_id) FROM collect_run", Integer.class);
        assertThat(runs).isEqualTo(1);
    }

    private static void awaitThen(CountDownLatch latch, Runnable task) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        task.run();
    }
}
