# Building a Custom World: How This Server's Biomes Came to Be

This is the story of how the custom biomes on this server were made - from the initial desire
for something that didn't exist yet, through figuring out what that something actually was,
to the moment it generated correctly in-game for the first time. It's written as a narrative
so you can follow the same thinking process yourself, not just copy the steps.

---

## It Started With a Feeling

The vanilla Minecraft world is fine. It's familiar. But "familiar" is exactly the problem when
you're trying to build a server that feels like its own place. The biomes players already know
carry too much baggage - they know what to expect, they know what the trees look like, they
know the music. There's no discovery.

So the starting point wasn't a technical question. It was: *what kind of world do I actually
want players to experience?*

That question is worth sitting with before touching anything. The answer shapes every decision
that follows.

There's a balance to hold here, though. Replacing too much of the vanilla world creates its
own problem: players lose their footing. Minecraft's biomes are a shared language - players
know what a forest means, what a desert means, what to expect from a plains village. Strip
all of that away and the world stops feeling like Minecraft and starts feeling like a mod
pack nobody asked for. The goal isn't to erase the familiar; it's to extend it. Custom biomes
should feel like discoveries within a world that still makes sense, not replacements that
leave players wondering where everything went.

---

## Figuring Out What I Wanted

The first instinct is to jump straight to "what plugin does this" or "how do I write the JSON."
That's the wrong order. The tool should serve the vision, not define it.

So instead, the first real work was just... thinking. What does this world feel like? What
should a player see when they first spawn? What's the moment I want them to have?

For this server, the answer that kept coming back was: *places that feel like they have a
history.* Not just terrain - terrain with a mood. A volcanic region that looks like something
happened there. A forest so dense it feels ancient. Biomes that make you want to build
*in response* to them, not just on top of them.

Once that feeling was named, it became possible to ask more specific questions:

- What climate does this place have? Hot, cold, wet, dry?
- What does it look like from a distance - what's the silhouette?
- What blocks are on the ground, and why would a player care about them?
- If someone built a structure here, what would it look like?

These aren't technical questions yet. They're design questions. Writing down the answers -
even roughly - is what turns a vague feeling into something buildable.

---

## Discovering How Iris Works

With a rough vision in hand, the next question became: *can the tools actually do this?*

Iris is a Paper plugin that replaces Minecraft's world generator for a specific world. Instead
of vanilla terrain, it reads a folder of JSON files - a "dimension pack" - that describe
biomes, terrain shapes, surface blocks, decorations, and effects. It generates the world from
those files every time a new chunk loads.

The thing that made Iris the right choice here was one specific detail: vanilla clients can
join without any mods. Iris maps every custom biome onto a vanilla biome ID internally, so
the client never sees anything foreign. Players don't need to install anything. The custom
world is entirely server-side.

The other thing worth understanding early: biomes in Iris are organized into climate folders.
Where you put a biome JSON determines where in the world it appears - a file in `frozen/`
generates in cold regions, a file in `tropical/` generates in hot ones. The folder is not
just organization; it's part of the climate system.

That's really all you need to understand about Iris before starting. The rest reveals itself
as you go.

*The full technical picture is in [`design/CUSTOM-BIOMES.md`](../design/CUSTOM-BIOMES.md)
if you want it up front.*

---

## From Feeling to Specification

Here's where the design questions from earlier become concrete.

Take the volcanic region idea. "Hot, dramatic, rare" is a feeling. To build it, that feeling
needs to become answers:

- **Climate:** Hot and high-altitude. That points to `tropical/` or `mountain/` as the folder.
- **Silhouette:** Tall spires rising from a dark plain. Something you can see from far away
  and immediately recognize.
- **Surface blocks:** Basalt, blackstone, terracotta in red and orange tones. Visually distinct
  from anything vanilla, and basalt is actually useful to players.
- **Build theme:** A geothermal outpost. Somewhere that looks like it was built by people who
  had to survive here.

Writing this down before opening any file is the most important step in the whole process.
It's the difference between a biome that feels intentional and one that feels like a
settings experiment.

