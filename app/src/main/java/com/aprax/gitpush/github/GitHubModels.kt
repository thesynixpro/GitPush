package com.aprax.gitpush.github

import com.google.gson.annotations.SerializedName

// --- User / repo ---
data class GhUser(val login: String = "", val id: Long = 0)
data class GhRepo(
    val id: Long = 0,
    @SerializedName("full_name") val fullName: String = "",
    val private: Boolean = false,
    @SerializedName("default_branch") val defaultBranch: String = "main",
    val permissions: GhPermissions? = null
)
data class GhPermissions(val admin: Boolean = false, val push: Boolean = false, val pull: Boolean = false)
data class GhBranch(val name: String = "", val commit: GhCommitRef? = null)
data class GhCommitRef(val sha: String = "")

// --- Contents API ---
data class GhContentGet(
    val sha: String? = null,
    val type: String? = null,
    val path: String? = null
)
data class GhCreateFileReq(
    val message: String,
    val content: String, // base64
    val branch: String,
    val sha: String? = null
)
data class GhCreateFileResp(val content: GhContentGet? = null, val commit: GhCommitRef? = null)

// --- Git Data API (single-commit multi-file push) ---
data class GhBlobReq(val content: String, val encoding: String = "base64")
data class GhBlobResp(val sha: String = "")
data class GhTreeEntry(
    val path: String,
    val mode: String = "100644",
    val type: String = "blob",
    val sha: String
)
data class GhTreeReq(
    @SerializedName("base_tree") val baseTree: String?,
    val tree: List<GhTreeEntry>
)
data class GhTreeResp(val sha: String = "")
data class GhCommitReq(
    val message: String,
    val tree: String,
    val parents: List<String>
)
data class GhCommitResp(val sha: String = "")
data class GhUpdateRefReq(val sha: String, val force: Boolean = false)
data class GhRefResp(val ref: String = "", val `object`: GhCommitRef? = null)
data class GhRefGet(@SerializedName("object") val obj: GhCommitRef? = null)
