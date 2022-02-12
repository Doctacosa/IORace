# IORace

![Logo](https://www.interordi.com/images/plugins/iorace-96.png)

Racing minigame for a Minecraft server, on foot! The players start from 0, 0 and need to get as far as they can heading East, trying to reach the highest X coordinate possible. Upon dying, a players is sent all the way back at the start to try again. The highest position reached by each player is logged, allowing you to give out prizes at the end of the event.

This event is designed to run for multiples days, the players progressing at their own pace. For example, it could last a week or a month without a reset.

The progress data is stored in a YAML configuration file.


## How to play

To understand how this is played from a player's perspective, [see this guide on our wiki](https://wiki.creeperslab.net/worlds/kenorland/race-away). Note that Merit Points are exclusive to the Creeper's Lab, you're free to implement your own rewards as none are built-in. The best score for each player is stored in `positions.yml` in the plugin's directory.


## Setup guide

1. Download the plugin and place it in the `plugins/` directory of the server.
2. Start and stop the server to create the configuration files.
3. Edit `plugins/IORace/config.yml` to set your settings, described below.
4. Set the spawn point of the world to 0, 0 (elevation doesn't matter).
5. To start a new game cycle, delete `positions.yml`, the player profiles and restart the server.


## Configuration

`announce-deaths`: If deaths should be announced in the chat  
`announce-interval`: How often progress updates should be broadcast in chat. Decrease the number on difficult maps  
`update-interval`: How often progress updates should happen in the scoreboard This is kept vague so that others can't know exactly where someone else and sabotage their run  
`use-iochatbridge`: If the [Interordi Chat Bridge](https://github.com/Doctacosa/IOChatServer) should be used to relay notification; false for standard in-game  

## Commands, permissions

None
