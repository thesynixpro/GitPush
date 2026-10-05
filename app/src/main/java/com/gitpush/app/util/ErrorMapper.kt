package com.gitpush.app.util

import retrofit2.HttpException
import java.net.UnknownHostException

object ErrorMapper {
    fun githubError(e: Exception): String = when {
        e is HttpException && e.code() == 401 ->
            "Your GitHub Personal Access Token is invalid or expired."
        e is HttpException && e.code() == 403 -> {
            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull() ?: ""
            when {
                body.contains("rate limit", true) || body.contains("abuse", true) ->
                    "GitHub API rate limit reached. Please wait and try again."
                else -> "This token does not have permission to write to this repository."
            }
        }
        e is HttpException && e.code() == 404 ->
            "The repository could not be accessed with this token."
        e is HttpException && e.code() == 422 ->
            "GitHub rejected the upload (validation failed). Check file sizes and branch name."
        e is HttpException && e.code() == 429 ->
            "GitHub API rate limit reached. Please wait and try again."
        e is UnknownHostException || e is java.net.ConnectException ->
            "Unable to connect to GitHub. Check your internet connection."
        e is java.net.SocketTimeoutException ->
            "Connection to GitHub timed out. Check your internet connection and retry."
        e.message?.contains("could not be read", true) == true ->
            "This file could not be read from the selected folder."
        else -> "Something went wrong. Check your connection and try again."
    }

    const val NO_INTERNET = "No internet connection. Connect to the internet and try again."
    const val AUTH_FAILED = "GitHub authentication failed. Please check your Personal Access Token and permissions."
}
