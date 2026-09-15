package com.airship.ctvlab.airship

import android.app.Activity
import android.app.Application
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import java.util.WeakHashMap

/**
 * Thomas layouts are Android views, and none of them is usable with a D-pad as rendered: the pager
 * wrapping every Scene claims the focus rather than offering it to what it holds, and a control laid
 * over another is unreachable because focus search only accepts a candidate lying in the direction
 * pressed. The embedded slot applies this to the view it hosts; a modal or a banner renders in one
 * of Airship's own activities, which the app can only reach as it starts.
 */
fun Application.correctThomasFocus() {
    registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            activity.window.trackDirectionalKeys()
            if (!activity.javaClass.name.startsWith(THOMAS_PACKAGE)) return
            val root = activity.window.decorView
            root.correctForDpad()
            root.focusFirstControlIfStranded()
            root.keepFocusThroughPageChanges()
            // Thomas sizes itself as its media loads, so a single pass at start is too early.
            root.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                view.correctForDpad()
                view.focusFirstControlIfStranded()
            }
        }

        override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, out: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    })
}

private const val THOMAS_PACKAGE = "com.urbanairship.android.layout"

fun View.correctForDpad() {
    yieldFocusToDescendants()
    linkOverlaidControls()
}

/**
 * Keep the focus inside a layout whose pages are recycled.
 *
 * Thomas takes the focused view away when the pager advances — on a CTA carrying `pager_next`, or on
 * a story timer — and nothing re-dispatches the focus, so the focus search hands it to whatever the
 * app draws around the layout. Put it back on the page that replaced it.
 *
 * Only a directional press tells the user meant to leave, hence the key tracking. The view that
 * lost the focus says nothing: the page is detached before the event is sent, which is also why the
 * listener has to remember the focus was inside rather than read it from the event.
 */
fun View.keepFocusThroughPageChanges() {
    if (watchedForPageChanges.put(this, Unit) != null) return
    val layout = this
    var wasInside = false
    viewTreeObserver.addOnGlobalFocusChangeListener { _, gained ->
        if (gained != null && layout.holds(gained)) {
            wasInside = true
            return@addOnGlobalFocusChangeListener
        }
        if (!wasInside) return@addOnGlobalFocusChangeListener
        wasInside = false
        if (layout.isShown && lostFocusToAPageChange()) layout.recoverFocus()
    }
}

private val watchedForPageChanges = WeakHashMap<View, Unit>()

private fun lostFocusToAPageChange(): Boolean {
    val now = SystemClock.uptimeMillis()
    if (now - lastDirectionalKeyAt < DIRECTIONAL_GRACE_MS) return false
    // The focus search runs again once the page is laid out, so a recovery can be undone and
    // observed as another loss. Recover once per page change rather than trading it back and forth.
    if (now - lastRecoveryAt < RECOVERY_DEBOUNCE_MS) return false
    lastRecoveryAt = now
    return true
}

/** The page that replaced the focused one can still be pending layout, hence the retries. */
private fun View.recoverFocus(attempts: Int = 12) {
    if (attempts == 0 || !isShown) return
    val controls = mutableListOf<View>()
    collectControls(controls)
    val onScreen = controls.filter { it.isShown && it.width > 0 && it.height > 0 }
    // A page covers the whole layout, while a dismiss cross drawn over it is small.
    val target = onScreen.maxByOrNull { it.width.toLong() * it.height }
    if (target?.requestFocus() != true) {
        postDelayed({ recoverFocus(attempts - 1) }, 50)
        return
    }
    // The focus search that took the focus away scrolled its own candidate into view, and the
    // scroll container doesn't follow the focus back into an Android view on its own.
    requestRectangleOnScreen(Rect(0, 0, width, height), true)
}

private const val DIRECTIONAL_GRACE_MS = 400L
private const val RECOVERY_DEBOUNCE_MS = 700L
private var lastDirectionalKeyAt = 0L
private var lastRecoveryAt = 0L

