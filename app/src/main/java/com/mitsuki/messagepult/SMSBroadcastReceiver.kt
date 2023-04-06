package com.mitsuki.messagepult

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.telephony.SmsMessage
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class SMSBroadcastReceiver : BroadcastReceiver() {
    private val iWechat by lazy { IWechat() }
    private val mHandlerThread by lazy { HandlerThread("SMSBroadcastReceiver-${System.currentTimeMillis()}").apply { start() } }
    private val mHandler by lazy { Handler(mHandlerThread.looper) }

    @SuppressLint("SimpleDateFormat")
    private val mDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "android.provider.Telephony.SMS_RECEIVED" -> {
                mHandler.post {
                    @Suppress("UNCHECKED_CAST", "DEPRECATION")
                    val pdus = intent.extras?.get("pdus") as? Array<Any>

                    val stringBuilder = StringBuilder()

                    var lastAddress: String? = null
                    var lastDate: String? = null

                    pdus?.forEach { pdu ->
                        @Suppress("DEPRECATION")
                        val sms: SmsMessage = SmsMessage.createFromPdu(pdu as ByteArray)
                        val date = mDateFormat.format(sms.timestampMillis) // 短信时间
                        val address: String? = sms.originatingAddress // 获取发信人号码
                        if (lastAddress != address) {
                            if (lastDate != null) {
                                stringBuilder.append("—— $lastDate\n\n")
                            }
                            lastAddress = address
                            stringBuilder.append("收到来自 $address 的短信：\n")
                        }
                        stringBuilder.append("${sms.messageBody}\n")
                        lastDate = date
                    }

                    if (lastDate != null) {
                        stringBuilder.append("—— $lastDate")
                    }
                    val content = stringBuilder.toString()
                    if (content.isNotEmpty()) {
                        try {
                            iWechat.sendText(context, content)
                        } catch (_: Exception) {

                        }
                    }
                }
            }
            "android.intent.action.PHONE_STATE" -> {
                @Suppress("DEPRECATION")
                if (ContextCompat.getSystemService(
                        context,
                        TelephonyManager::class.java
                    )?.callState == TelephonyManager.CALL_STATE_RINGING
                ) {
                    val phone = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                    val date = mDateFormat.format(System.currentTimeMillis())
                    try {
                        iWechat.sendText(context, "接到 $phone 的来电\n—— $date")
                    } catch (_: Exception) {

                    }
                }
            }
        }
    }
}