import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPClientConfig
import org.apache.commons.net.ftp.FTPConnectionClosedException
import org.apache.commons.net.ftp.FTPReply
import java.io.File


//https://stackoverflow.com/questions/7968703/is-there-a-public-ftp-server-to-test-upload-and-download
fun main() {
    println("Test Ftp Server Connection")

    val ftp = FTPClient()
//    val server = "speedtest.tele2.net" //http://speedtest.tele2.net/
    val server = "test.rebex.net" //http://speedtest.tele2.net/

    //config does not work well!need learn how does it works!
//    val config = FTPClientConfig()
//    config.serverTimeZoneId = "Europe/Rome" // change required options
//    // for example config.setServerTimeZoneId("Pacific/Pitcairn")
//    ftp.configure(config);

    runCatching {
        ftp.connect(server)
        if(ftp.login("demo","password")){
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
        println("${ftp.listFiles("/pub/example").count()} files trovati")
        println(ftp.listFiles("/pub/example").map { it.name }.toList())
        println(ftp.listDirectories("/pub/").map { it.name }.toList())

        //upload
//        ftp.appendFile("/pub/example/mime-explorer.png", File.createTempFile("mime-explorer","png").inputStream())
        //download
        ftp.retrieveFile("/pub/example/mime-explorer.png", File.createTempFile("mime-explorer",".png").outputStream())
        ftp.logout()
        ftp.disconnect()
    }.onFailure {
        println("login fail ${it.localizedMessage}")
        ftp.logout()
        ftp.disconnect()
    }
}
