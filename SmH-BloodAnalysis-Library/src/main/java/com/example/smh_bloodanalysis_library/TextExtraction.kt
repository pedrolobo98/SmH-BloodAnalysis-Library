package com.example.smh_bloodanalysis_library

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.lang.Double
import java.util.HashMap

class TextExtraction(context: Context) {

    val context = context

    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    var mapAnalyses = HashMap<String, String>()

    fun realTimeProcess (bitmap: Bitmap, imageRotationDegrees: Int, callback: (HashMap<String, String>) -> Unit){
        var bmp = rotateImage(bitmap, imageRotationDegrees.toFloat())
        bmp?.let {
            recognizer.process(it, 0)
                .addOnSuccessListener { visionText ->
                    mapAnalyses = findValue(visionText)
                    callback(mapAnalyses)
                }
                .addOnFailureListener { e ->
                    print("f")
                }
        }
    }

    fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true)
    }

    fun findValue(visionText: Text): HashMap<String, String>{
        var resultText = removeAccents(visionText.text)?.toUpperCase().toString()
        var Analyses = HashMap<String, String>()
        Dictionary().bloodComponentsWord.forEach {
            var minCenter =  9999999
            var elementText = ""
            if (it in resultText){
                var elementFrame: Rect
                for (block in visionText.textBlocks) {
                    if (it in removeAccents(block.text)?.toUpperCase().toString()){
                        for (line in block.lines) {
                            if (it in removeAccents(line.text)?.toUpperCase().toString()){
                                elementFrame = line.boundingBox!!
                                for (block in visionText.textBlocks) {
                                    if (((elementFrame.top + elementFrame.bottom)/2 >= block.boundingBox!!.top!!) && ((elementFrame.top + elementFrame.bottom)/2 <= block.boundingBox!!.bottom!!)){
                                        for (line in block.lines){
                                            if (((elementFrame.top + elementFrame.bottom)/2 >= line.boundingBox!!.top!!) && ((elementFrame.top + elementFrame.bottom)/2 <= line.boundingBox!!.bottom!!)) {
                                                for (element in line.elements) {
                                                    elementText = element.text
                                                    var numeric = true
                                                    try {
                                                        Double.parseDouble(elementText)
                                                    } catch (e: NumberFormatException) {
                                                        numeric = false
                                                    }
                                                    if (numeric){
                                                        if (((elementFrame.top + elementFrame.bottom) / 2 >= element.boundingBox!!.top!!) && ((elementFrame.top + elementFrame.bottom) / 2 <= element.boundingBox!!.bottom!!)
                                                            && ((elementFrame.left + elementFrame.right) / 2 < minCenter) && (removeAccents(
                                                                element.text)?.toUpperCase().toString() != it)) {
                                                            minCenter =
                                                                (elementFrame.left + elementFrame.right) / 2
                                                            Analyses[it] = line.text
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return Analyses
    }

    fun processPdfBox(uri: Uri, callback: (HashMap<String, String>) -> Unit){
        PDFBoxResourceLoader.init(context)
        var pdDocument: PDDocument? = null
        try {
            pdDocument =
                PDDocument.load(uri?.let { context.getContentResolver().openInputStream(it) })

            if (!pdDocument.isEncrypted()){
                val pdfStripper = PDFTextStripper()
                pdfStripper.sortByPosition = true
                pdfStripper.startPage = 0
                pdfStripper.endPage = pdDocument.numberOfPages
                pdfStripper.setShouldSeparateByBeads(true)
                var pdfFileInText = pdfStripper.getText(pdDocument)
                pdfFileInText = removeAccents(pdfFileInText)?.toUpperCase()
                var linex = pdfFileInText.split("\n","\r")
                mapAnalyses = HashMap<String, String>()
                linex.forEach{
                    var line = it
                    Dictionary().bloodComponentsWord.forEach {
                        var compound = it
                        if((compound in line) && compound != "VOLUME GLOBULAR"){
                            var wordx = line.split(" ")
                            var numeric = true
                            var jaPassou = true
                            wordx.forEach {
                                if (jaPassou){
                                    try {
                                        Double.parseDouble(it)
                                        mapAnalyses[compound] = it
                                        jaPassou = false
                                    } catch (e: NumberFormatException) {
                                        numeric = false
                                    }
                                }
                            }
                        }else if ((compound in line) && !("VOLUME GLOBULAR MEDIO" in line)){
                            var wordx = line.split(" ")
                            var numeric = true
                            var jaPassou = true
                            wordx.forEach {
                                if (jaPassou){
                                    try {
                                        Double.parseDouble(it)
                                        mapAnalyses[compound] = it
                                        jaPassou = false
                                    } catch (e: NumberFormatException) {
                                        numeric = false
                                    }
                                }
                            }
                        }
                    }
                }
                if(linex.filter { x: String? -> x != "" }.size < 3){
                    for (i in 0..pdDocument.numberOfPages - 1){
                        val renderer = PDFRenderer(pdDocument)
                        var bitmap = renderer.renderImage(i, 2f, ImageType.ARGB)
                        recognizer.process(bitmap, 0)
                            .addOnSuccessListener { visionText ->
                                findValue(visionText)
                                callback(mapAnalyses)
                            }
                            .addOnFailureListener { e ->
                                print("f")
                            }
                    }
                }else{
                    callback(mapAnalyses)
                }
            }
        } catch (e: ArithmeticException) {
            println(e)
        } finally {
            if (pdDocument != null){
                pdDocument.close()
            }
        }
    }

    fun removeAccents(value: String?): String? {
        var MAP_NORM: Map<Char, Char>? = null
        if (MAP_NORM == null || MAP_NORM.size == 0) {
            MAP_NORM = HashMap()
            MAP_NORM.put('??', 'A')
            MAP_NORM.put('??', 'A')
            MAP_NORM.put('??', 'A')
            MAP_NORM.put('??', 'A')
            MAP_NORM.put('??', 'A')
            MAP_NORM.put('??', 'E')
            MAP_NORM.put('??', 'E')
            MAP_NORM.put('??', 'E')
            MAP_NORM.put('??', 'E')
            MAP_NORM.put('??', 'I')
            MAP_NORM.put('??', 'I')
            MAP_NORM.put('??', 'I')
            MAP_NORM.put('??', 'I')
            MAP_NORM.put('??', 'U')
            MAP_NORM.put('??', 'U')
            MAP_NORM.put('??', 'U')
            MAP_NORM.put('??', 'U')
            MAP_NORM.put('??', 'O')
            MAP_NORM.put('??', 'O')
            MAP_NORM.put('??', 'O')
            MAP_NORM.put('??', 'O')
            MAP_NORM.put('??', 'O')
            MAP_NORM.put('??', 'N')
            MAP_NORM.put('??', 'C')
            MAP_NORM.put('??', 'A')
            MAP_NORM.put('??', 'O')
            MAP_NORM.put('??', 'S')
            MAP_NORM.put('??', '3')
            MAP_NORM.put('??', '2')
            MAP_NORM.put('??', '1')
            MAP_NORM.put('??', 'a')
            MAP_NORM.put('??', 'a')
            MAP_NORM.put('??', 'a')
            MAP_NORM.put('??', 'a')
            MAP_NORM.put('??', 'a')
            MAP_NORM.put('??', 'e')
            MAP_NORM.put('??', 'e')
            MAP_NORM.put('??', 'e')
            MAP_NORM.put('??', 'e')
            MAP_NORM.put('??', 'i')
            MAP_NORM.put('??', 'i')
            MAP_NORM.put('??', 'i')
            MAP_NORM.put('??', 'i')
            MAP_NORM.put('??', 'u')
            MAP_NORM.put('??', 'u')
            MAP_NORM.put('??', 'u')
            MAP_NORM.put('??', 'u')
            MAP_NORM.put('??', 'o')
            MAP_NORM.put('??', 'o')
            MAP_NORM.put('??', 'o')
            MAP_NORM.put('??', 'o')
            MAP_NORM.put('??', 'o')
            MAP_NORM.put('??', 'n')
            MAP_NORM.put('??', 'c')
        }
        if (value == null) {
            return ""
        }
        val sb = StringBuilder(value)
        for (i in 0 until value.length) {
            val c = MAP_NORM.get(sb[i])
            if (c != null) {
                sb.setCharAt(i, c.toChar())
            }
        }
        return sb.toString()
    }
}