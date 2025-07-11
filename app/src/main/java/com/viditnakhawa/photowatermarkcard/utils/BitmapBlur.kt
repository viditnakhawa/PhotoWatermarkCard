import android.content.Context
import android.graphics.*
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import androidx.annotation.RequiresApi


@RequiresApi(Build.VERSION_CODES.S)
fun blurBitmap(context: Context, bitmap: Bitmap, radius: Float): Bitmap {
    return blurBitmapHardwareRenderer(bitmap, radius)
}

// ---- API 31+ (Android 12+) ----
@RequiresApi(Build.VERSION_CODES.S)
private fun blurBitmapHardwareRenderer(bitmap: Bitmap, radius: Float): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val imageReader = ImageReader.newInstance(
        width, height,
        PixelFormat.RGBA_8888, 1,
        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
    )

    val renderNode = RenderNode("BlurEffect")
    renderNode.setPosition(0, 0, width, height)

    val blurEffect = RenderEffect.createBlurEffect(
        radius, radius, Shader.TileMode.CLAMP
    )
    renderNode.setRenderEffect(blurEffect)

    // Draw the bitmap into the RenderNode
    val canvas = renderNode.beginRecording()
    canvas.drawBitmap(bitmap, 0f, 0f, null)
    renderNode.endRecording()

    val hardwareRenderer = HardwareRenderer()
    hardwareRenderer.setSurface(imageReader.surface)
    hardwareRenderer.setContentRoot(renderNode)
    hardwareRenderer.createRenderRequest()
        .setWaitForPresent(true)
        .syncAndDraw()

    val image = imageReader.acquireNextImage() ?: throw RuntimeException("No Image")
    val hardwareBuffer = image.hardwareBuffer ?: throw RuntimeException("No HardwareBuffer")
    val hardwareBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
        ?: throw RuntimeException("Create Bitmap Failed")

    //Convert the hardware-accelerated bitmap into a regular,
    val softwareBitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)

    // Cleanup
    hardwareBuffer.close()
    image.close()
    imageReader.close()
    renderNode.discardDisplayList()
    hardwareRenderer.destroy()

    return softwareBitmap
}

