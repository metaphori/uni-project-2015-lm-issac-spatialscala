package it.unibo.spala.actors.support

import java.net.{URL, URLClassLoader}
import java.nio.file.{DirectoryStream, Files, Paths, Path}

import akka.actor.Actor

/**
 * Created by roby on 04/09/15.
 */

class ClassLoaderActor extends Actor {
  override def receive: Actor.Receive = {
    case (clazz: String, classBytes: Array[Byte]) => {
      println("Received class to load: " + clazz)
      ByteClassLoader.register(clazz, classBytes)
    }
    case x => println("Ops, I got " + x + " but I dont know it")
  }
}

class ByteArrayClassLoader(parent: ClassLoader) extends URLClassLoader(Array[URL](), parent) {
  import ByteClassLoader._
  override
  protected def findClass(name: String) : Class[_] = {
    Console.println(s"Looking for class ${name}")
    var result = findLoadedClass(name);
    if (result == null) {
      try {
        result = findSystemClass(name);
      } catch {
        case e => () /* ignore */
      }
    }
    if (result == null) {
      try {
        val classBytes = registeredClasses(name)
        result = defineClass(name, classBytes, 0, classBytes.length);
      } catch {
        case e: Exception => {
          throw new ClassNotFoundException(name);
        }
      }
    }
    result;
  }
}

object ByteClassLoader {
  var registeredClasses: Map[String, Array[Byte]] = Map()

  def register(name: String, classBytes: Array[Byte]) {
    registeredClasses += (name -> classBytes)
  }
}

object LoadClassBytes {
  def apply(clazz: Class[_]) : Map[String, Array[Byte]] = {
    val basePath : Path = Paths.get(clazz.getProtectionDomain.getCodeSource.getLocation.toURI)
    val relName = clazz.getName().replace('.', '/')
    val fullPath = basePath.resolve(relName)
    val fileName = fullPath.getFileName()
    val fileNameHead = (fileName.toString()).split("\\.")(0)
    val parentDir = fullPath.getParent()

    var res : Map[String, Array[Byte]] = Map()
    // find class file and class files of inner classes
    val ds = Files.newDirectoryStream(
      parentDir,
      new DirectoryStream.Filter[Path]{
        def accept(file: Path) : Boolean = file.getFileName().toString().matches(fileNameHead+"(\\$.+)?\\.class")
      })
    try {
      val iter = ds.iterator()
      while (iter.hasNext()) {
        val p = iter.next()
        res += (((basePath.relativize(p)).toString().split("\\.")(0).replace('/', '.')) -> Files.readAllBytes(p))
      }
    } finally {
      ds.close
    }
    res
  }
}