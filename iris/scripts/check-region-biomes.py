import os, re

pack = r'C:\GameServers\Minecraft\testServer\RTP-Paper\1.21.11\plugins\Iris\packs\overworld'

biomes = set()
for root, dirs, files in os.walk(os.path.join(pack, 'biomes')):
    for f in files:
        if f.endswith('.json'):
            rel = os.path.join(root, f)[len(os.path.join(pack, 'biomes')) + 1:]
            rel = rel.replace('\\', '/').replace('.json', '')
            biomes.add(rel)

missing = []
for rf in os.listdir(os.path.join(pack, 'regions')):
    if not rf.endswith('.json'):
        continue
    with open(os.path.join(pack, 'regions', rf), 'r', encoding='utf-8') as fh:
        raw = fh.read()
    m = re.search(r'"landBiomes"\s*:\s*\[(.*?)\]', raw, re.DOTALL)
    if m:
        entries = re.findall(r'"([^"]+)"', m.group(1))
        for e in entries:
            if e not in biomes:
                missing.append(f'{rf}: {e}')

for x in sorted(missing):
    print(x)

if not missing:
    print('All biome references are valid.')
