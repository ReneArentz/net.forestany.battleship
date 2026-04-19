# Battleship Android Project

A classic two-player Battleship game with multiple modes, custom rules, and modern Android design.  

*net.forestany.battleship*  
*must use JDK 21 for compilation*

---

## Description

Relive the classic pen-and-paper game of Battleship in this modern Android adaptation for two players. Each player sets up their fleet and takes turns guessing the opponent's ship locations using coordinate-based attacks. Sink the enemy fleet before they sink yours!

The game offers several play modes:
- Alternate mode: Players take turns one shot at a time.
- One-shot-per-ship mode: Each ship gets a single attack per round.
- 3-shots and 5-shots modes: Fire multiple shots in succession for faster gameplay.

You can also customize your battles with optional rules:
- Bonus shot on hit: Earn an extra shot each time you score a hit.
- Touching ships: Allow ships to touch by their edges - or keep the classic separation rule.

Choose from different fleet setups:
- Standard fleet: One 5-cell ship, two 4-cell ships, three 3-cell ships, and four 2-cell ships.
- Frigate fleet: Ten fast 2-cell ships for a quick, tactical battle.

You can connect to another user in two ways:
- Automatic discovery: Find available game rooms automatically using UDP multicast over local wifi.
- Manual connection: Enter the known IP address and port of another device in the local wifi to connect directly.

The app is built with Android (Java 21 and Kotlin) and uses the custom forestJ framework. All graphics are hand-drawn, and sound effects are sourced from freesound.org under the CC0 1.0 Universal license.

Enjoy a fresh take on a timeless strategy game - open source, offline, and completely free.

---

## Tech Stack

- Android: Tested Android 36, Minimum Android 27
- Language: Kotlin, Java 21
- Framework: [forestJ](https://github.com/ReneArentz/forestJ)  
- IDE: Android Studio Narwhal for Linux | 2025.1.1 Patch 1
- IDE: Android Studio Panda 2 for Linux | 2025.3.2

---

## License

This project is open source under the GNU GPL v3 license — feel free to host, modify, and improve it while maintaining attribution.  

Sound effects:
- Cannon Shot by qubodup -- https://freesound.org/s/187767/ -- License: Creative Commons 0
- Water Explosion by Sheyvan -- https://freesound.org/s/519008/ -- License: Creative Commons 0
- tadaa.wav by Maikkihapsis -- https://freesound.org/s/626950/ -- License: Creative Commons 0
- Explosion.mp3 by florianreichelt -- https://freesound.org/s/563010/ -- License: Creative Commons 0
