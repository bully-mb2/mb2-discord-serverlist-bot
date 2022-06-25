# MB2 Discord Serverlist Bot
This simple discord bot reports the [Movie Battles](https://community.moviebattles.org/) server list to your discord channels using it's public [API] (https://servers.moviebattles.org/api).

![image](https://user-images.githubusercontent.com/86576295/175781484-7e814dfb-f747-41e6-aa60-b8b3d009762c.png)


# Usage
Host the bot yourself and create an invite link with the scopes: bot, applications.commands and permissions: send messages.

Or use this [invite link](https://discord.com/api/oauth2/authorize?client_id=989968344394387546&permissions=2048&scope=bot%20applications.commands) for the one hosted by me (if it's still online).

```
Commands:
  /watch -- Configure the current channel to track the server list in
    Options:
      Optional: Region (EU, NA, SA, OC)
      Optional: Mode (Open, Duel, Legends, Authentic)
      Optional: Minimum Player Count (0-32)
      
 /unwatch -- Remove server list from channel
```

# Running
```
java -jar mb2-discord-serverlist-bot-VERSION.jar
```
After your first run a settings file will be generated next to the jar. Fill your credentials there and run again.


## License
MB2 Discord Serverlist Bot is licensed under GPLv2 as free software. You are free to use, modify and redistribute MB2 Discord Serverlist Bot following the terms in LICENSE.txt
