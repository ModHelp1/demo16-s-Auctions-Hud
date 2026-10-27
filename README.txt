demo16's Auction's Hud - v1.6.0 source update  (Yarn-mapped fix)
=============================================
Drop-in update for your GitHub repo (Minecraft 1.21.11, Fabric).

1. Copy everything inside  src/  over the  src/  folder of your repo
   (same paths: src/main/java/com/demo16/auctionhud/*.java and src/main/resources/...).
   New files: Gfx.java, HudStyle.java, StyleScreen.java, WinnerPopup.java
   Changed:   AuctionHud.java, AuctionState.java, Fireworks.java, SetupScreen.java
   Unchanged: AuctionHudClient.java, MoveScreen.java, ItemPickerScreen.java
2. Keep your own build.gradle / gradle.properties / workflow. Only bump the mod version to 1.6.0.
3. Push -> GitHub Actions builds the jar.

In game:  K -> setup screen -> "Choose Style" (next to the item dropdown).
          In the style screen use "Test win screen" to preview the winner screen in that style.
          Command  /auctionhud testwin  also works.
