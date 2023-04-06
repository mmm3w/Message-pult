package com.mitsuki.messagepult

import com.mitsuki.armory.httprookie.convert.Convert
import okhttp3.Response
import org.json.JSONObject

class SendConvert : Convert<Boolean> {
    override fun convertResponse(response: Response): Boolean {
        val webStr =
            response.body?.string() ?: throw IllegalArgumentException("response content is null")
        response.close()
        val json = JSONObject(webStr)
        return when (val errorCode = json.getInt("errcode")) {
            0 -> false
            42001 -> true
            else -> {
                val message = json.getString("errmsg")
                throw IllegalStateException("Wechat error: $errorCode $message")
            }
        }
    }
}