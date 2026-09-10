# Immersive Gateways

Ancient gateways connecting the world in an immersive way, preserving exploration and encouraging traditional travel.

## Structure NBT processor

A python script copies the exported structure NBTs and prepares them for generation (Adds air blocks, populates loot
tables etc.).

```bash
uv run python main.py
```

## Admin commands

Operators with permission level 2 can manage saved gateway links with `/gateway start|finish|detect` while looking at a
gateway block, or toggle portal generation with `/gateway automatic_generation on|off`.
Use `/gateway start` at the first gateway, `/gateway finish` at the second to link them, and `/gateway detect` to print
the selected gateway's target.
