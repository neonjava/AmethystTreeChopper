# AmethystTreeChopper

A Folia-compatible Minecraft plugin implementing a custom Netherite Axe—the **Amethyst Tree Chopper**—featuring animated tree felling, leaves shredding, custom effects, and a countdown timer leading to self-destruction.

## Features

- **Amethyst Tree Chopper Axe**: A custom Netherite Axe with the following enchantments:
  - Silk Touch
  - Efficiency V
  - Unbreaking III
  - Mending
- **Custom Lore**: Displays dynamic countdown timer (e.g. `3d 0h 0m`) updating dynamically.
- **Self Destruct Mechanism**: The axe breaks/destroys itself once the time expires, whether it's held, in an inventory, or placed somewhere.
- **Tree Felling & Leaves Shredding Animation**: Fells entire trees log by log with sound effects (Amethyst block break) and amethyst particles.
- **Folia Compatibility**: Built entirely with region-scheduled tasks for full asynchronous tick thread-safety.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
