package com.terminalindex.gitclock.data

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GitClockRepository {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    suspend fun fetchContributions(username: String, token: String?): GitHubData {
        return withContext(Dispatchers.IO) {
            if (!token.isNullOrBlank()) {
                fetchWithToken(username, token)
            } else {
                fetchPublic(username)
            }
        }
    }

    suspend fun fetchUser(token: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.github.com/user")
                .addHeader("Authorization", "Bearer $token")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to fetch user: ${response.code}")
                val body = response.body?.string() ?: throw IOException("Empty body")
                 val json = gson.fromJson(body, Map::class.java)
                 json["login"] as String
            }
        }
    }

    private fun fetchPublic(username: String): GitHubData {
        val request = Request.Builder()
            .url("https://github-contributions-api.jogruber.de/v4/$username?y=last")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to fetch public data: ${response.code}")
            
            val body = response.body?.string() ?: throw IOException("Empty body")
            val data = gson.fromJson(body, JogruberResponse::class.java)
            
            val contributions = data.contributions.map { 
                ContributionDay(it.date, it.count, it.level)
            }
            return GitHubData(contributions, 
                avatarUrl = "https://github.com/$username.png"
            )
        }
    }

    private fun fetchWithToken(username: String, token: String): GitHubData {
        val query = """
            query {
              user(login: "$username") {
                avatarUrl
                contributionsCollection {
                  contributionCalendar {
                    weeks {
                      contributionDays {
                        contributionCount
                        date
                        color
                      }
                    }
                  }
                }
                pullRequests(states: OPEN) {
                  totalCount
                }
                issues(filterBy: {assignee: "$username"}, states: OPEN) {
                  totalCount
                }
              }
            }
        """.trimIndent()
        
        val jsonQuery = gson.toJson(GraphQLRequest(query))
        
        val request = Request.Builder()
            .url("https://api.github.com/graphql")
            .addHeader("Authorization", "Bearer $token")
            .post(jsonQuery.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("GraphQL Error: ${response.code}")
            
            val body = response.body?.string() ?: throw IOException("Empty body")
            val graphQLResponse = gson.fromJson(body, GraphQLResponse::class.java)
            val user = graphQLResponse.data?.user ?: throw IOException("No data found for user")
            val calendar = user.contributionsCollection.contributionCalendar

            val days = mutableListOf<ContributionDay>()
            calendar.weeks.forEach { week ->
                week.contributionDays.forEach { day ->
                    days.add(ContributionDay(day.date, day.contributionCount, 0)) 
                }
            }
            
            return GitHubData(
                contributions = days,
                prCount = user.pullRequests?.totalCount ?: 0,
                issueCount = user.issues?.totalCount ?: 0,
                avatarUrl = user.avatarUrl
            )
        }
    }
}
