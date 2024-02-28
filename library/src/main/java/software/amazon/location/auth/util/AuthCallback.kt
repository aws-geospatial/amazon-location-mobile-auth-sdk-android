package software.amazon.location.auth.util

interface AuthCallback {
    fun newPasswordRequired()
    fun signInFailed()
}
