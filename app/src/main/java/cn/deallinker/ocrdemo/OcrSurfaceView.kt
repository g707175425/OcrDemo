package cn.deallinker.ocrdemo

import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.SurfaceHolder
import com.googlecode.leptonica.android.Pix
import com.googlecode.tesseract.android.TessBaseAPI
import android.R.attr.data
import android.graphics.*
import android.os.Environment
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


/**
 * surfaceview
 * Created by gongyasen on 2017/7/6.
 */
class OcrSurfaceView
@JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        SurfaceView(context, attrs, defStyleAttr) {

    private var camera:Camera? = null

    init {
        OcrHelper.initTessBaseData(getContext())
        setOnClickListener {
//            camera?.autoFocus{ b, camera -> }
//            camera?.takePicture(object:Camera.ShutterCallback{
//                override fun onShutter() {
//
//                }
//
//            })
            camera?.setOneShotPreviewCallback { data, camera ->
                OcrHelper.resolve(data, camera)
            }
        }

        holder.addCallback(object : SurfaceHolder.Callback{
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

    private fun start() {
        camera?.parameters?.apply {
            //设置预览照片的大小
            setPreviewFpsRange(width, height)
            //设置相机预览照片帧数
            setPreviewFpsRange(4, 10)
            //设置图片格式
            pictureFormat = ImageFormat.JPEG
            //设置图片的质量
            jpegQuality = 90
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
    }



}


object OcrHelper{
    val mTess = TessBaseAPI()

    init {
    }

    fun initTessBaseData(context: Context) {
        val datapath = "${Environment.getExternalStorageDirectory().absolutePath}/tesseract/tessdata"
        println("datapath:$datapath")
        // String language = “num”;
        val dir = File(datapath)
        if (!dir.exists())
            dir.mkdirs()

        copyFilesFassets(context, "tess", datapath)

        mTess.init(
                "${Environment.getExternalStorageDirectory().absolutePath}/tesseract/",
                "eng"
        )
        mTess.setVariable("tessedit_char_whitelist", "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");

    }

    fun resolve(data: ByteArray,camera:Camera) {
        mTess.apply {
//            val decodeByteArray = BitmapFactory.decodeByteArray(data, 0, data.size)
//            println("decodeByteArray:$decodeByteArray")
            try {
                val size = camera.parameters.previewSize
                val image = YuvImage(data, ImageFormat.NV21, size.width, size.height, null)
                val stream = ByteArrayOutputStream()
                image.compressToJpeg(Rect(0, 0, size.width, size.height), 80, stream)

                val bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())

                //**********************
                //因为图片会放生旋转，因此要对图片进行旋转到和手机在一个方向上
//                rotateMyBitmap(bmp)
                //**********************************

                stream.close()

                setImage(bmp)
                setDebug(BuildConfig.DEBUG)
                println("结果:${utF8Text}}")
            } catch (ex: Exception) {
                Log.e("Sys", "Error:" + ex.message)
            }

        }
    }


    /**
     * 从assets目录中复制整个文件夹内容
     * @param  context  Context 使用CopyFiles类的Activity
     * *
     * @param  oldPath  String  原文件路径  如：/aa
     * *
     * @param  newPath  String  复制后路径  如：xx:/bb/cc
     */
    fun copyFilesFassets(context: Context, oldPath: String, newPath: String) {
        try {
            val fileNames = context.assets.list(oldPath)//获取assets目录下的所有文件及目录名
            if (fileNames.isNotEmpty()) {//如果是目录
                val file = File(newPath)
                file.mkdirs()//如果文件夹不存在，则递归
                for (fileName in fileNames) {
                    copyFilesFassets(context, oldPath + "/" + fileName, newPath + "/" + fileName)
                }
            } else {//如果是文件
                val inputStream = context.assets.open(oldPath)
                val fos = FileOutputStream(File(newPath))
                val buffer = ByteArray(1024)
                var byteCount = 0
                val read = fun():Int{
                    byteCount = inputStream.read(buffer)
                    return byteCount
                }

                while (read() != -1) {//循环从输入流读取 buffer字节
                    fos.write(buffer, 0, byteCount)//将读取的输入流写入到输出流
                }
                fos.flush()//刷新缓冲区
                inputStream.close()
                fos.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            //如果捕捉到错误则通知UI线程
        }

    }
}