package `dev.mb.aws.cli`

import com.soywiz.klock.DateTime
import dev.mb.aws.*
import dev.mb.aws.rest.RestClient
import kotlinx.cinterop.toKString
import kotlinx.cli.ArgParser
import kotlinx.coroutines.runBlocking
import platform.posix.getenv

fun main(args: Array<String>) = runBlocking {
    val parser = ArgParser("kaws")

//    val input by parser.option(ArgType.String, shortName = "i", description = "Input file").required()
//    val output by parser.option(ArgType.String, shortName = "o", description = "Output file name")
//    val format by parser.option(ArgType.Choice(listOf("html", "csv", "pdf")), shortName = "f",
//    	description = "Format for output file").default("csv").multiple()
//    val debug by parser.option(ArgType.Boolean, shortName = "d", description = "Turn on debug mode").default(false)
//    val eps by parser.option(ArgType.Double, description = "Observational error").default(0.01)

//    parser.parse(args)

    run()
}

suspend fun run() {
    val credentials = credentialsFromEnv() ?: return
    val dateTimeStamp = dateTimeStamp()
    val regionName = "ap-northeast-1"
    val serviceName = "ec2"

    val request = HttpRequest(
        method = Method.POST,
        url = "http://$serviceName.$regionName.amazonaws.com/",
        headers = listOf(
            Header("Host", "$serviceName.$regionName.amazonaws.com"),
            Header("x-amz-date", "${dateTimeStamp.dateStamp}T${dateTimeStamp.timeStamp}Z")
        ),
        payload = "Action=DescribeInstances&Version=2016-11-15"
    )

    val httpRequest = authorizedRequest(request, credentials, dateTimeStamp, regionName, serviceName)

    val restClient = RestClient()
    val response = restClient.execute(httpRequest)
    println(response)
}

private

fun dateTimeStamp(): DateTimeStamp {
    val dateTime = DateTime.now().utc

    return DateTimeStamp(
        "${dateTime.yearInt}${dateTime.month1}${dateTime.dayOfMonth}",
        "${dateTime.hours}${dateTime.minutes}${dateTime.seconds}"
    )
}

fun credentialsFromEnv(): Credentials? {
    val accessKeyId = getenv("AWS_ACCESS_KEY_ID")?.toKString() ?: return null
    val secretAccessKey = getenv("AWS_SECRET_ACCESS_KEY")?.toKString() ?: return null
    return Credentials(accessKeyId, secretAccessKey)
}
