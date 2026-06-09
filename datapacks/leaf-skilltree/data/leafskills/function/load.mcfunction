# leaf-skilltree: one-time setup of the scoreboard plumbing the leaf-skills plugin reads.
# skill_pick  - "trigger" objective: the player's single selection command writes the node id here.
# skill_points - "dummy" objective: how many points the player has spent (drives base+step*n cost).
scoreboard objectives add skill_pick trigger
scoreboard objectives add skill_points dummy
tellraw @a {"text":"[leaf-skills] Skill tree loaded. Open advancements and use /trigger skill_pick set <id>.","color":"gray"}
