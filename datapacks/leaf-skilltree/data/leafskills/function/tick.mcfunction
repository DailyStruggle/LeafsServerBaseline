# Re-enable the selection trigger every tick so players can always issue /trigger skill_pick set <id>.
# The leaf-skills plugin consumes the written value, validates cost/prereqs, then resets it to 0.
scoreboard players enable @a skill_pick
