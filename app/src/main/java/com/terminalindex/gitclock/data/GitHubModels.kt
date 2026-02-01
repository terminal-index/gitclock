package com.terminalindex.gitclock.data

data class ContributionDay(
    val date: String,
    val count: Int,
    val level: Int 
)

data class JogruberResponse(
    val total: Map<String, Int>,
    val contributions: List<JogruberDay>
)

data class JogruberDay(
    val date: String,
    val count: Int,
    val level: Int
)

data class GraphQLRequest(val query: String)

data class GraphQLResponse(
    val data: GraphQLData?
)

data class GraphQLData(
    val user: GraphQLUser?
)

data class GraphQLUser(
    val contributionsCollection: ContributionsCollection,
    val pullRequests: TotalCount?,
    val issues: TotalCount?,
    val avatarUrl: String?
)

data class TotalCount(
    val totalCount: Int
)

data class ContributionsCollection(
    val contributionCalendar: ContributionCalendar
)

data class ContributionCalendar(
    val totalContributions: Int,
    val weeks: List<CalendarWeek>
)

data class CalendarWeek(
    val contributionDays: List<CalendarDay>
)

data class CalendarDay(
    val contributionCount: Int,
    val date: String,
    val color: String 
)
