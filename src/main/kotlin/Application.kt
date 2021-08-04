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
        val password=System.getenv("RASPBERRY_PASSWORD")
        if (ftp.login("ubuntu", password)) {
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
        ftp.listFiles("/home/ubuntu/docker-compose.yml").forEach {
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
        println(ftp.getModificationTime("/home/ubuntu/docker-compose.yml"))

        println(ftp.getLastModifiedAsInstant("/home/ubuntu/docker-compose.yml"))
        var totalchanges=0
        repeat(30){
            println("-----START ATTEMPT $it--------")
            synchronizeDir("C:\\Users\\fscar\\AppData\\Local\\Temp\\FTPClientExample\\data", "/home/ubuntu/", ftp).apply {
                println("file changed this attempt: $this")
                totalchanges+=this
            }
            println("-----END ATTEMPT $it--------")
            Thread.sleep(10000)
        }
        println("Total Changes of all attempts: $totalchanges")

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

fun synchronizeDir(localDir: String, remoteDir: String, ftp: FTPClient):Int {
    var filesUpdated=0
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
                //ftpFile.timestamp.toInstant() this give not seconds information
            )) {
                FtpFileSyncStrategy.UPLOAD_LOCALE -> {
                    println("remote ${ftpFile.name} need to be updated")
                    ftp.deleteFile(remoteFilePath)
                    println("result of upload="+ftp.appendFile(remoteFilePath, localFile!!.inputStream()))
                    filesUpdated++
                }
                FtpFileSyncStrategy.DOWNLOAD_REMOTE -> {
                    println("local ${ftpFile.name} need to be updated")
                    if (localFile != null && localFile.exists() && localFile.isFile)  localFile.delete()
                    val localOutFile = File(localDir + File.separator + ftpFile.name).apply { createNewFile() }
                    ftp.retrieveFile(remoteFilePath, localOutFile.outputStream())
                    filesUpdated++
                }
                FtpFileSyncStrategy.EQUALS -> println("remote ${ftpFile.name} has the same lastModified value of local file")
            }
        } else {
            println("remote ${ftpFile.name} is not a File! SKIPPED")
        }
    }
    return filesUpdated
}

fun FTPClient.getLastModifiedAsInstant(pathName: String): Instant =
    SimpleDateFormat("yyyyMMddHHmmss").parse(this.getModificationTime(pathName)).toInstant()

fun whatFileToSync(localFile: File?, remoteFileLastModified: Instant, millisecondErrorAdmitted: Long=5000): FtpFileSyncStrategy =
    localFile?.let {
        val localLastModified = it.lastModified()
        val remoteLastModified = remoteFileLastModified.toEpochMilli()
//        println("last modified> \nLocal:$localLastModified  \nRemote:$remoteLastModified")
//        println("last modified> \nLocal:${Instant.ofEpochMilli(localLastModified)}  \nRemote:${remoteFileLastModified}")
//        println("delta time> ${(localLastModified - remoteLastModified)} millis")
        when {
            (localLastModified - remoteLastModified) > millisecondErrorAdmitted -> FtpFileSyncStrategy.UPLOAD_LOCALE
            (localLastModified - remoteLastModified) < -millisecondErrorAdmitted -> FtpFileSyncStrategy.DOWNLOAD_REMOTE
            else -> FtpFileSyncStrategy.EQUALS
        }
    } ?: FtpFileSyncStrategy.DOWNLOAD_REMOTE
