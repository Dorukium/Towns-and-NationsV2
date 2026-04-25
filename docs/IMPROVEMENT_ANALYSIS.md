# Towns and Nations V2 - Comprehensive Improvement Analysis

This document provides a detailed analysis of the Towns and Nations Minecraft plugin codebase, identifying areas for improvement, potential new features, and architectural enhancements. The analysis is based on a thorough review of the project structure, source code, configuration files, and existing systems.

---

## Table of Contents

1. [Code Architecture & Quality](#1-code-architecture--quality)
2. [Storage & Database](#2-storage--database)
3. [API Improvements](#3-api-improvements)
4. [War System Enhancements](#4-war-system-enhancements)
5. [Economy System](#5-economy-system)
6. [GUI & User Experience](#6-gui--user-experience)
7. [Testing & CI/CD](#7-testing--cicd)
8. [Performance Optimizations](#8-performance-optimizations)
9. [New Feature Ideas](#9-new-feature-ideas)
10. [Configuration & Localization](#10-configuration--localization)
11. [Security & Validation](#11-security--validation)
12. [Documentation](#12-documentation)

---

## 1. Code Architecture & Quality

### 1.1 TerritoryData God Class (HIGH PRIORITY)

**File:** `TownsAndNations-Common/src/main/java/org/leralix/tan/dataclass/territory/TerritoryData.java` (1194 lines)

The `TerritoryData` class is a massive abstract class that handles territory management, economy, diplomacy, claims, forts, upgrades, banners, and more. This violates the Single Responsibility Principle.

**Recommendation:** Split into focused components using composition:
- `TerritoryEconomy` - treasury, taxes, budget management
- `TerritoryDiplomacy` - relations, proposals, truces
- `TerritoryClaims` - chunk claiming, unclaiming, validation
- `TerritoryUpgrades` - upgrade status, stats computation
- `TerritoryCosmetics` - icons, colors, banners

### 1.2 Static Storage Singletons

Storage classes like `TownDataStorage`, `PlayerDataStorage`, `NewClaimedChunkStorage` etc. use singleton patterns with `getInstance()`. This creates tight coupling and makes testing harder.

**Recommendation:**
- Introduce a `StorageManager` or use dependency injection (e.g., via constructor injection)
- This would make unit testing much easier with mock implementations
- Consider a lightweight DI container or service locator pattern

### 1.3 Naming Inconsistencies

Several naming issues exist in the codebase:
- `RegionDeletednternalEvent` - typo, missing "I" (should be `RegionDeletedInternalEvent`)
- `TerritoryIndependanceInternalEvent` - misspelling (should be `Independence`)
- `SelectTerritoryForLIberation` - incorrect capitalization
- `RessourceRequirement` - French spelling (should be `ResourceRequirement`)
- `UuidLeader` field in `TownData` doesn't follow Java naming conventions (should be `uuidLeader`)
- `PlayerJoinRequestSet` field should be `playerJoinRequestSet`

### 1.4 Reduce Wildcard Imports

Several files use wildcard imports (`import org.bukkit.*;`), which can lead to name collisions and make it harder to track dependencies. Switch to explicit imports.

### 1.5 Missing `@Override` Annotations

Review abstract method implementations to ensure all overrides are properly annotated for compile-time safety.

---

## 2. Storage & Database

### 2.1 Full Database Migration (HIGH PRIORITY)

**Current State:** The config notes "EXPERIMENTAL FEATURE" for database support. Only tax history and newsletters are stored in the database as of v0.14.4+. All core data (towns, players, regions, kingdoms, wars, forts, landmarks) is stored as JSON files via `JsonStorage`.

**Recommendation:**
- Complete the database migration for all data types
- Create a `StorageBackend` interface that both `JsonStorageBackend` and `DatabaseStorageBackend` implement
- Allow server admins to choose their preferred storage backend
- Support PostgreSQL in addition to MySQL and SQLite
- Add Redis support for caching frequently accessed data

### 2.2 Async Save Operations

**File:** `TownsAndNations-Common/src/main/java/org/leralix/tan/storage/stored/JsonStorage.java`

The `save()` method writes to disk synchronously. For large servers with many towns, this can cause lag spikes on the main thread.

**Recommendation:**
- Move all file I/O to async tasks using `BukkitScheduler.runTaskAsynchronously()`
- Implement a write queue with debouncing to avoid excessive disk writes
- Add periodic auto-save with configurable intervals instead of saving on every change

### 2.3 Data Backup System

The `JsonStorage` class uses atomic file moves (temp file then rename), which is good. However, there's no automatic backup rotation.

**Recommendation:**
- Implement automatic backup rotation (keep last N backups)
- Add configurable backup schedules (beyond the existing `CreateBackup` debug command)
- Support compressed backups (gzip)
- Add data integrity validation on load

### 2.4 Migration Framework

When data formats change between versions, there's no structured migration system.

**Recommendation:**
- Create a versioned migration framework
- Store data schema version in each JSON file
- Auto-migrate on plugin load if schema version mismatch detected

---

## 3. API Improvements

### 3.1 Expand Public API

**Current State:** The API module (`TownsAndNations-API`) provides read-only interfaces (`TanTown`, `TanKingdom`, `TanRegion`, etc.) and manager classes. The API is relatively new (v0.15.0+).

**Missing API capabilities:**
- No way to create/delete towns programmatically via API
- No economy operations (deposit/withdraw from territory treasury)
- No way to modify territory settings
- No bulk operations (e.g., get all towns in a region)
- No async versions of operations

**Recommendation:**
- Add `TanTownManager` with CRUD operations
- Add `TanEconomyManager` for economic operations
- Add event cancellation support for all events
- Add `CompletableFuture<T>` wrappers for async operations
- Version the API properly with `@Since` annotations

### 3.2 REST API / Web Interface

Add an optional REST API module for web dashboards:
- Town/kingdom statistics
- Map data export for web maps
- Player lookup
- Admin operations

### 3.3 PlaceholderAPI Expansion

**Current State:** There are ~40+ PAPI entries covering town, region, kingdom data.

**Missing placeholders:**
- `%tan_player_war_count%` - number of active wars
- `%tan_town_member_online%` - online member count
- `%tan_town_age_days%` - town age in days
- `%tan_town_rank_position%` - leaderboard position by various metrics
- `%tan_player_total_claims%` - total chunks claimed
- `%tan_territory_tax_rate%` - current tax rate
- `%tan_war_status%` - current war status text

---

## 4. War System Enhancements

### 4.1 War System Refactoring

**Current State:** The war system has both a "legacy" system and a newer system. Files in `war/legacy/` suggest an incomplete migration.

**Recommendation:**
- Complete the migration away from legacy war code
- Remove or deprecate `war/legacy/` classes once the new system is stable
- Document the differences between "simple war mode" and the full war system

### 4.2 New War Features

**Siege Mechanics:**
- Add siege equipment (trebuchets, battering rams) as buildable structures
- Implement supply lines - territories far from the attacker's base are harder to maintain
- Add morale system - troops lose morale over long wars

**Naval Warfare:**
- Support for ocean chunk combat
- Ship-based territory control
- Port cities with naval bonuses

**Guerrilla Warfare:**
- Allow small groups to conduct raids without full war declaration
- Sabotage operations (disable landmarks, reduce resource production)
- Espionage system (spy on enemy treasury, troop counts)

### 4.3 War Scoring & Statistics

- Track kill/death ratios per war
- Record territory gained/lost over time
- War MVP awards
- Post-war reports with statistics
- War history archive accessible in-game

### 4.4 Diplomacy Expansion

**Current Relations:** Self, Alliance, Non-Aggression, Neutral, Embargo, War

**New Relations to Add:**
- **Trade Agreement** - reduced trade costs between territories
- **Military Access** - allow troops to pass through territory
- **Defensive Pact** - auto-join wars when an ally is attacked
- **Federation** - multiple kingdoms form a superstate with shared policies
- **Tributary** - lighter form of vassalage, pay tribute for protection

### 4.5 Peace Treaties

Currently, the `relationAfterSurrender` config is a simple setting. Expand into a negotiation system:
- Territory exchanges
- Reparation payments
- Demilitarized zones
- Forced alliance/non-aggression periods

---

## 5. Economy System

### 5.1 Economy Architecture

**Current State:** The plugin supports its own economy (TanEconomyStandalone), Vault integration (TanEconomyVault), and an external economy mode (TanEconomyExternal).

**Improvements:**
- Add support for multiple currencies (e.g., gold, silver, special resources)
- Implement inflation/deflation mechanics based on server money supply
- Add economic statistics and graphs (money supply, GDP per territory)

### 5.2 Trade System

**Current State:** Players can pay each other within a distance limit (`maxPayDistance: 15`).

**New Features:**
- **Marketplace** - global or regional auction house
- **Trade Routes** - connect towns for automatic resource exchange
- **Trade Caravans** - physical entities moving between towns (can be raided)
- **Stock Market** - territories can issue shares, players invest
- **Banking System** - loans with interest, deposits with returns

### 5.3 Tax System Improvements

- Progressive taxation (higher earners pay more)
- Tax exemptions for new players
- Tax holidays for events
- Automatic tax adjustment based on territory size/population
- Tax revolt mechanics (if taxes are too high, players get debuffs)

### 5.4 Resource Economy

The landmark system provides some resource generation. Expand this:
- Territory-specific resources based on biome
- Resource processing chains (raw material -> refined goods)
- Resource scarcity and surplus effects on pricing
- Import/export statistics per territory

---

## 6. GUI & User Experience

### 6.1 GUI Framework

**Current State:** Uses `triumph-gui:3.1.11` for GUI management. The GUI system has many menu classes (~80+ files).

**Improvements:**
- Create a unified GUI builder/template system to reduce code duplication
- Add pagination support for large lists (some menus like member lists will grow)
- Add search/filter functionality in browsing menus
- Implement GUI animations for transitions
- Add tooltips with more detailed information

### 6.2 Chat System

**Current State:** Has chat scoping (town, region, alliance chat) and chat listener events for input.

**Improvements:**
- Add clickable chat messages for common actions (accept/deny proposals)
- Implement chat formatting with territory colors
- Add notification preferences (per-player notification settings)
- Rich hover text on territory/player mentions
- Add `/tan broadcast` for territory-wide announcements

### 6.3 Map System

**Current State:** Has `ChatChunkMapRenderer` for in-chat maps and supports Dynmap/Squaremap/Bluemap via external plugin.

**Improvements:**
- Improve the in-chat map with better ASCII rendering
- Add `/tan map` with customizable radius
- Show territory borders, landmarks, forts on the map
- Add minimap support (via client-side mod integration)
- Real-time web map updates via WebSocket

### 6.4 Scoreboard Integration

**Current State:** The plugin can create scoreboards for player colors but warns about breaking vanilla scoreboards.

**Improvements:**
- Use packet-based scoreboards (via ProtocolLib) to avoid conflicts
- Add configurable sidebar information display
- Show territory info, balance, war status on scoreboard
- Add tab list formatting with territory prefixes

---

## 7. Testing & CI/CD

### 7.1 Test Coverage (HIGH PRIORITY)

**Current State:** There are ~30+ test files, which is a good start. Tests use MockBukkit v4.72.9 and JUnit 5.

**Areas Missing Tests:**
- War system (only `PlannedAttackTest`, `FortTest`, `ShowBoundariesTest`)
- Economy system (no tests for `EconomyUtil`, `VaultManager`)
- Storage system (only `PlayerDataStorageTest`)
- Event system (no tests for `EventManager`)
- Territory operations (only `TownDataTest`, `RegionDataTest`)
- GUI service/requirements (only `RessourceRequirementTest`)

**Recommendation:**
- Target 70%+ code coverage
- Add integration tests for critical flows (town creation, war declaration, tax collection)
- Add property-based tests for edge cases
- Create test fixtures for common setups

### 7.2 CI/CD Pipeline

**Current State:** `.github/` directory exists but we didn't inspect its contents.

**Recommendation:**
- Automated build and test on every PR
- Code coverage reporting (JaCoCo)
- Static analysis (SpotBugs, PMD, or SonarQube)
- Automated release builds with version tagging
- Deployment to a test Minecraft server for integration testing

### 7.3 Code Quality Tools

- Add Checkstyle configuration for consistent code style
- Add `.editorconfig` for IDE consistency
- Add `spotless` Gradle plugin for auto-formatting
- Set up CodeQL for security scanning

---

## 8. Performance Optimizations

### 8.1 Chunk Lookup Performance

**File:** `TownsAndNations-Common/src/main/java/org/leralix/tan/storage/stored/NewClaimedChunkStorage.java`

Chunk lookups happen very frequently (every time a player enters a chunk). Ensure the data structure is optimized.

**Recommendation:**
- Use a spatial index (R-tree or chunk-coordinate-based HashMap) for O(1) lookups
- Cache recently accessed chunks with an LRU cache
- Pre-compute territory boundaries and invalidate cache on claim/unclaim

### 8.2 Event Processing

The `EventManager` and newsletter system process many events. For large servers:
- Batch newsletter writes to the database
- Use event queues with async processing
- Debounce rapid-fire events (e.g., chunk enter/exit)

### 8.3 GUI Performance

- Cache GUI item stacks instead of rebuilding on every open
- Lazy-load paginated content
- Use item stack pooling for common items

### 8.4 Task Scheduling

**Current Tasks:**
- `DailyTasks` - runs at configured tax time
- `SaveStats` - periodic save
- `SecondTask` - runs every second

**Recommendation:**
- Review `SecondTask` - running something every second is expensive; consider if it can run less frequently
- Use Bukkit's scheduled tasks with appropriate intervals
- Profile the daily task to ensure it doesn't cause lag at midnight

### 8.5 Memory Usage

- Implement lazy loading for territory data (don't load all towns into memory at startup)
- Use weak references for player data caching
- Add memory usage monitoring and alerts
- Consider data compression for in-memory storage of large datasets

---

## 9. New Feature Ideas

### 9.1 Building System

**Current State:** There's a `Building.java` file but the building system appears minimal.

**Recommendation:** Expand into a full building/structure system:
- **Town Hall** - central building, required for town creation (already exists as concept)
- **Barracks** - increases military capacity
- **Market** - enables trade with other towns
- **Walls** - provides defensive bonuses during attacks
- **Farm** - passive food/resource generation
- **Library** - unlocks research/upgrade branches
- **Bank** - enables loans and deposits
- **Embassy** - improves diplomatic relations
- Buildings should be physical structures that can be damaged during war

### 9.2 Quest/Mission System

- Territory-specific quests (gather resources, explore, defend)
- Daily/weekly challenges for territory members
- Quest rewards (money, resources, special items)
- Territory reputation gained through quest completion

### 9.3 Culture & Religion System

- Cultural influence spreading to nearby chunks
- Cultural buildings and bonuses
- Religious temples providing unique buffs
- Cultural victory conditions (spread culture to X% of the map)

### 9.4 Technology/Research Tree

Expand the existing upgrade system:
- Multi-path research trees with mutual exclusions
- Era/age progression (Stone Age -> Iron Age -> etc.)
- Shared research within kingdoms
- Research speed bonuses from buildings and population

### 9.5 NPC System

- Hire NPC guards for territory defense
- NPC merchants in town markets
- NPC tax collectors for visual flavor
- NPC workers in buildings

### 9.6 Seasons & Weather System

- Seasonal effects on resource production
- Weather affecting combat (rain reduces fire damage, etc.)
- Seasonal events (harvest festivals, winter challenges)
- Climate zones affecting territory bonuses

### 9.7 Transportation Network

- Build roads between towns for speed boost
- Horse relay stations
- Teleportation network (beyond current spawn system)
- Portal system between allied territories

### 9.8 Achievement System

- Personal achievements (first town, first war, etc.)
- Territory achievements (reach population milestones, win wars)
- Achievement rewards (titles, cosmetic items)
- Leaderboards

### 9.9 Territory Reputation & Influence

- Reputation system based on actions (helping allies, winning wars)
- Influence radius that grows with territory level
- Reputation affects trade prices and diplomatic options
- Negative reputation for betrayals, breaking treaties

### 9.10 Custom Events & Festivals

- Configurable server events (trading fairs, tournaments)
- Territory-hosted events with rewards
- Global events that affect all territories (plague, natural disasters)
- Seasonal festivals with special mechanics

---

## 10. Configuration & Localization

### 10.1 Configuration Improvements

**Current State:** Single `config.yml` with ~700 lines covering everything.

**Recommendation:**
- Split config into multiple files:
  - `economy.yml` - all economy settings
  - `war.yml` - war and combat settings
  - `territory.yml` - town/region/kingdom settings
  - `permissions.yml` - chunk and role permissions
  - `notifications.yml` - event broadcast settings
- Add config validation on startup with clear error messages
- Support config hot-reload without server restart
- Add config migration between versions

### 10.2 Localization

**Current State:** 22 languages supported via Crowdin, which is excellent. Uses a `Lang` enum and `DynamicLang` system.

**Improvements:**
- Add right-to-left (RTL) support for Arabic/Hebrew (already has translations)
- Add pluralization rules per language
- Support custom language files (server-specific overrides)
- Add missing translations - audit all `Lang` entries for completeness
- Consider MiniMessage format support for modern text formatting

### 10.3 Config Documentation

- Generate config documentation automatically from annotations
- Add a web-based config editor
- Include examples and presets (PvP server, roleplay server, economy server)

---

## 11. Security & Validation

### 11.1 Input Validation

- Validate all player inputs (town names, descriptions) against injection attacks
- Sanitize chat messages in territory channels
- Add rate limiting for command execution
- Validate config values on load (ranges, types)

### 11.2 Permission System

**Current State:** Uses basic Bukkit permissions (`tan.base.*`, `tan.admin.*`).

**Improvements:**
- Add more granular permissions (per-command, per-action)
- Add permission groups for common setups
- LuckPerms integration for context-aware permissions
- Add bypass permissions for admins

### 11.3 Anti-Abuse Measures

- Cooldowns on town creation/deletion to prevent abuse
- Maximum number of towns per player
- Anti-claim spam (rapid claim/unclaim)
- War declaration cooldowns
- Economy exploit prevention (rapid buy/sell)

### 11.4 Audit Logging

- Log all administrative actions
- Log significant economic transactions
- Log territory ownership changes
- Make logs queryable through commands or web interface

---

## 12. Documentation

### 12.1 Code Documentation

- Add Javadoc to all public methods in the API module
- Document the internal architecture with diagrams
- Add inline comments for complex algorithms (especially in war/claim systems)
- Create a CONTRIBUTING.md guide

### 12.2 User Documentation

- Complete the GitBook documentation
- Add video tutorials for common operations
- Create a quick-start guide for server administrators
- Add troubleshooting guide

### 12.3 Developer Documentation

- API usage examples and tutorials
- Event handling guide
- Custom extension development guide
- Database schema documentation

---

## Priority Matrix

| Priority | Area | Impact | Effort |
|----------|------|--------|--------|
| P0 | TerritoryData refactoring | High | High |
| P0 | Test coverage improvement | High | Medium |
| P0 | Async save operations | High | Medium |
| P1 | Full database migration | High | High |
| P1 | Legacy war system cleanup | Medium | Medium |
| P1 | Config file splitting | Medium | Low |
| P1 | API expansion | Medium | Medium |
| P2 | Trade system | High | High |
| P2 | Building system expansion | High | High |
| P2 | Diplomacy expansion | Medium | Medium |
| P2 | Performance optimizations | Medium | Medium |
| P3 | Quest system | Medium | High |
| P3 | NPC system | Medium | High |
| P3 | REST API / Web interface | Low | High |
| P3 | Culture/Religion system | Low | High |

---

## Conclusion

Towns and Nations V2 is a well-structured plugin with a solid foundation covering towns, regions, kingdoms, wars, economy, and diplomacy. The codebase shows good practices like multi-language support (22 languages), an API module for third-party integration, and a modular upgrade system.

The most impactful improvements would be:
1. **Refactoring `TerritoryData`** to reduce complexity and improve maintainability
2. **Completing the database migration** for better scalability
3. **Expanding test coverage** to prevent regressions
4. **Async I/O operations** for better server performance
5. **Expanding the war and diplomacy systems** for richer gameplay

The new feature opportunities (trade routes, buildings, quests, NPC systems) could transform the plugin from a territory management tool into a comprehensive civilization-building experience.
