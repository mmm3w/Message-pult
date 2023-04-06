package com.mitsuki.messagepult

import com.mitsuki.armory.httprookie.convert.Convert
import okhttp3.Response
import org.json.JSONObject

class AccessTokenConvert : Convert<Pair<String, Long>> {
    override fun convertResponse(response: Response): Pair<String, Long> {
        val webStr =
            response.body?.string() ?: throw IllegalArgumentException("response content is null")
        response.close()
        val json = JSONObject(webStr)

        val errorCode = json.getInt("errcode")
        if (errorCode == 0) {
            return json.getString("access_token") to json.getLong("expires_in")
        } else {
            val message = json.getString("errmsg")
            throw IllegalStateException("Wechat error: $errorCode $message")
        }
    }
}