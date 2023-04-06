package com.mitsuki.messagepult

import android.content.Context
import android.content.SharedPreferences
import com.mitsuki.armory.httprookie.get
import com.mitsuki.armory.httprookie.post
import com.mitsuki.armory.httprookie.request.json
import com.mitsuki.armory.httprookie.request.urlParams
import com.mitsuki.armory.httprookie.response.Response
import okhttp3.OkHttpClient
import org.json.JSONObject

class IWechat {

    private val accessTokenSync = Any()
    private val client by lazy { OkHttpClient.Builder().build() }

    private val mAccessTokenConvert by lazy { AccessTokenConvert() }
    private val mSendConvert by lazy { SendConvert() }

    private fun getAccessToken(sp: SharedPreferences, isExpired: Boolean): String {
        return if (isExpired) {
            //过期的，强制重新获取
            synchronized(accessTokenSync) {
                getAccessTokenFromNet(sp)
            }
        } else {
            getAccessTokenFromSP(sp) ?: synchronized(accessTokenSync) {
                getAccessTokenFromSP(sp) ?: getAccessTokenFromNet(sp)
            }
        }
    }

    private fun getAccessTokenFromSP(sp: SharedPreferences): String? {
        return sp.getString("accessToken", null)?.takeIf { it.isNotEmpty() } //不是空串
            ?.takeIf { sp.getLong("expiresTime", 0) > System.currentTimeMillis() } //还未过期
    }

    private fun getAccessTokenFromNet(sp: SharedPreferences): String {
        val id = sp.getString("corpid", null)
            ?.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("corpid is null")
        val secret = sp.getString("corpsecret", null)
            ?.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("corpsecret is null")
        val currentTime = System.currentTimeMillis()

        val result =
            client.get<Pair<String, Long>>("https://qyapi.weixin.qq.com/cgi-bin/gettoken") {
                convert = mAccessTokenConvert
                urlParams("corpid", id)
                urlParams("corpsecret", secret)
            }.execute()

        return when (result) {
            is Response.Success<Pair<String, Long>> -> {
                val body = result.requireBody()
                sp.edit()
                    .putString("accessToken", body.first)
                    .putLong("expiresTime", currentTime + body.second * 1000)
                    .apply()
                body.first
            }
            is Response.Fail<*> -> throw result.throwable
        }
    }


    fun sendText(context: Context, content: String, reloadToken: Boolean = false) {
        val sp = context.getSharedPreferences("iWechat", Context.MODE_PRIVATE)

        val agentId = sp.getInt("agentid", 0).takeIf { it != 0 }
            ?: throw IllegalArgumentException("agent id is null")

        val token = getAccessToken(sp, reloadToken)
        val result = client.post<Boolean>("https://qyapi.weixin.qq.com/cgi-bin/message/send") {
            convert = mSendConvert
            urlParams("access_token", token)
            json(JSONObject().apply {
                put("touser", "@all")
                put("msgtype", "text")
                put("agentid", agentId)
                put("text", JSONObject().apply {
                    put("content", content)
                })
            }.toString())
        }.execute()
        when (result) {
            is Response.Success<Boolean> -> {
                if (result.requireBody()) {
                    sendText(context, content, true)
                }
            }
            is Response.Fail<*> -> throw  result.throwable
        }
    }

}