This is also the point where it's worth checking whether something close already exists.
The Iris overworld pack ships with hundreds of biomes. [`design/IRIS-BIOME-RESEARCH.md`](../design/IRIS-BIOME-RESEARCH.md)
maps those against the design goals for this server. If something already covers the concept
well enough, using it as a base saves a lot of iteration. If nothing does, that gap is worth
noting so future work doesn't re-evaluate the same ground.

---

## Making the Key Decisions

With the concept clear and the existing landscape understood, two decisions lock in the
biome's place in the world:

**Which folder does it live in?** This is the climate decision. For the volcanic biome, `tropical/`
was the right call - hot, high-altitude, dramatic. A cold volcanic peak would go in `mountain/`
or `frozen/` instead. The folder controls which part of the world the biome generates in and
which vanilla biomes it borders.

**What vanilla biome does it map to?** Iris needs a `derivative` - a vanilla biome ID that the
client receives. This choice affects the sky color, ambient sounds, and fog the player
experiences. For the volcanic biome, `minecraft:crimson_forest` gave the right eerie red
atmosphere on the client side. But for server-side logic - mob spawning, weather - `minecraft:badlands`
made more sense. Iris lets you set these separately, which is exactly the right tool for
that situation.

These two decisions are small but they have large downstream effects. Getting them right
early means less rework later.

*Vanilla biome IDs are listed in [`design/BIOMES.md`](../design/BIOMES.md).*

---

## Building It

This is where the JSON file actually gets written. The approach that worked best was
incremental: start with the minimum that Iris will load, verify it works, then add one
thing at a time.

The minimum is just a name, a color, a rarity, and the derivative fields. That's it. Iris
will generate the biome with flat default terrain and no features. It's not impressive, but
it proves the file is valid and the biome is in the world.

From there, each addition is a small experiment:

- Add a `generators` entry to give the terrain its shape. The volcanic biome got a mountain
  generator with a wide height range - that's what produces the spires.
- Add `layers` to define the surface blocks, from the top down. Basalt on top, blackstone
  and tuff underneath.
- Add `objects` to place schematics - the custom magma spire structures that give the biome
  its silhouette.
- Add `effects` for ambient particles. A slow campfire smoke effect makes the whole region
  feel like it's breathing.
- Add a `children` entry for a lava-lake child biome that generates as small patches inside
  the main biome.

Each of these is a separate pass. Load the world, fly around, see what it looks like, adjust.
The rarity value gets tuned the same way - start at 5, see how often it appears, move it up
or down until it feels right. The volcanic biome ended up at 20, which makes it rare enough
to feel like a discovery.

*The JSON schema with all available fields is in [`design/CUSTOM-BIOMES.md`](../design/CUSTOM-BIOMES.md).*

---

## Knowing When It's Done

A biome is done when it does what it was designed to do - not when every possible field is
filled in.

For this server, the bar is: does it work for vanilla clients, does it have a distinct look,
does it have a recognizable shape from a distance, does it give players a reason to be there,
and can you describe it in one sentence to someone who's never seen it?

The volcanic biome - Cinderfall - passes all of those. "A rare volcanic highland with basalt
spires, ambient smoke, and lava lake inclusions, themed around a geothermal outpost" is a
sentence that tells you exactly what to expect and why you'd want to go there.

When a biome can be described that clearly, it's ready.

---

## The Result

The world has biomes now that don't exist anywhere else. Players find Cinderfall and it
feels like a discovery because it is one - it wasn't in any vanilla pack, it was designed
for this specific place.

That's the whole process: a feeling, turned into questions, turned into decisions, turned
into a JSON file, turned into terrain. Each step is just making the previous step more
concrete until there's something in the world you can walk around in.

---

## See Also

- [`design/CUSTOM-BIOMES.md`](../design/CUSTOM-BIOMES.md) - full schema reference and biome catalog
- [`design/IRIS-BIOME-RESEARCH.md`](../design/IRIS-BIOME-RESEARCH.md) - preexisting biome coverage map
- [`design/BIOMES.md`](../design/BIOMES.md) - vanilla biome ID list
- [`design/adr/ADR-003-custom-biome-design-goals.md`](../design/adr/ADR-003-custom-biome-design-goals.md) - the five design goals
- [`design/GLOSSARY.md`](../design/GLOSSARY.md) - canonical term definitions
