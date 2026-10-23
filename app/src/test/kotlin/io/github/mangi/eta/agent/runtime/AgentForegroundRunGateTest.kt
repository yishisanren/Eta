package io.github.mangi.eta.agent.runtime

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentForegroundRunGateTest {
    @Test
    fun screenBelongsToOneRunUntilThatRunFinishes() {
        val first = AgentForegroundRunGate(AgentRunController())
        val second = AgentForegroundRunGate(AgentRunController())
        val attempted = CountDownLatch(1)
        val entered = CountDownLatch(1)
        first.enter()
        val waiter = Thread {
            attempted.countDown()
            second.enter()
            entered.countDown()
            second.close()
        }
        try {
            waiter.start()
            assertTrue(attempted.await(1, TimeUnit.SECONDS))
            assertFalse(entered.await(200, TimeUnit.MILLISECONDS))
            first.close()
            assertTrue(entered.await(1, TimeUnit.SECONDS))
        } finally {
            first.close()
            waiter.join(1_000)
        }
    }

    @Test
    fun cancelledRunStopsWaitingForScreen() {
        val first = AgentForegroundRunGate(AgentRunController())
        val controller = AgentRunController()
        val second = AgentForegroundRunGate(controller)
        val attempted = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        first.enter()
        val waiter = Thread {
            attempted.countDown()
            try {
                second.enter()
            } catch (_: AgentRunCancelledException) {
                cancelled.countDown()
            } finally {
                second.close()
            }
        }
        try {
            waiter.start()
            assertTrue(attempted.await(1, TimeUnit.SECONDS))
            controller.cancel()
            assertTrue(cancelled.await(1, TimeUnit.SECONDS))
        } finally {
            first.close()
            waiter.join(1_000)
        }
    }
}
