package com.informatlux.test

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.objects.DetectedObject
import kotlinx.coroutines.tasks.await

object MLKitImageAnalyzer {
    suspend fun labelImage(bitmap: Bitmap): List<String> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        val result = labeler.process(image).await()
        return result.map { "${it.text} (${String.format("%.2f", it.confidence * 100)}%)" }
    }

    suspend fun detectObjects(bitmap: Bitmap): List<String> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        val detector = ObjectDetection.getClient(options)
        val objects = detector.process(image).await()
        return objects.flatMap { obj: DetectedObject ->
            obj.labels.map { "${it.text} (${String.format("%.2f", it.confidence * 100)}%)" }
        }.ifEmpty { listOf("No objects detected") }
    }
}
