package com.example.userdetectjdgp.ui.login

import android.annotation.SuppressLint
import android.app.Activity
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.userdetectjdgp.R
import com.gaineslabs.userdetecthuawei.UserDetectHuawei
//import com.huawei.hms.common.ApiException
//import com.huawei.hms.support.api.safetydetect.SafetyDetect
//import com.huawei.hms.support.api.safetydetect.SafetyDetectStatusCodes
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutionException

class LoginActivity : AppCompatActivity() {

    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        username = findViewById<EditText>(R.id.username)
        password = findViewById<EditText>(R.id.password)
        val login = findViewById<Button>(R.id.login)
        val loading = findViewById<ProgressBar>(R.id.loading)

        loginViewModel = ViewModelProviders.of(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
            }
            setResult(Activity.RESULT_OK)

            //Complete and destroy login activity once successful
            //finish()
        })

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                detect()
            }
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }

    private fun detect() {
        Log.i(
            TAG,
            "User detection start."
        )

        val userDetect = UserDetectHuawei(this, APP_ID,
            onSuccess = { responseToken ->
                val verifySucceed: Boolean =
                    verify(
                        responseToken
                    )
                if (verifySucceed) {
                    Toast.makeText(
                        this,
                        "User detection succeed and verify succeed",
                        Toast.LENGTH_SHORT
                    )
                        .show()

                } else {
                    Toast.makeText(
                        this,
                        "User detection succeed but verify fail,"
                                + "please replace verify url with your's server address",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
                loginViewModel.login(username.text.toString(), password.text.toString())
            },
            onError = {errorMsg->
                Log.i(
                    TAG,
                    "User detection fail. Error info: $errorMsg"
                )
                Toast.makeText(
                    this,
                    errorMsg,
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        userDetect.detect()
        /*

        SafetyDetect.getClient(this)
            .userDetection(APP_ID)
            .addOnSuccessListener { userDetectResponse ->
                /**
                 * Called after successfully communicating with the SafetyDetect API.
                 * The #onSuccess callback receives an
                 * [com.huawei.hms.support.api.entity.safetydetect.UserDetectResponse] that contains a
                 * responseToken that can be used to get user detect result.
                 */
                // Indicates communication with the service was successful.
                Log.i(
                    TAG,
                    "User detection succeed, response = $userDetectResponse"
                )
                val verifySucceed: Boolean =
                    verify(
                        userDetectResponse.responseToken
                    )
                if (verifySucceed) {
                    Toast.makeText(
                        this,
                        "User detection succeed and verify succeed",
                        Toast.LENGTH_SHORT
                    )
                        .show()

                } else {
                    Toast.makeText(
                       this,
                        "User detection succeed but verify fail,"
                                + "please replace verify url with your's server address",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
                loginViewModel.login(username.text.toString(), password.text.toString())
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
                Log.i(
                    TAG,
                    "User detection fail. Error info: $errorMsg"
                )
                Toast.makeText(
                    this,
                    errorMsg,
                    Toast.LENGTH_SHORT
                ).show()
            }

         */
    }

    companion object {
        val TAG: String = LoginActivity::class.java.simpleName
        const val APP_ID: String = "102817989"

        @SuppressLint("StaticFieldLeak")
        fun verify(responseToken: String): Boolean {
            try {
                return object : AsyncTask<String, Void, Boolean>() {
                    override fun doInBackground(vararg strings: String?): Boolean {
                        val input = strings[0]!!
                        val jsonObject = JSONObject()
                        return try {
                            val baseUrl = "http://192.168.1.32:8084/userdetect/verify"
                            jsonObject.put("response", input)
                            val result: String? =
                                sendPost(
                                    baseUrl,
                                    jsonObject
                                )
                            val resultJson = JSONObject(result!!)
                            val success = resultJson.getBoolean("success")
                            // if success is true that means the user is real human instead of a robot.
                            Log.i(
                                TAG,
                                "verify: result = $success"
                            )
                            success
                        } catch (e: Exception) {
                            e.printStackTrace()
                            false
                        }
                    }

                }.execute(responseToken).get()
            } catch (e: ExecutionException) {
                e.printStackTrace()
                return false
            } catch (e: InterruptedException) {
                e.printStackTrace()
                return false
            }

        }

        @Throws(java.lang.Exception::class)
        fun sendPost(baseUrl: String, postDataParams: JSONObject): String? {
            val url = URL(baseUrl)
            val conn =
                url.openConnection() as HttpURLConnection
            conn.readTimeout = 20000
            conn.connectTimeout = 20000
            conn.requestMethod = "POST"
            conn.doInput = true
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.outputStream.use { os ->
                BufferedWriter(
                    OutputStreamWriter(
                        os,
                        StandardCharsets.UTF_8
                    )
                ).use { writer ->
                    writer.write(postDataParams.toString())
                    writer.flush()
                }
            }
            val responseCode = conn.responseCode // To Check for 200
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val `in` =
                    BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuffer()
                var line: String?
                while (`in`.readLine().also { line = it } != null) {
                    sb.append(line)
                    break
                }
                `in`.close()
                return sb.toString()
            }
            return null
        }


    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}