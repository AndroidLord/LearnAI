package com.example.learnai

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.learnai.ui.theme.LearnAITheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.common.internal.MultiFlavorDetectorCreator.DetectorCreator
import com.google.mlkit.vision.interfaces.Detector
import com.google.mlkit.vision.interfaces.Detector.DetectorType
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

val TAG = "Image Recognition"


class MainActivity : ComponentActivity() {

    companion object {
        val recognizer = TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LearnAITheme {
                App()
            }
        }
    }
}

@Composable
fun App() {

    val context = LocalContext.current

    var bitmap: ImageBitmap? by remember {
        mutableStateOf(null)
    }

    var textState by remember {
        mutableStateOf("")
    }

    var imageUri : Uri? by remember {
        mutableStateOf(null)
    }

    val lines = remember {
        mutableStateListOf<Line>()
    }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) {result ->
        if(result != null) {
            imageUri = result
        }
    }


    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .pointerInput(true) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()

                        val line = Line(
                            start = change.position - dragAmount,
                            end = change.position
                        )

                        lines.add(line)
                    }
                }
        ) {
            lines.forEach { line ->
                drawLine(
                    color = line.color,
                    start = line.start,
                    end = line.end,
                    strokeWidth = line.strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Convert canvas lines to bitmap
            bitmap = drawToBitmap(size, lines)

        }


        Image(
            painter = rememberAsyncImagePainter(
                model =
                if(imageUri != null) imageUri
                else if (bitmap != null) bitmap?.asAndroidBitmap()
                else R.drawable.ic_launcher_background,
            ),
            contentDescription = "Drawn Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )


        Button(
            modifier = Modifier
                .height(50.dp)
                .fillMaxWidth(),
            onClick = {
                // Recognise the character

                imageUri?.let {
                    val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                    recogniseCharacter(bitmap)
                }

               // bitmap?.asAndroidBitmap()?.let { recogniseCharacter(it) }



            }
        ) {
            Text("Recognise Character")
        }

        Button(
            modifier = Modifier
                .height(50.dp)
                .fillMaxWidth(),
            onClick = {
                imageLauncher.launch("image/*")

            }
        ) {
            Text("Open Gallery")
        }



        if (textState.isNotEmpty()) {
            Text(textState)
        }

    }


}

fun recogniseCharacter(bitmap: Bitmap) {


    val image = InputImage.fromBitmap(bitmap, 0)
    val result = MainActivity.recognizer
        .process(image)
        .addOnSuccessListener { visionText ->
            processResultText(visionText)

        }
        .addOnFailureListener { e ->

            Log.d(TAG, "Error: ${e.message}")
        }
}

data class Line(
    val start: Offset,
    val end: Offset,
    val color: Color = Color.Black,
    val strokeWidth: Dp = 12.dp
)

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun App_Prev() {
    LearnAITheme {
        App()
    }

}

private fun processResultText(visionText: Text) {


    if (visionText.textBlocks.size == 0) {
        Log.d(TAG, "No text found")
        return
    }
    for (block in visionText.textBlocks) {
        val blockText = block.text
        Log.d(TAG, "Block text: $blockText")
    }
}

fun drawToBitmap(size: Size, lines: SnapshotStateList<Line>): ImageBitmap {
    val drawScope = CanvasDrawScope()
    val bitmap = ImageBitmap(size.width.toInt(), size.height.toInt())
    val canvas = Canvas(bitmap)

    drawScope.draw(
        density = Density(1.5f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = size,
    ) {
        lines.forEach { line ->
            drawLine(
                color = line.color,
                start = line.start,
                end = line.end,
                strokeWidth = line.strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
    return bitmap
}