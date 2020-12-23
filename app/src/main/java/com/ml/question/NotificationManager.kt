package com.ml.question

import android.annotation.SuppressLint
import android.app.*
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.FragmentActivity


/**
 * Author: Menglong Ma
 * Email: mml2015@126.com
 * Date: 12/21/20 8:52 PM
 * Description: This is NotificationManager
 * Package: com.ml.question
 * Project: Question
 */
object NotificationManager {
    private const val NOTIFICATION_CHANNEL_NAME = "tz"
    private var isCreateChannel = false
    private var isRegister = false
    val notificationManager by lazy {
        App.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @SuppressLint("NewApi")
    private fun buildNotification(): Notification? {
        var builder: Notification.Builder? = null
        var notification: Notification? = null
        if (Build.VERSION.SDK_INT >= 26) {
            //Android O上对Notification进行了修改，如果设置的targetSDKVersion>=26建议使用此种方式创建通知栏
            val channelId: String = App.context.packageName
            if (!isCreateChannel) {
                val notificationChannel = NotificationChannel(
                    channelId,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationChannel.lightColor = Color.BLUE //小圆点颜色
                notificationChannel.setShowBadge(true) //是否在久按桌面图标时显示此渠道的通知
                notificationChannel.setSound(null, null)
                notificationManager.createNotificationChannel(notificationChannel)
                isCreateChannel = true
            }
            builder = Notification.Builder(App.context, channelId)
        } else {
            builder = Notification.Builder(App.context)
        }
        builder.setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("question me")
            .setContentText("小可爱正在守护你哦~")
            .setWhen(System.currentTimeMillis())
            .setContentIntent(
                PendingIntent.getActivity(
                    App.context, 100,
                    Intent(App.context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setOngoing(true)

        val actionHide = Notification.Action.Builder(
            Icon.createWithResource("", R.drawable.ic_baseline_visibility_off_24),
            "隐藏",
            getPendingIntent(WindowsHelper.ACTION_HIDE)
        )
            .build()
        val actionShow = Notification.Action.Builder(
            Icon.createWithResource(
                "",
                R.drawable.ic_baseline_visibility_24
            ),
            "显示",
            getPendingIntent(WindowsHelper.ACTION_SHOW)
        )
            .build()
        val actionDestroy = Notification.Action.Builder(
            Icon.createWithResource(
                "",
                R.drawable.ic_baseline_visibility_24
            ),
            "销毁",
            getPendingIntent(WindowsHelper.ACTION_DESTROY)
        )
            .build()
        builder.addAction(actionShow)
        builder.addAction(actionHide)
        builder.addAction(actionDestroy)
        notification = if (Build.VERSION.SDK_INT >= 16) {
            builder.build()
        } else {
            return builder.notification
        }
        return notification
    }
    fun showNotification(){
        notificationManager.notify(22, buildNotification())
        register()
    }
    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent()
        intent.action = action
        return PendingIntent.getBroadcast(
            App.context,
            System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    var mReceiver = object :BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action){
                WindowsHelper.ACTION_SHOW -> {
                    WindowsHelper.show()
                }
                WindowsHelper.ACTION_HIDE -> {
                    WindowsHelper.hideWindowView()
                    InputMethodUtils.closedInputMethod()
                }
                WindowsHelper.ACTION_DESTROY->{
                    WindowsHelper.dismissWindow()
                    InputMethodUtils.closedInputMethod()
                    notificationManager.cancel(22)
                }
            }
        }
    }
    private fun register(){
        if (!isRegister) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(WindowsHelper.ACTION_HIDE)
            intentFilter.addAction(WindowsHelper.ACTION_SHOW)
            intentFilter.addAction(WindowsHelper.ACTION_DESTROY)
            App.context.registerReceiver(mReceiver, intentFilter)
            isRegister = true
        }
    }
   fun shouldOpenNotificationPermission(fragmentActivity: FragmentActivity){
       if (isNotificationEnable()){
           Toast.makeText(fragmentActivity,"🐖已经打开了,不用点了",Toast.LENGTH_SHORT).show()
       }else{
           AlertDialog.Builder(fragmentActivity)
               .setTitle("提示")
               .setMessage("需要打开通知权限,否则你隐藏了悬浮窗以后,不能打开")
               .setPositiveButton("去开启"){d,w->
                   goOpenNotificationPermission()
               }
               .setNegativeButton("我不"){d,w->
                   Toast.makeText(fragmentActivity,"你没的选择",Toast.LENGTH_SHORT).show()
                   shouldOpenNotificationPermission(fragmentActivity)
               }
               .setCancelable(false)
               .show()
       }
   }

    fun isNotificationEnable():Boolean{
        val notification = NotificationManagerCompat.from(App.context)
        return notification.areNotificationsEnabled()
    }
    fun goOpenNotificationPermission(){
        val localIntent = Intent()
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 9) {
            localIntent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
            localIntent.data =
                Uri.fromParts("package", App.context.packageName, null)
        } else if (Build.VERSION.SDK_INT <= 8) {
            localIntent.action = Intent.ACTION_VIEW
            localIntent.setClassName(
                "com.android.settings",
                "com.android.settings.InstalledAppDetails"
            )
            localIntent.putExtra(
                "com.android.settings.ApplicationPkgName",
                App.context.packageName
            )
        }
        App.context.startActivity(localIntent)
    }
}