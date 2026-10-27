# demo16's Auction's Hud (Fabric, Minecraft 1.21.1)

Yellow auction HUD overlay. Client-side only.

## Use
- Press K to open setup (item, worth, min bid, quantity, timer, HUD position).
- Start Auction -> HUD appears. Bids are read from chat lines like "Steve paid you $1,500".
- Each new highest bid resets the timer. When it hits 0 the item is SOLD and session money goes up.
- Test without a server: /auctionhud bid Steve 500   (also: start, stop, clear)

## Build (pick ONE, easiest first)

**A) Windows - double-click**
1. Install JDK 21 (https://adoptium.net) if you don't have it.
2. Double-click `BUILD-WINDOWS.bat`. It downloads Gradle by itself and builds.
3. Your mod appears in the `MOD-JAR` folder.

**B) Mac / Linux**
1. Install JDK 21, then run: `./build-mac-linux.sh`
2. Your mod appears in `MOD-JAR/`.

**C) Install nothing - let GitHub build it**
1. Make a free GitHub repo and upload everything in this folder.
2. Open the repo's Actions tab, run "Build mod" (it also runs on every push).
3. Download the finished jar from the run's "Artifacts" section.

Put the jar in `.minecraft/mods` with Fabric Loader + Fabric API.

## Why it's safe to read
The whole mod is 4 small Java files. It uses NO networking (no java.net, no HTTP),
NO file reading/writing, NO process execution, and never touches your session/account token.
Search the source for "net.", "File", "Runtime", "Process", "Session" to verify.
