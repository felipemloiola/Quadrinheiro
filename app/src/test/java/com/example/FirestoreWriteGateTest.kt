package com.example

import com.example.data.FirestoreWriteGate
import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirestoreWriteGateTest {
    @Test fun timeoutAndConcurrentRetriesDoNotEnqueueAnotherMutation()=runBlocking {
        val gate=FirestoreWriteGate()
        val source=TaskCompletionSource<Void>()
        val starts=AtomicInteger()
        val first=launch(start=CoroutineStart.UNDISPATCHED){gate.execute("account","revision-1"){starts.incrementAndGet();source.task}}
        first.cancelAndJoin()
        val retries=List(20){async(start=CoroutineStart.UNDISPATCHED){gate.execute("account","revision-1"){starts.incrementAndGet();source.task}}}
        assertEquals(1,starts.get())
        source.setResult(null)
        withTimeout(2000){retries.awaitAll()}
        assertEquals(1,starts.get())
    }

    @Test fun changedRevisionWaitsAndAnotherAccountIsIndependent()=runBlocking {
        val gate=FirestoreWriteGate()
        val old=TaskCompletionSource<Void>();val changed=TaskCompletionSource<Void>();val other=TaskCompletionSource<Void>()
        var changes=0;var others=0
        val first=launch(start=CoroutineStart.UNDISPATCHED){gate.execute("a","old"){old.task}}
        val next=async(start=CoroutineStart.UNDISPATCHED){gate.execute("a","new"){changes++;changed.task}}
        val independent=async(start=CoroutineStart.UNDISPATCHED){gate.execute("b","new"){others++;other.task}}
        assertEquals(0,changes);assertEquals(1,others)
        other.setResult(null);independent.await()
        old.setResult(null);first.join();yield()
        assertEquals(1,changes)
        changed.setResult(null);next.await()
    }

    @Test fun aDefinitiveFailureAllowsTheNextRetry()=runBlocking {
        val gate=FirestoreWriteGate();val failed=TaskCompletionSource<Void>()
        val first=async(start=CoroutineStart.UNDISPATCHED){runCatching{gate.execute("a","old"){failed.task}}}
        failed.setException(IllegalStateException("offline"));assertTrue(first.await().isFailure)
        val success=TaskCompletionSource<Void>().apply{setResult(null)}
        gate.execute("a","new"){success.task}
    }
}