/**
 * Every key reaches the activity before the view tree, so wrapping the window callback records the
 * press wherever the layout is hosted — the app's own screen, or one of Airship's activities.
 */
private fun Window.trackDirectionalKeys() {
    if (callback is DirectionalKeys) return
    callback = DirectionalKeys(callback)
}

private class DirectionalKeys(
    private val delegate: Window.Callback,
) : Window.Callback by delegate {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode in DIRECTIONS) {
            lastDirectionalKeyAt = SystemClock.uptimeMillis()
        }
        return delegate.dispatchKeyEvent(event)
    }

    private companion object {
        val DIRECTIONS = setOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
        )
    }
}

private fun View.holds(target: View): Boolean {
    var node: View? = target
    while (node != null) {
        if (node === this) return true
        node = node.parent as? View
    }
    return false
}

/**
 * Let every container defer to its children. The pager stays focusable as a fallback, for a layout
 * that holds nothing focusable at all.
 */
private fun View.yieldFocusToDescendants() {
    if (this !is ViewGroup) return
    if (descendantFocusability == ViewGroup.FOCUS_BEFORE_DESCENDANTS) {
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
    }
    for (index in 0 until childCount) {
        getChildAt(index).yieldFocusToDescendants()
    }
}

/**
 * Wire a control that sits inside the bounds of whatever is focusable around it — a dismiss cross
 * over a full-width button, or over the scrolling body of a modal — on the horizontal axis, which
 * is the one such a layout leaves unused since up and down walk its content.
 */
private fun View.linkOverlaidControls() {
    val controls = mutableListOf<View>()
    collectControls(controls)
    if (controls.isEmpty()) return

    val hosts = mutableListOf<View>()
    collectFocusable(hosts)
    val bounds = (controls + hosts).distinct().associateWith { it.windowBounds() }

    controls.forEach { overlay ->
        val overlayBounds = bounds.getValue(overlay)
        val under = hosts
            .filter { host ->
                host !== overlay && bounds.getValue(host).let {
                    it.contains(overlayBounds) && it != overlayBounds
                }
            }
            // A container that isn't a control itself and holds every one of them is the layout's
            // own scaffolding — the pager — rather than something a control is laid over.
            .filterNot { host ->
                !host.isClickable && controls.all { bounds.getValue(host).contains(bounds.getValue(it)) }
            }
            .minByOrNull { bounds.getValue(it).let { rect -> rect.width().toLong() * rect.height() } }
            ?: return@forEach

        under.nextFocusRightId = overlay.ensureId()
        overlay.nextFocusLeftId = under.ensureId()
    }
}

/**
 * The pager holds the focus by the time the containers are told to defer, and it keeps it: nothing
 * re-dispatches focus on its own. Hand it to a control, unless one already has it.
 */
private fun View.focusFirstControlIfStranded() {
    val focused = findFocus() ?: return
    if (focused.isClickable) return
    val controls = mutableListOf<View>()
    collectControls(controls)
    controls.firstOrNull()?.requestFocus()
}

/** The layout's own controls, as opposed to the containers the pager makes focusable. */
private fun View.collectControls(into: MutableList<View>) {
    if (isFocusable && isClickable) into += this
    if (this !is ViewGroup) return
    for (index in 0 until childCount) {
        getChildAt(index).collectControls(into)
    }
}

private fun View.collectFocusable(into: MutableList<View>) {
    if (isFocusable) into += this
    if (this !is ViewGroup) return
    for (index in 0 until childCount) {
        getChildAt(index).collectFocusable(into)
    }
}

private fun View.windowBounds(): Rect {
    val corner = IntArray(2)
    getLocationInWindow(corner)
    return Rect(corner[0], corner[1], corner[0] + width, corner[1] + height)
}

private fun View.ensureId(): Int {
    if (id == View.NO_ID) id = View.generateViewId()
    return id
}
