# FTPClient Example
How To use an FTPClient {apache} with kotlin

This is a basic usage! will be improved in the future... 

if you can help me whit more specific example or feature, you are welcome!!

Is possible to test and improve this code by using a free online ftp server

this is a free ftp server where only read is allowed (you can't upload your files)

`server = "test.rebex.net"
ftp.login("demo","password")`

## HOW TO CREATE A FTP SERVER
-I'm using a raspberry py 3b with ubuntu 20 local fossa

install server ftp

`sudo apt update && sudo apt install vsftpd
`

check status

`sudo service vsftpd status
`

(not necessary) create an ftp user and password (follow steps)

`sudo adduser ftpusername
`

(NECESSARY) enable any kind of write from ftp :) (it tooks 2 days of work to find this setting! fool!)
edit file etc/vsftpd.conf and uncomment line #writeEnabled=TRUE

restart service or machine!

(NECESSARY) set the same time zone for server and client (unluckily ftp server give us back time as long with not zoneId infos...)

more FTP configuration needed-> https://devanswers.co/install-ftp-server-vsftpd-ubuntu-20-04/



### WARNING

-apache common lib provides 2 ways for check last file modification:

    
    #### FTPFile.timestamp
    Gets the file timestamp. This usually the last modification time.
    Returns:A Calendar instance
    representing the file timestamp.
    
    **this method don't return seconds!<---CARE**
    so if you are polling this check more than 1 time per minute.. this is not accurate!!
    `es.2021-03-13T21:43:00Z`
    
    #### ftpConnection.getModificationTime("/file or dir")
    Returns:A String that PROBABLY is ISO 3077 form YYYYMMDDhhmmss
    Issue the FTP MDTM command **_(not supported by all servers)_** to retrieve the last modification
    time of a file. The modification string should be in the **ISO 3077 form "YYYYMMDDhhmmss(.xxx)?**".
    The timestamp represented should also be in GMT, **but not all FTP servers honor this**.
    
    `es.20210313224334`
