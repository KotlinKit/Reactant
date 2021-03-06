package org.brightify.reactant.core

import android.app.Application
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.AttributeSet
import android.view.View
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import org.brightify.reactant.controller.ViewController
import org.brightify.reactant.controller.util.TransactionManager
import java.util.Stack
import java.util.UUID

/**
 *  @author <a href="mailto:filip@brightify.org">Filip Dolnik</a>
 */
open class ReactantActivity(private val wireframeFactory: (Application) -> Wireframe): AppCompatActivity() {

    val resumed: Observable<Unit>
        get() = isResumed.filter { it }.map { }

    val paused: Observable<Unit>
        get() = isResumed.filter { !it }.map { }

    val isResumed: Observable<Boolean>
        get() = isResumedSubject

    val destroyed: Observable<Unit>
        get() = onDestroySubject

    val beforeKeyboardVisibilityChanged: Observable<Boolean>
        get() = contentView.beforeKeyboardVisibilityChangeSubject

    val afterKeyboardVisibilityChanged: Observable<Boolean>
        get() = beforeKeyboardVisibilityChanged.flatMap { value -> onLayoutSubject.take(1).map { value } }

    var screenOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    val lifetimeDisposeBag = CompositeDisposable()

    private lateinit var contentView: ReactantActivityContentView

    private val transactionManager = TransactionManager()
    private val isResumedSubject = BehaviorSubject.createDefault(false)
    private val onDestroySubject = ReplaySubject.create<Unit>(1)

    private val onLayoutSubject = PublishSubject.create<Unit>()

    private val viewControllerStack = Stack<ViewController>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contentView = ReactantActivityContentView(this)
        setContentView(contentView)

        transactionManager.transaction {
            if (savedInstanceState != null) {
                savedInstanceState.getString(SAVED_STATE_KEY)?.let { key ->
                    viewControllerStack.addAll(savedStates[key] ?: emptyList())
                    savedStates.remove(key)
                }
            }

            if (viewControllerStack.empty()) {
                viewControllerStack.push(wireframeFactory(application).entrypoint())
            }

            viewControllerStack.forEach { it.activity_ = this }
        }

        contentView.viewTreeObserver.addOnGlobalLayoutListener {
            onLayoutSubject.onNext(Unit)
        }

        transactionManager.enabled = true
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        val key = UUID.randomUUID().toString()
        outState?.putString(SAVED_STATE_KEY, key)
        savedStates[key] = viewControllerStack
    }

    override fun onBackPressed() {
        if (!viewControllerStack.peek().onBackPressed()) {
            dismissOrFinish()
        }
    }

    override fun onStart() {
        super.onStart()

        transactionManager.transaction {
            viewControllerStack.lastOrNull()?.let {
                it.viewWillAppear()
                contentView.addView(it.view)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        transactionManager.transaction {
            viewControllerStack.lastOrNull()?.viewDidAppear()
        }
        isResumedSubject.onNext(true)
    }

    override fun onPause() {
        super.onPause()

        transactionManager.transaction {
            viewControllerStack.lastOrNull()?.viewWillDisappear()
        }
        isResumedSubject.onNext(false)
    }

    override fun onStop() {
        super.onStop()

        contentView.removeAllViews()
        transactionManager.transaction {
            viewControllerStack.lastOrNull()?.viewDidDisappear()
        }
    }

    override fun onDestroy() {
        transactionManager.enabled = false

        onDestroySubject.onNext(Unit)

        lifetimeDisposeBag.clear()

        if (isFinishing) {
            viewControllerStack.forEach {
                it.activity_ = null
            }
        } else {
            viewControllerStack.forEach {
                it.destroyViewHierarchy()
            }
        }

        super.onDestroy()
    }

    override fun onCreateView(parent: View?, name: String?, context: Context?, attrs: AttributeSet?): View? {
        if (name == null) { return super.onCreateView(parent, name, context, attrs) }
        return try {
            val cls = Class.forName(name)

            val componentView = try {
                val constructor = cls.getConstructor(Context::class.java, AttributeSet::class.java)
                constructor.newInstance(context, attrs) as? ViewBase<*, *>
            } catch (e: NoSuchMethodException) {
                val constructor = cls.getConstructor(Context::class.java)
                constructor.newInstance(context) as? ViewBase<*, *>
            }

            componentView?.apply {
                init()
            }
        } catch (e: Exception) {
            super.onCreateView(parent, name, context, attrs)
        }
    }

    fun present(viewController: ViewController, animated: Boolean = true): Observable<Unit> {
        transactionManager.transaction {
            viewControllerStack.lastOrNull()?.let {
                it.viewWillDisappear()
                contentView.removeView(it.view)
                it.viewDidDisappear()
            }
            viewController.activity_ = this
            viewControllerStack.push(viewController)
            viewController.viewWillAppear()
            contentView.addView(viewController.view)
            viewController.viewDidAppear()
        }
        return Observable.just(Unit)
    }

    fun dismiss(animated: Boolean = true): Observable<Unit> {
        dismissOrFinish()
        return Observable.just(Unit)
    }

    fun <C: ViewController> present(viewController: Observable<C>, animated: Boolean = true): Observable<C> {
        val replay = ReplaySubject.create<C>(1)
        viewController
                .switchMap { controllerInstance ->
                    present(viewController = controllerInstance).map { controllerInstance } ?: Observable.empty<C>()
                }
                .subscribeBy(onNext = {
                    replay.onNext(it)
                }, onComplete = {
                    replay.onComplete()
                })
                .addTo(lifetimeDisposeBag)
        return replay
    }

    fun invalidateChildren() {
        transactionManager.transaction {
            contentView.removeAllViews()
            viewControllerStack.lastOrNull()?.let { contentView.addView(it.view) }
        }
    }

    fun updateScreenOrientation() {
        requestedOrientation = screenOrientation
    }

    private fun dismissOrFinish() {
        transactionManager.transaction {
            if (viewControllerStack.size > 1) {
                viewControllerStack.peek().viewWillDisappear()
                contentView.removeView(viewControllerStack.peek().view)
                viewControllerStack.peek().viewDidDisappear()
                viewControllerStack.pop().activity_ = null
                viewControllerStack.peek().viewWillAppear()
                contentView.addView(viewControllerStack.peek().view)
                viewControllerStack.peek().viewDidAppear()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAfterTransition()
                } else {
                    finish()
                }
            }
        }
    }

    companion object {

        private const val SAVED_STATE_KEY = "saved_state_key"

        private val savedStates = HashMap<String, Stack<ViewController>>()
    }
}
