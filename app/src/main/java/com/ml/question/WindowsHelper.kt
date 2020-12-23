package com.ml.question

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.*
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.ml.question.databinding.FloatViewBinding
import kotlin.math.abs

/**
 * Author: Menglong Ma
 * Email: mml2015@126.com
 * Date: 19-10-12 上午11:19
 * Description: This is WindowsHelper
 * Project: OnePlusNH
 */
@SuppressLint("StaticFieldLeak")
object WindowsHelper {
    const val ACTION_SHOW = "ACTION_SHOW"
    const val ACTION_HIDE = "ACTION_HIDE"
    const val ACTION_DESTROY = "ACTION_DESTROY"
    private const val LONG_CLICK_LIMIT: Long = 400
    private const val CLICK_LIMIT: Long = 300
    private var mTouchSlop: Int = 0
    private const val TAG = "WindowsHelper"
    var mWindowManager: WindowManager
    var mLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams()
    private lateinit var floatViewBinding: FloatViewBinding
    private val displaySizePoint = Point()
    private var statusBarHeight = 0
    private var context: Context = App.context
    private var lastDownX =0f
    private var lastDownY =0f
    //标识 是否已经添加过view
    private var isViewAdd = false
    private var isVisibility  = false
    var mLastDownTime: Long = 0
    var mIsTouching: Boolean = false  //是否点击
    var mIsLongTouch: Boolean = false//是否长点击
    val mVibrator: Vibrator =
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val mPattern = longArrayOf(0, 100)

    private fun log(msg:String){
        Log.i("WindowsHelper",msg)
    }
    init {
        mWindowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        init()
    }

