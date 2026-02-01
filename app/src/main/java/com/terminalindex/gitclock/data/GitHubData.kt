package com.terminalindex.gitclock.data

data class GitHubData(
    val contributions: List<ContributionDay>,
    val prCount: Int = 0,
    val issueCount: Int = 0,
    val avatarUrl: String? = null
)
