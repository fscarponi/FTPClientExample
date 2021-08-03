import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPClientConfig
import org.apache.commons.net.ftp.FTPConnectionClosedException
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant


//https://stackoverflow.com/questions/7968703/is-there-a-public-ftp-server-to-test-upload-and-download
fun main() {
    println("Test Ftp Server Connection")

    val ftp = FTPClient()
//    val server = "speedtest.tele2.net" //http://speedtest.tele2.net/
//    val server = "test.rebex.net" //http://speedtest.tele2.net/
    val server = "192.168.1.19" //http://speedtest.tele2.net/

    //config does not work well!need learn how does it works!
//    val config = FTPClientConfig()
//    config.serverTimeZoneId = "Europe/Rome" // change required options
//    // for example config.setServerTimeZoneId("Pacific/Pitcairn")
//    ftp.configure(config);

    runCatching {
        ftp.connect(server)

//        if(ftp.login("demo","password")){
//            println("login successful")
//        }
        if(ftp.login("ftpuser","ftppassword")){
            println("login successful")
        }
        println("Connected to $server.")
        print(ftp.replyString)
        if (!FTPReply.isPositiveCompletion(ftp.replyCode)) {
            println("FTP server refused connection.")
            throw FTPConnectionClosedException("FTP server refused connection.")
        }

    }.onSuccess {
        //file and dir lists
//        println("${ftp.listFiles("/pub/example").count()} files trovati")
        println("${ftp.listFiles("/").count()} files trovati")
//        println(ftp.listFiles("/pub/example").map { it.name }.toList())
        println(ftp.listFiles("/").map { it.name }.toList())
//        println(ftp.listDirectories("/pub/").map { it.name }.toList())
        println(ftp.listDirectories("/").map { it.name }.toList())

        //get last modification time
        //Gets the file timestamp. This usually the last modification time.
        //Returns:
        //A Calendar instance representing the file timestamp.
        //this method don't know seconds!<---CARE
        ftp.listFiles("/docker-compose.yml").forEach {
            println("file> ${it.name} ultima modifica>${it.timestamp.toInstant()}")
        }




        println("test 2....")
        ftp.mlistDir("/").forEach {
            println("file> ${it.name} ultima modifica>${it.timestamp.toInstant()}")
        }
        println("....end test 2....")
        //other method
        // Issue the FTP MDTM command (not supported by all servers) to retrieve the last modification
        // time of a file. The modification string should be in the ISO 3077 form "YYYYMMDDhhmmss(.xxx)?".
        // The timestamp represented should also be in GMT, but not all FTP servers honor this.
//        println("/pub/example/mime-explorer.png  modification time")
        println("/docker-compose.yml  modification time")
        println(ftp.getModificationTime("/docker-compose.yml"))

        println(ftp.getLastModifiedAsInstant("/docker-compose.yml"))

        synchronizeDir( "C:\\Users\\fscar\\AppData\\Local\\Temp\\FTPClientExample\\data","/",ftp)



        //upload
//        ftp.appendFile("/pub/example/mime-explorer.png", File.createTempFile("mime-explorer","png").inputStream())
        //download
//        val file=File.createTempFile("mime-explorer",".png")
//        ftp.retrieveFile("/pub/example/mime-explorer.png", file.outputStream())
//        println("download result: file size-> ${file.totalSpace}")

        //other APi https://commons.apache.org/proper/commons-net/apidocs/org/apache/commons/net/ftp/FTPClient.html
        ftp.logout()
        ftp.disconnect()
    }.onFailure {
        println("login fail ${it.localizedMessage}")
        ftp.logout()
        ftp.disconnect()
    }
}



enum class FtpFileSyncStrategy {
    UPLOAD_LOCALE, DOWNLOAD_REMOTE, EQUALS
}

fun synchronizeDir(localDir: String, remoteDir: String,ftp: FTPClient) {

//    val remoteLogPath by instance("REMOTE_LOG_PATH")
//    val localLogPath by instance("LOCAL_LOG_PATH")
    val localDirFiles = File(localDir).listFiles()
    ftp.listFiles(remoteDir).forEach { ftpFile ->
        if (ftpFile.isFile) {
            val localFile = localDirFiles?.firstOrNull { it.name == ftpFile.name }
            val remoteFilePath = "$remoteDir/${ftpFile.name}"
            when (whatFileToSync(
                localFile,
                ftp.getLastModifiedAsInstant(pathName = remoteFilePath)
            )) {
                FtpFileSyncStrategy.UPLOAD_LOCALE -> {
                    println("remote ${ftpFile.name} need to be updated")
                    ftp.appendFile(remoteFilePath, localFile!!.inputStream())
                }
                FtpFileSyncStrategy.DOWNLOAD_REMOTE -> {
                    println("local ${ftpFile.name} need to be updated")
                    val localOutFile =
                        localFile ?: File(localDir + File.separator + ftpFile.name).apply { createNewFile() }
                    ftp.retrieveFile(remoteFilePath, localOutFile.outputStream())
                }
                FtpFileSyncStrategy.EQUALS -> println("remote ${ftpFile.name} has the same lastModified value of local file")
            }
        }else {
            println("remote ${ftpFile.name} is not a File! SKIPPED")
        }
    }

}

fun FTPClient.getLastModifiedAsInstant(pathName: String): Instant =
    SimpleDateFormat("yyyyMMddHHmmss").parse(this.getModificationTime(pathName)).toInstant()

fun whatFileToSync(localFile: File?, remoteFileLastModified: Instant): FtpFileSyncStrategy =
    localFile?.let {
        val localLastModified = it.lastModified()
        val remoteLastModified = remoteFileLastModified.toEpochMilli()
        when {
            (localLastModified - remoteLastModified) > 0 -> FtpFileSyncStrategy.UPLOAD_LOCALE
            (localLastModified - remoteLastModified) < 0 -> FtpFileSyncStrategy.DOWNLOAD_REMOTE
            else -> FtpFileSyncStrategy.EQUALS
        }
    } ?: FtpFileSyncStrategy.DOWNLOAD_REMOTE