    /*
   * 窗口占满整个屏幕，忽略周围的装饰边框（例如状态栏）。此窗口需考虑到装饰边框的内容。
   public static final int FLAG_LAYOUT_IN_SCREEN =0x00000100;

   允许窗口扩展到屏幕之外。
   public static final int FLAG_LAYOUT_NO_LIMITS =0x00000200;

   窗口显示时，隐藏所有的屏幕装饰（例如状态条）。使窗口占用整个显示区域。
   public static final int FLAG_FULLSCREEN = 0x00000400;
    */
    fun init() {
        mWindowManager.defaultDisplay.getRealSize(
            displaySizePoint
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        mLayoutParams.format = PixelFormat.RGBA_8888   //窗口透明
        mLayoutParams.gravity = Gravity.START or Gravity.TOP  //窗口位置
        mLayoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        mLayoutParams.width = displaySizePoint.x/3 *2
        mLayoutParams.height = displaySizePoint.y/2
        // 可以修改View的初始位置
        mLayoutParams.x = 0
        mLayoutParams.y = 400
        //设置允许显示在刘海
        /*     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                 mLayoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
             }*/
        initDefaultView()
    }

    private fun initDefaultView() {
        context.setTheme(R.style.Theme_Question)
        floatViewBinding = FloatViewBinding.inflate(LayoutInflater.from(context))
        initDefaultViewListener()
        mTouchSlop = ViewConfiguration.get(
            context
        ).scaledTouchSlop
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initDefaultViewListener() {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = App.context.resources.getDimensionPixelSize(resourceId)
            log("statusBarHeight---->$statusBarHeight")
        }
        //设置触摸滑动事件
        floatViewBinding.ivMove.setOnTouchListener { v, event -> //获取到手指处的横坐标和纵坐标
            val x = event.rawX
            val y = event.rawY
            Log.d("click", "onTouch:${event.action} x:$x y:$y")
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    lastDownX = x
                    lastDownY = y
                    mIsTouching = true
                    mLastDownTime = System.currentTimeMillis()
                    v.postDelayed({
                        if (isLongTouch()) {
                            mIsLongTouch = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                mVibrator.vibrate(
                                    VibrationEffect.createWaveform(mPattern, -1),
                                    null
                                )
                            } else {
                                mVibrator.vibrate(mPattern, -1)
                            }
                        }
                    },
                        LONG_CLICK_LIMIT
                    )
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mIsLongTouch) {
                        Log.d(
                            "click",
                            "x:$x y:$y lastDownX:$lastDownX lastDownY:$lastDownY mLayoutParams.x:${mLayoutParams.x} mLayoutParams.y:${mLayoutParams.y}"
                        )
                        val offsetx = x - lastDownX
                        val offsety = y - lastDownY
                        mLayoutParams.x = (mLayoutParams.x + offsetx).toInt()
                        mLayoutParams.y = (mLayoutParams.y + offsety).toInt()
                        Log.d("click", "offsetx:$offsetx offsety:$offsety ")
                        lastDownX = x
                        lastDownY = y
                        Log.d(
                            "click",
                            "lastDownX:$lastDownX lastDownY:$lastDownY mLayoutParams.x:${mLayoutParams.x} mLayoutParams.y:${mLayoutParams.y}"
                        )
                        updateViewLayout()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    mIsLongTouch = false
                    mIsTouching = false
                    if (isClick(event)){
                        v.performClick()
                    }
                }
            }
            true
        }
        floatViewBinding.ivAdjust.setOnTouchListener { v, event -> //获取到手指处的横坐标和纵坐标
            val x = event.rawX
            val y = event.rawY
            Log.d("click", "onTouch:${event.action} x:$x y:$y")
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    lastDownX = x
                    lastDownY = y
                    mIsTouching = true
                    mLastDownTime = System.currentTimeMillis()
                    v.postDelayed({
                        if (isLongTouch()) {
                            mIsLongTouch = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                mVibrator.vibrate(
                                    VibrationEffect.createWaveform(mPattern, -1),
                                    null
                                )
                            } else {
                                mVibrator.vibrate(mPattern, -1)
                            }
                        }
                    },
                        LONG_CLICK_LIMIT
                    )
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mIsLongTouch) {
                        Log.d(
                            "click",
                            "x:$x y:$y lastDownX:$lastDownX lastDownY:$lastDownY mLayoutParams.x:${mLayoutParams.x} mLayoutParams.y:${mLayoutParams.y}"
                        )
                        val offsetx = x - lastDownX
                        val offsety = y - lastDownY
                        mLayoutParams.width = (mLayoutParams.width + offsetx).toInt()
                        mLayoutParams.height = (mLayoutParams.height + offsety).toInt()
                        Log.d("click", "offsetx:$offsetx offsety:$offsety ")
                        lastDownX = x
                        lastDownY = y
                        Log.d(
                            "click",
                            "lastDownX:$lastDownX lastDownY:$lastDownY mLayoutParams.x:${mLayoutParams.x} mLayoutParams.y:${mLayoutParams.y}"
                        )
                        updateViewLayout()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    mIsLongTouch = false
                    mIsTouching = false
                    if (isClick(event)){
                        v.performClick()
                    }
                }
            }
            true
        }

        floatViewBinding.ivSearch.setOnClickListener {
            floatViewBinding.webView.apply {
                loadUrl(floatViewBinding.editText.text.toString())
            }
            InputMethodUtils.closedInputMethod()
        }
        floatViewBinding.editText.setOnClickListener {
            InputMethodUtils.openInputMethod(floatViewBinding.editText)
        }
        floatViewBinding.ivBack.setOnClickListener {
            if (floatViewBinding.webView.canGoBack()){
                floatViewBinding.webView.goBack()
            }
        }
        floatViewBinding.webView.apply {
            webViewClient = object: WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    view?.loadUrl(request?.url.toString())
                    return true
                }
            }
            settings.javaScriptEnabled = true
            settings.setSupportZoom(true)
            settings.setAppCacheEnabled(true);
            //设置 缓存模式
            settings.setCacheMode(WebSettings.LOAD_DEFAULT)
            // 开启 DOM storage API 功能
            settings.domStorageEnabled = true
            settings.displayZoomControls = true
            loadUrl(floatViewBinding.editText.text.toString())
        }
        floatViewBinding.ivHide.setOnClickListener {
            hideWindowView()
        }
    }
    /**
     * 判断是都长按
     */
    private fun isLongTouch(): Boolean {
        val time = System.currentTimeMillis()
        return mIsTouching && time - mLastDownTime >= LONG_CLICK_LIMIT
    }

    /**
     * 判断是否是单击
     *
     * @param event
     * @return
     */
    private fun isClick(event: MotionEvent): Boolean {
        val offsetX = abs(event.rawX - lastDownX)
        val offsetY = abs(event.rawX - lastDownY)
        val time = System.currentTimeMillis() - mLastDownTime
        return offsetX < mTouchSlop * 2 && offsetY < mTouchSlop * 2 && time < CLICK_LIMIT
    }
    private fun addView() {
        log("addView()")
        isViewAdd = true
        kotlin.runCatching {
            mWindowManager.addView(
                floatViewBinding.root,
                mLayoutParams
            )
        }
    }

    private fun removeView() {
        log("removeView()")
        isViewAdd = false
        kotlin.runCatching {
            mWindowManager.removeViewImmediate(
                floatViewBinding.root
            )
        }
    }

    fun updateViewLayout() {
        kotlin.runCatching {
            mWindowManager.updateViewLayout(
                floatViewBinding.root,
                mLayoutParams
            )
        }
    }

    /**
     * 暴露的接口,用于显示windows
     */
    fun show() {
        log("show() isViewAdd:$isViewAdd isVisibility:$isVisibility")
        when{
            !isViewAdd->{
                isViewAdd = true
                mWindowManager.addView(
                    floatViewBinding.root,
                    mLayoutParams
                )
                NotificationManager.showNotification()
            }
            !isVisibility->{
                visibleWindowView()
            }
        }
    }

    /**
     * 暴露的接口,用于移除windows
     */
    fun dismissWindow() {
        isVisibility = false
        log("dismissWindow()")
        removeView()
        destroy()
    }

    /**
     * 暴露的接口,隐藏view
     */
    fun hideWindowView() {
        isVisibility = false
        log("hideWindowView()")
        floatViewBinding.root.visibility = View.GONE
    }

    /**
     * 暴露的接口,显示view ,适配屏幕位置
     */
    fun visibleWindowView() {
        isVisibility = true
        log("visibleWindowView()")
        floatViewBinding.root.visibility = View.VISIBLE
    }


    /**
     * 暴露的接口,检查悬浮窗权限
     * @param context
     */
    fun checkFloatWindowPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }
    fun gotoSetting(context: FragmentActivity){
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS")
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        context.startActivity(intent)
    }
    /**
     * 暴露的接口,显示开启悬浮窗权限的弹窗
     * @param fragment
     */
    fun showOpenPermissionDialog(context: FragmentActivity) {
        AlertDialog.Builder(context)
            .setTitle("此功能需要开启显示在应用上层权限")
            .setMessage("此功能需要开启显示在应用上层权限,请开启!")
            .setNegativeButton("我不") { dialogInterface, i ->
                Toast.makeText(context,"不开权限,你玩个屁~",Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    Process.killProcess(Process.myPid())
                    System.exit(10)
                },1000)

            }
            .setPositiveButton("去开启") { dialogInterface, i ->
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivityForResult(intent, 0X001)
                dialogInterface.dismiss()
            }
            .create().show()
    }

    /**
     * 取消一些回调,避免内存泄漏
     */
    private fun destroy() {
        floatViewBinding.root.handler?.removeCallbacksAndMessages(null)
    }
}