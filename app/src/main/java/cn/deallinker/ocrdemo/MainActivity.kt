package cn.deallinker.ocrdemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import java.io.*


class MainActivity : AppCompatActivity() {

    companion object{
        /**
         * TessBaseAPI初始化用到的第一个参数，是个目录。
         */
        val DATAPATH = Environment.getExternalStorageDirectory().absolutePath + File.separator
        /**
         * 在DATAPATH中新建这个目录，TessBaseAPI初始化要求必须有这个目录。
         */
        val tessdata = DATAPATH + File.separator + "tessdata"
        /**
         * TessBaseAPI初始化测第二个参数，就是识别库的名字不要后缀名。
         */
        val DEFAULT_LANGUAGE = "mcr+ocr"//"eng" "chi_sim" "equ"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_main)
        surfaceView.setPreViewIV(preview)
        surfaceView.setMask(mask)

        // Example of a call to a native method
//        TessBaseAPI().setImage()

        doAsync {
            copyFilesFassets(this@MainActivity,"tess", tessdata)
            surfaceView.startTakePic()
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
                    val newFileName = newPath + "/" + fileName
                    if(!File(newFileName).exists()){
                        copyFilesFassets(context, oldPath + "/" + fileName, newFileName)
                    }
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


//    /**
//     * 请求到权限后在这里复制识别库
//     * @param requestCode
//     * *
//     * @param permissions
//     * *
//     * @param grantResults
//     */
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
//        when (requestCode) {
//            PERMISSION_REQUEST_CODE ->
//                if (grantResults.isNotEmpty()
//                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    //Android6.0之前安装时就能复制，6.0之后要先请求权限，所以6.0以上的这个方法无用。
//                    copyToSD(this, LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME)
//                }
//            else -> {
//            }
//        }
//    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
//    external fun stringFromJNI(): String

//    companion object {
//
//        // Used to load the 'native-lib' library on application startup.
//        init {
//            System.loadLibrary("native-lib")
//        }
//    }

    /**
     * 将assets中的识别库复制到SD卡中
     * @param path  要存放在SD卡中的 完整的文件名。这里是"/storage/emulated/0//tessdata/chi_sim.traineddata"
     * *
     * @param name  assets中的文件名 这里是 "chi_sim.traineddata"
     */
    fun copyToSD(context: Context, path: String, name: String) {

        //如果存在就删掉
        val f = File(path)
        if (f.exists()) {
            f.delete()
        }
        if (!f.exists()) {
            val p = File(f.parent)
            if (!p.exists()) {
                p.mkdirs()
            }
            println("path:${p.absolutePath}")
            try {
                f.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        var inputStream: InputStream? = null
        var os: OutputStream? = null
        try {
            inputStream = context.assets.open(name)
            val file = File(path)
            os = FileOutputStream(file)
            val bytes = ByteArray(2048)
            var len = 0
            while ({
                val read = inputStream!!.read(bytes)
                len = read
                read
            }() != -1) {
                os.write(bytes, 0, len)
            }
            os.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close()
                if (os != null)
                    os.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

    }

}
