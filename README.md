# ⚡ DevStats

<p align="center">
  <img src="docs/logo.png" alt="DevStats Widget Preview" width="470">
</p>

<p align="center">
  <strong>Display your GitHub activity directly on your Discord profile.</strong>
</p>

<p align="center">
A Discord Widget powered by the Discord Social SDK that automatically synchronizes GitHub statistics and displays them on your Discord profile.
</p>

# 📖 Overview

DevStats is an open-source project that creates a **dynamic Discord Widget** capable of displaying real GitHub statistics directly on a user's Discord profile.

Instead of static information, the widget is synchronized with GitHub and automatically displays development activity such as:

- 👤 GitHub Profile
- 💼 Bio
- 💻 Primary Language
- 🔥 Contribution Streak
- 📦 Latest Repository
- 📝 Latest Commit
- 📈 Daily Commits

The project is currently being developed as a **personal implementation**, but its architecture has been designed from the beginning to evolve into a public platform where any developer can connect their GitHub account and use their own widget.

# ✨ Preview

<p align="center">
  <img src="docs/widget-preview.gif" width="470">
</p>

## 🚀 Features

- Today's commits counter
- Contribution streak tracking
- Featured repository display
- Live GitHub activity sync
- Discord profile widget integration

## 🛠️ Tech Stack

- Node.js
- Discord Social SDK / Widgets v2
- GitHub REST API
- Express.js

## 📦 How it works

1. User connects GitHub account via OAuth2
2. Backend fetches GitHub activity
3. Data is processed (commits, streaks, stats)
4. Discord widget is updated via API

## 🔒 Privacy

DevStats only uses GitHub data required to generate statistics. No data is sold or permanently stored.

See: `privacy.html`

## 📜 Terms

See: `terms.html`

## 📄 License

MIT License

## 💡 Future ideas

- Steam stats integration
- LeetCode tracking
- XP/gamification system
- Multi-platform developer profile card