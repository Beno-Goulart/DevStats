# Graph Report - C:\Users\Windows\Downloads\DevStats  (2026-07-12)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 341 nodes · 807 edges · 17 communities (10 shown, 7 thin omitted)
- Extraction: 79% EXTRACTED · 21% INFERRED · 0% AMBIGUOUS · INFERRED: 173 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c1564a48`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- UserData
- DatabaseService
- DynamicField
- GithubProfile
- OAuthService
- WebhookHandler
- Main.java
- DateUtilsTest
- DiscordTokenResponse
- HttpUtils
- Config.java
- .encodeFormBody
- ImageFieldTest
- mvnw
- recordToolUse.sh script
- devstats:devstats-bot

## God Nodes (most connected - your core abstractions)
1. `DatabaseService` - 42 edges
2. `GithubProfile` - 28 edges
3. `UserData` - 27 edges
4. `OAuthService` - 23 edges
5. `WidgetSyncService` - 18 edges
6. `DiscordTokenResponse` - 17 edges
7. `DynamicField` - 16 edges
8. `GithubService` - 16 edges
9. `DatabaseServiceTest` - 14 edges
10. `DiscordUser` - 12 edges

## Surprising Connections (you probably didn't know these)
- `Main` --references--> `DatabaseService`  [EXTRACTED]
  DevStats-Backend/src/main/java/devstats/Main.java → DevStats-Backend/src/main/java/devstats/services/DatabaseService.java
- `WidgetCommand` --references--> `WidgetSetupCommand`  [EXTRACTED]
  DevStats-Backend/src/main/java/devstats/commands/WidgetCommand.java → DevStats-Backend/src/main/java/devstats/commands/WidgetSetupCommand.java
- `WidgetRefreshCommand` --references--> `OAuthService`  [EXTRACTED]
  DevStats-Backend/src/main/java/devstats/commands/WidgetRefreshCommand.java → DevStats-Backend/src/main/java/devstats/services/OAuthService.java
- `OAuthCallbackHandler` --references--> `DatabaseService`  [EXTRACTED]
  DevStats-Backend/src/main/java/devstats/http/OAuthCallbackHandler.java → DevStats-Backend/src/main/java/devstats/services/DatabaseService.java
- `WebhookHandler` --references--> `DatabaseService`  [EXTRACTED]
  DevStats-Backend/src/main/java/devstats/http/WebhookHandler.java → DevStats-Backend/src/main/java/devstats/services/DatabaseService.java

## Import Cycles
- None detected.

## Communities (17 total, 7 thin omitted)

### Community 0 - "UserData"
Cohesion: 0.12
Nodes (6): BeforeEach, UserData, Test, UserDataTest, DatabaseServiceTest, Test

### Community 1 - "DatabaseService"
Cohesion: 0.09
Nodes (19): Connection, Logger, Override, SlashCommandInteractionEvent, WidgetCommand, Logger, SlashCommandInteractionEvent, WidgetGithubCommand (+11 more)

### Community 2 - "DynamicField"
Cohesion: 0.09
Nodes (8): DynamicField, ImageField, WidgetData, WidgetPayload, DiscordWidgetService, Logger, DynamicFieldTest, Test

### Community 3 - "GithubProfile"
Cohesion: 0.12
Nodes (6): GithubProfile, GithubService, JsonNode, Logger, GithubServiceTest, Test

### Community 4 - "OAuthService"
Cohesion: 0.10
Nodes (12): SlashCommandInteractionEvent, WidgetSetupCommand, HttpExchange, Logger, Override, OAuthCallbackHandler, DiscordUser, JsonIgnoreProperties (+4 more)

### Community 5 - "WebhookHandler"
Cohesion: 0.11
Nodes (10): HttpExchange, Logger, Override, WebhookHandler, JsonNode, JsonUtils, Test, JsonUtilsTest (+2 more)

### Community 6 - "Main.java"
Cohesion: 0.14
Nodes (11): HttpServer, Logger, OAuthServer, HttpServer, Logger, WebhookServer, Logger, Main (+3 more)

### Community 7 - "DateUtilsTest"
Cohesion: 0.20
Nodes (3): DateUtils, DateUtilsTest, Test

### Community 9 - "HttpUtils"
Cohesion: 0.29
Nodes (4): HttpUtils, HttpClient, HttpRequest, HttpResponse

## Knowledge Gaps
- **2 isolated node(s):** `recordToolUse.sh script`, `devstats:devstats-bot`
  These have ≤1 connection - possible missing edges or undocumented components.
- **7 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DatabaseService` connect `DatabaseService` to `UserData`, `OAuthService`, `WebhookHandler`, `Main.java`?**
  _High betweenness centrality (0.207) - this node is a cross-community bridge._
- **Why does `GithubService` connect `GithubProfile` to `DatabaseService`, `DynamicField`, `Main.java`?**
  _High betweenness centrality (0.098) - this node is a cross-community bridge._
- **Why does `OAuthService` connect `OAuthService` to `UserData`, `DatabaseService`, `Main.java`?**
  _High betweenness centrality (0.098) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `OAuthService` (e.g. with `.handlePush()` and `.main()`) actually correct?**
  _`OAuthService` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `recordToolUse.sh script`, `devstats:devstats-bot` to the rest of the system?**
  _2 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `UserData` be split into smaller, more focused modules?**
  _Cohesion score 0.1192156862745098 - nodes in this community are weakly interconnected._
- **Should `DatabaseService` be split into smaller, more focused modules?**
  _Cohesion score 0.09175377468060394 - nodes in this community are weakly interconnected._