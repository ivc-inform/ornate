package com.novocode.ornate.js

import java.io.InputStream
import java.util.regex.Pattern

import better.files._
import org.slf4j.Logger
import org.webjars.WebJarAssetLocator

import scala.collection.JavaConverters._
import scala.io.Codec
import java.io.InputStreamReader

trait WebJarSupport {

    import WebJarSupport._

    def logger: Logger

    def loadAsset(webjar: String, exactPath: String): Option[String] = getFullPathExact(webjar, exactPath).map { path =>
        logger.debug(s"Loading WebJar asset: $webjar/$exactPath")

        val inputStream: InputStream = getClass.getClassLoader.getResourceAsStream(path)
        val bufferSize = 1024
        val buffer = new Array[Char](bufferSize)
        val out = new StringBuilder
        val in = new InputStreamReader(inputStream, "UTF-8")

        try {
            var rsz = in.read(buffer, 0, buffer.length)
            while (rsz >= 0) {
                out.append(buffer, 0, rsz)
                rsz = in.read(buffer, 0, buffer.length)
            }
            out.toString()
        }
        finally
            inputStream.close()
    }
}

object WebJarSupport {
    @volatile private lazy val fullPathIndex = {
        val loader = Option(Thread.currentThread.getContextClassLoader).getOrElse(getClass.getClassLoader)
        val fpi = WebJarAssetLocator.getFullPathIndex(Pattern.compile(".*"), loader)
        val offset = WebJarAssetLocator.WEBJARS_PATH_PREFIX.length + 1
        fpi.asScala.valuesIterator.collect { case s if s.startsWith(WebJarAssetLocator.WEBJARS_PATH_PREFIX) =>
            val sep1 = s.indexOf('/', offset + 1)
            val sep2 = s.indexOf('/', sep1 + 1)
            val noVer = s.substring(offset, sep1) + s.substring(sep2)
            (noVer, s)
        }.toMap
    }

    /* This is orders of magnitude faster than WebJarLocator.getFullPathExact: */
    def getFullPathExact(webjar: String, exactPath: String): Option[String] =
        fullPathIndex.get(webjar + "/" + exactPath)

    def listAssets(webjar: String, path: String): Vector[String] = {
        val prefix = if (path.startsWith("/")) webjar + path else webjar + "/" + path
        fullPathIndex.keysIterator.filter(_.startsWith(prefix)).map(_.substring(prefix.length)).toVector
    }
}
