package com.ml.question

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ml.question.databinding.ActivityMainBinding
import com.ml.question.databinding.FloatViewBinding

class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        initView()
    }

    private fun initView() {
        activityMainBinding.btnShowFloat.setOnClickListener {
            when{
                !NotificationManager.isNotificationEnable()->{
                    NotificationManager.shouldOpenNotificationPermission(fragmentActivity = this)
                }
                WindowsHelper.checkFloatWindowPermission(this)->{
                    WindowsHelper.show()
                }
                else->{
                    WindowsHelper.showOpenPermissionDialog(this)
                }
            }
        }
        activityMainBinding.btnOpenNotification.setOnClickListener {
            NotificationManager.shouldOpenNotificationPermission(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            1->{
                if (WindowsHelper.checkFloatWindowPermission(this)) {
                    WindowsHelper.show()
                }else{
                    WindowsHelper.gotoSetting(this)
                }
            }
        }
    }
}