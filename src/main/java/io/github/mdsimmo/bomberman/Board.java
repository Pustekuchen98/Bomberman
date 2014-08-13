package io.github.mdsimmo.bomberman;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class Board {

	private static Plugin plugin = Bomberman.instance;
	public String name;
	public final int xSize;
	public final int ySize;
	public final int zSize;
	public BlockRep[][][] blocks;
	HashMap<Vector, BlockRep> delayed = new HashMap<>();
	public ArrayList<Vector> spawnPoints = new ArrayList<>();
	
	public Board(int xSize, int ySize, int zSize) {
		this.xSize = xSize;
		this.ySize = ySize;
		this.zSize = zSize;
		blocks = new BlockRep[xSize][ySize][zSize];
	}
	
	public void saveBoard() throws IOException {
		YamlConfiguration save = new YamlConfiguration();
		save.set("name", name);
		save.set("size.x", xSize);
		save.set("size.y", ySize);
		save.set("size.z", zSize);
		
		// save standard blocks
		CompressedSection section = new CompressedSection();
		for (int i = 0; i < xSize; i++) {
			for (int j = 0; j < ySize; j++) {
				for (int k = 0; k < zSize; k++) {
					BlockRep rep = blocks[i][j][k];
					section.addParts(rep.toString());
				}
			}
		}
		save.set("blocks.standard", section.getValue());
		
		// save special blocks
		section.reset();
		for (Map.Entry<Vector, BlockRep> entry : delayed.entrySet()) {
			BlockRep rep = entry.getValue();
			Vector v = entry.getKey();
			CompressedSection special = new CompressedSection(':');
			special.addParts(v.getBlockX(), v.getBlockY(), v.getBlockZ());
			special.addParts(rep.toString());
			section.addParts(special.getValue());
		}
		save.set("blocks.special", section.getValue());
		
		save.save(new File(plugin.getDataFolder(), name.toLowerCase() + ".arena"));
	}
	
	/**
	 * adds the block to the arena
	 * @param block the block to add
	 * @param place the position to add it at
	 */
	public void addBlock(BlockRep block, Vector place) {
		if (block.material.isSolid() || block.material == Material.AIR) {
			int x = place.getBlockX();
			int y = place.getBlockY();
			int z = place.getBlockZ();
			blocks[x][y][z] = block;
		} else {
			blocks[place.getBlockX()][place.getBlockY()][place.getBlockZ()] = new BlockRep();
			delayed.put(place, block);
		}
		if (block.material == Material.WOOL) {
			spawnPoints.add(place.add(new Vector(0.5, 1, 0.5)));
		}
	}
	
	public static Board loadBoard(String name) throws IOException {
		File f = new File(plugin.getDataFolder(), name.toLowerCase() + ".arena");
		YamlConfiguration save = YamlConfiguration.loadConfiguration(f);
		int x = save.getInt("size.x");
		int y = save.getInt("size.y");
		int z = save.getInt("size.z");
		Board board = new Board(x, y, z);
		board.name = save.getString("name");
		
		// Read out normal blocks
		CompressedSection blocks = new CompressedSection();
		blocks.setValue(save.getString("blocks.standard"));
		List<String> sections = blocks.readParts();
		// decode read string
		int count = 0;
		for (int i = 0; i < board.xSize; i++) {
			for (int j = 0; j < board.ySize; j++) {
				for (int k = 0; k < board.zSize; k++) {
					BlockRep block = BlockRep.loadFrom(sections.get(count++));
					board.addBlock(block, new Vector(i, j, k));
				}
			}
		}
		
		// read out the delayed blocks
		blocks.setValue(save.getString("blocks.special"));
		sections = blocks.readParts();
		CompressedSection special = new CompressedSection(':');
		for (String s : sections) {
			special.setValue(s);
			List<String> parts = special.readParts();
			int x2 = Integer.parseInt(parts.get(0), 10);
			int y2 = Integer.parseInt(parts.get(1), 10);
			int z2 = Integer.parseInt(parts.get(2), 10);
			Vector v = new Vector(x2, y2, z2);
			BlockRep b = BlockRep.loadFrom(parts.get(3));
			board.addBlock(b, v);
		}
		return board;
	}
	
	public static class CompressedSection {

		private String value = "";
		private final char seperator;

		public CompressedSection(char seperator) {
			this.seperator = seperator;
		}
		
		public CompressedSection() {
			this(';');
		}
		
		public void addParts(Object... parts) {
			for (Object part : parts)
				value += part.toString() + seperator;
		}

		/**
		 * reads the next part of the file.
		 * 
		 * @return the read part. null if the end is reached
		 */
		public List<String> readParts() {
			List<String> parts = new ArrayList<>();
			String part = "";
			for (char c : value.toCharArray()) {
				if (c == seperator) {
					parts.add(part);
					part = "";
				} else {
					part += c;
				}
			}
			return parts;
		}

		public String getValue() {
			return value;
		}
		
		public void reset() {
			value = "";
		}
		
		public void setValue(String value) {
			this.value = value;
		}
	}
}
