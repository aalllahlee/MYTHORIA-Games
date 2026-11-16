package com.example.game.engine3d

import android.content.Context
import android.opengl.GLSurfaceView

class GameGLSurfaceView(
    context: Context,
    world: GamePhysicsWorld,
    onHudTick: (HudSnapshot) -> Unit
) : GLSurfaceView(context) {

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
        setRenderer(GameRenderer(world, onHudTick))
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}
