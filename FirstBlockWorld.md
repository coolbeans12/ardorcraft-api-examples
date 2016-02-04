# Simplest setup #

(Based on the SimpleGame example in the source)

## Initialize the world ##

The world is created using a WorldSettings object. After it is created, we extract the Node holding everything to be rendered and add it to our main scenegraph root.
Finally, we kick the world into processing mode with startThreads.
```
blockWorld = new BlockWorld(settings);
root.attachChild(blockWorld.getWorldNode());
blockWorld.startThreads();
```

The WorldSettings object needs to be configured at least a little bit. First create the settings object.
```
final WorldSettings settings = new WorldSettings();
```
Set the location of the texture atlas (texturepack terrain image) that you want to use for all your blocks. Has to be a texture containing 16x16 sub-images currently.
```
settings.setTerrainTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "terrainQ.png"));
```
Then set the size of a texture tile, in this case 16x16 pixels.
```
settings.setTerrainTextureTileSize(16);
```
Set the location of the texture to use for water rendering. This texture is currently not used (tile 223 is used from the main atlas).
```
settings.setWaterTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "water.png"));
```
The world is divided into chunks. This one sets the size of each chunk in blocks. Due to lighting transport, this can be no less that 16 (which is usually the best value performance wise too).
```
settings.setTileSize(tileSize);
```
Set the height of the world (ie 128 etc)
```
settings.setTileHeight(height);
```
Set the view distance, which is how many chunks to generate and render.
```
settings.setGridSize(gridSize);
```

**Setting up the server connection**

This is the most complex part of the setup. Here, we tell the blockworld which class to use to request data for loading chunks etc. This is the interface we use to create a real client/server multiplayer connection later on. In this case, we just use a fake local server that works against a map file locally. It also takes a DataGenerator parameter, which is what is used to generate the terrain (a simple sinewave terrain in this case).
```
final IServerConnection serverConnection = new LocalServerConnection(new LocalServerDataHandler(tileSize, height, gridSize, simpleSineGenerator, null));
settings.setServerConnection(serverConnection);
```

## Update the world ##

In your update, you run.
```
blockWorld.updatePlayer(player.getPosition(), player.getDirection());
blockWorld.update(timer);
```
Updateplayer gives information about where the player is, and the update takes care of updating the world based on the player position.