package it.unibo.spala.actors.support

import java.net.{URL, URLClassLoader}
import java.nio.file.{DirectoryStream, Files, Paths, Path}

import akka.actor.Actor

/**
 * Created by: Roberto Casadei
 * Created on date: 04/09/15.
 */

class ClassLoaderActor extends Actor {
  override def receive: Actor.Receive = {
    case m: Map[String, Array[Byte]] => {
      m.foreach { case (clazz, classBytes) =>
        println("Received class to load: " + clazz)
        CustomClassLoaderRegistry.register(clazz, classBytes)
      }
    }
    case x => println("Ops, I got " + x + " but I dont know it")
  }
}

class CustomClassLoader(parent: ClassLoader) extends URLClassLoader(Array[URL](), parent) {
  import CustomClassLoaderRegistry._
  override
  protected def findClass(name: String) : Class[_] = {
    Console.println(s"Looking for class ${name}")
    var result = findLoadedClass(name);
    var toreg = classesToRegister.contains(name)
    if (result == null && !toreg) {
      try {
        result = findSystemClass(name);
      } catch {
        case e => () /* ignore */
      }
    }
    if (result == null && toreg) {
      try {
        val classBytes = classesToRegister(name)
        Console.println(s"Defining class ${name}")
        result = defineClass(name, classBytes, 0, classBytes.length);
        save(name)
        Console.println(s"Class ${name} defined!")
      } catch {
        case e: Exception => {
          throw new ClassNotFoundException(name);
        }
      }
    }
    result;
  }
}

object CustomClassLoaderRegistry {
  var classesToRegister: Map[String, Array[Byte]] = Map()
  var registeredClasses: Map[String, Array[Byte]] = Map()

  def register(name: String, classBytes: Array[Byte]) {
    classesToRegister += (name -> classBytes)
  }

  def save(name: String): Unit ={
    registeredClasses += (name -> classesToRegister(name))
    classesToRegister -= name
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
        def accept(file: Path) : Boolean = {
          // Examples of a filename head: SomeClass, SomeClass$, SomeClass$$anonFun$3
          // Thus, base filename may include $ chars
          val fnameRegex = fileNameHead.replace("$","\\$")
          file.getFileName().toString().matches(fnameRegex+".*\\.class")
        }
      })
    try {
      val iter = ds.iterator()
      while (iter.hasNext()) {
        val p = iter.next()
        val classQualifiedName = ((basePath.relativize(p)).toString().split("\\.")(0).replace('/', '.'))
        res += (classQualifiedName -> Files.readAllBytes(p))
      }
    } finally {
      ds.close
    }
    res
  }
}