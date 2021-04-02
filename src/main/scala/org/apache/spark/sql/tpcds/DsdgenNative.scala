package org.apache.spark.sql.tpcds

import java.io.{File, FileOutputStream, InputStream, OutputStream}

import org.xerial.snappy.OSInfo

/**
  * Class to copy binary files fromresources folder to temp folder , for nodes to use.
  */
case class DsdgenNative() {

  val (dir, cmd) = {
    val classLoader = Thread.currentThread().getContextClassLoader
    val tempDir = Utils.createTempDir()
    val srcDatagenDir = s"binaries/${OSInfo.getOSName}/${OSInfo.getArchName}"

    def copyNativeBinaryFromResource(resourceName: String, to: File): Unit = {
      var in: InputStream = null
      var out: OutputStream = null
      try {
        in = classLoader.getResourceAsStream(resourceName)
        if (in == null) {
          throw new RuntimeException(
            s"Unsupported platform: OS=${OSInfo.getOSName} Arch=${OSInfo.getArchName}")
        }

        out = new FileOutputStream(to)
        val buffer = new Array[Byte](8192)
        var bytesRead = 0
        val canRead = () => {
          bytesRead = in.read(buffer)
          bytesRead != -1
        }
        while (canRead()) {
          out.write(buffer, 0, bytesRead)
        }
      } finally {
        if (in != null) {
          in.close()
        }
        if (out != null) {
          out.close()
        }
      }
    }

    Seq("dsdgen", "tpcds.idx").foreach { resource =>
      copyNativeBinaryFromResource(s"$srcDatagenDir/$resource", new File(tempDir, resource))
    }

    // Set executable (x) flag to run the copyed binary
    val dstDatagenPath = new File(tempDir, "dsdgen")
    if (!dstDatagenPath.setExecutable(true)) {
      throw new RuntimeException(
        s"Can't set a executable flag on: ${dstDatagenPath.getAbsolutePath}")
    }

    (tempDir.getAbsolutePath, "dsdgen")
  }
}
