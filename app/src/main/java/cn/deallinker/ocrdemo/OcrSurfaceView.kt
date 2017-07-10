package cn.deallinker.ocrdemo

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import cn.deallinker.ocrdemo.MainActivity.Companion.DATAPATH
import cn.deallinker.ocrdemo.MainActivity.Companion.DEFAULT_LANGUAGE
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.ByteArrayOutputStream
import android.graphics.ColorMatrixColorFilter
import android.graphics.ColorMatrix
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.R.attr.bitmap
import android.content.ContentValues.TAG













/**
 * surfaceview
 * Created by gongyasen on 2017/7/6.
 */
class OcrSurfaceView
@JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        SurfaceView(context, attrs, defStyleAttr) {

    private var camera: Camera? = null
    private var preview: ImageView? = null
    private var mask: MaskView? = null
    private val centerRect = RectF()

    init {
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                destroy()
            }

            override fun surfaceCreated(holder: SurfaceHolder?) {
                start()
            }
        })
    }

    fun startTakePic(){
        OcrHelper.initTessBaseData(context)
        setOnClickListener {
            camera?.autoFocus{ b, camera ->
                if(b){
                    camera?.setOneShotPreviewCallback { data, camera ->
                        OcrHelper.resolve(centerRect, mask, this@OcrSurfaceView, context,
                                preview, data, camera)
                    }
                }
            }
        }
    }

    private fun start() {
        camera?.parameters?.apply {
            //设置预览照片的大小
            setPreviewFpsRange(width, height)
            //设置相机预览照片帧数
            setPreviewFpsRange(4, 10)
            //设置图片格式
            pictureFormat = ImageFormat.JPEG
            //设置图片的质量
            jpegQuality = 80
            //设置照片的大小
            setPictureSize(width, height)
            //通过SurfaceView显示预览
            camera?.setPreviewDisplay(holder)
            //开始预览
            camera?.startPreview()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        create()
    }

    private fun create() {
        camera = Camera.open().apply {
            setDisplayOrientation(90)
        }

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        destroy()
    }

    private fun destroy() {
        if (camera != null) {
            camera?.stopPreview()
            camera?.release()
            camera = null
        }
        OcrHelper.destroyTessBase()
    }


    /**
     * 设置预览图
     */
    fun setPreViewIV(preview: ImageView?) {
        this.preview = preview
    }


    /**
     * 设置遮罩
     */
    fun setMask(mask: MaskView?) {
        this.mask = mask
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        centerRect.left = width*0.2f
        centerRect.right = width - width*0.2f
        centerRect.top = height*0.4f
        centerRect.bottom = height - height*0.4f
        mask?.centerMask(centerRect)
    }

}


object OcrHelper {
    var mTess: TessBaseAPI? = null

    fun initTessBaseData(context: Context) {
        mTess = TessBaseAPI().apply {
            init(DATAPATH, DEFAULT_LANGUAGE)
//            setVariable("tessedit_char_whitelist", "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
//            setVariable("classify_bln_numeric_mode", "1")

        }
    }

    fun destroyTessBase(){
        mTess?.apply {
            end()
        }
    }

