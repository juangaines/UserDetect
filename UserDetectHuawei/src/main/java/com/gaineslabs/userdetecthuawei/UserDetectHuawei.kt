package com.gaineslabs.userdetecthuawei

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.huawei.hms.common.ApiException
import com.huawei.hms.support.api.safetydetect.SafetyDetect
import com.huawei.hms.support.api.safetydetect.SafetyDetectStatusCodes

class UserDetectHuawei constructor(val context: Context, val appId:String, val onSuccess:((String)->Unit), val onError:((String)->Unit)){

    fun detect (){
        SafetyDetect.getClient(context)
            .userDetection(appId)
            .addOnSuccessListener { userDetectResponse ->
                // Indicates communication with the service was successful.
                onSuccess(userDetectResponse.responseToken)
            }
            .addOnFailureListener { e -> // There was an error communicating with the service.
                val errorMsg: String?
                errorMsg = if (e is ApiException) {
                    // An error with the HMS API contains some additional details.
                    val apiException = e
                    (SafetyDetectStatusCodes.getStatusCodeString(apiException.statusCode)
                            + ": " + apiException.message)
                    // You can use the apiException.getStatusCode() method to get the status code.
                } else {
                    // Unknown type of error has occurred.
                    e.message
                }
                onError( errorMsg?:"Error not recognized")

            }
    }

}