package com.badoo.reaktive.observable

import com.badoo.reaktive.disposable.disposable
import com.badoo.reaktive.test.observable.DefaultObservableObserver
import com.badoo.reaktive.test.observable.TestObservable
import com.badoo.reaktive.test.observable.test
import com.badoo.reaktive.test.utils.SafeMutableList
import com.badoo.reaktive.utils.atomicreference.AtomicReference
import com.badoo.reaktive.utils.atomicreference.update
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DoOnBeforeFinallyTest
    : ObservableToObservableTests by ObservableToObservableTests<Unit>({ doOnBeforeFinally {} }) {

    private val upstream = TestObservable<Int>()

    @Test
    fun calls_action_before_completion() {
        val callOrder = SafeMutableList<String>()

        upstream
            .doOnBeforeFinally {
                callOrder += "action"
            }
            .subscribe(
                object : DefaultObservableObserver<Int> {
                    override fun onComplete() {
                        callOrder += "onComplete"
                    }
                }
            )

        upstream.onComplete()

        assertEquals(listOf("action", "onComplete"), callOrder.items)
    }

    @Test
    fun calls_action_before_failing() {
        val callOrder = SafeMutableList<String>()
        val exception = Exception()

        upstream
            .doOnBeforeFinally {
                callOrder += "action"
            }
            .subscribe(
                object : DefaultObservableObserver<Int> {
                    override fun onError(error: Throwable) {
                        callOrder += "onError"
                    }
                }
            )

        upstream.onError(exception)

        assertEquals(listOf("action", "onError"), callOrder.items)
    }

    @Test
    fun calls_action_before_disposing_upstream() {
        val callOrder = SafeMutableList<String>()

        observableUnsafe<Unit> { observer ->
            observer.onSubscribe(
                disposable {
                    callOrder += "dispose"
                }
            )
        }
            .doOnBeforeFinally {
                callOrder += "action"
            }
            .test()
            .dispose()

        assertEquals(listOf("action", "dispose"), callOrder.items)
    }

    @Test
    fun does_not_call_action_second_time_WHEN_downstream_disposed_and_upstream_completed() {
        val count = AtomicReference(0)

        upstream
            .doOnBeforeFinally {
                count.update(Int::inc)
            }
            .test()
            .dispose()

        upstream.onComplete()

        assertEquals(1, count.value)
    }

    @Test
    fun does_not_call_action_second_time_WHEN_downstream_disposed_and_upstream_produced_error() {
        val count = AtomicReference(0)

        upstream
            .doOnBeforeFinally {
                count.update(Int::inc)
            }
            .test()
            .dispose()

        upstream.onError(Throwable())

        assertEquals(1, count.value)
    }

    @Test
    fun does_not_call_action_second_time_WHEN_upstream_completed_and_downstream_disposed() {
        val count = AtomicReference(0)

        val observer =
            upstream
                .doOnBeforeFinally {
                    count.update(Int::inc)
                }
                .test()

        upstream.onComplete()
        observer.dispose()

        assertEquals(1, count.value)
    }

    @Test
    fun does_not_call_action_second_time_WHEN_upstream_produced_error_and_downstream_disposed() {
        val count = AtomicReference(0)

        val observer =
            upstream
                .doOnBeforeFinally {
                    count.update(Int::inc)
                }
                .test()

        upstream.onError(Throwable())
        observer.dispose()

        assertEquals(1, count.value)
    }

    @Test
    fun does_not_call_action_WHEN_emitted_value() {
        val isCalled = AtomicReference(false)

        upstream
            .doOnBeforeFinally {
                isCalled.value = true
            }
            .test()

        upstream.onNext(0)

        assertFalse(isCalled.value)
    }
}