    fun resolve(centerRect: RectF,mask: MaskView?, surfaceView: SurfaceView, context: Context, preview: ImageView?,
                data: ByteArray, camera: Camera) {
        mTess?.apply {
            try {
                val size = camera.parameters.previewSize
                val image = YuvImage(data, ImageFormat.NV21, size.width, size.height, null)
                val stream = ByteArrayOutputStream()

                image.compressToJpeg(Rect( centerRect.top.toInt(),centerRect.left.toInt(),
                        centerRect.bottom.toInt(), centerRect.right.toInt()), 100, stream)

                var bmp = BitmapFactory
                        .decodeByteArray(stream.toByteArray(), 0, stream.size(),
                                BitmapFactory.Options().apply {
                                    inSampleSize = 4
                                    inScaled = true
                                })

                //旋转图片
                val matrix = Matrix()
                matrix.postRotate(90f)
                val cacheBm = bmp
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
//                bmp = changeGrey(bmp)//convertToBlackWhite(bmp)
//                bmp = changeGrey(bmp)
//                bmp = lineGrey(bmp)
                bmp = binarization(bmp)
//                bmp = gray2Binary(bmp)

                cacheBm?.recycle()
                //**********************
                //因为图片会放生旋转，因此要对图片进行旋转到和手机在一个方向上
//                rotateMyBitmap(bmp)
                //**********************************
                preview?.setImageBitmap(bmp)

                println("图片大小:width:${bmp.width};height:${bmp.height}")

                stream.close()

                setImage(bmp)
                setDebug(BuildConfig.DEBUG)
                println("结果:$utF8Text")

                var confidenceIndex = -1 to -1

                wordConfidences().forEachIndexed { index, i ->
                    if(confidenceIndex.second < i && i > 50){//信任度大于80
                        confidenceIndex = index to i
                    }
                }

                if(confidenceIndex.first != -1){
                    setImage(words.getPix(confidenceIndex.first))
                }

                AlertDialog.Builder(context).apply {
                    setTitle("结果")
                    setView(TextView(context).apply {
                        if(confidenceIndex.first != -1)text = utF8Text
                        gravity = Gravity.CENTER
                    })
                    setPositiveButton("确定"){
                        dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    show()
                }

                mask?.invalidate(mTess!!)

                println("信任度:${wordConfidences().map { "$it ," }}")
//                end()
            } catch (ex: Exception) {
                Log.e("Sys", "Error:" + ex.message)
            }

        }
    }

    /**
     *
     */
    fun binarization(img: Bitmap) : Bitmap {
        val width = img.width
        val height = img.height
        val area = width * height
        val gray = Array(width) { IntArray(height) }
        var average = 0// 灰度平均值
        var graysum = 0
        var graymean = 0
        var grayfrontmean = 0
        var graybackmean = 0
        var pixelGray: Int
        var front = 0
        var back = 0
        val pix = IntArray(width * height)
        img.getPixels(pix, 0, width, 0, 0, width, height)
        for (i in 1..width - 1) { // 不算边界行和列，为避免越界
            for (j in 1..height - 1) {
                val x = j * width + i
                val r = pix[x] shr 16 and 0xff
                val g = pix[x] shr 8 and 0xff
                val b = pix[x] and 0xff
                pixelGray = (0.3 * r + 0.59 * g + 0.11 * b).toInt()// 计算每个坐标点的灰度
                gray[i][j] = (pixelGray shl 16) + (pixelGray shl 8) + pixelGray
                graysum += pixelGray
            }
        }
        graymean = (graysum / area).toInt()// 整个图的灰度平均值
        average = graymean
        Log.i(TAG, "Average:" + average)
        for (i in 0..width - 1)
        // 计算整个图的二值化阈值
        {
            for (j in 0..height - 1) {
                if (gray[i][j] and 0x0000ff < graymean) {
                    graybackmean += gray[i][j] and 0x0000ff
                    back++
                } else {
                    grayfrontmean += gray[i][j] and 0x0000ff
                    front++
                }
            }
        }
        val frontvalue = (grayfrontmean / front).toInt()// 前景中心
        val backvalue = (graybackmean / back).toInt()// 背景中心
        val G = FloatArray(frontvalue - backvalue + 1)// 方差数组
        for ((s, i1) in (backvalue..frontvalue + 1 - 1).withIndex())
        // 以前景中心和背景中心为区间采用大津法算法（OTSU算法）
        {
            back = 0
            front = 0
            grayfrontmean = 0
            graybackmean = 0
            for (i in 0..width - 1) {
                for (j in 0..height - 1) {
                    if (gray[i][j] and 0x0000ff < i1 + 1) {
                        graybackmean += gray[i][j] and 0x0000ff
                        back++
                    } else {
                        grayfrontmean += gray[i][j] and 0x0000ff
                        front++
                    }
                }
            }
            grayfrontmean = (grayfrontmean / front).toInt()
            graybackmean = (graybackmean / back).toInt()
            G[s] = back.toFloat() / area * (graybackmean - average).toFloat() * (graybackmean - average).toFloat() + front.toFloat() / area * (grayfrontmean - average).toFloat() * (grayfrontmean - average).toFloat()
        }
        var max = G[0]
        var index = 0
        for (i in 1..frontvalue - backvalue + 1 - 1) {
            if (max < G[i]) {
                max = G[i]
                index = i
            }
        }

        for (i in 0..width - 1) {
            for (j in 0..height - 1) {
                val `in` = j * width + i
                if (gray[i][j] and 0x0000ff < index + backvalue) {
                    pix[`in`] = Color.rgb(0, 0, 0)
                } else {
                    pix[`in`] = Color.rgb(255, 255, 255)
                }
            }
        }

        val temp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        temp.setPixels(pix, 0, width, 0, 0, width, height)
        return temp
    }

    /**
     * 二值化
     */
    // 该函数实现对图像进行二值化处理
    fun gray2Binary(graymap: Bitmap): Bitmap {
        //得到图形的宽度和长度
        val width = graymap.width
        val height = graymap.height
        //创建二值化图像
        var binarymap: Bitmap? = null
        binarymap = graymap.copy(Bitmap.Config.ARGB_8888, true)
        //依次循环，对图像的像素进行处理
        for (i in 0..width - 1) {
            for (j in 0..height - 1) {
                //得到当前像素的值
                val col = binarymap!!.getPixel(i, j)
                //得到alpha通道的值
                val alpha = col and 0xFF000000.toInt()
                //得到图像的像素RGB的值
                val red = col and 0x00FF0000 shr 16
                val green = col and 0x0000FF00 shr 8
                val blue = col and 0x000000FF
                // 用公式X = 0.3×R+0.59×G+0.11×B计算出X代替原来的RGB
                var gray = (red.toFloat() * 0.3 + green.toFloat() * 0.59 + blue.toFloat() * 0.11).toInt()
                //对图像进行二值化处理
                if (gray <= 95) {
                    gray = 0
                } else {
                    gray = 255
                }
                // 新的ARGB
                val newColor = alpha or (gray shl 16) or (gray shl 8) or gray
                //设置新图像的当前像素值
                binarymap.setPixel(i, j, newColor)
            }
        }
        return binarymap
    }

    /**
     * 线性灰度
     */
    fun lineGrey(image: Bitmap): Bitmap {
        //得到图像的宽度和长度
        val width = image.width
        val height = image.height
        //创建线性拉升灰度图像
        var linegray: Bitmap? = null
        linegray = image.copy(Bitmap.Config.ARGB_8888, true)
        //依次循环对图像的像素进行处理
        for (i in 0..width - 1) {
            for (j in 0..height - 1) {
                //得到每点的像素值
                val col = image.getPixel(i, j)
                val alpha = col and 0xFF000000.toInt()
                var red = col and 0x00FF0000 shr 16
                var green = col and 0x0000FF00 shr 8
                var blue = col and 0x000000FF
                // 增加了图像的亮度
                red = (1.1 * red + 30).toInt()
                green = (1.1 * green + 30).toInt()
                blue = (1.1 * blue + 30).toInt()
                //对图像像素越界进行处理
                if (red >= 255) {
                    red = 255
                }

                if (green >= 255) {
                    green = 255
                }

                if (blue >= 255) {
                    blue = 255
                }
                // 新的ARGB
                val newColor = alpha or (red shl 16) or (green shl 8) or blue
                //设置新图像的RGB值
                linegray!!.setPixel(i, j, newColor)
            }
        }
        return linegray
    }

    /**
     * 灰度转换
     */
    fun changeGrey(bitmap: Bitmap): Bitmap? {
        val width = bitmap.width
        val height = bitmap.height
        var grayImg: Bitmap? = null
        try {
            grayImg = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(grayImg)
            val paint = Paint()
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            val colorMatrixFilter = ColorMatrixColorFilter(
                    colorMatrix)
            paint.colorFilter = colorMatrixFilter
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            bitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return grayImg
    }

    /**
     * 将彩色图转换为黑白图

     * @param 位图
     * *
     * @return 返回转换好的位图
     */
    fun convertToBlackWhite(bmp: Bitmap): Bitmap {
        val width = bmp.width // 获取位图的宽
        val height = bmp.height // 获取位图的高
        val pixels = IntArray(width * height) // 通过位图的大小创建像素点数组

        bmp.getPixels(pixels, 0, width, 0, 0, width, height)
        val alpha = 0xFF shl 24
        for (i in 0..height - 1) {
            for (j in 0..width - 1) {
                var grey = pixels[width * i + j]

                val red = grey and 0x00FF0000 shr 16
                val green = grey and 0x0000FF00 shr 8
                val blue = grey and 0x000000FF

                grey = (red * 0.3 + green * 0.59 + blue * 0.11).toInt()
                grey = alpha or (grey shl 16) or (grey shl 8) or grey
                pixels[width * i + j] = grey
            }
        }
        val newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        newBmp.setPixels(pixels, 0, width, 0, 0, width, height)

        val resizeBmp = ThumbnailUtils.extractThumbnail(newBmp, 380, 460)
        return resizeBmp
    }

}

/**
 * 遮罩视图
 */
class MaskView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    private var tessBaseAPI: TessBaseAPI? = null
    private val boxPaint = Paint().apply {
        color = Color.RED
    }
    private val maskPaint = Paint().apply {
        color = (0xbb000000).toInt()
        xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
    }

    private val drawRect = RectF()
    private var imgHeight: Float = 0f
    private var imgWidth: Float = 0f
    private var centerRect: RectF? = null

    override fun onDraw(canvas: Canvas?) {
        println("高:$height;宽$width")
        if(centerRect != null){
            canvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)
            canvas?.drawRect(centerRect, maskPaint)
        }

        tessBaseAPI?.words?.boxRects?.forEach {
            if(centerRect != null){
                drawRect.left = centerRect!!.left+(it.left.toFloat()/imgWidth)*centerRect!!.width()
                drawRect.top = centerRect!!.top+(it.top.toFloat()/imgHeight)*centerRect!!.height()
                drawRect.right = centerRect!!.left+(it.right.toFloat()/imgWidth)*centerRect!!.width()
                drawRect.bottom = centerRect!!.top+(it.bottom.toFloat()/imgHeight)*centerRect!!.height()
            }
            println("盒子:$drawRect")
            canvas?.drawRect(drawRect, boxPaint)
        }
    }

    fun invalidate(tessBaseAPI: TessBaseAPI){
        this.tessBaseAPI = tessBaseAPI
        this.imgHeight = tessBaseAPI.thresholdedImage?.height?.toFloat()?:0f
        this.imgWidth = tessBaseAPI.thresholdedImage?.width?.toFloat()?:0f
        invalidate()
    }

    fun centerMask(centerRect:RectF){
        this.centerRect = centerRect
        invalidate()
    }
}