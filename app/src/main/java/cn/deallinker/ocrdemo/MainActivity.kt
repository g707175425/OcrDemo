package cn.deallinker.ocrdemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
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
        val DEFAULT_LANGUAGE = "eng"//"eng" "chi_sim"
        /**
         * assets中的文件名
         */
        val DEFAULT_LANGUAGE_NAME = DEFAULT_LANGUAGE + ".traineddata"
        /**
         * 保存到SD卡中的完整文件名
         */
        val LANGUAGE_PATH = tessdata + File.separator + DEFAULT_LANGUAGE_NAME
        /**
         * 权限请求值
         */
        val PERMISSION_REQUEST_CODE = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceView.setPreViewIV(preview)
        surfaceView.setMask(mask)

        // Example of a call to a native method
//        TessBaseAPI().setImage()

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                        PERMISSION_REQUEST_CODE)
            }else{
                copyToSD(this, LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME)
            }
        }else{
            copyToSD(this, LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME)
        }

    }


    /**
     * 请求到权限后在这里复制识别库
     * @param requestCode
     * *
     * @param permissions
     * *
     * @param grantResults
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE ->
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Android6.0之前安装时就能复制，6.0之后要先请求权限，所以6.0以上的这个方法无用。
                    copyToSD(this, LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME)
                }
            else -> {
            }
        }
    }

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
