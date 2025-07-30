package com.informatlux.test

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class TFLiteImageClassifier(context: Context, modelPath: String) {
    private val classifier: ImageClassifier = ImageClassifier.createFromFile(context, modelPath)

    fun classify(bitmap: Bitmap): List<Pair<String, Float>> {
        val tensorImage = TensorImage.fromBitmap(bitmap)
        val results = classifier.classify(tensorImage)
        return results.firstOrNull()?.categories
            ?.sortedByDescending { it.score }
            ?.map { it.label to it.score }
            ?: listOf()
    }
}
