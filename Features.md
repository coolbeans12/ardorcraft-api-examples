# Features #

(**Remember** - Since Ardorcraft is built on top of [Ardor3D](http://ardor3d.com/), all features of that powerful scenegraph engine can be used together with Ardorcraft effortlessly)

  * **Asynchronous loading/editing**
    * Supports loading and editing chunks fully asynchronously, to support loading all data from server (either locally or remotely).

  * **Fully configurable**
    * Use any texture atlas (supports minecraft texture packs by default)
    * Configurable mesh producers. Want to have a block that looks like a skull? Just plug in your own producer or use the MeshProducer that can handle any Ardor3D Mesh, loaded through collada for example.
    * Configurable texture tile for each block id
    * Configure if the block is pickable or collidable
    * Configure if the block is transparent or solid, and how much light it lets through, or if it emits light
    * Plug in your own terrain generation code

  * 100% fixed function, no shaders or fancy requirements. Runs on pretty much any computer.
  * Infinite world
  * Works seamlessly with the Ardor3D scenegraph engine
  * Fully configurable colors for sunlight, moonlight, cavelight and torchlight
  * Smooth fast day/night cycle updates without needing any chunk updates
  * Local lights (torches, lamps)
  * Pathfinding
  * Player physics/collision
  * Fast and compressed map format
  * 2D terrain generator viewer source (for fast terrain algo development)
  * Minecraft map to ardorcraft map converter (and texturepack compatibility)
  * Examples from easy to advanced
  * Sample terrain generators and block producers
  * Lots lots more